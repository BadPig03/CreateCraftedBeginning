package net.ty.createcraftedbeginning.content.airtights.gascanister;

import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.api.gascanisters.events.GasTypeChangedEvent;
import net.ty.createcraftedbeginning.content.airtights.creativegascanister.CreativeGasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClients;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackContainerContents;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
final class GasCanisterEvents {
    private static final Map<Player, OverlayState> LAST_OVERLAY_STATES = new WeakHashMap<>();

    private GasCanisterEvents() {
    }

    @SubscribeEvent
    private static void onPlayerPostTick(Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        OverlaySelection overlaySelection = findFirstAvailable(CanisterContainerSuppliers.getAllSuppliers(player));
        Gas currentGasType = overlaySelection.content().getGasType();
        Gas storedGasType = CanisterContainerClients.getStoredGasType(player);
        if (currentGasType != storedGasType) {
            NBTHelper.writeResourceLocation(player.getPersistentData(), CanisterContainerClients.COMPOUND_KEY_STORED_GAS_TYPE, currentGasType.getResourceLocation());
            NeoForge.EVENT_BUS.post(new GasTypeChangedEvent(player, currentGasType, storedGasType));
        }

        syncOverlay(serverPlayer, overlaySelection.content(), overlaySelection.capacity(), overlaySelection.packType(), overlaySelection.creative());
    }

    @SubscribeEvent
    private static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        LAST_OVERLAY_STATES.remove(event.getEntity());
    }

    private static OverlaySelection findFirstAvailable(List<IGasCanisterContainer> containers) {
        for (IGasCanisterContainer container : containers) {
            for (int tankIndex = 0; tankIndex < container.getTanks(); tankIndex++) {
                GasStack storedGas = container.getGasInTank(tankIndex);
                long tankCapacity = container.getTankCapacity(tankIndex);
                if (storedGas.isEmpty() || tankCapacity <= 0) {
                    continue;
                }

                return createSelection(container, tankIndex, storedGas, tankCapacity);
            }
        }
        return OverlaySelection.EMPTY;
    }

    private static OverlaySelection createSelection(IGasCanisterContainer container, int tankIndex, GasStack storedGas, long tankCapacity) {
        int packType = -1;
        boolean isCreative = container instanceof CreativeGasCanisterContainerContents;
        if (container instanceof GasCanisterPackContainerContents packContents) {
            packType = packContents.getContainer().getOrDefault(CCBDataComponents.GAS_CANISTER_PACK_FLAGS, 0);
            isCreative = packContents.isCreative(tankIndex);
        }
        else if (isCreative) {
            packType = -2;
        }

        return new OverlaySelection(storedGas, tankCapacity, packType, isCreative);
    }

    private static void syncOverlay(ServerPlayer player, GasStack content, long capacity, int packType, boolean creative) {
        OverlayState previousOverlayState = LAST_OVERLAY_STATES.get(player);
        if (previousOverlayState != null && previousOverlayState.matches(content, capacity, packType, creative)) {
            return;
        }

        OverlayState overlayState = new OverlayState(content, capacity, packType, creative);
        LAST_OVERLAY_STATES.put(player, overlayState);
        CatnipServices.NETWORK.sendToClient(player, new GasCanisterOverlayPacket(overlayState.content().copy(), overlayState.capacity(), overlayState.packType(), overlayState.creative()));
    }

    private record OverlaySelection(GasStack content, long capacity, int packType, boolean creative) {
        private static final OverlaySelection EMPTY = new OverlaySelection(GasStack.EMPTY, -1, -1, false);

        private OverlaySelection {
            content = content.copy();
        }
    }

    private record OverlayState(GasStack content, long capacity, int packType, boolean creative) {
        private OverlayState {
            content = content.copy();
        }

        private boolean matches(GasStack currentContent, long currentCapacity, int currentPackType, boolean currentCreative) {
            return capacity == currentCapacity && packType == currentPackType && creative == currentCreative && GasStack.matches(content, currentContent);
        }
    }
}
