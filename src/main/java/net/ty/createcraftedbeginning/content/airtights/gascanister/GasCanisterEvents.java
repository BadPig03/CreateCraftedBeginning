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
import net.ty.createcraftedbeginning.content.airtights.airtightextendarm.AirtightExtendArmUtils;
import net.ty.createcraftedbeginning.content.airtights.creativegascanister.CreativeGasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClients;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers.AffordableFuel;
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
public class GasCanisterEvents {
    private static final Map<Player, OverlayState> LAST_OVERLAY_STATES = new WeakHashMap<>();

    @SubscribeEvent
    public static void onPlayerPostTick(Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        OverlaySelection selection = findCurrentOverlay(player);
        Gas currentGasType = selection.content().getGasType();
        Gas storedGasType = CanisterContainerClients.getStoredGasType(player);
        if (currentGasType != storedGasType) {
            NBTHelper.writeResourceLocation(player.getPersistentData(), CanisterContainerClients.COMPOUND_KEY_STORED_GAS_TYPE, currentGasType.getResourceLocation());
            NeoForge.EVENT_BUS.post(new GasTypeChangedEvent(player, currentGasType, storedGasType));
        }

        syncOverlay(serverPlayer, selection.content(), selection.capacity(), selection.packType(), selection.creative());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        LAST_OVERLAY_STATES.remove(event.getEntity());
    }

    private static OverlaySelection findCurrentOverlay(Player player) {
        List<IGasCanisterContainer> containers = CanisterContainerSuppliers.getAllSuppliers(player);
        Gas preferredGas = AirtightExtendArmUtils.getCurrentFuelSelection(player).map(AffordableFuel::gasType).orElse(GasStack.EMPTY.getGasType());

        if (!preferredGas.isEmpty()) {
            OverlaySelection preferred = findFirstMatching(containers, preferredGas);
            if (!preferred.content().isEmpty()) {
                return preferred;
            }
        }
        return findFirstAvailable(containers);
    }

    private static OverlaySelection findFirstAvailable(List<IGasCanisterContainer> containers) {
        for (IGasCanisterContainer container : containers) {
            for (int tank = 0; tank < container.getTanks(); tank++) {
                GasStack content = container.getGasInTank(tank);
                long capacity = container.getTankCapacity(tank);
                if (content.isEmpty() || capacity <= 0) {
                    continue;
                }

                return createSelection(container, tank, content, capacity);
            }
        }
        return OverlaySelection.EMPTY;
    }

    private static OverlaySelection findFirstMatching(List<IGasCanisterContainer> containers, Gas gasType) {
        for (IGasCanisterContainer container : containers) {
            for (int tank = 0; tank < container.getTanks(); tank++) {
                GasStack content = container.getGasInTank(tank);
                long capacity = container.getTankCapacity(tank);
                if (content.isEmpty() || !content.is(gasType) || capacity <= 0) {
                    continue;
                }

                return createSelection(container, tank, content, capacity);
            }
        }
        return OverlaySelection.EMPTY;
    }

    private static OverlaySelection createSelection(IGasCanisterContainer container, int tank, GasStack content, long capacity) {
        int packType = -1;
        boolean isCreative = container instanceof CreativeGasCanisterContainerContents;
        if (container instanceof GasCanisterPackContainerContents packContents) {
            packType = packContents.getContainer().getOrDefault(CCBDataComponents.GAS_CANISTER_PACK_FLAGS, 0);
            isCreative = packContents.getCreatives(tank);
        }
        else if (isCreative) {
            packType = -2;
        }

        return new OverlaySelection(content, capacity, packType, isCreative);
    }

    private static void syncOverlay(ServerPlayer player, GasStack content, long capacity, int packType, boolean creative) {
        OverlayState previous = LAST_OVERLAY_STATES.get(player);
        if (previous != null && previous.matches(content, capacity, packType, creative)) {
            return;
        }

        OverlayState state = new OverlayState(content, capacity, packType, creative);
        LAST_OVERLAY_STATES.put(player, state);
        CatnipServices.NETWORK.sendToClient(player, new GasCanisterOverlayPacket(state.content().copy(), state.capacity(), state.packType(), state.creative()));
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
