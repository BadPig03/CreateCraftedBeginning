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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor.AmethystSoundSuppressorSerialization.ClientSnapshot;

import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.UUID;
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
    private boolean clientStateRejected;
    private @Nullable UUID pendingPlacer;

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
        if (clientPacket) {
            ClientSnapshot snapshot = AmethystSoundSuppressorSerialization.readClient(compoundTag, worldPosition, !clientStateRejected);
            if (snapshot == null) {
                clientStateRejected = true;
                return;
            }

            super.read(compoundTag, provider, true);
            controller.restoreClientState(snapshot);
            clientStateRejected = false;
        }
        else {
            super.read(compoundTag, provider, false);
            controller.readSaved(compoundTag);
        }
        showOutline = AmethystSoundSuppressorSerialization.readShowOutline(compoundTag, showOutline);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        AmethystSoundSuppressorTooltip.addToGoggleTooltip(tooltip, controller, clientStateRejected);
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

    void place(ServerLevel level, UUID player) {
        pendingPlacer = player;
        if (level.captureBlockSnapshots) {
            return;
        }

        commitPlacement(level);
    }

    void commitPlacement(ServerLevel level) {
        UUID placer = pendingPlacer;
        pendingPlacer = null;
        if (placer == null || !level.getBiome(worldPosition).is(Biomes.DEEP_DARK) || !(level.getPlayerByUUID(placer) instanceof ServerPlayer player)) {
            return;
        }

        CCBAdvancements.DEEP_DARK_EXPLORER.awardTo(player);
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
        return !clientStateRejected && controller.isIlluminated();
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
