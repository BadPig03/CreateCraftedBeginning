package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.compat.computercraft.events.PackageEvent;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlock;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.RequestPromiseQueue;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
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
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasContents;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonUtils;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasManipulationBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IGasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("FieldCanBeLocal")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasPackagerBlockEntity extends PackagerBlockEntity implements Clearable {
    private static final String COMPOUND_KEY_PENDING_GASES = "PendingGases";

    private static final ItemStackHandler EMPTY_GAS_INVENTORY_HANDLER = new ItemStackHandler(0);

    private InventorySummary availableItems = new InventorySummary();
    @Nullable
    private InventoryIdentifier availableItemsIdentifier;
    @Nullable
    private IGasHandler availableItemsHandler;
    private List<GasStack> availableTankSnapshot = List.of();
    private long availableItemsScanTick = Long.MIN_VALUE;
    private GasManipulationBehaviour gasInventory;
    private CCBAdvancementBehaviour advancementBehaviour;
    private BalloonGasContents pendingGases = BalloonGasContents.EMPTY;

    public GasPackagerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.GAS_PACKAGER.get(), (be, context) -> be.inventory);
    }

    private static boolean containsMatchingGas(List<GasStack> gases, GasStack target) {
        return gases.stream().anyMatch(gas -> GasStack.isSameGasSameComponents(gas, target));
    }

    private static void addPackedGas(List<GasStack> gases, GasStack added) {
        for (int i = 0; i < gases.size(); i++) {
            GasStack existing = gases.get(i);
            if (!GasStack.isSameGasSameComponents(existing, added)) {
                continue;
            }

            gases.set(i, existing.copyWithAmount(existing.getAmount() + added.getAmount()));
            return;
        }

        gases.add(added.copy());
    }

    private static void discardInvalidLeadingGasRequests(List<PackagingRequest> queuedRequests) {
        while (!queuedRequests.isEmpty() && !isValidGasRequest(queuedRequests.getFirst())) {
            queuedRequests.removeFirst();
        }
    }

    private static boolean isValidGasRequest(PackagingRequest request) {
        return request.getCount() > 0 && GasVirtualUtils.isVirtualItem(request.item()) && !GasVirtualUtils.getGasType(request.item()).isEmpty();
    }

    private static GasRequestPlan planGasRequestBatch(List<PackagingRequest> queuedRequests, long capacity) {
        if (queuedRequests.isEmpty() || capacity <= 0) {
            return GasRequestPlan.EMPTY;
        }

        PackagingRequest metadata = queuedRequests.getFirst();
        List<PlannedGasRequest> planned = new ArrayList<>();
        List<GasStack> plannedGases = new ArrayList<>();
        long remaining = capacity;
        for (PackagingRequest request : queuedRequests) {
            if (!GasPackagerUtils.isSameLink(metadata, request)) {
                break;
            }
            if (!isValidGasRequest(request)) {
                continue;
            }

            ItemStack token = request.item().copyWithCount(1);
            GasStack gasType = GasVirtualUtils.getGasType(token);
            if (!containsMatchingGas(plannedGases, gasType) && plannedGases.size() >= BalloonGasContents.MAX_GAS_TYPES) {
                break;
            }

            long requested = Math.max(0, request.getCount());
            long amount = Math.min(remaining, requested);
            if (amount <= 0) {
                break;
            }

            planned.add(new PlannedGasRequest(request, token, gasType, amount));
            addPackedGas(plannedGases, gasType.copyWithAmount(amount));
            remaining -= amount;
            if (remaining > 0) {
                continue;
            }

            break;
        }

        return planned.isEmpty() ? GasRequestPlan.EMPTY : new GasRequestPlan(List.copyOf(planned));
    }

    private static GasRequestExtraction extractGasRequestBatch(IGasHandler handler, GasRequestPlan plan) {
        List<ExtractedGasRequest> transfers = new ArrayList<>(plan.requests().size());
        List<GasStack> packedGases = new ArrayList<>();
        for (PlannedGasRequest planned : plan.requests()) {
            GasStack simulated = handler.drain(planned.gasType().copyWithAmount(planned.amount()), GasAction.SIMULATE);
            if (simulated.isEmpty() || !GasStack.isSameGasSameComponents(simulated, planned.gasType())) {
                break;
            }

            long executableAmount = Math.min(planned.amount(), simulated.getAmount());
            GasStack drained = handler.drain(planned.gasType().copyWithAmount(executableAmount), GasAction.EXECUTE);
            if (drained.isEmpty() || !GasStack.isSameGasSameComponents(drained, planned.gasType())) {
                break;
            }

            int transferred = Math.min(GasRequestUtils.toLogisticsAmount(drained.getAmount()), GasRequestUtils.toLogisticsAmount(planned.amount()));
            if (transferred <= 0) {
                break;
            }

            transfers.add(new ExtractedGasRequest(planned.request(), planned.token(), transferred));
            addPackedGas(packedGases, drained.copyWithAmount(transferred));
            if (transferred < planned.amount()) {
                break;
            }
        }

        if (transfers.isEmpty()) {
            return GasRequestExtraction.EMPTY;
        }
        return new GasRequestExtraction(List.copyOf(transfers), new BalloonGasContents(packedGases));
    }

    private static GasRequestCommit commitGasRequestBatch(List<PackagingRequest> queuedRequests, GasRequestExtraction extraction) {
        ExtractedGasRequest firstTransfer = extraction.transfers().getFirst();
        PackagingRequest packageMetadata = firstTransfer.request();
        int packageIndexAtLink = packageMetadata.packageCounter().getAndIncrement();
        boolean finalPackageAtLink = false;
        PackageOrderWithCrafts orderContext = null;
        List<GasDeduction> deductions = new ArrayList<>();

        for (ExtractedGasRequest transfer : extraction.transfers()) {
            PackagingRequest request = transfer.request();
            if (queuedRequests.isEmpty() || queuedRequests.getFirst() != request) {
                throw new IllegalStateException("Gas packaging request queue changed during commit");
            }
            if (request.context() != null) {
                orderContext = request.context();
            }

            request.subtract(transfer.amount());
            addGasDeduction(deductions, transfer.token(), transfer.amount());
            if (!request.isEmpty()) {
                break;
            }

            PackagingRequest completed = queuedRequests.removeFirst();
            finalPackageAtLink = GasPackagerUtils.propagatePackageCounter(completed, queuedRequests, packageIndexAtLink + 1);
            if (finalPackageAtLink) {
                break;
            }
        }

        return new GasRequestCommit(packageMetadata, extraction.contents(), orderContext, packageIndexAtLink, finalPackageAtLink, List.copyOf(deductions));
    }

    private static void addGasDeduction(List<GasDeduction> deductions, ItemStack token, int amount) {
        for (int i = 0; i < deductions.size(); i++) {
            GasDeduction existing = deductions.get(i);
            if (!ItemStack.isSameItemSameComponents(existing.token(), token)) {
                continue;
            }

            int mergedAmount = GasRequestUtils.toLogisticsAmount((long) existing.amount() + amount);
            deductions.set(i, new GasDeduction(existing.token(), mergedAmount));
            return;
        }

        deductions.add(new GasDeduction(token.copyWithCount(1), amount));
    }

    private static ItemStack createRequestedBalloon(GasRequestCommit committed) {
        PackagingRequest metadata = committed.metadata();
        ItemStack balloon = BalloonUtils.containing(committed.contents());
        if (balloon.isEmpty()) {
            return ItemStack.EMPTY;
        }

        PackageItem.clearAddress(balloon);
        PackageItem.addAddress(balloon, metadata.address());
        PackageItem.setOrder(balloon, metadata.orderId(), metadata.linkIndex(), metadata.finalLink().booleanValue(), committed.packageIndexAtLink(), committed.finalPackageAtLink(), committed.orderContext());
        return balloon;
    }

    private static InventorySummary createGasInventorySummary(List<GasStack> snapshot) {
        InventorySummary summary = new InventorySummary();
        for (GasStack gas : snapshot) {
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
        return summary;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        gasInventory = new GasManipulationBehaviour(this, InterfaceProvider.oppositeOfBlockFacing()).withFilter(GasPackagerUtils::supportsGasHandler);
        behaviours.add(gasInventory);

        targetInventory = new InvManipulationBehaviour(this, InterfaceProvider.oppositeOfBlockFacing()).withFilter(GasPackagerUtils::supportsItemHandler);
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
        InventoryIdentifier identifier = getGasInventoryIdentifier();
        if (identifier == null || gasInventory == null) {
            return clearAvailableItemsCache();
        }

        IGasHandler handler = gasInventory.getInventory();
        if (handler == null) {
            return clearAvailableItemsCache();
        }

        boolean sameSource = handler == availableItemsHandler && identifier.equals(availableItemsIdentifier);
        long currentTick = level == null ? Long.MIN_VALUE : level.getGameTime();
        if (sameSource && availableItemsScanTick == currentTick) {
            return availableItems;
        }

        availableItemsScanTick = currentTick;
        if (sameSource && GasPackagerUtils.matchesTankSnapshot(handler, availableTankSnapshot)) {
            return availableItems;
        }

        InventorySummary previous = sameSource ? availableItems : null;
        List<GasStack> snapshot = GasPackagerUtils.snapshotTanks(handler);
        InventorySummary summary = createGasInventorySummary(snapshot);
        availableItems = summary;
        availableItemsIdentifier = identifier;
        availableItemsHandler = handler;
        availableTankSnapshot = snapshot;
        submitNewGasArrivals(previous, identifier, summary);
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
        if (contents.isEmpty() || !BalloonUtils.fitsInBalloon(contents) || !GasPackagerUtils.canInsertAll(handler, contents)) {
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

    private InventorySummary clearAvailableItemsCache() {
        if (availableItemsIdentifier != null || availableItemsHandler != null || !availableTankSnapshot.isEmpty()) {
            availableItems = new InventorySummary();
        }
        availableItemsIdentifier = null;
        availableItemsHandler = null;
        availableTankSnapshot = List.of();
        availableItemsScanTick = Long.MIN_VALUE;
        return availableItems;
    }

    private void invalidateAvailableItemsCache() {
        availableItemsScanTick = Long.MIN_VALUE;
    }

    private void onGasInventoryChanged() {
        invalidateAvailableItemsCache();
        triggerStockCheck();
    }

    @Nullable
    public InventoryIdentifier getGasInventoryIdentifier() {
        if (level == null || gasInventory == null || !gasInventory.hasInventory()) {
            return null;
        }

        BlockFace targetFace = gasInventory.getTarget().getOpposite();
        BlockPos targetPos = targetFace.getPos();
        BlockEntity target = level.getBlockEntity(targetPos);
        if (!(target instanceof IGasInventoryIdentifierProvider provider)) {
            return null;
        }
        return provider.getGasInventoryIdentifier(targetFace.getFace());
    }

    @Nullable
    public IdentifiedInventory getIdentifiedGasInventory() {
        InventoryIdentifier identifier = getGasInventoryIdentifier();
        if (identifier == null) {
            return null;
        }
        return new IdentifiedInventory(identifier, EMPTY_GAS_INVENTORY_HANDLER);
    }

    private void attemptToPackageAnyGas() {
        if (!heldBox.isEmpty() || animationTicks != 0 || buttonCooldown > 0) {
            return;
        }

        IGasHandler handler = gasInventory.getInventory();
        if (handler == null) {
            return;
        }

        BalloonGasContents drained = GasPackagerUtils.drainContents(handler, BalloonUtils.getCapacity());
        if (drained.isEmpty()) {
            return;
        }

        ItemStack balloon = BalloonUtils.containing(drained);
        PackageItem.clearAddress(balloon);
        if (!signBasedAddress.isBlank()) {
            PackageItem.addAddress(balloon, signBasedAddress);
        }
        enqueueCreatedBalloon(balloon);
        onGasInventoryChanged();
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

        discardInvalidLeadingGasRequests(queuedRequests);
        GasRequestPlan plan = planGasRequestBatch(queuedRequests, capacity);
        if (plan.isEmpty()) {
            return;
        }

        GasRequestExtraction extraction = extractGasRequestBatch(handler, plan);
        if (extraction.isEmpty()) {
            return;
        }

        GasRequestCommit committed = commitGasRequestBatch(queuedRequests, extraction);
        ItemStack balloon = createRequestedBalloon(committed);
        if (balloon.isEmpty()) {
            return;
        }

        deductFromAccurateGasSummary(committed.deductions());
        enqueueCreatedBalloon(balloon);
        onGasInventoryChanged();
        notifyUpdate();
    }

    private void deductFromAccurateGasSummary(List<GasDeduction> deductions) {
        PackagerLinkBlockEntity link = getConnectedStockLink();
        if (link == null || deductions.isEmpty()) {
            return;
        }

        ItemStackHandler contents = new ItemStackHandler(deductions.size());
        for (int slot = 0; slot < deductions.size(); slot++) {
            GasDeduction deduction = deductions.get(slot);
            contents.setStackInSlot(slot, deduction.token().copyWithCount(deduction.amount()));
        }
        link.behaviour.deductFromAccurateSummary(contents);
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

            if (!(level.getBlockEntity(linkPos) instanceof PackagerLinkBlockEntity link)) {
                continue;
            }

            return link;
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

    private void submitNewGasArrivals(@Nullable InventorySummary previous, InventoryIdentifier identifier, InventorySummary current) {
        if (level == null || level.isClientSide()) {
            return;
        }

        Set<RequestPromiseQueue> promiseQueues = new HashSet<>();
        for (Direction direction : Iterate.directions) {
            BlockPos adjacentPos = worldPosition.relative(direction);
            if (!level.isLoaded(adjacentPos)) {
                continue;
            }

            BlockState adjacentState = level.getBlockState(adjacentPos);
            addFactoryPanelPromiseQueues(promiseQueues, direction, adjacentPos, adjacentState);
            addStockLinkPromiseQueue(promiseQueues, direction, adjacentPos, adjacentState);
        }

        GasLogisticsUtils.submitNewArrivals(promiseQueues, identifier, previous, current);
    }

    private void addFactoryPanelPromiseQueues(Set<RequestPromiseQueue> promiseQueues, Direction direction, BlockPos panelPos, BlockState panelState) {
        if (!(panelState.getBlock() instanceof FactoryPanelBlock) || FactoryPanelBlock.connectedDirection(panelState) != direction) {
            return;
        }

        if (level == null || !(level.getBlockEntity(panelPos) instanceof FactoryPanelBlockEntity panel) || !panel.restocker) {
            return;
        }

        for (FactoryPanelBehaviour behaviour : panel.panels.values()) {
            if (behaviour.isActive()) {
                promiseQueues.add(behaviour.restockerPromises);
            }
        }
    }

    private void addStockLinkPromiseQueue(Set<RequestPromiseQueue> promiseQueues, Direction direction, BlockPos linkPos, BlockState linkState) {
        if (!(linkState.getBlock() instanceof PackagerLinkBlock) || PackagerLinkBlock.getConnectedDirection(linkState) != direction) {
            return;
        }

        if (level == null || !(level.getBlockEntity(linkPos) instanceof PackagerLinkBlockEntity link)) {
            return;
        }

        UUID network = link.behaviour.freqId;
        if (!Create.LOGISTICS.hasQueuedPromises(network)) {
            return;
        }

        promiseQueues.add(Create.LOGISTICS.getQueuedPromises(network));
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

        List<GasStack> gases = contents.copyGasStacks();
        if (gases.size() > 1) {
            if (!handler.tryFillAtomically(gases, GasAction.EXECUTE).isSuccess()) {
                returnPreviouslyUnwrapped();
                return;
            }

            finishPendingGasInsertion();
            return;
        }

        List<GasStack> remainders = new ArrayList<>();
        for (GasStack gas : gases) {
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

        finishPendingGasInsertion();
    }

    private void finishPendingGasInsertion() {
        pendingGases = BalloonGasContents.EMPTY;
        onGasInventoryChanged();
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

    private record PlannedGasRequest(PackagingRequest request, ItemStack token, GasStack gasType, long amount) {}

    private record GasRequestPlan(List<PlannedGasRequest> requests) {
        private static final GasRequestPlan EMPTY = new GasRequestPlan(List.of());

        private boolean isEmpty() {
            return requests.isEmpty();
        }
    }

    private record ExtractedGasRequest(PackagingRequest request, ItemStack token, int amount) {}

    private record GasRequestExtraction(List<ExtractedGasRequest> transfers, BalloonGasContents contents) {
        private static final GasRequestExtraction EMPTY = new GasRequestExtraction(List.of(), BalloonGasContents.EMPTY);

        private boolean isEmpty() {
            return transfers.isEmpty() || contents.isEmpty();
        }
    }

    private record GasRequestCommit(PackagingRequest metadata, BalloonGasContents contents, @Nullable PackageOrderWithCrafts orderContext, int packageIndexAtLink, boolean finalPackageAtLink, List<GasDeduction> deductions) {}

    private record GasDeduction(ItemStack token, int amount) {}
}