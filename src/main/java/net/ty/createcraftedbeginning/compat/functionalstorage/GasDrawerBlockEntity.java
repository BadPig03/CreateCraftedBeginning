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
import net.minecraft.util.Mth;
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
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.compat.functionalstorage.client.GasDrawerInfoGuiAddon;
import net.ty.createcraftedbeginning.compat.functionalstorage.registry.CCBFunctionalStorageBlockEntities;
import net.ty.createcraftedbeginning.gas.storage.GasTankLimits;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasDrawerBlockEntity extends ControllableDrawerTile<GasDrawerBlockEntity> {
    private static final int STORAGE_UPGRADE_SLOTS = 4;
    private static final long BASE_TOTAL_GAS_VOLUME = 64 * GasUnits.LITERS_PER_KILOLITER;
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
        gasHandler = new GasDrawerHandler(this, drawerType.getSlots(), slot -> new GasDrawerTank(calculateTankLimits(getStorageMultiplier()), this, stack -> matchesLockedFilter(slot, stack)));
        gasStorage = new GasDrawerStorage(gasHandler);
        getUtilityUpgrades().setInputFilter((stack, slot) -> stack.is(FunctionalStorage.PUSHING_UPGRADE.get()) || stack.is(FunctionalStorage.PULLING_UPGRADE.get()) || stack.is(FunctionalStorage.VOID_UPGRADE.get()));
    }

    @Override
    public void loadAdditional(CompoundTag compound, Provider provider) {
        super.loadAdditional(compound, provider);
        updateTankLimits();
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

        GasCanisterContainer gasCanister = heldStack.getCapability(CanisterCapabilities.ITEM);
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

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasCapabilities.BLOCK, CCBFunctionalStorageBlockEntities.GAS_DRAWER_1.get(), (drawer, ignoredDirection) -> drawer.gasHandler);
        event.registerBlockEntity(GasCapabilities.BLOCK, CCBFunctionalStorageBlockEntities.GAS_DRAWER_2.get(), (drawer, ignoredDirection) -> drawer.gasHandler);
        event.registerBlockEntity(GasCapabilities.BLOCK, CCBFunctionalStorageBlockEntities.GAS_DRAWER_4.get(), (drawer, ignoredDirection) -> drawer.gasHandler);
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

    public long getTankVolume() {
        return calculateTankVolume(getStorageMultiplier());
    }

    public void beginTransaction() {
        if (transactionActive) {
            throw new IllegalStateException("Nested gas drawer transactions are not supported.");
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

    private static long calculateTankVolume(double storageMultiplier) {
        double scaledVolume = storageMultiplier / BASE_STORAGE_MULTIPLIER * BASE_TOTAL_GAS_VOLUME;
        if (Double.isNaN(scaledVolume) || scaledVolume <= 0) {
            return 0;
        }

        if (Double.isInfinite(scaledVolume) || scaledVolume >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        return Mth.lfloor(scaledVolume);
    }

    private static GasTankLimits calculateTankLimits(double storageMultiplier) {
        return GasTankLimits.atReferencePressure(calculateTankVolume(storageMultiplier));
    }

    private static boolean fillCanister(GasDrawerTank drawerTank, GasCanisterContainer canister) {
        for (int targetTank = 0; targetTank < canister.getTanks(); targetTank++) {
            long transferredAmount = GasDrawerTransfer.transferDrawerToCanister(drawerTank, canister, targetTank, Long.MAX_VALUE);
            if (transferredAmount <= 0) {
                continue;
            }

            canister.save();
            return true;
        }
        return false;
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

    private void updateTankLimits() {
        GasTankLimits limits = calculateTankLimits(getStorageMultiplier());
        beginTransaction();
        try {
            gasHandler.reconfigure(limits);
        }
        finally {
            endTransaction(false);
        }
    }

    private boolean canChangeMultiplier(double storageMultiplier) {
        GasTankLimits limits = calculateTankLimits(storageMultiplier);
        for (GasDrawerTank tank : gasHandler.getInternalTanks()) {
            if (tank.canContain(limits, tank.getStoredStack())) {
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

    private boolean interactWithCanister(int slot, GasCanisterContainer canister) {
        GasDrawerTank drawerTank = gasHandler.getInternalTank(slot);
        return fillFromCanister(slot, drawerTank, canister) || fillCanister(drawerTank, canister);
    }

    private boolean fillFromCanister(int slot, GasDrawerTank drawerTank, GasCanisterContainer canister) {
        for (int canisterTank = 0; canisterTank < canister.getTanks(); canisterTank++) {
            GasStack canisterGas = canister.getGasInTank(canisterTank);
            if (canisterGas.isEmpty()) {
                continue;
            }

            boolean claimedLockedFilter = claimLockedFilter(slot, drawerTank, canisterGas);
            long transferredAmount = GasDrawerTransfer.transferCanisterToDrawer(canister, canisterTank, drawerTank, drawerTank, Long.MAX_VALUE);
            if (transferredAmount <= 0) {
                releaseLockedFilter(slot, claimedLockedFilter);
                continue;
            }

            canister.save();
            return true;
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

    private void processUtilityUpgrades(Level level) {
        for (int upgradeSlot = 0; upgradeSlot < getUtilityUpgrades().getSlots(); upgradeSlot++) {
            ItemStack upgrade = getUtilityUpgrades().getStackInSlot(upgradeSlot);
            if (upgrade.is(FunctionalStorage.PUSHING_UPGRADE.get())) {
                GasDrawerTransfer.push(level, this, upgrade);
                continue;
            }

            if (!upgrade.is(FunctionalStorage.PULLING_UPGRADE.get())) {
                continue;
            }

            GasDrawerTransfer.pull(level, this, upgrade);
        }
    }

    private void onStorageUpgradeChanged() {
        setNeedsUpgradeCache(true);
        updateTankLimits();
        syncObject(gasStorage);
        markDirty();
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
