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
public record AirtightUpgradePacket(ResourceLocation id, boolean install) implements ServerboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, AirtightUpgradePacket> STREAM_CODEC = StreamCodec.composite(ResourceLocation.STREAM_CODEC, AirtightUpgradePacket::id, ByteBufCodecs.BOOL, AirtightUpgradePacket::install, AirtightUpgradePacket::new);

    @Override
    public void handle(ServerPlayer player) {
        if (!(player.containerMenu instanceof AirtightUpgradableMenu menu)) {
            return;
        }

        if (!menu.stillValid(player)) {
            player.closeContainer();
            return;
        }

        AirtightUpgrade upgrade = AirtightUpgrade.getByID(id);
        if (upgrade == null) {
            return;
        }

        boolean changed = install ? menu.tryInstallUpgrade(id) : menu.tryToggleUpgrade(id);
        if (!changed) {
            return;
        }

        menu.slots.get(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX + Inventory.INVENTORY_SIZE).setChanged();
        menu.broadcastChanges();
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AIRTIGHT_UPGRADE;
    }
}
