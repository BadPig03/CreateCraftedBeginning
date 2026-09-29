package net.ty.createcraftedbeginning.content.airtights.balloon;

import com.simibubi.create.content.logistics.box.PackageEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlers;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonShotContext;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.AirtightCannonVisualHandlers;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfile;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BalloonGasEffects {
    private static final double MIN_TRAIL_MOTION_LENGTH_SQR = 1.0E-4;

    private BalloonGasEffects() {
    }

    static void renderTrail(PackageEntity balloon) {
        Level level = balloon.level();
        if (!level.isClientSide) {
            return;
        }

        Vec3 delta = balloon.getDeltaMovement();
        if (delta.lengthSqr() < MIN_TRAIL_MOTION_LENGTH_SQR || (balloon.tickCount & 1) != 0) {
            return;
        }

        GasStack gas = BalloonItem.getGas(balloon.getBox());
        if (gas.isEmpty()) {
            return;
        }

        GameplayPressureProfile pressureProfile = BalloonPressureSemantics.gameplayProfile(level, balloon.blockPosition());
        AirtightCannonVisualHandlers.resolve(gas.getGasType(), pressureProfile).renderTrailParticles(level, balloon.getBoundingBox().getCenter(), delta);
    }

    static void release(PackageEntity balloon) {
        GasStack gas = BalloonItem.getGas(balloon.getBox());
        if (gas.isEmpty()) {
            return;
        }

        long referenceGasAmount = BalloonPackingLimits.getBaseAmount();
        if (referenceGasAmount <= 0) {
            return;
        }

        float releasedGasRatio = Mth.clamp((float) gas.getAmount() / referenceGasAmount, 0.0F, 1.0F);
        float burstMultiplier = releasedGasRatio * 2;
        if (burstMultiplier <= 0) {
            return;
        }

        Level level = balloon.level();
        GameplayPressureProfile pressureProfile = BalloonPressureSemantics.gameplayProfile(level, balloon.blockPosition());
        Vec3 burstPosition = balloon.getBoundingBox().getCenter();
        AirtightCannonHandler gasHandler = AirtightCannonHandlers.resolve(gas.getGasType(), pressureProfile);
        gasHandler.explode(level, burstPosition, AirtightCannonShotContext.external(balloon, gas.getGasHolder(), burstMultiplier));
    }
}
