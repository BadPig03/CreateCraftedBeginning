package net.ty.createcraftedbeginning.gas.overpressure;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseCause;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.gas.release.GasReleaseRequest;
import net.ty.createcraftedbeginning.gas.release.GasReleaseService;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class PressureRuptureService {
    private PressureRuptureService() {
    }

    public static void rupture(Level level, BlockPos rupturePos, GasPressureCompartment failedCompartment) {
        if (level.isClientSide) {
            return;
        }

        releaseFailedCompartment(level, rupturePos, failedCompartment);
        createMechanicalBlast(level, rupturePos);
        level.destroyBlock(rupturePos, false);
    }

    private static void createMechanicalBlast(Level level, BlockPos rupturePos) {
        float explosionStrength = Math.max(0, CCBConfig.server().gas.pressureRupture.explosionStrength.getF());
        if (explosionStrength <= 0) {
            return;
        }

        ExplosionInteraction interaction = CCBConfig.server().gas.pressureRupture.explosionDamagesSurroundingBlocks.get() ? ExplosionInteraction.BLOCK : ExplosionInteraction.NONE;
        level.explode(null, rupturePos.getX() + 0.5, rupturePos.getY() + 0.5, rupturePos.getZ() + 0.5, explosionStrength, false, interaction);
    }

    private static void releaseFailedCompartment(Level level, BlockPos rupturePos, GasPressureCompartment failedCompartment) {
        long storedAmount = failedCompartment.getStoredAmount();
        if (storedAmount <= 0) {
            return;
        }

        long sourcePressurePa = failedCompartment.getPressurePa();
        GasStack releasedGas = failedCompartment.drain(storedAmount, GasAction.EXECUTE);
        if (releasedGas.isEmpty()) {
            return;
        }

        GasReleaseService.release(level, GasReleaseRequest.radial(releasedGas, rupturePos, GasReleaseCause.RUPTURE, sourcePressurePa));
    }
}
