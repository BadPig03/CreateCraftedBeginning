package net.ty.createcraftedbeginning.content.opticalpower.amethystantenna;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentMap.Builder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.opticalpower.amethystantenna.AmethystAntennaSerialization.StoredState;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkBinding;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystAntennaBlockEntity extends SmartBlockEntity {
    private static final int RECONCILE_INTERVAL = 20;
    private UUID identity = UUID.randomUUID();
    private @Nullable LoadingNetworkBinding binding;
    private @Nullable PendingPlacement pendingPlacement;

    public AmethystAntennaBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(RECONCILE_INTERVAL);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        AmethystAntennaConnection.reconcile(this, binding);
    }

    @Override
    protected void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        super.read(tag, provider, clientPacket);
        StoredState stored = AmethystAntennaSerialization.read(tag, identity);
        identity = stored.identity();
        binding = stored.binding();
    }

    @Override
    protected void write(CompoundTag tag, Provider provider, boolean clientPacket) {
        super.write(tag, provider, clientPacket);
        AmethystAntennaSerialization.write(this, tag);
    }

    @Override
    protected void collectImplicitComponents(Builder components) {
        super.collectImplicitComponents(components);
        LoadingNetworkBinding current = binding();
        if (current == null) {
            return;
        }

        components.set(CCBDataComponents.LOADING_NETWORK, current);
    }

    public UUID identity() {
        return identity;
    }

    public @Nullable LoadingNetworkBinding binding() {
        if (binding != null && level instanceof ServerLevel serverLevel) {
            return LoadingNetworkSavedData.get(serverLevel).binding(binding.id());
        }

        return binding;
    }

    void place(ServerLevel level, @Nullable UUID owner, @Nullable LoadingNetworkBinding network) {
        pendingPlacement = new PendingPlacement(owner, network);
        if (level.captureBlockSnapshots) {
            return;
        }

        commitPlacement(level);
    }

    void commitPlacement(ServerLevel level) {
        PendingPlacement placement = pendingPlacement;
        if (placement == null) {
            return;
        }

        pendingPlacement = null;
        UUID owner = placement.owner();
        LoadingNetworkBinding network = placement.binding();
        if (network == null && owner != null) {
            network = LoadingNetworkSavedData.get(level).create(owner);
        }
        attach(network);
        AmethystAntennaAdvancements.awardPlacement(this, level, owner, network);
    }

    void attach(@Nullable LoadingNetworkBinding network) {
        binding = network;
        AmethystAntennaConnection.reconcile(this, binding);
        notifyUpdate();
    }

    private record PendingPlacement(@Nullable UUID owner, @Nullable LoadingNetworkBinding binding) {}
}
