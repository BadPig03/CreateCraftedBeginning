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
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
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
    private static final long BASE_TOTAL_GAS_CAPACITY = 64 * GasAmounts.MILLIBUCKETS_PER_BUCKET;
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
        event.registerBlockEntity(GasHandler.BLOCK, CCBFunctionalStorageBlockEntities.GAS_DRAWER_1.get(), (drawer, ignoredDirection) -> drawer.gasHandler);
        event.registerBlockEntity(GasHandler.BLOCK, CCBFunctionalStorageBlockEntities.GAS_DRAWER_2.get(), (drawer, ignoredDirection) -> drawer.gasHandler);
        event.registerBlockEntity(GasHandler.BLOCK, CCBFunctionalStorageBlockEntities.GAS_DRAWER_4.get(), (drawer, ignoredDirection) -> drawer.gasHandler);
    }

    private static long calculateTankCapacity(double storageMultiplier) {
        double scaledCapacity = storageMultiplier / BASE_STORAGE_MULTIPLIER * BASE_TOTAL_GAS_CAPACITY;
        if (Double.isNaN(scaledCapacity) || scaledCapacity <= 0) {
            return 0;
        }
        if (Double.isInfinite(scaledCapacity) || scaledCapacity >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return (long) Math.floor(scaledCapacity);
    }

    private static void restoreRemainder(IGasCanisterContainer canister, int canisterTank, GasStack drainedGas, long insertedAmount) {
        if (insertedAmount >= drainedGas.getAmount()) {
            return;
        }

        canister.fill(canisterTank, drainedGas.copyWithAmount(drainedGas.getAmount() - insertedAmount), GasAction.EXECUTE);
    }

    private static void restoreRemainder(GasDrawerTank drawerTank, GasStack drainedGas, long insertedAmount) {
        if (insertedAmount >= drainedGas.getAmount()) {
            return;
        }

        drawerTank.fill(drainedGas.copyWithAmount(drainedGas.getAmount() - insertedAmount), GasAction.EXECUTE);
    }

    private static boolean fillCanister(GasDrawerTank drawerTank, IGasCanisterContainer canister) {
        GasStack availableGas = drawerTank.drain(Long.MAX_VALUE, GasAction.SIMULATE);
        if (availableGas.isEmpty()) {
            return false;
        }

        for (int targetTank = 0; targetTank < canister.getTanks(); targetTank++) {
            long acceptedAmount = canister.fill(targetTank, availableGas, GasAction.SIMULATE);
            if (acceptedAmount <= 0) {
                continue;
            }

            GasStack drainedGas = drawerTank.drain(availableGas.copyWithAmount(acceptedAmount), GasAction.EXECUTE);
            if (drainedGas.isEmpty()) {
                continue;
            }

            long insertedAmount = canister.fill(targetTank, drainedGas, GasAction.EXECUTE);
            restoreRemainder(drawerTank, drainedGas, insertedAmount);
            canister.save();
            return insertedAmount > 0;
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
        GasStack storedGas = gasHandler.getInternalTank(slot).getStoredStack();
        if (!storedGas.isEmpty()) {
            return new RenderGas(getVisibleStack(storedGas), false);
        }

        GasStack filterGas = gasFilter.get(slot);
        if (!isLocked() || filterGas.isEmpty()) {
            return RenderGas.EMPTY;
        }

        return new RenderGas(filterGas, true);
    }

    private GasStack getVisibleStack(GasStack storedGas) {
        if (!isCreative()) {
            return storedGas;
        }
        return storedGas.copyWithAmount(Long.MAX_VALUE);
    }

    private boolean matchesLockedFilter(int slot, GasStack gasStack) {
        if (!isLocked()) {
            return true;
        }

        GasStack filterGas = gasFilter.get(slot);
        return !filterGas.isEmpty() && GasStack.isSameGasSameComponents(filterGas, gasStack);
    }

    public long getPhysicalTankCapacity() {
        return calculateTankCapacity(getStorageMultiplier());
    }

    private void updateTankCapacities() {
        long tankCapacity = getPhysicalTankCapacity();
        for (GasDrawerTank tank : gasHandler.getInternalTanks()) {
            tank.setCapacity(tankCapacity);
        }
    }

    private boolean canChangeMultiplier(double storageMultiplier) {
        long tankCapacity = calculateTankCapacity(storageMultiplier);
        for (GasDrawerTank tank : gasHandler.getInternalTanks()) {
            if (tank.getStoredStack().getAmount() <= tankCapacity) {
                continue;
            }

            return false;
        }
        return true;
    }

    private boolean storageUpgradeFits(int slot, ItemStack replacementUpgrade) {
        if (replacementUpgrade.is(FunctionalStorage.CREATIVE_UPGRADE.get())) {
            return true;
        }
        if (!replacementUpgrade.has(FSAttachments.FLUID_STORAGE_MODIFIER)) {
            return false;
        }

        ItemStack[] upgradeReplacements = new ItemStack[getStorageUpgrades().getSlots()];
        ItemStack singleReplacement = replacementUpgrade.copy();
        singleReplacement.setCount(1);
        upgradeReplacements[slot] = singleReplacement;
        float newStorageMultiplier = SizeProvider.calculateAsFactor(getStorageUpgrades(), FSAttachments.FLUID_STORAGE_MODIFIER, baseSize, upgradeReplacements);
        return canChangeMultiplier(newStorageMultiplier);
    }

    private boolean interactWithCanister(int slot, IGasCanisterContainer canister) {
        GasDrawerTank drawerTank = gasHandler.getInternalTank(slot);
        return fillFromCanister(slot, drawerTank, canister) || fillCanister(drawerTank, canister);
    }

    private boolean fillFromCanister(int slot, GasDrawerTank drawerTank, IGasCanisterContainer canister) {
        for (int canisterTank = 0; canisterTank < canister.getTanks(); canisterTank++) {
            GasStack canisterGas = canister.getGasInTank(canisterTank);
            if (canisterGas.isEmpty()) {
                continue;
            }

            boolean claimedLockedFilter = claimLockedFilter(slot, drawerTank, canisterGas);
            long acceptedAmount = drawerTank.fill(canisterGas, GasAction.SIMULATE);
            if (acceptedAmount <= 0) {
                releaseLockedFilter(slot, claimedLockedFilter);
                continue;
            }

            GasStack drainedGas = canister.drain(canisterTank, canisterGas.copyWithAmount(acceptedAmount), GasAction.EXECUTE);
            if (drainedGas.isEmpty()) {
                releaseLockedFilter(slot, claimedLockedFilter);
                continue;
            }

            long insertedAmount = drawerTank.fill(drainedGas, GasAction.EXECUTE);
            restoreRemainder(canister, canisterTank, drainedGas, insertedAmount);
            if (insertedAmount <= 0) {
                releaseLockedFilter(slot, claimedLockedFilter);
            }
            canister.save();
            return insertedAmount > 0;
        }
        return false;
    }

    private boolean claimLockedFilter(int slot, GasDrawerTank drawerTank, GasStack sourceGas) {
        if (!isLocked() || !drawerTank.getStoredStack().isEmpty() || !gasFilter.get(slot).isEmpty()) {
            return false;
        }

        gasFilter.set(slot, sourceGas);
        syncGasFilter();
        return true;
    }

    private void releaseLockedFilter(int slot, boolean claimedLockedFilter) {
        if (!claimedLockedFilter) {
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
        ItemStack heldStack = player.getItemInHand(hand);
        if (slot < 0 || heldStack.isEmpty()) {
            return super.onSlotActivated(player, hand, facing, hitX, hitY, hitZ, slot);
        }

        IGasCanisterContainer gasCanister = heldStack.getCapability(GasHandler.ITEM);
        if (gasCanister == null || !interactWithCanister(slot, gasCanister)) {
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

                if (!upgrades.getStackInSlot(slot).has(FSAttachments.FLUID_STORAGE_MODIFIER)) {
                    return true;
                }

                ItemStack[] upgradeReplacements = new ItemStack[upgrades.getSlots()];
                upgradeReplacements[slot] = ItemStack.EMPTY;
                float newStorageMultiplier = SizeProvider.calculateAsFactor(upgrades, FSAttachments.FLUID_STORAGE_MODIFIER, baseSize, upgradeReplacements);
                return canChangeMultiplier(newStorageMultiplier);
            }
        }.setInputFilter((stack, slot) -> !isStorageUpgradeLocked() && storageUpgradeFits(slot, stack)).setOnSlotChanged((stack, slot) -> onStorageUpgradeChanged()).setSlotLimit(1);
    }

    private void processUtilityUpgrades(Level level) {
        for (int upgradeSlot = 0; upgradeSlot < getUtilityUpgrades().getSlots(); upgradeSlot++) {
            ItemStack upgrade = getUtilityUpgrades().getStackInSlot(upgradeSlot);
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

    public void beginTransaction() {
        if (transactionActive) {
            throw new IllegalStateException("Nested Gas Drawer transactions are not supported");
        }

        transactionActive = true;
        transactionDirty = false;
    }

    public void endTransaction(boolean commit) {
        boolean hadChanges = transactionDirty;
        transactionActive = false;
        transactionDirty = false;
        if (!commit || !hadChanges) {
            return;
        }

        markDirty();
        syncObject(gasStorage);
    }

    public void onGasChanged() {
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
