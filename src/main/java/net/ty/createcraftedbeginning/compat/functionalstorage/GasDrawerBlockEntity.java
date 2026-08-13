package net.ty.createcraftedbeginning.compat.functionalstorage;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.FunctionalStorage.DrawerType;
import com.buuz135.functionalstorage.block.config.FunctionalStorageConfig;
import com.buuz135.functionalstorage.block.tile.ControllableDrawerTile;
import com.buuz135.functionalstorage.block.tile.DrawerProperties;
import com.buuz135.functionalstorage.item.FSAttachments;
import com.buuz135.functionalstorage.item.component.SizeProvider;
import com.hrznstudio.titanium.annotation.Save;
import com.hrznstudio.titanium.block.BasicTileBlock;
import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.compat.functionalstorage.client.GasDrawerInfoGuiAddon;
import net.ty.createcraftedbeginning.compat.functionalstorage.registry.CCBFunctionalStorageBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasDrawerBlockEntity extends ControllableDrawerTile<GasDrawerBlockEntity> {
    private static final int STORAGE_UPGRADE_SLOTS = 4;
    private static final long BASE_TOTAL_GAS_CAPACITY = 64 * GasAmountUtils.MILLIBUCKETS_PER_BUCKET;
    private static final double BASE_STORAGE_MULTIPLIER = DrawerType.X_1.getSlotAmount();

    private final DrawerType drawerType;
    private final GasDrawerHandler gasHandler;
    @Save
    private final GasDrawerStorage gasStorage;
    @Save
    private final GasDrawerFilter gasFilter;
    private boolean transactionActive;
    private boolean transactionDirty;

    public GasDrawerBlockEntity(BasicTileBlock<GasDrawerBlockEntity> base, BlockEntityType<GasDrawerBlockEntity> blockEntityType, BlockPos pos, BlockState state, DrawerType drawerType) {
        super(base, blockEntityType, pos, state, new DrawerProperties(drawerType.getSlotAmount(), FSAttachments.FLUID_STORAGE_MODIFIER));
        this.drawerType = drawerType;
        gasFilter = new GasDrawerFilter(drawerType.getSlots());
        gasHandler = new GasDrawerHandler(this, drawerType.getSlots(), slot -> new GasDrawerTank(calculateTankCapacity(getStorageMultiplier()), this, stack -> matchesLockedFilter(slot, stack)));
        gasStorage = new GasDrawerStorage(gasHandler);
        getUtilityUpgrades().setInputFilter((stack, slot) -> stack.is(FunctionalStorage.PUSHING_UPGRADE.get()) || stack.is(FunctionalStorage.PULLING_UPGRADE.get()) || stack.is(FunctionalStorage.VOID_UPGRADE.get()));
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBFunctionalStorageBlockEntities.GAS_DRAWER_1.get(), (drawer, side) -> drawer.gasHandler);
        event.registerBlockEntity(GasHandler.BLOCK, CCBFunctionalStorageBlockEntities.GAS_DRAWER_2.get(), (drawer, side) -> drawer.gasHandler);
        event.registerBlockEntity(GasHandler.BLOCK, CCBFunctionalStorageBlockEntities.GAS_DRAWER_4.get(), (drawer, side) -> drawer.gasHandler);
    }

    private static long calculateTankCapacity(double storageMultiplier) {
        double capacity = storageMultiplier / BASE_STORAGE_MULTIPLIER * BASE_TOTAL_GAS_CAPACITY;
        if (Double.isNaN(capacity) || capacity <= 0) {
            return 0;
        }
        if (Double.isInfinite(capacity) || capacity >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return (long) Math.floor(capacity);
    }

    private static void restoreRemainder(IGasCanisterContainer canister, int tank, GasStack drained, long inserted) {
        if (inserted >= drained.getAmount()) {
            return;
        }

        canister.fill(tank, drained.copyWithAmount(drained.getAmount() - inserted), GasAction.EXECUTE);
    }

    private static void restoreRemainder(GasDrawerTank tank, GasStack drained, long inserted) {
        if (inserted >= drained.getAmount()) {
            return;
        }

        tank.fill(drained.copyWithAmount(drained.getAmount() - inserted), GasAction.EXECUTE);
    }

    private static boolean fillCanister(GasDrawerTank tank, IGasCanisterContainer canister) {
        GasStack available = tank.drain(Long.MAX_VALUE, GasAction.SIMULATE);
        if (available.isEmpty()) {
            return false;
        }

        for (int targetTank = 0; targetTank < canister.getTanks(); targetTank++) {
            long accepted = canister.fill(targetTank, available, GasAction.SIMULATE);
            if (accepted <= 0) {
                continue;
            }

            GasStack drained = tank.drain(available.copyWithAmount(accepted), GasAction.EXECUTE);
            if (drained.isEmpty()) {
                continue;
            }

            long inserted = canister.fill(targetTank, drained, GasAction.EXECUTE);
            restoreRemainder(tank, drained, inserted);
            canister.save();
            return inserted > 0;
        }
        return false;
    }

    public DrawerType getDrawerType() {
        return drawerType;
    }

    public GasDrawerHandler getGasHandler() {
        return gasHandler;
    }

    public RenderGas getRenderGas(int slot) {
        GasStack stored = gasHandler.getInternalTank(slot).getStoredStack();
        if (!stored.isEmpty()) {
            return new RenderGas(getVisibleStack(stored), false);
        }

        GasStack filter = gasFilter.get(slot);
        if (!isLocked() || filter.isEmpty()) {
            return RenderGas.EMPTY;
        }

        return new RenderGas(filter, true);
    }

    private GasStack getVisibleStack(GasStack stack) {
        if (!isCreative()) {
            return stack;
        }
        return stack.copyWithAmount(Long.MAX_VALUE);
    }

    private boolean matchesLockedFilter(int slot, GasStack stack) {
        if (!isLocked()) {
            return true;
        }

        GasStack filter = gasFilter.get(slot);
        return !filter.isEmpty() && GasStack.isSameGasSameComponents(filter, stack);
    }

    public long getPhysicalTankCapacity() {
        return calculateTankCapacity(getStorageMultiplier());
    }

    private void updateTankCapacities() {
        long capacity = getPhysicalTankCapacity();
        for (GasDrawerTank tank : gasHandler.getInternalTanks()) {
            tank.setCapacity(capacity);
        }
    }

    private boolean canChangeMultiplier(double storageMultiplier) {
        long capacity = calculateTankCapacity(storageMultiplier);
        for (GasDrawerTank tank : gasHandler.getInternalTanks()) {
            if (tank.getStoredStack().getAmount() <= capacity) {
                continue;
            }

            return false;
        }
        return true;
    }

    private boolean storageUpgradeFits(int slot, ItemStack replacement) {
        if (replacement.is(FunctionalStorage.CREATIVE_UPGRADE.get())) {
            return true;
        }
        if (!replacement.has(FSAttachments.FLUID_STORAGE_MODIFIER)) {
            return false;
        }

        ItemStack[] replacements = new ItemStack[getStorageUpgrades().getSlots()];
        ItemStack single = replacement.copy();
        single.setCount(1);
        replacements[slot] = single;
        float newSize = SizeProvider.calculateAsFactor(getStorageUpgrades(), FSAttachments.FLUID_STORAGE_MODIFIER, baseSize, replacements);
        return canChangeMultiplier(newSize);
    }

    private boolean interactWithCanister(int slot, IGasCanisterContainer canister) {
        GasDrawerTank tank = gasHandler.getInternalTank(slot);
        return fillFromCanister(slot, tank, canister) || fillCanister(tank, canister);
    }

    private boolean fillFromCanister(int slot, GasDrawerTank tank, IGasCanisterContainer canister) {
        for (int sourceTank = 0; sourceTank < canister.getTanks(); sourceTank++) {
            GasStack source = canister.getGasInTank(sourceTank);
            if (source.isEmpty()) {
                continue;
            }

            boolean claimedFilter = claimLockedFilter(slot, tank, source);
            long accepted = tank.fill(source, GasAction.SIMULATE);
            if (accepted <= 0) {
                releaseLockedFilter(slot, claimedFilter);
                continue;
            }

            GasStack drained = canister.drain(sourceTank, source.copyWithAmount(accepted), GasAction.EXECUTE);
            if (drained.isEmpty()) {
                releaseLockedFilter(slot, claimedFilter);
                continue;
            }

            long inserted = tank.fill(drained, GasAction.EXECUTE);
            restoreRemainder(canister, sourceTank, drained, inserted);
            if (inserted <= 0) {
                releaseLockedFilter(slot, claimedFilter);
            }
            canister.save();
            return inserted > 0;
        }
        return false;
    }

    private boolean claimLockedFilter(int slot, GasDrawerTank tank, GasStack source) {
        if (!isLocked() || !tank.getStoredStack().isEmpty() || !gasFilter.get(slot).isEmpty()) {
            return false;
        }

        gasFilter.set(slot, source);
        syncGasFilter();
        return true;
    }

    private void releaseLockedFilter(int slot, boolean claimedFilter) {
        if (!claimedFilter) {
            return;
        }

        gasFilter.set(slot, GasStack.EMPTY);
        syncGasFilter();
    }

    private void syncGasFilter() {
        syncObject(gasFilter);
        markDirty();
    }

    @Override
    public void loadAdditional(CompoundTag compound, Provider provider) {
        super.loadAdditional(compound, provider);
        updateTankCapacities();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initClient() {
        super.initClient();
        addGuiAddonFactory(() -> new GasDrawerInfoGuiAddon(64, 16, this));
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state, GasDrawerBlockEntity drawer) {
        super.serverTick(level, pos, state, drawer);
        if (level.getGameTime() % (long) FunctionalStorageConfig.UPGRADE_TICK != 0) {
            return;
        }

        processUtilityUpgrades(level);
    }

    @Override
    public InteractionResult onSlotActivated(Player player, InteractionHand hand, Direction facing, double hitX, double hitY, double hitZ, int slot) {
        ItemStack held = player.getItemInHand(hand);
        if (slot < 0 || held.isEmpty()) {
            return super.onSlotActivated(player, hand, facing, hitX, hitY, hitZ, slot);
        }

        IGasCanisterContainer canister = held.getCapability(GasHandler.ITEM);
        if (canister == null || !interactWithCanister(slot, canister)) {
            return super.onSlotActivated(player, hand, facing, hitX, hitY, hitZ, slot);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public int getStorageSlotAmount() {
        return STORAGE_UPGRADE_SLOTS;
    }

    @Override
    public void setLocked(boolean locked) {
        super.setLocked(locked);
        if (!locked) {
            gasFilter.clear();
            syncGasFilter();
            return;
        }

        for (int slot = 0; slot < gasFilter.size(); slot++) {
            gasFilter.set(slot, gasHandler.getInternalTank(slot).getStoredStack());
        }
        syncGasFilter();
    }

    @Override
    public boolean isEverythingEmpty() {
        return !isLocked() && gasHandler.isEmpty() && super.isEverythingEmpty();
    }

    @Override
    public InventoryComponent<ControllableDrawerTile<GasDrawerBlockEntity>> getStorageUpgradesConstructor() {
        return new InventoryComponent<ControllableDrawerTile<GasDrawerBlockEntity>>("storage_upgrades", 10, 70, STORAGE_UPGRADE_SLOTS) {
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (!canExtractStorageUpgrade(this, slot)) {
                    return ItemStack.EMPTY;
                }
                return super.extractItem(slot, amount, simulate);
            }

            private boolean canExtractStorageUpgrade(InventoryComponent<ControllableDrawerTile<GasDrawerBlockEntity>> upgrades, int slot) {
                if (isStorageUpgradeLocked()) {
                    return false;
                }

                ItemStack current = upgrades.getStackInSlot(slot);
                if (!current.has(FSAttachments.FLUID_STORAGE_MODIFIER)) {
                    return true;
                }

                ItemStack[] replacements = new ItemStack[upgrades.getSlots()];
                replacements[slot] = ItemStack.EMPTY;
                float newSize = SizeProvider.calculateAsFactor(upgrades, FSAttachments.FLUID_STORAGE_MODIFIER, baseSize, replacements);
                return canChangeMultiplier(newSize);
            }
        }.setInputFilter((stack, slot) -> !isStorageUpgradeLocked() && storageUpgradeFits(slot, stack)).setOnSlotChanged((stack, slot) -> onStorageUpgradeChanged()).setSlotLimit(1);
    }

    private void processUtilityUpgrades(Level level) {
        for (int slot = 0; slot < getUtilityUpgrades().getSlots(); slot++) {
            ItemStack upgrade = getUtilityUpgrades().getStackInSlot(slot);
            if (upgrade.is(FunctionalStorage.PUSHING_UPGRADE.get())) {
                GasDrawerTransfer.push(level, this, upgrade);
                continue;
            }
            if (upgrade.is(FunctionalStorage.PULLING_UPGRADE.get())) {
                GasDrawerTransfer.pull(level, this, upgrade);
            }
        }
    }

    private void onStorageUpgradeChanged() {
        setNeedsUpgradeCache(true);
        updateTankCapacities();
        syncObject(gasStorage);
        markDirty();
    }

    @Override
    public GasDrawerBlockEntity getSelf() {
        return this;
    }

    @Override
    public void syncObject(Object object) {
        if (level == null) {
            return;
        }

        super.syncObject(object);
    }

    void beginTransaction() {
        if (transactionActive) {
            throw new IllegalStateException("Nested Gas Drawer transactions are not supported");
        }

        transactionActive = true;
        transactionDirty = false;
    }

    void endTransaction(boolean commit) {
        boolean dirty = transactionDirty;
        transactionActive = false;
        transactionDirty = false;
        if (!commit || !dirty) {
            return;
        }

        markDirty();
        syncObject(gasStorage);
    }

    void onGasChanged() {
        if (transactionActive) {
            transactionDirty = true;
            return;
        }

        markDirty();
        syncObject(gasStorage);
    }

    private void markDirty() {
        setChanged();
        if (level == null) {
            return;
        }

        level.blockEntityChanged(worldPosition);
    }

    public record RenderGas(GasStack stack, boolean filterOnly) {
        private static final RenderGas EMPTY = new RenderGas(GasStack.EMPTY, false);

        public boolean isEmpty() {
            return stack.isEmpty();
        }
    }
}
