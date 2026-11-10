package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AirtightUpgradePacket(int containerId, ResourceLocation id, boolean install) implements ServerboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, AirtightUpgradePacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, AirtightUpgradePacket::containerId, ResourceLocation.STREAM_CODEC, AirtightUpgradePacket::id, ByteBufCodecs.BOOL, AirtightUpgradePacket::install, AirtightUpgradePacket::new);

    @Override
    public void handle(ServerPlayer player) {
        if (!(player.containerMenu instanceof AirtightUpgradableMenu menu) || menu.containerId != containerId) {
            return;
        }

        if (!menu.stillValid(player)) {
            player.closeContainer();
            return;
        }

        boolean upgradeChanged = install ? menu.tryInstallUpgrade(id) : menu.tryToggleUpgrade(id);
        if (upgradeChanged) {
            menu.saveData(menu.contentHolder);
            menu.slots.get(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX + Inventory.INVENTORY_SIZE).setChanged();
            menu.broadcastChanges();
        }
        menu.syncToClient(player, id);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AIRTIGHT_UPGRADE;
    }
}
