package net.ty.createcraftedbeginning.gametests;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonShotContext;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge.AirtightCannonWindChargeProjectileEntity;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.ethereal.EnergizedEtherealAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.moist.MoistAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.natural.EnergizedNaturalAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.ultrawarm.UltrawarmAirCannonHandler;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindChargeEffectGameTests {
    private WindChargeEffectGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5")
    public static void energizedNaturalAppliesDamageAndWindCharged(GameTestHelper helper) {
        IronGolem target = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 1, 2));
        Holder<Gas> gas = CCBGases.ENERGIZED_NATURAL_AIR.get().getHolder();
        AirtightCannonWindChargeProjectileEntity projectile = new AirtightCannonWindChargeProjectileEntity(helper.getLevel(), gas, Vec3.ZERO);
        Vec3 pos = target.getBoundingBox().getCenter();
        new EnergizedNaturalAirCannonHandler().explode(helper.getLevel(), pos, AirtightCannonShotContext.external(projectile, gas, 1));
        boolean windCharged = target.hasEffect(MobEffects.WIND_CHARGED);
        float health = target.getHealth();
        helper.assertTrue(windCharged && health == 96, "Observed windCharged=" + windCharged + ", health=" + health);
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5")
    public static void ultrawarmIgnitionIgnoresNegativeFireTimer(GameTestHelper helper) {
        Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 1, 2));
        Holder<Gas> gas = CCBGases.ULTRAWARM_AIR.get().getHolder();
        AirtightCannonWindChargeProjectileEntity projectile = new AirtightCannonWindChargeProjectileEntity(helper.getLevel(), gas, Vec3.ZERO);
        int before = target.getRemainingFireTicks();
        helper.assertTrue(before < 0, "Ignition fixture did not start with a negative fire timer");
        new UltrawarmAirCannonHandler().explode(helper.getLevel(), target.getBoundingBox().getCenter(), AirtightCannonShotContext.external(projectile, gas, 1));
        int after = target.getRemainingFireTicks();
        helper.assertTrue(after == 40, "Observed fireTicks before=" + before + ", after=" + after);
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5")
    public static void moistRestoresDolphinMaximumAir(GameTestHelper helper) {
        Holder<Gas> gas = CCBGases.MOIST_AIR.get().getHolder();
        MoistAirCannonHandler handler = new MoistAirCannonHandler();
        for (int initialAir : List.of(600, 4800)) {
            Dolphin target = helper.spawnWithNoFreeWill(EntityType.DOLPHIN, new BlockPos(2, 1, 2));
            helper.assertValueEqual(target.getMaxAirSupply(), 4800, "dolphin maximum air supply");
            target.setAirSupply(initialAir);
            helper.assertValueEqual(target.getAirSupply(), initialAir, "dolphin initial air supply");
            AirtightCannonWindChargeProjectileEntity projectile = new AirtightCannonWindChargeProjectileEntity(helper.getLevel(), gas, Vec3.ZERO);
            handler.explode(helper.getLevel(), target.getBoundingBox().getCenter(), AirtightCannonShotContext.external(projectile, gas, 1));
            int restoredAir = target.getAirSupply();
            helper.assertTrue(restoredAir == 4800, "Observed dolphin air before=" + initialAir + ", after=" + restoredAir);
            target.discard();
        }

        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5")
    public static void energizedEtherealDamageCreditsShooter(GameTestHelper helper) {
        IronGolem target = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 1, 2));
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        Holder<Gas> gas = CCBGases.ENERGIZED_ETHEREAL_AIR.get().getHolder();
        AirtightCannonWindChargeProjectileEntity projectile = new AirtightCannonWindChargeProjectileEntity(helper.getLevel(), gas, Vec3.ZERO);
        projectile.setOwner(owner);
        AirtightCannonShotContext context = new AirtightCannonShotContext(projectile, owner, gas, 101325, 1, 0, false);
        new EnergizedEtherealAirCannonHandler().explode(helper.getLevel(), target.getBoundingBox().getCenter(), context);
        float health = target.getHealth();
        LivingEntity killCredit = target.getKillCredit();
        helper.assertTrue(health == 94 && killCredit == owner, "Observed health=" + health + ", killCredit=" + killCredit);
        helper.succeed();
    }
}
