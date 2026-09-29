package net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower;

import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseCause;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.config.CCBMachines.AirtightFractionationTower;
import net.ty.createcraftedbeginning.gas.behaviour.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.gas.release.GasReleaseRequest;
import net.ty.createcraftedbeginning.recipe.FractionationTowerCraftPlanner;
import net.ty.createcraftedbeginning.recipe.FractionationTowerOutput;
import net.ty.createcraftedbeginning.recipe.transaction.MachineResourceSnapshots;
import net.ty.createcraftedbeginning.recipe.transaction.MachineResourceSnapshots.FluidTankSnapshot;
import net.ty.createcraftedbeginning.recipe.transaction.MachineResourceSnapshots.GasTankSnapshot;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightFractionationTowerInventory extends BlockEntityBehaviour {
    private static final int MAX_SAVED_SLOTS = 64;

    private static final BehaviourType<AirtightFractionationTowerInventory> TYPE = new BehaviourType<>();
    private static final BehaviourType<SmartFluidTankBehaviour> FLUID_TYPE = new BehaviourType<>("LayerFluid");
    private static final BehaviourType<SmartGasTankBehaviour> GAS_TYPE = new BehaviourType<>("LayerGas");
    private static final String COMPOUND_KEY_INVENTORY = "LayerInventory";
    private static final String COMPOUND_KEY_ITEMS = "Items";
    private static final String COMPOUND_KEY_ITEM_SLOTS = "ItemSlots";
    private static final String COMPOUND_KEY_FLUID_TANKS = "FluidTanks";
    private static final String COMPOUND_KEY_GAS_TANKS = "GasTanks";
    private static final String COMPOUND_KEY_FLUID_CAPACITY = "FluidCapacity";
    private static final String COMPOUND_KEY_GAS_VOLUME = "GasVolume";

    private final AirtightFractionationTowerBlockEntity owner;
    private @Nullable StorageSize size;
    private @Nullable SmartInventory items;
    private @Nullable SmartFluidTankBehaviour fluids;
    private @Nullable SmartGasTankBehaviour gases;
    private boolean initialized;

    AirtightFractionationTowerInventory(AirtightFractionationTowerBlockEntity owner) {
        super(owner);
        this.owner = owner;
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public void tick() {
        if (fluids == null || gases == null) {
            return;
        }

        if (!initialized) {
            initialized = true;
            fluids.initialize();
            gases.initialize();
        }
        fluids.tick();
        gases.tick();
    }

    @Override
    public void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (!owner.isLayerController()) {
            clear();
            return;
        }

        CompoundTag inventoryTag = compoundTag.getCompound(COMPOUND_KEY_INVENTORY);
        StorageSize loadedSize = new StorageSize(inventoryTag.getInt(COMPOUND_KEY_ITEM_SLOTS), inventoryTag.getInt(COMPOUND_KEY_FLUID_TANKS), inventoryTag.getInt(COMPOUND_KEY_GAS_TANKS), inventoryTag.getInt(COMPOUND_KEY_FLUID_CAPACITY), inventoryTag.getLong(COMPOUND_KEY_GAS_VOLUME));
        if (!loadedSize.isValid()) {
            clear();
            return;
        }

        configure(loadedSize);
        if (items != null) {
            items.deserializeNBT(provider, inventoryTag.getCompound(COMPOUND_KEY_ITEMS));
        }
        if (fluids != null) {
            fluids.read(inventoryTag, provider, clientPacket);
        }
        if (gases == null) {
            return;
        }

        gases.read(inventoryTag, provider, clientPacket);
    }

    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (size == null || items == null || fluids == null || gases == null) {
            return;
        }

        CompoundTag inventoryTag = new CompoundTag();
        inventoryTag.putInt(COMPOUND_KEY_ITEM_SLOTS, size.itemSlots());
        inventoryTag.putInt(COMPOUND_KEY_FLUID_TANKS, size.fluidTanks());
        inventoryTag.putInt(COMPOUND_KEY_GAS_TANKS, size.gasTanks());
        inventoryTag.putInt(COMPOUND_KEY_FLUID_CAPACITY, size.fluidCapacity());
        inventoryTag.putLong(COMPOUND_KEY_GAS_VOLUME, size.gasVolume());
        inventoryTag.put(COMPOUND_KEY_ITEMS, items.serializeNBT(provider));
        fluids.write(inventoryTag, provider, clientPacket);
        gases.write(inventoryTag, provider, clientPacket);
        compoundTag.put(COMPOUND_KEY_INVENTORY, inventoryTag);
    }

    @Nullable SmartInventory getItems() {
        return items;
    }

    @Nullable FractionationTowerCraftPlanner createPlanner() {
        if (items == null || fluids == null || gases == null) {
            return null;
        }

        return new FractionationTowerCraftPlanner(items, fluids.getCapability(), gases.getCapability());
    }

    Snapshot snapshot(Provider provider) {
        if (items == null || fluids == null || gases == null) {
            throw new IllegalStateException("Cannot snapshot an unassembled fractionation tower layer at " + owner.getBlockPos() + '.');
        }

        return new Snapshot(MachineResourceSnapshots.copyItems(items), MachineResourceSnapshots.snapshotFluidTanks(provider, fluids), MachineResourceSnapshots.snapshotGasTanks(gases));
    }

    boolean transferOutput(FractionationTowerOutput output, boolean simulate) {
        if (items == null || fluids == null || gases == null) {
            return false;
        }

        FractionationTowerCraftPlanner planner = new FractionationTowerCraftPlanner(items, fluids.getCapability(), gases.getCapability());
        items.allowInsertion();
        fluids.allowInsertion();
        gases.allowInsertion();
        try {
            return planner.transferOutput(output, simulate);
        }
        finally {
            updateInsertionPermissions();
        }
    }

    void restore(Provider provider, Snapshot snapshot) {
        if (items == null || fluids == null || gases == null) {
            throw new IllegalStateException("Cannot restore an unassembled fractionation tower layer at " + owner.getBlockPos() + '.');
        }

        MachineResourceSnapshots.restoreItems(items, snapshot.items());
        MachineResourceSnapshots.restoreFluidTanks(provider, snapshot.fluids(), fluids);
        MachineResourceSnapshots.restoreGasTanks(snapshot.gases(), gases);
        owner.notifyUpdate();
    }

    @Nullable IFluidHandler getFluids() {
        if (fluids == null) {
            return null;
        }

        return fluids.getCapability();
    }

    @Nullable GasStorageHandler getGases() {
        if (gases == null) {
            return null;
        }

        return gases.getCapability();
    }

    void assemble() {
        if (!owner.isLayerController()) {
            return;
        }

        AirtightFractionationTower config = CCBConfig.server().machines.airtightFractionationTower;
        configure(new StorageSize(config.itemSlotsPerLayer.get(), config.fluidTanksPerLayer.get(), config.gasTanksPerLayer.get(), config.fluidCapacityPerTank.get() * FluidType.BUCKET_VOLUME, config.gasVolumePerTank.get() * GasUnits.LITERS_PER_KILOLITER));
    }

    List<GasReleaseRequest> dropItemsAndCollectGasReleases(Level level, BlockPos dropPos) {
        if (level.isClientSide) {
            return List.of();
        }

        if (items != null) {
            ItemHelper.dropContents(level, dropPos, items);
        }
        if (gases == null) {
            return List.of();
        }

        List<GasReleaseRequest> releases = new ArrayList<>();
        GasStorageHandler handler = gases.getCapability();
        gases.beginMutation();
        try {
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                GasPressureCompartment compartment = handler.getPressureCompartment(tank);
                long storedAmount = compartment.getStoredAmount();
                if (storedAmount <= 0) {
                    continue;
                }

                long sourcePressurePa = compartment.getPressurePa();
                GasStack releasedGas = compartment.drain(storedAmount, GasAction.EXECUTE);
                if (releasedGas.isEmpty()) {
                    continue;
                }

                releases.add(GasReleaseRequest.radial(releasedGas, owner.getBlockPos(), GasReleaseCause.TANK_REMOVAL, sourcePressurePa));
            }
        }
        finally {
            gases.endMutation();
        }
        return releases;
    }

    void clear() {
        if (items != null) {
            for (int slot = 0; slot < items.getSlots(); slot++) {
                items.setStackInSlot(slot, ItemStack.EMPTY);
            }
            items.forbidInsertion().forbidExtraction();
        }
        if (fluids != null) {
            IFluidHandler handler = fluids.getCapability();
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                handler.drain(handler.getFluidInTank(tank).copy(), FluidAction.EXECUTE);
            }
            fluids.forbidInsertion().forbidExtraction();
        }
        if (gases != null) {
            GasStorageHandler handler = gases.getCapability();
            gases.beginMutation();
            try {
                for (int tank = 0; tank < handler.getTanks(); tank++) {
                    handler.getPressureCompartment(tank).drain(Long.MAX_VALUE, GasAction.EXECUTE);
                }
            }
            finally {
                gases.endMutation();
            }
            gases.forbidInsertion().forbidExtraction();
        }
        items = null;
        fluids = null;
        gases = null;
        size = null;
        initialized = false;
    }

    private void configure(StorageSize newSize) {
        if (newSize.equals(size)) {
            updateInsertionPermissions();
            return;
        }

        clear();
        size = newSize;
        items = new SmartInventory(newSize.itemSlots(), owner);
        fluids = new SmartFluidTankBehaviour(FLUID_TYPE, owner, newSize.fluidTanks(), newSize.fluidCapacity(), true).whenFluidUpdates(owner::setChanged);
        gases = new SmartGasTankBehaviour(GAS_TYPE, owner, newSize.gasTanks(), newSize.gasVolume(), GasPressureLimits.HARD_PRESSURE_PA, true).whenTankUpdates(owner::setChanged);
        updateInsertionPermissions();
        owner.invalidateLayerCapabilities();
    }

    private void updateInsertionPermissions() {
        if (items == null || fluids == null || gases == null) {
            return;
        }

        BlockState state = owner.getBlockState();
        if (state.getValue(AirtightFractionationTowerBlock.TOP) || state.getValue(AirtightFractionationTowerBlock.BOTTOM)) {
            items.allowInsertion();
            fluids.allowInsertion();
            gases.allowInsertion();
            return;
        }

        items.forbidInsertion();
        fluids.forbidInsertion();
        gases.forbidInsertion();
    }

    record Snapshot(List<ItemStack> items, FluidTankSnapshot fluids, GasTankSnapshot gases) {}

    private record StorageSize(int itemSlots, int fluidTanks, int gasTanks, int fluidCapacity, long gasVolume) {
        private boolean isValid() {
            return itemSlots > 0 && itemSlots <= MAX_SAVED_SLOTS && fluidTanks > 0 && fluidTanks <= MAX_SAVED_SLOTS && gasTanks > 0 && gasTanks <= MAX_SAVED_SLOTS && fluidCapacity > 0 && gasVolume > 0;
        }
    }
}
