package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import org.jetbrains.annotations.NotNull;

public record GasCanisterPackMenuSyncPacket(int slot, long amount) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, GasCanisterPackMenuSyncPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, GasCanisterPackMenuSyncPacket::slot, ByteBufCodecs.VAR_LONG, GasCanisterPackMenuSyncPacket::amount, GasCanisterPackMenuSyncPacket::new);

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(@NotNull LocalPlayer player) {
        if (!(player.containerMenu instanceof GasCanisterPackMenu menu)) {
            return;
        }

        menu.updateCanister(slot, amount);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.GAS_CANISTER_PACK_MENU_SYNC;
    }
}
