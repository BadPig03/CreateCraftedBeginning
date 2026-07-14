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
import net.minecraft.nbt.Tag;
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
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasContents;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonUtils;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasPackagerBlockEntity extends PackagerBlockEntity implements Clearable {
    private static final String COMPOUND_KEY_PENDING_GASES = "PendingGases";

    private InventorySummary availableItems;
    private GasManipulationBehaviour gasInventory;
    private CCBAdvancementBehaviour advancementBehaviour;
    private BalloonGasContents pendingGases = BalloonGasContents.EMPTY;

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

    private static boolean isSameLink(PackagingRequest first, PackagingRequest second) {
        return first.orderId() == second.orderId() && first.linkIndex() == second.linkIndex() && first.address().equals(second.address());
    }

    private static boolean propagatePackageCounter(PackagingRequest completed, List<PackagingRequest> queue, int nextPackageIndex) {
        while (!queue.isEmpty() && isSameLink(completed, queue.getFirst())) {
            PackagingRequest next = queue.getFirst();
            if (next.getCount() > 0 && GasVirtualUtils.isVirtualItem(next.item()) && !GasVirtualUtils.getGasType(next.item()).isEmpty()) {
                next.packageCounter().setValue(nextPackageIndex);
                return false;
            }
            queue.removeFirst();
        }
        return true;
    }

    private static BalloonGasContents snapshotContents(IGasHandler handler) {
        List<GasStack> gases = new ArrayList<>();
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            GasStack gas = handler.getGasInTank(tank);
            if (!gas.isEmpty()) {
                gases.add(gas.copy());
            }
        }
        return new BalloonGasContents(gases);
    }

    private static BalloonGasContents drainContents(IGasHandler handler, BalloonGasContents available, long maxAmount) {
        if (available.isEmpty() || maxAmount <= 0) {
            return BalloonGasContents.EMPTY;
        }

        List<GasStack> drainedGases = new ArrayList<>();
        long remaining = maxAmount;
        for (GasStack gas : available.gases()) {
            if (remaining <= 0) {
                break;
            }

            long amount = Math.min(remaining, gas.getAmount());
            GasStack simulated = handler.drain(gas.copyWithAmount(amount), GasAction.SIMULATE);
            if (simulated.isEmpty() || !GasStack.isSameGasSameComponents(simulated, gas)) {
                continue;
            }

            GasStack drained = handler.drain(simulated.copyWithAmount(Math.min(amount, simulated.getAmount())), GasAction.EXECUTE);
            if (drained.isEmpty() || !GasStack.isSameGasSameComponents(drained, gas)) {
                continue;
            }

            drainedGases.add(drained.copy());
            remaining -= drained.getAmount();
        }
        return new BalloonGasContents(drainedGases);
    }

    private static boolean canInsertAll(IGasHandler handler, BalloonGasContents contents) {
        List<SimulatedTank> tanks = new ArrayList<>(handler.getTanks());
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            tanks.add(new SimulatedTank(handler.getGasInTank(tank).copy(), Math.max(0, handler.getTankCapacity(tank))));
        }

        for (GasStack gas : contents.gases()) {
            if (handler.fill(gas.copy(), GasAction.SIMULATE) < gas.getAmount()) {
                return false;
            }

            long remaining = gas.getAmount();
            for (int tank = 0; tank < tanks.size() && remaining > 0; tank++) {
                SimulatedTank simulatedTank = tanks.get(tank);
                if (simulatedTank.gas().isEmpty() || !GasStack.isSameGasSameComponents(simulatedTank.gas(), gas) || !handler.isGasValid(tank, gas)) {
                    continue;
                }

                remaining -= simulatedTank.fill(gas, remaining);
            }

            for (int tank = 0; tank < tanks.size() && remaining > 0; tank++) {
                SimulatedTank simulatedTank = tanks.get(tank);
                if (!simulatedTank.gas().isEmpty() || !handler.isGasValid(tank, gas)) {
                    continue;
                }

                remaining -= simulatedTank.fill(gas, remaining);
            }

            if (remaining > 0) {
                return false;
            }
        }
        return true;
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
        boolean shouldInsertGas = level != null && !level.isClientSide() && animationInward && animationTicks == 1 && !pendingGases.isEmpty();
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

        for (GasStack gas : snapshotContents(handler).gases()) {
            int amount = GasRequestUtils.toLogisticsAmount(gas.getAmount());
            if (amount <= 0) {
                continue;
            }

            ItemStack virtualItem = GasVirtualUtils.createVirtualItem(gas.copyWithAmount(1));
            if (virtualItem.isEmpty()) {
                continue;
            }

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

        BalloonGasContents contents = BalloonUtils.getGasContents(box);
        if (contents.isEmpty() || !canInsertAll(handler, contents)) {
            return false;
        }

        if (simulate) {
            return true;
        }

        pendingGases = contents.copy();
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
        if (!compoundTag.contains(COMPOUND_KEY_PENDING_GASES) || clientPacket) {
            return;
        }

        Tag pendingTag = compoundTag.get(COMPOUND_KEY_PENDING_GASES);
        pendingGases = pendingTag == null ? BalloonGasContents.EMPTY : BalloonGasContents.parseOptional(provider, pendingTag);
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        if (clientPacket) {
            return;
        }

        compoundTag.put(COMPOUND_KEY_PENDING_GASES, pendingGases.saveOptional(provider));
    }

    @Override
    public void clearContent() {
        super.clearContent();
        pendingGases = BalloonGasContents.EMPTY;
    }

    @Override
    public void destroy() {
        if (level != null && !level.isClientSide() && !pendingGases.isEmpty() && !previouslyUnwrapped.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), previouslyUnwrapped.copy());
        }
        pendingGases = BalloonGasContents.EMPTY;
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

        BalloonGasContents drained = drainContents(handler, snapshotContents(handler), BalloonUtils.getCapacity());
        if (drained.isEmpty()) {
            return;
        }

        ItemStack balloon = BalloonUtils.containing(drained);
        PackageItem.clearAddress(balloon);
        if (!signBasedAddress.isBlank()) {
            PackageItem.addAddress(balloon, signBasedAddress);
        }
        enqueueCreatedBalloon(balloon);
        triggerStockCheck();
        notifyUpdate();
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
        long capacity = BalloonUtils.getCapacity();
        if (handler == null || capacity <= 0) {
            return;
        }

        List<GasStack> packedGases = new ArrayList<>();
        List<GasDeduction> deductions = new ArrayList<>();
        PackagingRequest packageMetadata = null;
        int packageIndexAtLink = -1;
        boolean finalPackageAtLink = false;
        long remainingCapacity = capacity;

        while (remainingCapacity > 0 && !queuedRequests.isEmpty()) {
            PackagingRequest request = queuedRequests.getFirst();
            if (packageMetadata != null && !isSameLink(packageMetadata, request)) {
                break;
            }

            ItemStack requestedToken = request.item();
            GasStack gasType = GasVirtualUtils.isVirtualItem(requestedToken) ? GasVirtualUtils.getGasType(requestedToken) : GasStack.EMPTY;
            long requestedAmount = Math.max(0, request.getCount());
            if (gasType.isEmpty() || requestedAmount <= 0) {
                PackagingRequest discarded = queuedRequests.removeFirst();
                if (packageMetadata == null) {
                    continue;
                }
                finalPackageAtLink = propagatePackageCounter(discarded, queuedRequests, packageIndexAtLink + 1);
                if (finalPackageAtLink) {
                    break;
                }
                continue;
            }

            long amountToDrain = Math.min(remainingCapacity, requestedAmount);
            GasStack toDrain = gasType.copyWithAmount(amountToDrain);
            GasStack simulated = handler.drain(toDrain, GasAction.SIMULATE);
            if (simulated.isEmpty() || !GasStack.isSameGasSameComponents(simulated, gasType)) {
                if (packageMetadata == null) {
                    return;
                }
                break;
            }

            GasStack drained = handler.drain(simulated.copyWithAmount(Math.min(amountToDrain, simulated.getAmount())), GasAction.EXECUTE);
            if (drained.isEmpty() || !GasStack.isSameGasSameComponents(drained, gasType)) {
                if (packageMetadata == null) {
                    return;
                }
                break;
            }

            if (packageMetadata == null) {
                packageMetadata = request;
                packageIndexAtLink = request.packageCounter().getAndIncrement();
            }

            int transferred = GasRequestUtils.toLogisticsAmount(drained.getAmount());
            if (transferred <= 0) {
                break;
            }

            packedGases.add(drained.copyWithAmount(transferred));
            deductions.add(new GasDeduction(requestedToken.copyWithCount(1), transferred));
            request.subtract(transferred);
            remainingCapacity -= transferred;

            if (!request.isEmpty()) {
                break;
            }

            PackagingRequest completed = queuedRequests.removeFirst();
            finalPackageAtLink = propagatePackageCounter(completed, queuedRequests, packageIndexAtLink + 1);
            if (finalPackageAtLink) {
                break;
            }
        }

        BalloonGasContents contents = new BalloonGasContents(packedGases);
        if (packageMetadata == null || contents.isEmpty()) {
            return;
        }

        ItemStack balloon = BalloonUtils.containing(contents);
        PackageItem.clearAddress(balloon);
        PackageItem.addAddress(balloon, packageMetadata.address());
        PackageItem.setOrder(balloon, packageMetadata.orderId(), packageMetadata.linkIndex(), packageMetadata.finalLink().booleanValue(), packageIndexAtLink, finalPackageAtLink, packageMetadata.context());

        PackagerLinkBlockEntity link = getConnectedStockLink();
        if (link != null) {
            for (GasDeduction deduction : deductions) {
                ItemStackHandler fakeContents = new ItemStackHandler(1);
                fakeContents.setStackInSlot(0, deduction.token().copyWithCount(deduction.amount()));
                link.behaviour.deductFromAccurateSummary(fakeContents);
            }
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
        BalloonGasContents contents = pendingGases.copy();
        if (contents.isEmpty()) {
            return;
        }

        IGasHandler handler = gasInventory.getInventory();
        if (handler == null) {
            returnPreviouslyUnwrapped();
            return;
        }

        List<GasStack> remainders = new ArrayList<>();
        for (GasStack gas : contents.gases()) {
            long filled = handler.fill(gas.copy(), GasAction.EXECUTE);
            if (filled < gas.getAmount()) {
                remainders.add(gas.copyWithAmount(gas.getAmount() - filled));
            }
        }

        BalloonGasContents remainderContents = new BalloonGasContents(remainders);
        if (!remainderContents.isEmpty() && !previouslyUnwrapped.isEmpty()) {
            ItemStack returned = previouslyUnwrapped.copy();
            BalloonUtils.setGasContents(returned, remainderContents);
            queuedExitingPackages.addFirst(new BigItemStack(returned, 1));
        }

        pendingGases = BalloonGasContents.EMPTY;
        triggerStockCheck();
        notifyUpdate();
    }

    private void returnPreviouslyUnwrapped() {
        if (!previouslyUnwrapped.isEmpty()) {
            queuedExitingPackages.addFirst(new BigItemStack(previouslyUnwrapped.copy(), 1));
        }
        pendingGases = BalloonGasContents.EMPTY;
        triggerStockCheck();
        notifyUpdate();
    }

    private record GasDeduction(ItemStack token, int amount) {}

    private static final class SimulatedTank {
        private final long capacity;
        private GasStack gas;

        private SimulatedTank(GasStack gas, long capacity) {
            this.gas = gas.isEmpty() ? GasStack.EMPTY : gas.copy();
            this.capacity = capacity;
        }

        private GasStack gas() {
            return gas;
        }

        private long fill(GasStack resource, long requested) {
            long current = gas.getAmount();
            long accepted = Math.clamp(capacity - current, 0, requested);
            if (accepted <= 0) {
                return 0;
            }

            gas = resource.copyWithAmount(current + accepted);
            return accepted;
        }
    }
}