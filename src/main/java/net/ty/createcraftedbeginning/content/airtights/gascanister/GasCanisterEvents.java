package net.ty.createcraftedbeginning.content.airtights.gascanister;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerClients;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.cansiters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.api.gas.cansiters.events.GasTypeChangedEvent;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.creativegascanister.CreativeGasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackContainerContents;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class GasCanisterEvents {

    @SubscribeEvent
    public static void onPlayerPostTick(@NotNull Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Gas currentGasType = CanisterContainerSuppliers.getFirstAvailableGasContent(player).getGasType();
        Gas storedGasType = CanisterContainerClients.getStoredGasType(player);
        if (currentGasType != storedGasType) {
            NBTHelper.writeResourceLocation(player.getPersistentData(), CanisterContainerClients.COMPOUND_KEY_STORED_GAS_TYPE, currentGasType.getResourceLocation());
            NeoForge.EVENT_BUS.post(new GasTypeChangedEvent(player, currentGasType, storedGasType));
        }

        IGasCanisterContainer container = CanisterContainerSuppliers.getFirstCanisterSupplier(player);
        if (container == null) {
            CatnipServices.NETWORK.sendToClient(serverPlayer, new GasCanisterOverlayPacket(GasStack.EMPTY, -1, -1));
            return;
        }

        int packType;
        if (container instanceof GasCanisterPackContainerContents packContents) {
            packType = packContents.getContainer().getOrDefault(CCBDataComponents.GAS_CANISTER_PACK_FLAGS, 0);
        }
        else if (container instanceof CreativeGasCanisterContainerContents) {
            packType = -2;
        }
        else {
            packType = -1;
        }

        Pair<GasStack, Long> pair = CanisterContainerSuppliers.getFirstCanisterSupplierPair(player);
        GasStack content = pair.getFirst();
        long capacity = pair.getSecond();
        if (content.isEmpty() || capacity == 0) {
            CatnipServices.NETWORK.sendToClient(serverPlayer, new GasCanisterOverlayPacket(GasStack.EMPTY, 0, packType));
            return;
        }

        CatnipServices.NETWORK.sendToClient(serverPlayer, new GasCanisterOverlayPacket(content, capacity, packType));
    }
}
