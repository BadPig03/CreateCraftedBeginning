package net.ty.createcraftedbeginning.content.breezes.breezecooler;

import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
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
import net.ty.createcraftedbeginning.recipe.CoolingRecipe.CoolingData;

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
        if (cooler.getLevel() == null || cooler.isStockKeeper()) {
            return false;
        }
        FrostLevel frostLevel = cooler.getFrostLevel();
        CCBLang.translate("gui.breeze_cooler").forGoggles(tooltip);
        CCBLang.translate("gui.breeze_cooler.current_state").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate(frostLevel.getTranslatable()).style(frostLevel.getChatFormatting()).forGoggles(tooltip, 1);
        int time = cooler.getCoolRemainingTime();
        if (time > 0) {
            CCBLang.translate("gui.breeze_cooler.remaining_time").style(ChatFormatting.GRAY).forGoggles(tooltip);
            if (cooler.isCreative()) {
                CCBLang.translate("gui.fluid_container.infinity").style(ChatFormatting.GREEN).forGoggles(tooltip, 1);
            }
            else {
                CCBLang.seconds(time, cooler.getLevel().tickRateManager().tickrate()).style(ChatFormatting.GREEN).forGoggles(tooltip, 1);
            }
        }

        IFluidHandler tank = cooler.getTankInventory();
        FluidStack fluid = tank.getFluidInTank(0);
        tooltip.add(CommonComponents.EMPTY);
        LangBuilder millibuckets = CCBLang.translate("gui.unit.milli_buckets");
        CCBLang.translate("gui.fluid_container.capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
        if (fluid.isEmpty()) {
            CCBLang.number(tank.getTankCapacity(0)).add(millibuckets).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        }
        else {
            CCBLang.fluidName(fluid).style(ChatFormatting.WHITE).forGoggles(tooltip, 1);
            CCBLang.number(fluid.getAmount()).add(millibuckets).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(tank.getTankCapacity(0)).add(millibuckets).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        }
        if (!isLiquidInvalid()) {
            return true;
        }
        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.warning").style(ChatFormatting.GOLD).forGoggles(tooltip);
        CCBLang.addToGoggles(tooltip, "gui.breeze_cooler.invalid_fluid");
        return true;
    }

    private boolean isLiquidInvalid() {
        FluidStack fluid = cooler.getTankInventory().getFluid();
        if (fluid.isEmpty() || cooler.getLevel() == null) {
            return false;
        }
        CoolingData data = cooler.getFluidCoolingData(fluid);
        return data.time() <= 0 || data.amount() <= 0;
    }

    void spawnParticles() {
        if (cooler.getLevel() == null) {
            return;
        }
        RandomSource random = cooler.getLevel().getRandom();
        if (random.nextInt(2) != 0) {
            return;
        }
        Vec3 center = VecHelper.getCenterOf(cooler.getBlockPos());
        Vec3 particlePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.125f).multiply(1, 0, 1));
        boolean openTop = cooler.getLevel().getBlockState(cooler.getBlockPos().above()).getCollisionShape(cooler.getLevel(), cooler.getBlockPos().above()).isEmpty();
        if (openTop || random.nextInt(4) == 0) {
            cooler.getLevel().addParticle(ParticleTypes.SNOWFLAKE, particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
        }
        Vec3 chilledParticlePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5f).multiply(1, 0.25, 1).normalize().scale((openTop ? 0.25 : 0.5) + random.nextDouble() * 0.125)).add(0, 0.5, 0);
        if (cooler.getFrostLevelFromBlock().isAtLeast(FrostLevel.CHILLED)) {
            cooler.getLevel().addParticle(ParticleTypes.SNOWFLAKE, chilledParticlePos.x, chilledParticlePos.y, chilledParticlePos.z, 0, openTop ? 0.0625 : random.nextDouble() * 0.0125, 0);
        }
    }

    void playSound() {
        if (cooler.getLevel() != null) {
            cooler.getLevel().playSound(null, cooler.getBlockPos(), SoundEvents.BREEZE_SHOOT, SoundSource.BLOCKS, 0.125f + cooler.getLevel().random.nextFloat() * 0.125f, 0.75f - cooler.getLevel().random.nextFloat() * 0.25f);
        }
    }

    void spawnParticleBurst() {
        Level level = cooler.getLevel();
        if (level == null) {
            return;
        }
        Vec3 center = VecHelper.getCenterOf(cooler.getBlockPos());
        RandomSource random = level.random;
        for (int i = 0; i < 20; i++) {
            Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5f).multiply(1, 0.25, 1).normalize();
            Vec3 particlePos = center.add(offset.scale(0.5 + random.nextDouble() * 0.125)).add(0, 0.125, 0);
            Vec3 motion = offset.scale(0.03125);
            level.addParticle(ParticleTypes.SNOWFLAKE, particlePos.x, particlePos.y, particlePos.z, motion.x, motion.y, motion.z);
        }
    }

    void tickAnimation(float targetAngle) {
        boolean active = cooler.getBlockState().getValue(BreezeCoolerBlock.ATTACHED);
        if (active) {
            float facingAngle = (AngleHelper.horizontalAngle(cooler.getBlockState().getOptionalValue(BreezeCoolerBlock.FACING).orElse(Direction.SOUTH)) + 180) % 360;
            cooler.headAngle.chase(facingAngle, 0.125f, Chaser.EXP);
        }
        else {
            cooler.headAngle.chase(targetAngle, 0.25f, Chaser.exp(5));
        }
        cooler.headAngle.tickChaser();
        cooler.getHeadAnimationInternal().chase(active ? 1 : 0, 0.25f, Chaser.exp(0.25f));
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
}
