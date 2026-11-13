package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import com.simibubi.create.foundation.gui.menu.MenuBase;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasCanisterPackMenu extends MenuBase<ItemStack> {
    static final int I_SLOT_INDEX = SlotType.I.getIndex();
    static final int II_SLOT_INDEX = SlotType.II.getIndex();
    static final int III_SLOT_INDEX = SlotType.III.getIndex();
    static final int IV_SLOT_INDEX = SlotType.IV.getIndex();
    private static final int PLAYER_INVENTORY_SLOTS = Inventory.INVENTORY_SIZE;

    private static final int MAX_COUNT = 4;

    private PackItemHandler packInventory;

    public GasCanisterPackMenu(int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(CCBMenuTypes.GAS_CANISTER_PACK_MENU.get(), id, inv, extraData);
    }

    private GasCanisterPackMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    GasCanisterPackMenu(MenuType<?> type, int id, Inventory inv, ItemStack contentHolder) {
        super(type, id, inv, contentHolder);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected ItemStack createOnClient(RegistryFriendlyByteBuf extraData) {
        return ItemStack.STREAM_CODEC.decode(extraData);
    }

    @Override
    protected void initAndReadInventory(ItemStack pack) {
        if (!(pack.getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterPackContainerContents packContents)) {
            return;
        }

        packInventory = new PackItemHandler();
        for (int canisterSlot = 0; canisterSlot < MAX_COUNT; canisterSlot++) {
            packInventory.setStackInSlot(canisterSlot, packContents.getCanister(canisterSlot));
        }
    }

    @Override
    protected void addSlots() {
        addPlayerSlots(20, 166);
        addSlot(SlotType.createCanisterSlot(packInventory, I_SLOT_INDEX, 23));
        addSlot(SlotType.createCanisterSlot(packInventory, II_SLOT_INDEX, 65));
        addSlot(SlotType.createCanisterSlot(packInventory, III_SLOT_INDEX, 107));
        addSlot(SlotType.createCanisterSlot(packInventory, IV_SLOT_INDEX, 149));
    }

    @Override
    protected void saveData(ItemStack pack) {
        if (!(pack.getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterPackContainerContents packContents)) {
            return;
        }

        List<ItemStack> storedCanisters = new ArrayList<>(MAX_COUNT);
        for (int canisterSlot = 0; canisterSlot < MAX_COUNT; canisterSlot++) {
            storedCanisters.add(packInventory.getStackInSlot(canisterSlot).copy());
        }
        packContents.replaceCanisters(storedCanisters);
        CanisterContainerSuppliers.invalidateCache(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return ItemStack.isSameItem(playerInventory.getSelected(), contentHolder);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot sourceSlot = slots.get(slotIndex);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack movedStack = sourceSlot.getItem();
        ItemStack originalStack = movedStack.copy();
        if (slotIndex >= PLAYER_INVENTORY_SLOTS) {
            if (!moveItemStackTo(movedStack, 0, PLAYER_INVENTORY_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }
        else if (!moveItemStackTo(movedStack, PLAYER_INVENTORY_SLOTS, PLAYER_INVENTORY_SLOTS + 4, false)) {
            return ItemStack.EMPTY;
        }

        if (movedStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        }
        else {
            sourceSlot.setChanged();
        }
        saveData(contentHolder);
        return originalStack;
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (clickType == ClickType.SWAP && dragType == playerInventory.selected) {
            return;
        }

        int selectedHotbarSlotId = playerInventory.selected + PLAYER_INVENTORY_SLOTS - 9;
        if (slotId == selectedHotbarSlotId && clickType != ClickType.THROW) {
            return;
        }

        super.clicked(slotId, dragType, clickType, player);
        saveData(contentHolder);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slotIn) {
        return slotIn.container == playerInventory;
    }

    @Override
    public void setItem(int slotId, int stateId, ItemStack stack) {
        super.setItem(slotId, stateId, stack);
        saveData(contentHolder);
    }

    @Override
    public boolean canDragTo(Slot slotIn) {
        return slotIn.container == playerInventory;
    }

    public void updateCanister(int slotIndex, ItemStack canister) {
        if (slotIndex < 0 || slotIndex >= MAX_COUNT || !(canister.getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterContainerContents)) {
            return;
        }

        packInventory.setStackInSlot(slotIndex, canister.copy());
    }

    IItemHandler getPackInventory() {
        return packInventory;
    }

    private enum SlotType {
        I(0),
        II(1),
        III(2),
        IV(3);

        private final int index;

        SlotType(int index) {
            this.index = index;
        }

        private static SlotItemHandler createCanisterSlot(IItemHandler itemHandler, int slotIndex, int x) {
            return new SlotItemHandler(itemHandler, slotIndex, x, 89) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterContainerContents;
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return 1;
                }
            };
        }

        private int getIndex() {
            return index;
        }
    }

    private static final class PackItemHandler extends ItemStackHandler {
        private PackItemHandler() {
            super(MAX_COUNT);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    }
}
