package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlockEntity;
import net.ty.createcraftedbeginning.gas.storage.GasTankLimits;
import net.ty.createcraftedbeginning.gas.storage.GasTankState;
import net.ty.createcraftedbeginning.gas.storage.SmartGasTank;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AirtightHatchSerializationGameTests {
    private static final BlockPos SOURCE_POS = new BlockPos(1, 1, 1);
    private static final BlockPos TARGET_POS = new BlockPos(2, 1, 1);

    private AirtightHatchSerializationGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void serverLoadRestoresGasAfterDynamicLimits(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AirtightHatchBlockEntity source = createFilledHatch(helper);
        SmartGasTank sourceTank = source.getGasTankBehaviour().getPrimaryHandler();
        CompoundTag stored = new CompoundTag();
        source.write(stored, level.registryAccess(), false);

        AirtightHatchBlockEntity loaded = placeHatch(helper, TARGET_POS);
        loaded.read(stored, level.registryAccess(), false);
        assertSameTankState(helper, sourceTank, loaded.getGasTankBehaviour().getPrimaryHandler(), "server load");
        helper.assertTrue(!loaded.createCanisterItemStack().isEmpty(), "server load did not restore the stored canister");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void targetPressureSettingSurvivesServerLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AirtightHatchBlockEntity source = createFilledHatch(helper);
        long expectedPressurePa = GasPressure.pascals(7.5);
        source.setTargetPressurePa(expectedPressurePa);

        CompoundTag stored = new CompoundTag();
        source.write(stored, level.registryAccess(), false);

        AirtightHatchBlockEntity loaded = placeHatch(helper, TARGET_POS);
        loaded.read(stored, level.registryAccess(), false);
        helper.assertValueEqual(loaded.getTargetPressurePa(), expectedPressurePa, "server load target pressure");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void clientPacketReplacesStaleTankStateBeforeGas(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AirtightHatchBlockEntity source = createFilledHatch(helper);
        SmartGasTank sourceTank = source.getGasTankBehaviour().getPrimaryHandler();
        CompoundTag packet = new CompoundTag();
        source.write(packet, level.registryAccess(), true);

        AirtightHatchBlockEntity target = placeHatch(helper, TARGET_POS);
        SmartGasTank targetTank = target.getGasTankBehaviour().getPrimaryHandler();
        long staleMaxPressurePa = sourceTank.getMaxPressurePa() * 2;
        long staleAmount = sourceTank.getMaxAmount() + 1;
        GasTankLimits staleLimits = new GasTankLimits(sourceTank.getVolume(), staleMaxPressurePa);
        targetTank.tryApplyState(new GasTankState(staleLimits, new GasStack(CCBGases.NATURAL_AIR.get(), staleAmount))).requireAccepted();

        target.read(packet, level.registryAccess(), true);
        assertSameTankState(helper, sourceTank, targetTank, "client packet");
        helper.succeed();
    }

    private static AirtightHatchBlockEntity createFilledHatch(GameTestHelper helper) {
        AirtightHatchBlockEntity hatch = placeHatch(helper, SOURCE_POS);
        ItemStack canister = new ItemStack(CCBItems.GAS_CANISTER.asItem());
        helper.assertTrue(hatch.installCanister(canister), "test gas canister could not be installed in airtight hatch");

        SmartGasTank tank = hatch.getGasTankBehaviour().getPrimaryHandler();
        helper.assertTrue(tank.getMaxAmount() > 0, "installed gas canister has no storage capacity");
        long amount = Math.max(1, tank.getMaxAmount() / 2);
        long filled = tank.fill(new GasStack(CCBGases.NATURAL_AIR.get(), amount), GasAction.EXECUTE);
        helper.assertValueEqual(filled, amount, "test gas amount inserted into airtight hatch");
        return hatch;
    }

    private static AirtightHatchBlockEntity placeHatch(GameTestHelper helper, BlockPos pos) {
        BlockPos attachmentPos = pos.relative(Direction.SOUTH);
        helper.setBlock(attachmentPos, CCBBlocks.AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        helper.setBlock(pos, CCBBlocks.AIRTIGHT_HATCH_BLOCK.get().defaultBlockState().setValue(AirtightHatchBlock.FACING, Direction.SOUTH));
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(blockEntity instanceof AirtightHatchBlockEntity, "Airtight hatch block entity was not initialized at " + pos);
        if (!(blockEntity instanceof AirtightHatchBlockEntity hatch)) {
            throw new IllegalStateException("Airtight hatch block entity missing at " + pos + '.');
        }

        return hatch;
    }

    private static void assertSameTankState(GameTestHelper helper, SmartGasTank expected, SmartGasTank actual, String context) {
        helper.assertValueEqual(actual.getVolume(), expected.getVolume(), context + " volume");
        helper.assertValueEqual(actual.getMaxPressurePa(), expected.getMaxPressurePa(), context + " max pressure");
        helper.assertValueEqual(actual.getStoredAmount(), expected.getStoredAmount(), context + " stored amount");
        helper.assertValueEqual(actual.getPressurePa(), expected.getPressurePa(), context + " pressure");
        helper.assertTrue(GasStack.matches(actual.getGasStack(), expected.getGasStack()), context + " gas stack");
    }
}
