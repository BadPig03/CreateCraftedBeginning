package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
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
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AirtightUpgradeMenuSyncPacket(int containerId, List<AirtightUpgradeStatus> statuses, ItemStack upgradeStack) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, AirtightUpgradeMenuSyncPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, AirtightUpgradeMenuSyncPacket::containerId, CatnipStreamCodecBuilders.list(AirtightUpgradeStatus.STREAM_CODEC), AirtightUpgradeMenuSyncPacket::statuses, ItemStack.OPTIONAL_STREAM_CODEC, AirtightUpgradeMenuSyncPacket::upgradeStack, AirtightUpgradeMenuSyncPacket::new);

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        if (!(player.containerMenu instanceof AirtightUpgradableMenu menu) || menu.containerId != containerId) {
            return;
        }

        menu.applyServerState(statuses, upgradeStack);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AIRTIGHT_UPGRADE_MENU_SYNC;
    }
}
