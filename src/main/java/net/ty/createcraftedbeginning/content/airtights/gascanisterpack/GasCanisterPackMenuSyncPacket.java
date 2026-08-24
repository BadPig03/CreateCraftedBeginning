package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasCanisterPackMenuSyncPacket(int containerId, int slot, ItemStack canister) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, GasCanisterPackMenuSyncPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, GasCanisterPackMenuSyncPacket::containerId, ByteBufCodecs.VAR_INT, GasCanisterPackMenuSyncPacket::slot, ItemStack.OPTIONAL_STREAM_CODEC, GasCanisterPackMenuSyncPacket::canister, GasCanisterPackMenuSyncPacket::new);

    public GasCanisterPackMenuSyncPacket {
        canister = canister.copy();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        if (!(player.containerMenu instanceof GasCanisterPackMenu menu) || menu.containerId != containerId) {
            return;
        }

        menu.updateCanister(slot, canister);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.GAS_CANISTER_PACK_MENU_SYNC;
    }
}
