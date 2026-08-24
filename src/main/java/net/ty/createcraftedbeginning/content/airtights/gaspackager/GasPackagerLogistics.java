package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlock;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.RequestPromiseQueue;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerRequestProcessor.Deduction;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasPackagerLogistics {
    private GasPackagerLogistics() {
    }

    static void deductFromAccurateGasSummary(@Nullable Level level, BlockPos worldPosition, List<Deduction> deductions) {
        PackagerLinkBlockEntity link = getConnectedStockLink(level, worldPosition);
        if (link == null || deductions.isEmpty()) {
            return;
        }

        ItemStackHandler deductionInventory = new ItemStackHandler(deductions.size());
        for (int deductionIndex = 0; deductionIndex < deductions.size(); deductionIndex++) {
            Deduction deduction = deductions.get(deductionIndex);
            deductionInventory.setStackInSlot(deductionIndex, deduction.token().copyWithCount(deduction.amount()));
        }
        link.behaviour.deductFromAccurateSummary(deductionInventory);
    }

    static void submitNewGasArrivals(@Nullable Level level, BlockPos worldPosition, @Nullable InventorySummary previous, InventoryIdentifier identifier, InventorySummary current) {
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
            addFactoryPanelPromiseQueues(level, promiseQueues, direction, adjacentPos, adjacentState);
            addStockLinkPromiseQueue(level, promiseQueues, direction, adjacentPos, adjacentState);
        }

        GasLogisticsUtils.submitNewArrivals(promiseQueues, identifier, previous, current);
    }

    private static @Nullable PackagerLinkBlockEntity getConnectedStockLink(@Nullable Level level, BlockPos worldPosition) {
        if (level == null) {
            return null;
        }

        for (Direction direction : Iterate.directions) {
            BlockPos linkPos = worldPosition.relative(direction);
            BlockState linkState = level.getBlockState(linkPos);
            if (!AllBlocks.STOCK_LINK.has(linkState) || PackagerLinkBlock.getConnectedDirection(linkState) != direction) {
                continue;
            }

            if (level.getBlockEntity(linkPos) instanceof PackagerLinkBlockEntity link) {
                return link;
            }
        }
        return null;
    }

    private static void addFactoryPanelPromiseQueues(Level level, Set<RequestPromiseQueue> promiseQueues, Direction direction, BlockPos panelPos, BlockState panelState) {
        if (!(panelState.getBlock() instanceof FactoryPanelBlock) || FactoryPanelBlock.connectedDirection(panelState) != direction) {
            return;
        }

        if (!(level.getBlockEntity(panelPos) instanceof FactoryPanelBlockEntity panel) || !panel.restocker) {
            return;
        }

        for (FactoryPanelBehaviour behaviour : panel.panels.values()) {
            if (behaviour.isActive()) {
                promiseQueues.add(behaviour.restockerPromises);
            }
        }
    }

    private static void addStockLinkPromiseQueue(Level level, Set<RequestPromiseQueue> promiseQueues, Direction direction, BlockPos linkPos, BlockState linkState) {
        if (!(linkState.getBlock() instanceof PackagerLinkBlock) || PackagerLinkBlock.getConnectedDirection(linkState) != direction) {
            return;
        }

        if (!(level.getBlockEntity(linkPos) instanceof PackagerLinkBlockEntity link)) {
            return;
        }

        UUID networkId = link.behaviour.freqId;
        if (!Create.LOGISTICS.hasQueuedPromises(networkId)) {
            return;
        }

        promiseQueues.add(Create.LOGISTICS.getQueuedPromises(networkId));
    }
}
