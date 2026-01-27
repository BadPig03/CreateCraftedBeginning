package net.ty.createcraftedbeginning.api.gas.cansiters;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.registry.CCBPackets;

public record CanisterContainerClientPacket(GasStack gasContent) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, CanisterContainerClientPacket> STREAM_CODEC = StreamCodec.composite(GasStack.OPTIONAL_STREAM_CODEC, CanisterContainerClientPacket::gasContent, CanisterContainerClientPacket::new);

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        if (gasContent.isEmpty()) {
            return;
        }

        CanisterContainerConsumers.interactContainer(player, gasContent.getGasType(), gasContent().getAmount(), () -> player.level().isClientSide);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.CANISTER_CONTAINER_CLIENT;
    }
}
