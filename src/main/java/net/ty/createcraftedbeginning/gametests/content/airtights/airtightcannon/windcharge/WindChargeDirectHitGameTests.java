package net.ty.createcraftedbeginning.gametests.content.airtights.airtightcannon.windcharge;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonShotContext;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge.AirtightCannonWindChargeProjectileEntity;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.sculk.SculkAirCannonHandler;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindChargeDirectHitGameTests {
    private WindChargeDirectHitGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5")
    public static void sculkOnlyDamagesDirectTarget(GameTestHelper helper) {
        IronGolem target = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 1, 2));
        IronGolem nearby = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(3, 1, 2));
        float targetHealth = target.getHealth();
        float nearbyHealth = nearby.getHealth();
        var projectile = new TestWindCharge(helper.getLevel(), CCBGases.SCULK_AIR.get().getHolder());
        projectile.setPos(target.position());
        projectile.hit(new EntityHitResult(target));
        helper.assertTrue(target.getHealth() == targetHealth - 6, "Sculk direct hit did not deal exactly six damage");
        helper.assertTrue(nearby.getHealth() == nearbyHealth, "Sculk direct hit dealt area damage");
        target.invulnerableTime = 0;
        new SculkAirCannonHandler().explode(helper.getLevel(), target.position(), AirtightCannonShotContext.external(projectile, CCBGases.SCULK_AIR.get().getHolder(), 1));
        helper.assertTrue(target.getHealth() == targetHealth - 6 && nearby.getHealth() == nearbyHealth, "Sculk explosion without a direct hit dealt damage");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5")
    public static void naturalDirectHitKeepsVanillaDamage(GameTestHelper helper) {
        IronGolem target = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 1, 2));
        float initialHealth = target.getHealth();
        var projectile = new TestWindCharge(helper.getLevel(), CCBGases.NATURAL_AIR.get().getHolder());
        projectile.setPos(target.position());
        projectile.hit(new EntityHitResult(target));
        helper.assertTrue(target.getHealth() == initialHealth - 1, "Natural wind charge direct-hit damage changed");
        helper.succeed();
    }

    private static final class TestWindCharge extends AirtightCannonWindChargeProjectileEntity {
        private TestWindCharge(Level level, Holder<Gas> gas) {
            super(level, gas, Vec3.ZERO);
        }

        private void hit(EntityHitResult hitResult) {
            onHitEntity(hitResult);
        }
    }
}
