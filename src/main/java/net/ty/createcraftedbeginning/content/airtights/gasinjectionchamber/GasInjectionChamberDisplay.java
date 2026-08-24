package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.particles.ColoredBreezeCloudParticleType.ColoredBreezeCloudParticleOptions;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberDisplay {
    private final GasInjectionChamberBlockEntity chamber;
    private final GasInjectionChamberOperationState operation;

    public GasInjectionChamberDisplay(GasInjectionChamberBlockEntity chamber, GasInjectionChamberOperationState operation) {
        this.chamber = chamber;
        this.operation = operation;
    }

    public boolean addToGoggleTooltip(List<Component> tooltip) {
        if (chamber.getLevel() == null) {
            return false;
        }

        IGasHandler gasHandler = chamber.getGasTankBehaviour().getPrimaryHandler();
        CCBLang.translate("gui.gas_container").forGoggles(tooltip);
        GasStack gas = gasHandler.getGasInTank(0);
        if (gas.isEmpty()) {
            CCBLang.translate("gui.gas_container.capacity").add(GasAmounts.precise(gasHandler.getTankCapacity(0)).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            return true;
        }

        CCBLang.gasName(gas).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        GasAmounts.precise(gas.getAmount()).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(GasAmounts.precise(gasHandler.getTankCapacity(0)).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        return true;
    }

    public int getMaxValue() {
        return GasAmounts.toMillibucketsClamped(chamber.getGasTankBehaviour().getPrimaryHandler().getCapacity());
    }

    public int getCurrentValue() {
        return GasAmounts.toMillibucketsClamped(chamber.getGasTankBehaviour().getPrimaryHandler().getGasAmount());
    }

    public MutableComponent format(int value) {
        return GasAmounts.precise(value).component();
    }

    public float getRenderedProcessingTicks(float partialTicks) {
        int processingTicks = operation.getProcessingTicks();
        if (processingTicks < 0) {
            return -1;
        }

        int previousProcessingTicks = operation.getPreviousProcessingTicks();
        return previousProcessingTicks < 0 ? processingTicks : Mth.lerp(partialTicks, previousProcessingTicks, processingTicks);
    }

    public void spawnCloud(int color) {
        Level level = chamber.getLevel();
        if (level == null || !level.isClientSide || chamber.isVirtual()) {
            return;
        }

        Vec3 cloudPos = VecHelper.getCenterOf(chamber.getBlockPos()).subtract(0, 1.6875, 0);
        int particleCount = level.random.nextInt(3, 6);
        for (int particleIndex = 0; particleIndex < particleCount; particleIndex++) {
            Vec3 velocity = VecHelper.offsetRandomly(Vec3.ZERO, level.random, 0.125f);
            velocity = new Vec3(velocity.x, Math.abs(velocity.y), velocity.z);
            level.addAlwaysVisibleParticle(new ColoredBreezeCloudParticleOptions(color), cloudPos.x, cloudPos.y, cloudPos.z, velocity.x, velocity.y, velocity.z);
        }
    }
}
