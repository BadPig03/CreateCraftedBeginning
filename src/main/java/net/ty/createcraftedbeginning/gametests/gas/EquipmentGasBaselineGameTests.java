package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandlers;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandlers;
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlers;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.AirtightCannonVisualHandlers;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandlers;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge.AirtightCannonWindChargeProjectileEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchController;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberCanisterTransfer;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EquipmentGasBaselineGameTests {
    private EquipmentGasBaselineGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void equipmentKeepsEachGasNormalBaselineAndArmHighPressureFallback(GameTestHelper helper) {
        List<Gas> gases = List.of(CCBGases.NATURAL_AIR.get(), CCBGases.ENERGIZED_NATURAL_AIR.get(), CCBGases.ULTRAWARM_AIR.get(), CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), CCBGases.ETHEREAL_AIR.get(), CCBGases.ENERGIZED_ETHEREAL_AIR.get(), CCBGases.MOIST_AIR.get(), CCBGases.SPORE_AIR.get(), CCBGases.SCULK_AIR.get(), CCBGases.STEAM.get(), CCBGases.CREATIVE_AIR.get());
        for (Gas gas : gases) {
            helper.assertTrue(AirtightArmHandlers.resolveForEquipment(gas) == AirtightArmHandlers.resolve(gas, GameplayPressureProfiles.NORMAL), "Equipment arm baseline changed for " + gas);
            helper.assertTrue(AirtightArmHandlers.resolve(gas, GameplayPressureProfiles.HIGH_PRESSURE) == AirtightArmHandlers.resolveForEquipment(gas), "High-pressure arm lookup did not retain the normal gas baseline for " + gas);
            helper.assertTrue(AirtightArmorsHandlers.resolveForEquipment(gas) == AirtightArmorsHandlers.resolve(gas, GameplayPressureProfiles.NORMAL), "Equipment armor baseline changed for " + gas);
            helper.assertTrue(AirtightDrillHandlers.resolveForEquipment(gas) == AirtightDrillHandlers.resolve(gas, GameplayPressureProfiles.NORMAL), "Equipment drill baseline changed for " + gas);
            helper.assertTrue(AirtightCannonHandlers.resolveForEquipment(gas) == AirtightCannonHandlers.resolve(gas, GameplayPressureProfiles.NORMAL), "Equipment cannon baseline changed for " + gas);
            helper.assertTrue(AirtightCannonVisualHandlers.resolveForEquipment(gas) == AirtightCannonVisualHandlers.resolve(gas, GameplayPressureProfiles.NORMAL), "Equipment cannon visuals changed for " + gas);
        }
        helper.assertTrue(AirtightArmHandlers.resolveForEquipment(CCBGases.ENERGIZED_NATURAL_AIR.get()).getIncreasedBlockInteractionRange() == 4, "Normal baseline discarded the energized gas identity");
        helper.assertTrue(AirtightArmHandlers.resolveForEquipment(CCBGases.NATURAL_AIR.get()).getIncreasedBlockInteractionRange() == 2, "Natural Air arm baseline changed");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void highPressureCannonProjectileUsesNormalEtherealEffect(GameTestHelper helper) {
        Pig target = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
        target.setNoAi(true);
        AirtightCannonWindChargeProjectileEntity projectile = new AirtightCannonWindChargeProjectileEntity(helper.getLevel(), CCBGases.ETHEREAL_AIR.get().getHolder(), Vec3.ZERO);
        projectile.setPos(target.position().add(0, 0.5, 0));
        projectile.setSourcePressurePa(GasPressure.pascals(16));
        projectile.hurt(helper.getLevel().damageSources().generic(), 1);

        helper.assertTrue(target.hasEffect(MobEffects.LEVITATION), "Ethereal projectile did not apply levitation");
        MobEffectInstance levitation = target.getEffect(MobEffects.LEVITATION);
        if (levitation == null) {
            throw new NullPointerException("Expected levitation on the ethereal projectile target.");
        }

        helper.assertValueEqual(levitation.getAmplifier(), 0, "High-pressure projectile retained the high-pressure effect level");
        helper.assertValueEqual(levitation.getDuration(), 100, "High-pressure projectile retained the high-pressure duration");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void normalCanisterFillStopsAtCapacityWithoutChangingStoredGas(GameTestHelper helper) {
        GasCanisterContainer canister = newCanister();
        long capacity = canister.getTankMaxAmount(0);
        GasStack incoming = new GasStack(CCBGases.NATURAL_AIR.get(), capacity + 100);
        helper.assertValueEqual(canister.fill(0, incoming, GasAction.SIMULATE), capacity, "Canister fill preview exceeded capacity");
        helper.assertTrue(canister.isEmpty(), "Canister fill preview mutated storage");
        helper.assertValueEqual(canister.fill(0, incoming, GasAction.EXECUTE), capacity, "Canister fill exceeded capacity");
        helper.assertValueEqual(canister.fill(0, incoming, GasAction.EXECUTE), 0L, "Full canister accepted more gas");
        helper.assertValueEqual(canister.getGasInTank(0).getAmount(), capacity, "Full canister lost gas");
        helper.assertTrue(canister.getTankPressurePa(0) <= canister.getTankMaxPressurePa(0), "Canister exceeded its safe pressure limit");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void highPressureInjectionRetainsUnacceptedGasAtSource(GameTestHelper helper) {
        GasCanisterContainer canister = newCanister();
        long capacity = canister.getTankMaxAmount(0);
        GasTank source = new GasTank(canister.getTankVolume(0) * 10, GasPressure.pascals(20));
        long startingAmount = GasPressure.amount(source.getVolume(), GasPressure.pascals(16));
        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), startingAmount);
        source.tryReplaceContents(gas).requireAccepted();

        helper.assertValueEqual(GasInjectionChamberCanisterTransfer.getTransferableAmount(source, canister, gas, Long.MAX_VALUE), capacity, "Injection did not cap the plan at canister capacity");
        helper.assertTrue(!GasInjectionChamberCanisterTransfer.transferExactly(source, canister, gas, capacity + 1), "Over-capacity injection succeeded");
        helper.assertValueEqual(source.getStoredAmount(), startingAmount, "Rejected injection drained the source");
        helper.assertTrue(GasInjectionChamberCanisterTransfer.transferExactly(source, canister, gas, capacity), "Safe capped injection failed");
        helper.assertValueEqual(source.getStoredAmount(), startingAmount - capacity, "Injection discarded gas beyond the accepted amount");
        helper.assertValueEqual(canister.getGasInTank(0).getAmount(), capacity, "Canister was not filled to capacity");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void highPressureHatchInputStopsAtCanisterLimit(GameTestHelper helper) {
        GasCanisterContainer canister = newCanister();
        GasTank hatch = new GasTank(canister.getTankVolume(0), canister.getTankMaxPressurePa(0));
        GasTank source = new GasTank(hatch.getVolume() * 10, GasPressure.pascals(20));
        long startingAmount = GasPressure.amount(source.getVolume(), GasPressure.pascals(16));
        source.tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), startingAmount)).requireAccepted();

        long accepted = AirtightHatchController.inputOnlyPressureSafe(hatch, source, Long.MAX_VALUE, false);
        helper.assertValueEqual(accepted, hatch.getMaxAmount(), "Hatch failed to stop at the canister limit");
        helper.assertValueEqual(hatch.getPressurePa(), canister.getTankMaxPressurePa(0), "Hatch exceeded canister safe pressure");
        helper.assertValueEqual(source.getStoredAmount(), startingAmount - accepted, "Hatch discarded unaccepted source gas");
        helper.succeed();
    }

    private static GasCanisterContainer newCanister() {
        GasCanisterContainer canister = new ItemStack(CCBItems.GAS_CANISTER.get()).getCapability(CanisterCapabilities.ITEM);
        if (canister == null) {
            throw new NullPointerException("Expected a gas canister capability for the equipment baseline fixture.");
        }

        return canister;
    }
}
