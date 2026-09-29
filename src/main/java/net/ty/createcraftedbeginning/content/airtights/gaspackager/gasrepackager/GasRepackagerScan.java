package net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager;

import com.simibubi.create.content.logistics.box.PackageItem;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasRepackagerScan {

    private GasRepackagerScan() {
    }

    public record Candidate(int slot, ItemStack box, GasStack gas) {
        public Candidate {
            box = box.copyWithCount(1);
            gas = gas.copy();
        }

        boolean isGasPackage() {
            return !gas.isEmpty();
        }
    }

    static ScanResult scanPackages(IItemHandler targetInv) {
        List<GasGroupCandidates> simpleGroups = new ArrayList<>();
        Map<Integer, List<Candidate>> orderedPackagesByOrder = new LinkedHashMap<>();
        Candidate firstPassThroughPackage = null;
        for (int slot = 0; slot < targetInv.getSlots(); slot++) {
            ItemStack simulatedExtraction = targetInv.extractItem(slot, 1, true);
            if (simulatedExtraction.isEmpty()) {
                continue;
            }

            ItemStack packageStack = simulatedExtraction.copyWithCount(1);
            GasStack gas = BalloonItem.getGas(packageStack);
            boolean isGasBalloon = !gas.isEmpty();
            Candidate candidate = new Candidate(slot, packageStack, gas);
            if (PackageItem.isPackage(packageStack) && PackageItem.hasOrderData(packageStack)) {
                orderedPackagesByOrder.computeIfAbsent(PackageItem.getOrderId(packageStack), ignored -> new ArrayList<>()).add(candidate);
                if (isGasBalloon && firstPassThroughPackage == null && isStandaloneFinalOrderPackage(packageStack)) {
                    firstPassThroughPackage = candidate;
                }
                continue;
            }

            if (!candidate.isGasPackage()) {
                if (PackageItem.isPackage(packageStack) && firstPassThroughPackage == null) {
                    firstPassThroughPackage = candidate;
                }
                continue;
            }

            addToGroup(simpleGroups, candidate, PackageItem.getAddress(packageStack));
            if (firstPassThroughPackage != null) {
                continue;
            }

            firstPassThroughPackage = candidate;
        }
        return new ScanResult(simpleGroups, orderedPackagesByOrder, firstPassThroughPackage);
    }

    static void addToGroup(List<GasGroupCandidates> groups, Candidate candidate, String address) {
        for (GasGroupCandidates group : groups) {
            if (!group.accepts(candidate, address)) {
                continue;
            }

            group.add(candidate);
            return;
        }

        groups.add(new GasGroupCandidates(candidate, address));
    }

    static boolean isStandaloneFinalOrderPackage(ItemStack box) {
        return PackageItem.hasOrderData(box) && PackageItem.getLinkIndex(box) == 0 && PackageItem.getIndex(box) == 0 && PackageItem.isFinalLink(box) && PackageItem.isFinal(box);
    }

    record ScanResult(List<GasGroupCandidates> simpleGroups, Map<Integer, List<Candidate>> orderedPackagesByOrder, @Nullable Candidate firstPassThroughPackage) {
        ScanResult {
            simpleGroups = List.copyOf(simpleGroups);
            Map<Integer, List<Candidate>> immutableOrders = new LinkedHashMap<>();
            orderedPackagesByOrder.forEach((orderId, candidates) -> immutableOrders.put(orderId, List.copyOf(candidates)));
            orderedPackagesByOrder = Collections.unmodifiableMap(immutableOrders);
        }
    }

    static final class GasGroupCandidates {
        private final String address;
        private final List<Candidate> candidates = new ArrayList<>();
        private final List<Candidate> candidateView = Collections.unmodifiableList(candidates);
        private final GasStack gasType;
        private long totalAmount;

        GasGroupCandidates(Candidate firstCandidate, String address) {
            this.address = address;
            gasType = firstCandidate.gas().copyWithAmount(1);
            add(firstCandidate);
        }

        void add(Candidate candidate) {
            candidates.add(candidate);
            totalAmount = BoundedMath.saturatedAdd(totalAmount, candidate.gas().getAmount());
        }

        boolean accepts(Candidate candidate, String address) {
            return this.address.equals(address) && GasStack.isSameGasSameComponents(gasType, candidate.gas());
        }

        GasStack gas() {
            if (gasType.isEmpty() || totalAmount <= 0) {
                return GasStack.EMPTY;
            }

            return gasType.copyWithAmount(totalAmount);
        }

        String address() {
            return address;
        }

        List<Candidate> candidates() {
            return candidateView;
        }
    }
}
