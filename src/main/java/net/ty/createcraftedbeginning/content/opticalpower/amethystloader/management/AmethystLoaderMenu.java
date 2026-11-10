package net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.AmethystLoaderBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.AmethystLoaderNetwork;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management.AmethystLoaderSnapshot.Member;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkBinding;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkRuntime.Status;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystLoaderMenu extends AbstractContainerMenu {
    private static final int SYNC_INTERVAL = 20;
    private static final int INTERACTION_DISTANCE_SQUARED = 64;
    private final Player player;
    private final BlockPos loaderPos;
    private final UUID loaderId;
    private final @Nullable UUID host;
    private AmethystLoaderSnapshot snapshot;
    private final AmethystLoaderMapSession maps;
    private int ticks;

    public AmethystLoaderMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), buffer.readUUID(), buffer.readNullable(input -> input.readUUID()), AmethystLoaderSnapshot.read(buffer));
    }

    AmethystLoaderMenu(int id, Inventory inventory, BlockPos pos, UUID identity, @Nullable UUID host, AmethystLoaderSnapshot snapshot) {
        super(CCBMenuTypes.AMETHYST_LOADER_MENU.get(), id);
        player = inventory.player;
        loaderPos = pos.immutable();
        loaderId = identity;
        this.host = host;
        this.snapshot = snapshot;
        maps = new AmethystLoaderMapSession(this, player);
    }

    @Internal
    public static void open(AmethystLoaderBlockEntity loader, ServerPlayer player) {
        BlockPos pos = loader.getBlockPos();
        if (!(loader.getLevel() instanceof ServerLevel level) || loader.isRemoved() || level.getBlockEntity(pos) != loader || AmethystLoaderNetwork.antenna(loader) == null) {
            return;
        }

        if (player.level() != level || player.distanceToSqr(SubLevelBridge.resolve(level, pos).worldPosition()) > INTERACTION_DISTANCE_SQUARED) {
            return;
        }

        UUID identity = loader.getOrCreateNetworkNodeId();
        UUID host = SubLevelBridge.subLevelId(level, pos);
        AmethystLoaderSnapshot snapshot = AmethystLoaderMembers.create(loader, player.getUUID());
        loader.notifyUpdate();
        player.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new AmethystLoaderMenu(id, inventory, pos, identity, host, snapshot), loader.getBlockState().getBlock().getName()), buffer -> {
            buffer.writeBlockPos(pos);
            buffer.writeUUID(identity);
            buffer.writeNullable(host, (output, value) -> output.writeUUID(value));
            snapshot.write(buffer);
        });
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        Level level = player.level();
        if (!level.isLoaded(loaderPos) || !(level.getBlockEntity(loaderPos) instanceof AmethystLoaderBlockEntity loader) || !loaderId.equals(loader.networkNodeId()) || !Objects.equals(host, SubLevelBridge.subLevelId(level, loaderPos)) || player.distanceToSqr(SubLevelBridge.resolve(level, loaderPos).worldPosition()) > INTERACTION_DISTANCE_SQUARED) {
            return false;
        }

        if (level.isClientSide) {
            return true;
        }

        if (AmethystLoaderNetwork.antenna(loader) == null) {
            return false;
        }

        LoadingNetworkBinding binding = AmethystLoaderNetwork.binding(loader);
        UUID network = binding == null ? null : binding.id();
        return Objects.equals(network, snapshot.network());
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (++ticks % SYNC_INTERVAL == 0) {
            synchronize();
        }
        maps.tick();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        maps.select(null);
    }

    AmethystLoaderSnapshot snapshot() {
        return snapshot;
    }

    AmethystLoaderMapSession maps() {
        return maps;
    }

    void acceptSnapshot(AmethystLoaderSnapshot incoming) {
        if (!Objects.equals(incoming.network(), snapshot.network())) {
            maps.select(null);
        }
        snapshot = incoming;
    }

    void rename(String name) {
        UUID network = snapshot.network();
        if (!(player instanceof ServerPlayer serverPlayer) || !stillValid(player) || network == null) {
            return;
        }

        if (!LoadingNetworkSavedData.get(serverPlayer.serverLevel()).rename(network, player.getUUID(), name)) {
            player.displayClientMessage(CCBLang.translateDirect("loading_network.rename_denied"), true);
            return;
        }

        synchronize();
    }

    void color(@Nullable Integer rgb) {
        UUID network = snapshot.network();
        if (!(player instanceof ServerPlayer serverPlayer) || !stillValid(player) || network == null) {
            return;
        }

        if (!LoadingNetworkSavedData.get(serverPlayer.serverLevel()).setColor(network, player.getUUID(), rgb)) {
            player.displayClientMessage(CCBLang.translateDirect("loading_network.denied"), true);
            return;
        }

        synchronize();
    }

    void control(@Nullable UUID loader, boolean enabled) {
        UUID network = snapshot.network();
        if (!(player instanceof ServerPlayer serverPlayer) || !stillValid(player) || network == null) {
            return;
        }

        ServerLevel level = serverPlayer.serverLevel();
        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(level);
        if (!data.runtime().control(level.getServer(), network, player.getUUID(), loader, enabled)) {
            player.displayClientMessage(CCBLang.translateDirect("loading_network.denied"), true);
        }
        synchronize();
    }

    void beam(UUID identity, boolean enabled) {
        UUID network = snapshot.network();
        if (!(player instanceof ServerPlayer serverPlayer) || !stillValid(player) || network == null) {
            return;
        }

        ServerLevel level = serverPlayer.serverLevel();
        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(level);
        Member member = AmethystLoaderMembers.find(level, network, identity);
        if (member == null || member.status() == Status.ASSEMBLED || !data.setBeamEnabled(network, player.getUUID(), identity, enabled)) {
            player.displayClientMessage(CCBLang.translateDirect("loading_network.denied"), true);
        }
        else {
            AmethystLoaderBlockEntity target = AmethystLoaderMembers.findLoaded(level, data, member);
            if (target != null) {
                target.setBeamEnabled(enabled);
            }
        }
        synchronize();
    }

    private void synchronize() {
        if (!(player instanceof ServerPlayer serverPlayer) || !stillValid(player) || !(player.level().getBlockEntity(loaderPos) instanceof AmethystLoaderBlockEntity loader)) {
            return;
        }

        AmethystLoaderSnapshot current = AmethystLoaderMembers.create(loader, player.getUUID());
        if (current.equals(snapshot)) {
            return;
        }

        snapshot = current;
        PacketDistributor.sendToPlayer(serverPlayer, new AmethystLoaderSyncPacket(containerId, snapshot));
    }
}
