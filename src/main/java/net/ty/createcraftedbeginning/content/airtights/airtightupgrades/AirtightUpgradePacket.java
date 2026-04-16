package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import org.jetbrains.annotations.NotNull;

public record AirtightUpgradePacket(ResourceLocation id, boolean install, @NotNull ItemStack upgradeItemStack) implements ServerboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, AirtightUpgradePacket> STREAM_CODEC = StreamCodec.composite(ResourceLocation.STREAM_CODEC, AirtightUpgradePacket::id, ByteBufCodecs.BOOL, AirtightUpgradePacket::install, ItemStack.OPTIONAL_STREAM_CODEC, AirtightUpgradePacket::upgradeItemStack, AirtightUpgradePacket::new);

    @Override
    public void handle(@NotNull ServerPlayer player) {
        if (!(player.containerMenu instanceof AirtightUpgradableMenu menu)) {
            return;
        }

        AirtightUpgrade upgrade = AirtightUpgrade.getByID(id);
        if (upgrade == null) {
            return;
        }

        if (install) {
            menu.getMenuInventory().setStackInSlot(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX, ItemStack.EMPTY);
            menu.installUpgrade(upgrade);
            menu.slots.get(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX + Inventory.INVENTORY_SIZE).setChanged();
        } else {
            menu.toggleUpgrade(upgrade);
        }
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AIRTIGHT_UPGRADE;
    }
}
