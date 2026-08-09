package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.end.endcasing.EndMechanicalBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EndSculkSilencerBlockEntity extends EndMechanicalBlockEntity<EndSculkSilencerStructuralBlockEntity> {
    public static final int LAZY_TICK_RATE = 20;

    private static final String COMPOUND_KEY_SHOW_OUTLINE = "ShowOutline";
    private static Consumer<EndSculkSilencerBlockEntity> clientTicker = silencer -> {};

    private final EndSculkSilencerAnimationState animationState;
    private final EndSculkSilencerController controller;
    private boolean showOutline;

    public EndSculkSilencerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        setLazyTickRate(LAZY_TICK_RATE);
        animationState = new EndSculkSilencerAnimationState();
        controller = new EndSculkSilencerController(this);
        showOutline = true;
    }

    public static void setClientTicker(Consumer<EndSculkSilencerBlockEntity> ticker) {
        clientTicker = ticker;
    }

    public static boolean meetsRequiredSpeed(float speed, short range) {
        float multiplier = Math.max(0, CCBConfig.server().endDevices.speedRequirementMultiplier.getF());
        return range > 0 && Mth.abs(speed) >= SpeedLevel.MEDIUM.getSpeedValue() * range * Mth.sqrt(range) * multiplier;
    }

    public static float calculateAnimationTargetSpeed(float kineticSpeed) {
        return EndSculkSilencerAnimationState.calculateTargetSpeed(kineticSpeed);
    }

    public static AABB calculateArea(Level level, BlockPos pos, short range) {
        int chunkRadius = Math.max(0, range - 1);
        int centerChunkX = pos.getX() >> 4;
        int centerChunkZ = pos.getZ() >> 4;
        int minX = centerChunkX - chunkRadius << 4;
        int minZ = centerChunkZ - chunkRadius << 4;
        int maxX = centerChunkX + chunkRadius + 1 << 4;
        int maxZ = centerChunkZ + chunkRadius + 1 << 4;
        return new AABB(minX, level.getMinBuildHeight(), minZ, maxX, level.getMaxBuildHeight(), maxZ);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.STEVES_REDEMPTION);
        behaviours.add(advancementBehaviour);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        controller.refresh();
    }

    @Override
    protected Class<EndSculkSilencerStructuralBlockEntity> getStructuralClass() {
        return EndSculkSilencerStructuralBlockEntity.class;
    }

    @Override
    public void updateStructural() {
        if (!convertCasingToStructural(CCBBlocks.END_SCULK_SILENCER_STRUCTURAL_BLOCK.getDefaultState())) {
            return;
        }

        controller.refresh();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            animationState.tick(isSpeedRequirementFulfilled(), getSpeed());
            clientTicker.accept(this);
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        controller.tickServer(serverLevel);
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        controller.refresh();
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.putBoolean(COMPOUND_KEY_SHOW_OUTLINE, showOutline);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (!compoundTag.contains(COMPOUND_KEY_SHOW_OUTLINE)) {
            return;
        }

        showOutline = compoundTag.getBoolean(COMPOUND_KEY_SHOW_OUTLINE);
    }

    @Override
    public boolean isSpeedRequirementFulfilled() {
        EndSculkSilencerStructuralBlockEntity structural = getStructuralForUse();
        return structural != null && meetsRequiredSpeed(getSpeed(), structural.getWorkingRange());
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        controller.refresh();
    }

    @Override
    public void invalidate() {
        controller.remove();
        super.invalidate();
    }

    public LerpedFloat getAnimation() {
        return animationState.getAnimation();
    }

    public void refreshSilencerState() {
        controller.refresh();
    }

    public void toggleShowOutline() {
        showOutline = !showOutline;
        setChanged();
        notifyUpdate();
    }

    public boolean isShowingOutline() {
        return showOutline;
    }

    public short getActiveWorkingRange() {
        EndSculkSilencerStructuralBlockEntity structural = getStructuralForUse();
        if (structural == null) {
            return 0;
        }

        short range = structural.getWorkingRange();
        return meetsRequiredSpeed(getSpeed(), range) ? range : 0;
    }
}
