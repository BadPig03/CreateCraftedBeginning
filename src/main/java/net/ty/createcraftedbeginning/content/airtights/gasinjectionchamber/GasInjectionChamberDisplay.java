package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.content.particles.ColoredBreezeCloudParticleType.ColoredBreezeCloudParticleOptions;
import net.ty.createcraftedbeginning.gas.visual.GasUnitFormat;
import net.ty.createcraftedbeginning.gas.visual.GasUnitsTooltips;
import net.ty.createcraftedbeginning.gas.visual.OverpressureTooltips;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasInjectionChamberDisplay {
    private final GasInjectionChamberBlockEntity chamber;
    private final GasInjectionChamberOperationState operation;

    GasInjectionChamberDisplay(GasInjectionChamberBlockEntity chamber, GasInjectionChamberOperationState operation) {
        this.chamber = chamber;
        this.operation = operation;
    }

    boolean addToGoggleTooltip(List<Component> tooltip) {
        if (chamber.getLevel() == null) {
            return false;
        }

        GasStorageHandler gasHandler = chamber.getGasTankBehaviour().getPrimaryHandler();
        GasUnitsTooltips.addContainer(tooltip, gasHandler, false, chamber);
        OverpressureTooltips.addStatus(tooltip, chamber.getOverpressureBehaviour(), gasHandler);
        return true;
    }

    int getMaxValue() {
        return GasUnits.toInt(chamber.getGasTankBehaviour().getPrimaryHandler().getMaxAmount());
    }

    int getCurrentValue() {
        return GasUnits.toInt(chamber.getGasTankBehaviour().getPrimaryHandler().getStoredAmount());
    }

    MutableComponent format(int value) {
        return GasUnitFormat.amount(value).component();
    }

    float getRenderedProcessingTicks(float partialTicks) {
        float processingTicks = operation.getProcessingTicks();
        if (processingTicks < 0) {
            return -1;
        }

        float previousProcessingTicks = operation.getPreviousProcessingTicks();
        if (previousProcessingTicks < 0) {
            return processingTicks;
        }

        return Mth.lerp(partialTicks, previousProcessingTicks, processingTicks);
    }

    void spawnCloud(int color) {
        Level level = chamber.getLevel();
        if (level == null || !level.isClientSide || chamber.isVirtual()) {
            return;
        }

        Vec3 cloudPos = VecHelper.getCenterOf(chamber.getBlockPos()).subtract(0, 1.6875, 0);
        int particleCount = level.random.nextInt(3, 6);
        for (int particleIndex = 0; particleIndex < particleCount; particleIndex++) {
            Vec3 velocity = VecHelper.offsetRandomly(Vec3.ZERO, level.random, 0.125F);
            velocity = new Vec3(velocity.x, Math.abs(velocity.y), velocity.z);
            level.addAlwaysVisibleParticle(new ColoredBreezeCloudParticleOptions(color), cloudPos.x, cloudPos.y, cloudPos.z, velocity.x, velocity.y, velocity.z);
        }
    }
}
