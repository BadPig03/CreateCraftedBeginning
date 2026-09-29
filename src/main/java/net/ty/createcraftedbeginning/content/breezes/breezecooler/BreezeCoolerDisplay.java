package net.ty.createcraftedbeginning.content.breezes.breezecooler;

import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip.Section;
import net.ty.createcraftedbeginning.recipe.CoolingRecipeLookup.CoolingData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BreezeCoolerDisplay {
    private final BreezeCoolerBlockEntity cooler;
    private boolean goggles;
    private boolean trainHat;

    BreezeCoolerDisplay(BreezeCoolerBlockEntity cooler) {
        this.cooler = cooler;
    }

    boolean addToGoggleTooltip(List<Component> tooltip) {
        Level level = cooler.getLevel();
        if (level == null || cooler.isStockKeeper()) {
            return false;
        }

        FrostLevel frostLevel = cooler.getFrostLevel();
        CCBLang.translate("gui.breeze_cooler").forGoggles(tooltip);
        CCBLang.translate("gui.breeze_cooler.current_state").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate(frostLevel.getTranslatable()).style(frostLevel.getChatFormatting()).forGoggles(tooltip, 1);
        int remainingCoolingTime = cooler.getCoolRemainingTime();
        if (remainingCoolingTime > 0 && GoggleTooltip.isVisible(tooltip, Section.BREEZE_TIME)) {
            CCBLang.translate("gui.breeze_cooler.remaining_time").style(ChatFormatting.GRAY).forGoggles(tooltip);
            if (cooler.isCreative()) {
                CCBLang.translate("gui.fluid_container.infinity").style(ChatFormatting.GREEN).forGoggles(tooltip, 1);
            }
            else {
                CCBLang.seconds(remainingCoolingTime, level.tickRateManager().tickrate()).style(ChatFormatting.GREEN).forGoggles(tooltip, 1);
            }
        }

        if (!GoggleTooltip.isVisible(tooltip, Section.FLUID_STORAGE)) {
            return true;
        }

        IFluidHandler fluidTank = cooler.getTankInventory();
        FluidStack storedFluid = fluidTank.getFluidInTank(0);
        tooltip.add(CommonComponents.EMPTY);
        LangBuilder millibuckets = CCBLang.translate("gui.unit.milli_buckets");
        CCBLang.translate("gui.fluid_container.capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
        if (storedFluid.isEmpty()) {
            CCBLang.number(fluidTank.getTankCapacity(0)).add(millibuckets).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        }
        else {
            CCBLang.fluidName(storedFluid).style(ChatFormatting.WHITE).forGoggles(tooltip, 1);
            CCBLang.number(storedFluid.getAmount()).add(millibuckets).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(fluidTank.getTankCapacity(0)).add(millibuckets).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        }
        return true;
    }

    boolean addToTooltip(List<Component> tooltip) {
        if (cooler.getLevel() == null || cooler.isStockKeeper() || !isLiquidInvalid()) {
            return false;
        }

        CCBLang.translate("gui.warning").style(ChatFormatting.GOLD).forGoggles(tooltip);
        CCBLang.addToGoggles(tooltip, "gui.breeze_cooler.invalid_fluid");
        return true;
    }

    void spawnParticles() {
        Level level = cooler.getLevel();
        if (level == null) {
            return;
        }

        RandomSource random = level.getRandom();
        if (random.nextInt(2) != 0) {
            return;
        }

        Vec3 center = VecHelper.getCenterOf(cooler.getBlockPos());
        Vec3 particlePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.125F).multiply(1, 0, 1));
        boolean isTopOpen = level.getBlockState(cooler.getBlockPos().above()).getCollisionShape(level, cooler.getBlockPos().above()).isEmpty();
        if (isTopOpen || random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SNOWFLAKE, particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
        }
        Vec3 chilledParticlePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5F).multiply(1, 0.25, 1).normalize().scale((isTopOpen ? 0.25 : 0.5) + random.nextDouble() * 0.125)).add(0, 0.5, 0);
        if (!cooler.getFrostLevelFromBlock().isAtLeast(FrostLevel.CHILLED)) {
            return;
        }

        level.addParticle(ParticleTypes.SNOWFLAKE, chilledParticlePos.x, chilledParticlePos.y, chilledParticlePos.z, 0, isTopOpen ? 0.0625 : random.nextDouble() * 0.0125, 0);
    }

    void playSound() {
        Level level = cooler.getLevel();
        if (level == null) {
            return;
        }

        level.playSound(null, cooler.getBlockPos(), SoundEvents.BREEZE_SHOOT, SoundSource.BLOCKS, 0.125F + level.random.nextFloat() * 0.125F, 0.75F - level.random.nextFloat() * 0.25F);
    }

    void spawnParticleBurst() {
        Level level = cooler.getLevel();
        if (level == null) {
            return;
        }

        Vec3 center = VecHelper.getCenterOf(cooler.getBlockPos());
        RandomSource random = level.random;
        for (int i = 0; i < 20; i++) {
            Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5F).multiply(1, 0.25, 1).normalize();
            Vec3 particlePos = center.add(offset.scale(0.5 + random.nextDouble() * 0.125)).add(0, 0.125, 0);
            Vec3 velocity = offset.scale(0.03125);
            level.addParticle(ParticleTypes.SNOWFLAKE, particlePos.x, particlePos.y, particlePos.z, velocity.x, velocity.y, velocity.z);
        }
    }

    void tickAnimation(float targetAngle) {
        cooler.getHeadAngle().chase(targetAngle, 0.25F, Chaser.exp(5));
        cooler.getHeadAngle().tickChaser();
        cooler.getHeadAnimationInternal().chase(0, 0.25F, Chaser.exp(0.25F));
        cooler.getHeadAnimationInternal().tickChaser();
    }

    boolean hasGoggles() {
        return goggles;
    }

    boolean hasTrainHat() {
        return trainHat;
    }

    void setGoggles(boolean goggles) {
        this.goggles = goggles;
    }

    void setTrainHat(boolean trainHat) {
        this.trainHat = trainHat;
    }

    private boolean isLiquidInvalid() {
        FluidStack fluid = cooler.getTankInventory().getFluid();
        if (fluid.isEmpty() || cooler.getLevel() == null) {
            return false;
        }

        CoolingData coolingData = cooler.getFluidCoolingData(fluid);
        return coolingData.time() <= 0 || coolingData.amount() <= 0;
    }
}
