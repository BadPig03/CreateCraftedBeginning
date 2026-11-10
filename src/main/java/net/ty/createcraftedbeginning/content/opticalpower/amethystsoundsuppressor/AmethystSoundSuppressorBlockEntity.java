package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystSoundSuppressorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    static final short BASE_WORKING_RANGE = 1;
    private static final int LAZY_TICK_RATE = 20;
    private static Consumer<AmethystSoundSuppressorBlockEntity> clientTicker = suppressor -> {};

    private final AmethystSoundSuppressorController controller;
    private final AmethystCrystalGrowthCandidates growthCandidates = new AmethystCrystalGrowthCandidates();
    private boolean showOutline;

    public AmethystSoundSuppressorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(LAZY_TICK_RATE);
        controller = new AmethystSoundSuppressorController(this);
        showOutline = true;
    }

    static void setClientTicker(Consumer<AmethystSoundSuppressorBlockEntity> ticker) {
        clientTicker = ticker;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new CCBAdvancementBehaviour(this, CCBAdvancements.STEVES_REDEMPTION));
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            clientTicker.accept(this);
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        controller.tickServer(serverLevel);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        controller.refresh();
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        AmethystSoundSuppressorSerialization.writeShowOutline(compoundTag, showOutline);
        controller.write(compoundTag, clientPacket);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        showOutline = AmethystSoundSuppressorSerialization.readShowOutline(compoundTag, showOutline);
        controller.read(compoundTag, clientPacket);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        AmethystSoundSuppressorTooltip.addToGoggleTooltip(tooltip, controller);
        return true;
    }

    @Override
    public void onChunkUnloaded() {
        controller.remove();
        growthCandidates.clear();
        super.onChunkUnloaded();
    }

    @Override
    public void invalidate() {
        controller.remove();
        growthCandidates.clear();
        super.invalidate();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        controller.refresh();
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        controller.refresh();
    }

    double getReceivedFluxLm() {
        return controller.getReceivedFluxLm();
    }

    double getGrowthQuantityLumenSeconds() {
        return controller.getQuantityLumenSeconds();
    }

    double getQualifiedFluxLm() {
        return controller.getQualifiedFluxLm();
    }

    boolean isIlluminated() {
        return controller.isIlluminated();
    }

    short getWorkingRange() {
        return BASE_WORKING_RANGE;
    }

    AmethystCrystalGrowthCandidates growthCandidates() {
        return growthCandidates;
    }

    boolean canGrowCrystal(long gameTime) {
        return controller.canGrowCrystal(gameTime);
    }

    void recordCrystalGrowth(long gameTime) {
        controller.recordCrystalGrowth(gameTime);
    }

    void toggleShowOutline() {
        showOutline = !showOutline;
        setChanged();
        notifyUpdate();
    }

    boolean isShowingOutline() {
        return showOutline;
    }
}
