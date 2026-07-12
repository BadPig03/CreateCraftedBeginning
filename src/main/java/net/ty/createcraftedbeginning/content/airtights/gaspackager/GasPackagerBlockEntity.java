package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.compat.computercraft.events.PackageEvent;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlock;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.RequestPromiseQueue;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase.InterfaceProvider;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasManipulationBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonUtils;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasPackagerBlockEntity extends PackagerBlockEntity implements Clearable {
    private static final String COMPOUND_KEY_PENDING_GAS = "PendingGas";

    private InventorySummary availableItems;
    private GasManipulationBehaviour gasInventory;
    private CCBAdvancementBehaviour advancementBehaviour;
    private GasStack pendingGas = GasStack.EMPTY;

    public GasPackagerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.GAS_PACKAGER.get(), (be, context) -> be.inventory);
    }

    private static boolean supportsGasHandler(@Nullable BlockEntity target) {
        return target != null && !(target instanceof PortableGasInterfaceBlockEntity);
    }

    private static boolean supportsItemHandler(@Nullable BlockEntity target) {
        return target != null && !(target instanceof PortableStorageInterfaceBlockEntity);
    }

    private static boolean hasNextRequestForSameLink(PackagingRequest completed, List<PackagingRequest> queue) {
        if (queue.isEmpty()) {
            return false;
        }

        PackagingRequest next = queue.getFirst();
        return completed.orderId() == next.orderId() && completed.linkIndex() == next.linkIndex() && completed.address().equals(next.address());
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        gasInventory = new GasManipulationBehaviour(this, InterfaceProvider.oppositeOfBlockFacing()).withFilter(GasPackagerBlockEntity::supportsGasHandler);
        behaviours.add(gasInventory);

        targetInventory = new InvManipulationBehaviour(this, InterfaceProvider.oppositeOfBlockFacing()).withFilter(GasPackagerBlockEntity::supportsItemHandler);
        behaviours.add(targetInventory);

        advancementBehaviour = new CCBAdvancementBehaviour(this);
        behaviours.add(advancementBehaviour);

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

        performPendingGasInsertion();
        setChanged();
    }

    @Override
    public InventorySummary getAvailableItems() {
        InventorySummary summary = new InventorySummary();
        if (getGasInventoryIdentifier() == null) {
            availableItems = summary;
            return summary;
        }

        IGasHandler handler = gasInventory.getInventory();
        if (handler == null) {
            availableItems = summary;
            return summary;
        }

        for (int tank = 0; tank < handler.getTanks(); tank++) {
            GasStack gas = handler.getGasInTank(tank);
            if (gas.isEmpty()) {
                continue;
            }

            ItemStack virtualItem = GasVirtualUtils.createVirtualItem(gas.copyWithAmount(1));
            int amount = GasRequestUtils.toLogisticsAmount(gas.getAmount());
            summary.add(virtualItem, amount);
        }

        submitNewGasArrivals(availableItems, summary);
        availableItems = summary;
        return summary;
    }

    @Override
    public boolean unwrapBox(ItemStack box, boolean simulate) {
        if (animationTicks > 0 || !BalloonUtils.containsGasContents(box)) {
            return false;
        }

        IGasHandler handler = gasInventory.getInventory();
        if (handler == null) {
            return false;
        }

        GasStack gas = BalloonUtils.getGasContents(box);
        if (gas.isEmpty()) {
            return false;
        }

        long accepted = handler.fill(gas.copy(), GasAction.SIMULATE);
        if (accepted < gas.getAmount()) {
            return false;
        }

        if (simulate) {
            return true;
        }

        pendingGas = gas.copy();
        previouslyUnwrapped = box.copy();
        animationInward = true;
        animationTicks = CYCLE;
        if (computerBehaviour != null) {
            computerBehaviour.prepareComputerEvent(new PackageEvent(box, "package_received"));
        }
        notifyUpdate();
        return true;
    }

    @Override
    public void attemptToSend(@Nullable List<PackagingRequest> queuedRequests) {
        if (queuedRequests == null) {
            attemptToPackageAnyGas();
            return;
        }

        attemptToSendGasRequest(queuedRequests);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (!compoundTag.contains(COMPOUND_KEY_PENDING_GAS) || clientPacket) {
            return;
        }

        pendingGas = GasStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_PENDING_GAS));
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        if (clientPacket) {
            return;
        }

        compoundTag.put(COMPOUND_KEY_PENDING_GAS, pendingGas.saveOptional(provider));
    }

    @Override
    public void clearContent() {
        super.clearContent();
        pendingGas = GasStack.EMPTY;
    }

    @Override
    public void destroy() {
        if (level != null && !level.isClientSide() && !pendingGas.isEmpty() && !previouslyUnwrapped.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), previouslyUnwrapped.copy());
        }
        pendingGas = GasStack.EMPTY;
        super.destroy();
    }

    @Nullable
    public InventoryIdentifier getGasInventoryIdentifier() {
        if (level == null || gasInventory == null || !gasInventory.hasInventory()) {
            return null;
        }

        BlockFace targetFace = gasInventory.getTarget().getOpposite();
        BlockPos targetPos = targetFace.getPos();
        BlockEntity targetBE = level.getBlockEntity(targetPos);
        if (!(targetBE instanceof IGasInventoryIdentifierProvider provider)) {
            return null;
        }
        return provider.getGasInventoryIdentifier(targetFace.getFace());
    }

    private void attemptToPackageAnyGas() {
        if (!heldBox.isEmpty() || animationTicks != 0 || buttonCooldown > 0) {
            return;
        }

        IGasHandler handler = gasInventory.getInventory();
        if (handler == null) {
            return;
        }

        long maxAmount = BalloonUtils.getCapacity();
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            GasStack stored = handler.getGasInTank(tank);
            if (stored.isEmpty()) {
                continue;
            }

            GasStack requested = stored.copyWithAmount(Math.min(maxAmount, stored.getAmount()));
            GasStack simulated = handler.drain(requested, GasAction.SIMULATE);
            if (simulated.isEmpty()) {
                continue;
            }

            GasStack drained = handler.drain(simulated, GasAction.EXECUTE);
            if (drained.isEmpty()) {
                continue;
            }

            ItemStack balloon = BalloonUtils.containing(drained);
            PackageItem.clearAddress(balloon);
            if (!signBasedAddress.isBlank()) {
                PackageItem.addAddress(balloon, signBasedAddress);
            }
            enqueueCreatedBalloon(balloon);
            triggerStockCheck();
            notifyUpdate();
            return;
        }
    }

    private void attemptToSendGasRequest(List<PackagingRequest> queuedRequests) {
        if (queuedRequests.isEmpty()) {
            return;
        }

        if (getGasInventoryIdentifier() == null) {
            queuedRequests.removeFirst();
            return;
        }

        IGasHandler handler = gasInventory.getInventory();
        if (handler == null) {
            return;
        }

        PackagingRequest request = queuedRequests.getFirst();
        ItemStack requestedToken = request.item();
        if (!GasVirtualUtils.isVirtualItem(requestedToken)) {
            queuedRequests.removeFirst();
            return;
        }

        GasStack gasType = GasVirtualUtils.getGasType(requestedToken);
        if (gasType.isEmpty()) {
            queuedRequests.removeFirst();
            return;
        }

        int packageIndexAtLink = request.packageCounter().getAndIncrement();
        long requestedAmount = Math.max(0, request.getCount());
        long maxAmount = Math.min(BalloonUtils.getCapacity(), requestedAmount);
        GasStack toDrain = gasType.copyWithAmount(maxAmount);
        GasStack simulated = handler.drain(toDrain, GasAction.SIMULATE);
        if (simulated.isEmpty()) {
            queuedRequests.removeFirst();
            return;
        }

        GasStack drained = handler.drain(simulated, GasAction.EXECUTE);
        if (drained.isEmpty()) {
            queuedRequests.removeFirst();
            return;
        }

        int transferred = GasRequestUtils.toLogisticsAmount(drained.getAmount());
        request.subtract(transferred);
        boolean finalPackageAtLink = false;
        if (request.isEmpty()) {
            queuedRequests.removeFirst();
            if (hasNextRequestForSameLink(request, queuedRequests)) {
                queuedRequests.getFirst().packageCounter().setValue(request.packageCounter().intValue());
            }
            else {
                finalPackageAtLink = true;
            }
        }

        ItemStack balloon = BalloonUtils.containing(drained);
        PackageItem.clearAddress(balloon);
        PackageItem.addAddress(balloon, request.address());
        PackageItem.setOrder(balloon, request.orderId(), request.linkIndex(), request.finalLink().booleanValue(), packageIndexAtLink, finalPackageAtLink, request.context());
        PackagerLinkBlockEntity link = getConnectedStockLink();
        if (link != null) {
            ItemStackHandler fakeContents = new ItemStackHandler(1);
            fakeContents.setStackInSlot(0, requestedToken.copyWithCount(transferred));
            link.behaviour.deductFromAccurateSummary(fakeContents);
        }
        enqueueCreatedBalloon(balloon);
        triggerStockCheck();
        notifyUpdate();
    }

    @Nullable
    private PackagerLinkBlockEntity getConnectedStockLink() {
        if (level == null) {
            return null;
        }

        for (Direction direction : Iterate.directions) {
            BlockPos linkPos = worldPosition.relative(direction);
            BlockState adjacentState = level.getBlockState(linkPos);
            if (!AllBlocks.STOCK_LINK.has(adjacentState) || PackagerLinkBlock.getConnectedDirection(adjacentState) != direction) {
                continue;
            }
            if (!(level.getBlockEntity(linkPos) instanceof PackagerLinkBlockEntity plbe)) {
                continue;
            }

            return plbe;
        }
        return null;
    }

    private void enqueueCreatedBalloon(ItemStack balloon) {
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

    private void submitNewGasArrivals(@Nullable InventorySummary before, InventorySummary after) {
        if (before == null || after.isEmpty() || level == null) {
            return;
        }

        Set<RequestPromiseQueue> promiseQueues = new HashSet<>();
        for (Direction direction : Iterate.directions) {
            if (!level.isLoaded(worldPosition.relative(direction))) {
                continue;
            }

            BlockState adjacentState = level.getBlockState(worldPosition.relative(direction));
            if (AllBlocks.FACTORY_GAUGE.has(adjacentState)) {
                if (FactoryPanelBlock.connectedDirection(adjacentState) != direction) {
                    continue;
                }
                if (!(level.getBlockEntity(worldPosition.relative(direction)) instanceof FactoryPanelBlockEntity fpbe) || !fpbe.restocker) {
                    continue;
                }

                fpbe.panels.values().stream().filter(FactoryPanelBehaviour::isActive).map(behaviour -> behaviour.restockerPromises).forEach(promiseQueues::add);
            }

            if (AllBlocks.STOCK_LINK.has(adjacentState)) {
                if (PackagerLinkBlock.getConnectedDirection(adjacentState) != direction) {
                    continue;
                }
                if (!(level.getBlockEntity(worldPosition.relative(direction)) instanceof PackagerLinkBlockEntity plbe)) {
                    continue;
                }

                UUID freqId = plbe.behaviour.freqId;
                if (Create.LOGISTICS.hasQueuedPromises(freqId)) {
                    promiseQueues.add(Create.LOGISTICS.getQueuedPromises(freqId));
                }
            }
        }
        if (promiseQueues.isEmpty()) {
            return;
        }

        after.getStacks().forEach(entry -> before.add(entry.stack, -entry.count));
        promiseQueues.forEach(queue -> before.getStacks().stream().filter(entry -> entry.count < 0).forEach(entry -> queue.itemEnteredSystem(entry.stack, -entry.count)));
    }

    private void performPendingGasInsertion() {
        GasStack gas = pendingGas.copy();
        if (gas.isEmpty()) {
            return;
        }

        IGasHandler handler = gasInventory.getInventory();
        if (handler == null) {
            if (!previouslyUnwrapped.isEmpty()) {
                queuedExitingPackages.addFirst(new BigItemStack(previouslyUnwrapped.copy(), 1));
            }
            pendingGas = GasStack.EMPTY;
            triggerStockCheck();
            notifyUpdate();
            return;
        }

        long accepted = handler.fill(gas.copy(), GasAction.SIMULATE);
        if (accepted < gas.getAmount()) {
            if (!previouslyUnwrapped.isEmpty()) {
                queuedExitingPackages.addFirst(new BigItemStack(previouslyUnwrapped.copy(), 1));
            }
            pendingGas = GasStack.EMPTY;
            triggerStockCheck();
            notifyUpdate();
            return;
        }

        long filled = handler.fill(gas.copy(), GasAction.EXECUTE);
        if (filled < gas.getAmount()) {
            GasStack remainder = gas.copyWithAmount(gas.getAmount() - filled);
            if (!remainder.isEmpty()) {
                ItemStack returned = previouslyUnwrapped.copy();
                BalloonUtils.setGasContents(returned, remainder);
                queuedExitingPackages.addFirst(new BigItemStack(returned, 1));
            }
        }
        pendingGas = GasStack.EMPTY;
        triggerStockCheck();
        notifyUpdate();
    }
}
