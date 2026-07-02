package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.end.endcasing.EndMechanicalBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EndSculkSilencerBlockEntity extends EndMechanicalBlockEntity<EndSculkSilencerStructuralBlockEntity> {
    public static final int LAZY_TICK_RATE = 5;

    private final LerpedFloat animationSpeed;
    private final LerpedFloat animation;

    private EndSculkSilencerStructuralBlockEntity structural;

    public EndSculkSilencerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        setLazyTickRate(LAZY_TICK_RATE);
        animationSpeed = LerpedFloat.linear().startWithValue(0);
        animation = LerpedFloat.angular().startWithValue(0);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.STEVES_REDEMPTION);
        behaviours.add(advancementBehaviour);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || !level.isClientSide) {
            return;
        }

        updateAnimation();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        updateSilencer();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (level == null || level.isClientSide) {
            return;
        }

        String dimension = level.dimension().location().toString();
        GlobalEndSculkSilencerManager.remove(worldPosition, dimension);
        CatnipServices.NETWORK.sendToAllClients(new EndSculkSilencerUpdatePacket(worldPosition, dimension, (short) 0, false));
    }

    @Override
    public void updateStructural() {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockPos structuralPos = worldPosition.below();
        if (!level.getBlockState(structuralPos).is(CCBBlocks.END_CASING_BLOCK)) {
            return;
        }
        if (!level.setBlockAndUpdate(structuralPos, CCBBlocks.END_SCULK_SILENCER_STRUCTURAL_BLOCK.getDefaultState())) {
            return;
        }

        structural = getStructural();
    }

    @Override
    public boolean isSpeedRequirementFulfilled() {
        if (structural == null) {
            structural = getStructural();
        }
        if (structural == null) {
            return super.isSpeedRequirementFulfilled();
        }

        short range = structural.getWorkingRange();
        return Mth.abs(getSpeed()) >= SpeedLevel.MEDIUM.getSpeedValue() * range * Mth.sqrt(range);
    }

    public LerpedFloat getAnimation() {
        return animation;
    }

    private void updateAnimation() {
        if (isSpeedRequirementFulfilled()) {
            float speed = getSpeed();
            float absSpeed = Mth.abs(speed);
            animationSpeed.chase(Math.signum(speed) * 2 * Mth.ceil(Math.log10(absSpeed) + Math.sqrt(absSpeed)), 0.1, Chaser.EXP);
        }
        else {
            animationSpeed.chase(0, 0.2, Chaser.EXP);
        }

        animationSpeed.tickChaser();
        animation.setValue(animation.getValue() + animationSpeed.getValue());
    }

    private void updateSilencer() {
        if (level == null || level.isClientSide) {
            return;
        }

        String dimension = level.dimension().location().toString();
        short range = getWorkingRange();
        if (!GlobalEndSculkSilencerManager.canUpdate(worldPosition, dimension, range)) {
            return;
        }

        GlobalEndSculkSilencerManager.add(worldPosition, dimension, range);
        CatnipServices.NETWORK.sendToAllClients(new EndSculkSilencerUpdatePacket(worldPosition, dimension, range, true));
    }

    private short getWorkingRange() {
        if (!isSpeedRequirementFulfilled()) {
            return 0;
        }

        if (structural == null) {
            structural = getStructural();
        }
        if (structural == null) {
            return 0;
        }

        return structural.getWorkingRange();
    }
}
