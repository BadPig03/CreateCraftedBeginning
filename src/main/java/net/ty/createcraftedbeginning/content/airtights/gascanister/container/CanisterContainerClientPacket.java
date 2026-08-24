package net.ty.createcraftedbeginning.content.airtights.gascanister.container;

import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record CanisterContainerClientPacket(List<InventoryStackSync> updates) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, CanisterContainerClientPacket> STREAM_CODEC = StreamCodec.composite(CatnipStreamCodecBuilders.list(InventoryStackSync.STREAM_CODEC), CanisterContainerClientPacket::updates, CanisterContainerClientPacket::new);

    public CanisterContainerClientPacket {
        updates = updates.stream().map(InventoryStackSync::copy).toList();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        if (updates.isEmpty()) {
            return;
        }

        Inventory inventory = player.getInventory();
        boolean inventoryChanged = false;
        for (InventoryStackSync stackUpdate : updates) {
            if (stackUpdate.slot() < 0 || stackUpdate.slot() >= inventory.getContainerSize()) {
                continue;
            }

            ItemStack currentStack = inventory.getItem(stackUpdate.slot());
            if (ItemStack.matches(currentStack, stackUpdate.stack()) || !ItemStack.matches(currentStack, stackUpdate.expectedStack())) {
                continue;
            }

            inventory.setItem(stackUpdate.slot(), stackUpdate.stack().copy());
            inventoryChanged = true;
        }
        if (inventoryChanged) {
            inventory.setChanged();
        }
        CanisterContainerSuppliers.invalidateCache(player);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.CANISTER_CONTAINER_CLIENT;
    }

    public record InventoryStackSync(int slot, ItemStack expectedStack, ItemStack stack) {
        private static final StreamCodec<RegistryFriendlyByteBuf, InventoryStackSync> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, InventoryStackSync::slot, ItemStack.OPTIONAL_STREAM_CODEC, InventoryStackSync::expectedStack, ItemStack.OPTIONAL_STREAM_CODEC, InventoryStackSync::stack, InventoryStackSync::new);

        public InventoryStackSync {
            expectedStack = expectedStack.copy();
            stack = stack.copy();
        }

        private InventoryStackSync copy() {
            return new InventoryStackSync(slot, expectedStack, stack);
        }
    }
}
