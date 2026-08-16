package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.compat.computercraft.events.PackageEvent;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase.InterfaceProvider;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasManipulationBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IGasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasPackagerBlockEntity extends PackagerBlockEntity implements Clearable {
    private static final ItemStackHandler EMPTY_GAS_INVENTORY_HANDLER = new ItemStackHandler(0);

    protected final GasPackagerPendingGas pendingGas;
    protected final GasPackagerController controller;
    protected GasManipulationBehaviour gasInventory;

    public GasPackagerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        pendingGas = new GasPackagerPendingGas();
        controller = new GasPackagerController(this, new GasPackagerInventoryTracker(), pendingGas);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.GAS_PACKAGER.get(), (be, context) -> be.inventory);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        gasInventory = new GasManipulationBehaviour(this, InterfaceProvider.oppositeOfBlockFacing()).withFilter(GasPackagerUtils::supportsGasHandler);
        behaviours.add(gasInventory);

        targetInventory = new InvManipulationBehaviour(this, InterfaceProvider.oppositeOfBlockFacing()).withFilter(GasPackagerUtils::supportsItemHandler);
        behaviours.add(targetInventory);

        behaviours.add(new CCBAdvancementBehaviour(this));
        computerBehaviour = ComputerCraftProxy.behaviour(this);
        behaviours.add(computerBehaviour);
    }

    @Override
    public void tick() {
        boolean shouldInsertGas = level != null && !level.isClientSide() && animationInward && animationTicks == 1 && !pendingGas.isEmpty();
        super.tick();
        if (!shouldInsertGas) {
            return;
        }

        controller.performPendingGasInsertion();
        setChanged();
    }

    @Override
    public InventorySummary getAvailableItems() {
        return controller.getAvailableItems();
    }

    @Override
    public boolean unwrapBox(ItemStack box, boolean simulate) {
        return controller.unwrapBox(box, simulate);
    }

    @Override
    public void attemptToSend(@Nullable List<PackagingRequest> queuedRequests) {
        if (queuedRequests == null) {
            controller.attemptToPackageAnyGas();
            return;
        }

        controller.attemptToSend(queuedRequests);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        pendingGas.read(compoundTag, provider, clientPacket);
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        pendingGas.write(compoundTag, provider, clientPacket);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        pendingGas.clear();
    }

    @Override
    public void destroy() {
        if (level != null && !level.isClientSide() && !pendingGas.isEmpty() && !previouslyUnwrapped.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), previouslyUnwrapped.copy());
        }
        pendingGas.clear();
        super.destroy();
    }

    @Override
    public boolean isTargetingSameInventory(@Nullable IdentifiedInventory inventory) {
        if (inventory == null) {
            return false;
        }

        InventoryIdentifier identifier = inventory.identifier();
        InventoryIdentifier ownIdentifier = getGasInventoryIdentifier();
        if (ownIdentifier != null && ownIdentifier.equals(identifier)) {
            return true;
        }

        if (identifier == null || gasInventory == null || !gasInventory.hasInventory()) {
            return super.isTargetingSameInventory(inventory);
        }

        BlockFace targetFace = gasInventory.getTarget().getOpposite();
        return identifier.contains(targetFace) || super.isTargetingSameInventory(inventory);
    }

    @Nullable
    public InventoryIdentifier getGasInventoryIdentifier() {
        if (level == null || gasInventory == null || !gasInventory.hasInventory()) {
            return null;
        }

        BlockFace targetFace = gasInventory.getTarget().getOpposite();
        BlockEntity target = level.getBlockEntity(targetFace.getPos());
        if (!(target instanceof IGasInventoryIdentifierProvider provider)) {
            return null;
        }
        return provider.getGasInventoryIdentifier(targetFace.getFace());
    }

    @Nullable
    public IdentifiedInventory getIdentifiedGasInventory() {
        InventoryIdentifier identifier = getGasInventoryIdentifier();
        return identifier == null ? null : new IdentifiedInventory(identifier, EMPTY_GAS_INVENTORY_HANDLER);
    }

    @Nullable public IGasHandler gasHandlerForController() {
        return gasInventory == null ? null : gasInventory.getInventory();
    }

    public boolean isGasPackageAnimationActive() {
        return animationTicks > 0;
    }

    public boolean canStartGasPackage() {
        return heldBox.isEmpty() && animationTicks == 0 && buttonCooldown <= 0;
    }

    public void beginGasPackageInsertion(ItemStack box) {
        previouslyUnwrapped = box.copy();
        animationInward = true;
        animationTicks = CYCLE;
    }

    public void emitGasPackageReceivedEvent(ItemStack box) {
        if (computerBehaviour == null) {
            return;
        }

        computerBehaviour.prepareComputerEvent(new PackageEvent(box, "package_received"));
    }

    public void enqueueCreatedGasBalloon(ItemStack balloon) {
        if (balloon.isEmpty()) {
            return;
        }

        if (computerBehaviour != null) {
            computerBehaviour.prepareComputerEvent(new PackageEvent(balloon, "package_created"));
        }
        if (!heldBox.isEmpty() || animationTicks != 0) {
            queuedExitingPackages.add(new BigItemStack(balloon, 1));
            return;
        }

        heldBox = balloon;
        animationInward = false;
        animationTicks = CYCLE;
    }

    public void enqueueReturnedGasBalloon(ItemStack balloon) {
        if (balloon.isEmpty()) {
            return;
        }

        queuedExitingPackages.addFirst(new BigItemStack(balloon, 1));
    }

    public ItemStack pendingUnwrappedPackage() {
        return previouslyUnwrapped;
    }

    public String signAddressForGasPackage() {
        return signBasedAddress;
    }

    public void markGasInventoryChanged() {
        controller.invalidateInventoryCache();
        triggerStockCheck();
    }

    public void requestGasStockCheck() {
        triggerStockCheck();
    }

    public void notifyGasPackageUpdate() {
        notifyUpdate();
    }
}
