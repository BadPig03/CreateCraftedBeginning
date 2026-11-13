package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.Node;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.PhysicalNode;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ChunkStabilizerBlockEntity extends SmartBlockEntity {
    private static final int LAZY_TICK_RATE = 20;
    private static Consumer<ChunkStabilizerBlockEntity> clientTicker = loader -> {};

    private static Predicate<ChunkStabilizerBlockEntity> subLevelTicker = loader -> false;

    private final ChunkStabilizerAnimation animation = new ChunkStabilizerAnimation();
    private @Nullable UUID networkNodeId;
    private String displayName = "";
    private boolean active;
    private boolean beamEnabled = true;
    private int beamColor = LoadingNetworkSavedData.UNBOUND_COLOR;
    private boolean showOutline = true;

    public ChunkStabilizerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(LAZY_TICK_RATE);
    }

    public static void setSubLevelTicker(Predicate<ChunkStabilizerBlockEntity> ticker) {
        subLevelTicker = ticker;
    }

    static void setClientTicker(Consumer<ChunkStabilizerBlockEntity> ticker) {
        clientTicker = ticker;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void onLoad() {
        super.onLoad();
        refreshLoading();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || !level.isClientSide) {
            return;
        }

        animation.tick(active && CCBConfig.server().opticalPower.chunkStabilizer.enableChunkLoading.get());
        clientTicker.accept(this);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        refreshName();
        refreshLoading();
    }

    @Override
    protected void write(CompoundTag tag, Provider provider, boolean clientPacket) {
        super.write(tag, provider, clientPacket);
        ChunkStabilizerSerialization.write(this, tag, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        super.read(tag, provider, clientPacket);
        ChunkStabilizerSerialization.read(this, tag, clientPacket);
    }

    public boolean isLoadingActive() {
        return active;
    }

    public int beamColor() {
        return beamColor;
    }

    public boolean beamEnabled() {
        return beamEnabled;
    }

    public void setBeamEnabled(boolean enabled) {
        if (beamEnabled == enabled) {
            return;
        }

        beamEnabled = enabled;
        setChanged();
        notifyUpdate();
    }

    public void startPonderLoading() {
        if (!isVirtual()) {
            return;
        }

        active = true;
    }

    public @Nullable UUID networkNodeId() {
        return networkNodeId;
    }

    public String displayName() {
        return displayName;
    }

    public void refreshLoading() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean inSubLevel = SubLevelBridge.resolve(serverLevel, worldPosition).inSubLevel();
        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(serverLevel);
        GlobalPos position = GlobalPos.of(serverLevel.dimension(), worldPosition);
        int color = data.color(position, networkNodeId);
        boolean enabled = inSubLevel ? subLevelTicker.test(this) : reconcileFixedLoading();
        Node node = data.nodeAt(position);
        if (node != null && !node.loaderId().equals(networkNodeId)) {
            node = null;
        }
        if (inSubLevel) {
            PhysicalNode physical = data.physicalNode(networkNodeId);
            node = physical == null || !physical.host().equals(SubLevelBridge.subLevelId(serverLevel, worldPosition)) ? node : physical.node();
        }
        boolean shouldShowBeam = node == null ? beamEnabled : node.beamEnabled();
        if (active == enabled && beamColor == color && beamEnabled == shouldShowBeam) {
            return;
        }

        if (!inSubLevel && active != enabled) {
            ChunkStabilizerTickets.setForced(serverLevel, worldPosition, enabled);
        }
        active = enabled;
        beamEnabled = shouldShowBeam;
        beamColor = color;
        notifyUpdate();
    }

    public UUID getOrCreateNetworkNodeId() {
        if (networkNodeId == null) {
            networkNodeId = UUID.randomUUID();
            setChanged();
        }
        return networkNodeId;
    }

    boolean reconcileFixedLoading() {
        return level instanceof ServerLevel serverLevel && CCBConfig.server().opticalPower.chunkStabilizer.enableChunkLoading.get() && LoadingNetworkSavedData.get(serverLevel).reconcileFixedLoading(GlobalPos.of(serverLevel.dimension(), worldPosition), networkNodeId);
    }

    float animationAngle(float partialTick) {
        return animation.angle(partialTick);
    }

    void toggleShowOutline() {
        showOutline = !showOutline;
        notifyUpdate();
    }

    boolean isShowingOutline() {
        return showOutline && active;
    }

    boolean showOutline() {
        return showOutline;
    }

    void restoreState(@Nullable UUID networkNodeId, boolean showOutline, boolean beamEnabled, String displayName, boolean active, int beamColor) {
        this.networkNodeId = networkNodeId;
        this.showOutline = showOutline;
        this.beamEnabled = beamEnabled;
        this.displayName = displayName;
        this.active = active;
        this.beamColor = beamColor;
    }

    private void refreshName() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        UUID host = SubLevelBridge.subLevelId(serverLevel, worldPosition);
        String name = "";
        for (Direction direction : Iterate.directions) {
            BlockPos adjacent = worldPosition.relative(direction);
            if (serverLevel.isOutsideBuildHeight(adjacent)) {
                continue;
            }

            if (!serverLevel.isLoaded(adjacent)) {
                return;
            }

            if (!Objects.equals(host, SubLevelBridge.subLevelId(serverLevel, adjacent)) || !(serverLevel.getBlockEntity(adjacent) instanceof SignBlockEntity sign)) {
                continue;
            }

            for (boolean front : Iterate.trueAndFalse) {
                StringBuilder text = new StringBuilder();
                for (Component line : sign.getText(front).getMessages(false)) {
                    String value = line.getString();
                    if (value.isBlank()) {
                        continue;
                    }

                    if (!text.isEmpty()) {
                        text.append(' ');
                    }
                    text.append(value.trim());
                }
                if (text.isEmpty()) {
                    continue;
                }

                name = text.toString().trim();
                break;
            }
        }
        if (displayName.equals(name)) {
            return;
        }

        displayName = name;
        setChanged();
        if (networkNodeId == null) {
            return;
        }

        LoadingNetworkSavedData.get(serverLevel).updateName(GlobalPos.of(serverLevel.dimension(), worldPosition), networkNodeId, name);
    }
}
