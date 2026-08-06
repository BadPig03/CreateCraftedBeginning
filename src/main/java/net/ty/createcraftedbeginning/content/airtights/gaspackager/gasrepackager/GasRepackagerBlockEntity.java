package net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager;

import com.simibubi.create.compat.computercraft.events.RepackageEvent;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerUtils.Candidate;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerUtils.ExtractionResult;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerUtils.GasGroupCandidates;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerUtils.ScanResult;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map.Entry;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasRepackagerBlockEntity extends RepackagerBlockEntity {

    public GasRepackagerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.GAS_REPACKAGER.get(), (be, context) -> be.inventory);
    }

    @Override
    public boolean unwrapBox(ItemStack box, boolean simulate) {
        return PackageItem.isPackage(box) && super.unwrapBox(box, simulate);
    }

    @Override
    protected void attemptToRepackage(IItemHandler targetInv) {
        ScanResult scan = GasRepackagerUtils.scanPackages(targetInv);
        if (tryHandleCompletedOrder(targetInv, scan) || tryRepackageSimpleGasGroup(targetInv, scan)) {
            return;
        }

        passThroughFirstReadyPackage(targetInv, scan);
    }

    private boolean tryHandleCompletedOrder(IItemHandler targetInv, ScanResult scan) {
        for (Entry<Integer, List<Candidate>> entry : scan.orderedPackagesByOrder().entrySet()) {
            int orderId = entry.getKey();
            List<Candidate> candidates = entry.getValue();
            if (!GasRepackagerUtils.isOrderComplete(candidates)) {
                continue;
            }

            boolean hasGasPackage = candidates.stream().anyMatch(Candidate::isGasPackage);
            List<BigItemStack> output;
            if (hasGasPackage) {
                boolean hasNonStandalonePackage = candidates.stream().anyMatch(candidate -> !GasRepackagerUtils.isStandaloneFinalOrderPackage(candidate.box()));
                if (!hasNonStandalonePackage) {
                    continue;
                }

                output = GasRepackagerUtils.createMixedOrderOutput(orderId, candidates);
            }
            else {
                output = GasRepackagerUtils.createItemOrderPassThroughOutput(candidates);
            }

            if (output.isEmpty()) {
                continue;
            }

            if (!extractCandidatesTransactionally(targetInv, candidates)) {
                return false;
            }

            if (hasGasPackage) {
                queueRepackaged(output);
            }
            else {
                queuePassThrough(output);
            }
            return true;
        }
        return false;
    }

    private boolean tryRepackageSimpleGasGroup(IItemHandler targetInv, ScanResult scan) {
        for (GasGroupCandidates group : scan.simpleGroups()) {
            if (group.candidates().size() < 2) {
                continue;
            }

            String address = resolveSimpleOutputAddress(group.address());
            List<BigItemStack> output = GasRepackagerUtils.createBalloons(group.outputTemplate(), group.contents(), address);
            if (!GasRepackagerUtils.isRepackUseful(group, output)) {
                continue;
            }

            if (!extractCandidatesTransactionally(targetInv, group.candidates())) {
                return false;
            }

            queueRepackaged(output);
            return true;
        }
        return false;
    }

    private void passThroughFirstReadyPackage(IItemHandler targetInv, ScanResult scan) {
        Candidate candidate = scan.firstPassThroughPackage();
        if (candidate == null || !extractCandidatesTransactionally(targetInv, List.of(candidate))) {
            return;
        }

        ItemStack box = candidate.box().copy();
        if (PackageItem.hasOrderData(box)) {
            queuedExitingPackages.add(new BigItemStack(box, 1));
            notifyUpdate();
            return;
        }

        heldBox = box;
        animationInward = false;
        animationTicks = CYCLE;
        notifyUpdate();
    }

    private boolean extractCandidatesTransactionally(IItemHandler targetInv, List<Candidate> candidates) {
        ExtractionResult result = GasRepackagerUtils.extractCandidates(targetInv, candidates);
        if (result.committed()) {
            return true;
        }

        preserveRollbackRemainders(result.rollbackRemainders());
        return false;
    }

    private void preserveRollbackRemainders(List<ItemStack> remainders) {
        boolean changed = false;
        for (ItemStack remainder : remainders) {
            if (remainder.isEmpty()) {
                continue;
            }

            changed = true;
            if (PackageItem.isPackage(remainder)) {
                queuedExitingPackages.addFirst(new BigItemStack(remainder.copyWithCount(1), remainder.getCount()));
                continue;
            }

            if (level != null) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainder.copy());
            }
        }
        if (!changed) {
            return;
        }

        notifyUpdate();
    }

    private String resolveSimpleOutputAddress(String originalAddress) {
        updateSignAddress();
        return signBasedAddress.isBlank() ? originalAddress : signBasedAddress;
    }

    private void queuePassThrough(List<BigItemStack> boxes) {
        if (boxes.isEmpty()) {
            return;
        }

        queuedExitingPackages.addAll(boxes);
        notifyUpdate();
    }

    private void queueRepackaged(List<BigItemStack> boxes) {
        if (boxes.isEmpty()) {
            return;
        }

        if (computerBehaviour != null && computerBehaviour.hasAttachedComputer()) {
            boxes.forEach(box -> computerBehaviour.prepareComputerEvent(new RepackageEvent(box.stack, box.count)));
        }
        queuedExitingPackages.addAll(boxes);
        notifyUpdate();
    }

}
