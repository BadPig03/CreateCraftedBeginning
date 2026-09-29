package net.ty.createcraftedbeginning.compat.curios;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerSuppliers.SupplyOrder;
import net.ty.createcraftedbeginning.registry.CCBItems;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.event.SlotModifiersUpdatedEvent;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CuriosCompat {
    public static final String GAS_CANISTER_PACK_SLOT = "ccb_gas_canister_pack";
    private static final ResourceLocation ID = CCBAPI.asResource("gas_canister_pack");

    private CuriosCompat() {
    }

    public static void register() {
        CuriosApi.registerCurioPredicate(ID, result -> result.stack().is(CCBItems.GAS_CANISTER_PACK));
        CanisterContainerSuppliers.addCanisterContainerSuppliers(CuriosCompat::getEquippedPacks, SupplyOrder.SLOT_ORDER);
        NeoForge.EVENT_BUS.addListener(CuriosCompat::onCurioChanged);
        NeoForge.EVENT_BUS.addListener(CuriosCompat::onSlotModifiersUpdated);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(CuriosCapability.ITEM, (stack, context) -> new GasCanisterPackCurio(stack), CCBItems.GAS_CANISTER_PACK.get());
    }

    private static List<GasCanisterContainer> getEquippedPacks(Player player) {
        ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) {
            return List.of();
        }

        ICurioStacksHandler slotHandler = inventory.getStacksHandler(GAS_CANISTER_PACK_SLOT).orElse(null);
        if (slotHandler == null) {
            return List.of();
        }

        IDynamicStackHandler stacks = slotHandler.getStacks();
        List<Boolean> activeStates = slotHandler.getActiveStates();
        List<GasCanisterContainer> containers = new ArrayList<>();
        for (int slot = 0; slot < stacks.getSlots(); slot++) {
            ItemStack stack = stacks.getStackInSlot(slot);
            if (slot >= activeStates.size() || !activeStates.get(slot) || !stack.is(CCBItems.GAS_CANISTER_PACK)) {
                continue;
            }

            GasCanisterContainer container = stack.getCapability(CanisterCapabilities.ITEM);
            if (container == null) {
                continue;
            }

            containers.add(container);
        }
        return containers;
    }

    private static void onCurioChanged(CurioChangeEvent event) {
        if (!GAS_CANISTER_PACK_SLOT.equals(event.getIdentifier()) || !(event.getEntity() instanceof Player player)) {
            return;
        }

        CanisterContainerSuppliers.invalidateCache(player);
    }

    private static void onSlotModifiersUpdated(SlotModifiersUpdatedEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        CanisterContainerSuppliers.invalidateCache(player);
    }
}
