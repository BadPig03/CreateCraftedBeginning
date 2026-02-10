package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import com.simibubi.create.foundation.gui.menu.MenuBase;
import net.minecraft.client.HotbarManager;
import net.minecraft.nbt.CompoundTag;
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
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackOverrides.GasCanisterPackType;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;

public class GasCanisterPackMenu extends MenuBase<ItemStack> {
    public static final int I_SLOT_INDEX = SlotType.I.getIndex();
    public static final int II_SLOT_INDEX = SlotType.II.getIndex();
    public static final int III_SLOT_INDEX = SlotType.III.getIndex();
    public static final int IV_SLOT_INDEX = SlotType.IV.getIndex();
    public static final int PLAYER_INVENTORY_SLOTS = Inventory.INVENTORY_SIZE;

    private static final String COMPOUND_KEY_CANISTER = "Canister";
    private static final int MAX_COUNT = 4;
    private static final int PLAYER_SLOT_X = 20;
    private static final int PLAYER_SLOT_Y = 166;
    private static final int SLOT_Y = 89;
    private static final int I_SLOT_X = 23;
    private static final int II_SLOT_X = 65;
    private static final int III_SLOT_X = 107;
    private static final int IV_SLOT_X = 149;

    protected PackItemHandler packInventory;

    public GasCanisterPackMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public GasCanisterPackMenu(MenuType<?> type, int id, Inventory inv, ItemStack contentHolder) {
        super(type, id, inv, contentHolder);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected ItemStack createOnClient(RegistryFriendlyByteBuf extraData) {
        return ItemStack.STREAM_CODEC.decode(extraData);
    }

    @Override
    protected void initAndReadInventory(@NotNull ItemStack pack) {
        if (!(pack.getCapability(GasHandler.ITEM) instanceof GasCanisterPackContainerContents packContents)) {
            return;
        }

        packInventory = new PackItemHandler();
        for (int i = 0; i < MAX_COUNT; i++) {
            ItemStack canister = ItemStack.parseOptional(player.level().registryAccess(), packContents.getCompoundTag(i).getCompound(COMPOUND_KEY_CANISTER));
            if (canister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents) {
                canisterContents.drain(0, canisterContents.getGasInTank(0), GasAction.EXECUTE);
                canisterContents.setCapacity(0, GasCanisterContainerContents.getEnchantedCapacity(canister));
                canisterContents.fill(0, packContents.getGasInTank(i), GasAction.EXECUTE);
            }
            packInventory.setStackInSlot(i, canister);
        }
    }

    @Override
    protected void addSlots() {
        addPlayerSlots(PLAYER_SLOT_X, PLAYER_SLOT_Y);
        addSlot(SlotType.getSlot(packInventory, I_SLOT_INDEX, I_SLOT_X));
        addSlot(SlotType.getSlot(packInventory, II_SLOT_INDEX, II_SLOT_X));
        addSlot(SlotType.getSlot(packInventory, III_SLOT_INDEX, III_SLOT_X));
        addSlot(SlotType.getSlot(packInventory, IV_SLOT_INDEX, IV_SLOT_X));
    }

    @Override
    protected void saveData(@NotNull ItemStack pack) {
        if (!(pack.getCapability(GasHandler.ITEM) instanceof GasCanisterPackContainerContents packContents)) {
            return;
        }

        for (int i = 0; i < MAX_COUNT; i++) {
            ItemStack canister = packInventory.getStackInSlot(i);
            if (canister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents) {
                packContents.setCapacity(i, canisterContents.getTankCapacity(0));
                packContents.drain(i, packContents.getGasInTank(i), GasAction.EXECUTE);
                packContents.fill(i, canisterContents.getGasInTank(0), GasAction.EXECUTE);
            }
            else {
                packContents.drain(i, packContents.getGasInTank(i), GasAction.EXECUTE);
                packContents.setCapacity(i, 0);
            }
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.put(COMPOUND_KEY_CANISTER, canister.saveOptional(player.level().registryAccess()));
            packContents.setCompoundTag(i, compoundTag);
            packContents.setCreatives(i, CanisterContainerSuppliers.isValidCreativeGasCanister(canister));
        }
        pack.set(CCBDataComponents.GAS_CANISTER_PACK_FLAGS, getPackType());
    }

    @Override
    public boolean stillValid(Player player) {
        return ItemStack.isSameItem(playerInventory.getSelected(), contentHolder);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack slotStack = slot.getItem();
        ItemStack originalStack = slotStack.copy();
        if (slotIndex >= PLAYER_INVENTORY_SLOTS) {
            if (!moveItemStackTo(slotStack, 0, PLAYER_INVENTORY_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }
        else {
            if (!moveItemStackTo(slotStack, PLAYER_INVENTORY_SLOTS, PLAYER_INVENTORY_SLOTS + 4, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        }
        else {
            slot.setChanged();
        }

        saveData(contentHolder);
        return originalStack;
    }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player player) {
        if (slotId == playerInventory.selected + PLAYER_INVENTORY_SLOTS - HotbarManager.NUM_HOTBAR_GROUPS && clickType != ClickType.THROW) {
            return;
        }

        super.clicked(slotId, dragType, clickType, player);
        saveData(contentHolder);
    }

    @Override
    public boolean canTakeItemForPickAll(@NotNull ItemStack stack, @NotNull Slot slotIn) {
        return slotIn.container == playerInventory;
    }

    @Override
    public void setItem(int slotId, int stateId, @NotNull ItemStack stack) {
        super.setItem(slotId, stateId, stack);
        saveData(contentHolder);
    }

    @Override
    public boolean canDragTo(@NotNull Slot slotIn) {
        return slotIn.container == playerInventory;
    }

    public void updateCanister(int slot, long amount) {
        ItemStack canister = packInventory.getStackInSlot(slot);
        if (!(canister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return;
        }

        canisterContents.drain(0, amount, GasAction.EXECUTE);
        packInventory.setStackInSlot(slot, canister);
    }

    protected int getPackType() {
        ItemStack firstSlot = packInventory.getStackInSlot(I_SLOT_INDEX);
        boolean rightDown = CanisterContainerSuppliers.isValidGasCanister(firstSlot) || CanisterContainerSuppliers.isValidCreativeGasCanister(firstSlot);

        ItemStack secondSlot = packInventory.getStackInSlot(II_SLOT_INDEX);
        boolean leftDown = CanisterContainerSuppliers.isValidGasCanister(secondSlot) || CanisterContainerSuppliers.isValidCreativeGasCanister(secondSlot);

        ItemStack thirdSlot = packInventory.getStackInSlot(III_SLOT_INDEX);
        boolean rightUp = CanisterContainerSuppliers.isValidGasCanister(thirdSlot) || CanisterContainerSuppliers.isValidCreativeGasCanister(thirdSlot);

        ItemStack fourthSlot = packInventory.getStackInSlot(IV_SLOT_INDEX);
        boolean leftUp = CanisterContainerSuppliers.isValidGasCanister(fourthSlot) || CanisterContainerSuppliers.isValidCreativeGasCanister(fourthSlot);
        int flags = GasCanisterPackOverrides.calculateFlags(leftUp, rightUp, leftDown, rightDown);
        return GasCanisterPackType.getTypeFromFlags(flags).ordinal();
    }

    protected enum SlotType {
        I(0),
        II(1),
        III(2),
        IV(3);

        private final int index;

        SlotType(int index) {
            this.index = index;
        }

        private static @NotNull SlotItemHandler getSlot(IItemHandler itemHandler, int index, int x) {
            return new SlotItemHandler(itemHandler, index, x, SLOT_Y) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return stack.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents;
                }

                @Override
                public int getMaxStackSize(@NotNull ItemStack stack) {
                    return 1;
                }
            };
        }

        private int getIndex() {
            return index;
        }
    }

    protected static class PackItemHandler extends ItemStackHandler {
        public PackItemHandler() {
            super(MAX_COUNT);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    }
}
