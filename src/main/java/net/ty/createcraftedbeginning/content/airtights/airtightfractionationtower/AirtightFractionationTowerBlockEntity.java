package net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.api.packager.InventoryIdentifier.Single;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.logistics.GasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandler;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.UnaryOperator;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightFractionationTowerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, GasInventoryIdentifierProvider {
    private final AirtightFractionationTowerSerialization serialization;
    private final AirtightFractionationTowerStructureManager structureManager;
    private final AirtightFractionationTowerTooltipBuilder tooltipBuilder;
    private final AirtightFractionationTowerCrafting crafting;
    private @Nullable BlockPos origin;
    private int height;
    private AirtightFractionationTowerInventory inventory;

    public AirtightFractionationTowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        structureManager = new AirtightFractionationTowerStructureManager(this);
        serialization = new AirtightFractionationTowerSerialization(this);
        tooltipBuilder = new AirtightFractionationTowerTooltipBuilder(this);
        crafting = new AirtightFractionationTowerCrafting(this);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.AIRTIGHT_FRACTIONATION_TOWER.get(), (be, direction) -> be.getItemCapability());
        event.registerBlockEntity(FluidHandler.BLOCK, CCBBlockEntities.AIRTIGHT_FRACTIONATION_TOWER.get(), (be, direction) -> be.getFluidCapability());
        event.registerBlockEntity(GasCapabilities.BLOCK, CCBBlockEntities.AIRTIGHT_FRACTIONATION_TOWER.get(), (be, direction) -> be.getGasCapability());
    }

    public static void transformStructureNbt(CompoundTag tag, UnaryOperator<BlockPos> transform) {
        AirtightFractionationTowerSerialization.transformStructureNbt(tag, transform);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        inventory = new AirtightFractionationTowerInventory(this);
        behaviours.add(inventory);
    }

    @Override
    public void initialize() {
        super.initialize();
        invalidateLayerCapabilities();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || !isTowerController()) {
            return;
        }

        structureManager.tick();
        crafting.tick();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateLayerCapabilities();
    }

    @Override
    public InventoryIdentifier getGasInventoryIdentifier(Direction direction) {
        if (origin == null) {
            return new Single(worldPosition);
        }

        return new Single(origin.offset(1, worldPosition.getY() - origin.getY(), 1));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return tooltipBuilder.addToGoggleTooltip(tooltip);
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        serialization.write(compoundTag, provider);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        serialization.read(compoundTag, provider, clientPacket);
        super.read(compoundTag, provider, clientPacket);
    }

    public float getRecipeTemperature() {
        AirtightFractionationTowerBlockEntity controller = findTowerController();
        if (controller == null) {
            return AirtightThermoregulatorHandler.NONE;
        }

        return controller.structureManager.getTemperature();
    }

    public AirtightFractionationTowerMode getProcessingMode() {
        AirtightFractionationTowerBlockEntity controller = findTowerController();
        if (controller == null) {
            return AirtightFractionationTowerMode.NONE;
        }

        return controller.structureManager.getProcessingMode();
    }

    public boolean isPartOfSameTower(AirtightFractionationTowerBlockEntity other) {
        return origin != null && origin.equals(other.origin) && height == other.height && findTowerController() != null;
    }

    AirtightFractionationTowerStructureManager getStructureManager() {
        return structureManager;
    }

    AirtightFractionationTowerCrafting getCrafting() {
        return crafting;
    }

    AirtightFractionationTowerInventory getInventory() {
        return inventory;
    }

    boolean isTowerController() {
        return origin != null && isLayerController() && worldPosition.getY() == origin.getY();
    }

    boolean isLayerController() {
        return origin != null && worldPosition.getX() == origin.getX() + 1 && worldPosition.getZ() == origin.getZ() + 1;
    }

    void invalidateLayerCapabilities() {
        if (level == null) {
            return;
        }

        if (origin == null) {
            invalidateCapabilities();
            return;
        }

        BlockPos min = origin.above(worldPosition.getY() - origin.getY());
        for (BlockPos pos : BlockPos.betweenClosed(min, min.offset(2, 0, 2))) {
            if (!level.isLoaded(pos)) {
                continue;
            }

            level.invalidateCapabilities(pos);
        }
    }

    @Nullable BlockPos getOrigin() {
        return origin;
    }

    int getHeight() {
        return height;
    }

    void assemble(BlockPos origin, int height) {
        loadStructure(origin, height);
        inventory.assemble();
        structureManager.tick();
        invalidateCapabilities();
        notifyUpdate();
    }

    void clearStructure() {
        inventory.clear();
        invalidateLayerCapabilities();
        loadStructure(null, 0);
        setChanged();
    }

    void loadStructure(@Nullable BlockPos origin, int height) {
        this.origin = null;
        this.height = 0;
        structureManager.clear();
        crafting.clear();
        if (origin == null || height < AirtightFractionationTowerBlock.MIN_HEIGHT || height > AirtightFractionationTowerBlock.MAX_HEIGHT) {
            return;
        }

        int x = worldPosition.getX() - origin.getX();
        int y = worldPosition.getY() - origin.getY();
        int z = worldPosition.getZ() - origin.getZ();
        int width = AirtightFractionationTowerBlock.WIDTH;
        if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= width) {
            return;
        }

        this.origin = origin.immutable();
        this.height = height;
    }

    @Nullable AirtightFractionationTowerBlockEntity findTowerController() {
        if (origin == null || isRemoved()) {
            return null;
        }

        if (isTowerController()) {
            return this;
        }

        if (level == null) {
            return null;
        }

        BlockPos controllerPos = origin.offset(1, 0, 1);
        if (!level.isLoaded(controllerPos) || !(level.getBlockEntity(controllerPos) instanceof AirtightFractionationTowerBlockEntity controller) || controller.isRemoved() || !origin.equals(controller.origin) || height != controller.height) {
            return null;
        }

        return controller;
    }

    @Nullable IItemHandler getItemCapability() {
        AirtightFractionationTowerBlockEntity controller = findLayerController();
        if (controller == null) {
            return null;
        }

        return controller.inventory.getItems();
    }

    @Nullable IFluidHandler getFluidCapability() {
        AirtightFractionationTowerBlockEntity controller = findLayerController();
        if (controller == null) {
            return null;
        }

        return controller.inventory.getFluids();
    }

    @Nullable GasStorageHandler getGasCapability() {
        AirtightFractionationTowerBlockEntity controller = findLayerController();
        if (controller == null) {
            return null;
        }

        return controller.inventory.getGases();
    }

    private @Nullable AirtightFractionationTowerBlockEntity findLayerController() {
        if (origin == null || level == null || isRemoved()) {
            return null;
        }

        BlockPos center = origin.offset(1, worldPosition.getY() - origin.getY(), 1);
        if (!level.isLoaded(center) || !(level.getBlockEntity(center) instanceof AirtightFractionationTowerBlockEntity controller) || controller.isRemoved() || !origin.equals(controller.origin) || height != controller.height) {
            return null;
        }

        return controller;
    }
}
