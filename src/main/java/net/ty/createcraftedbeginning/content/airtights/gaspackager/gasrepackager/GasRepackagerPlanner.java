package net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonPackingLimits;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerScan.Candidate;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerScan.GasGroupCandidates;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasRepackagerPlanner {
    static final Comparator<Candidate> ORDER_POSITION = Comparator.comparingInt((Candidate candidate) -> PackageItem.getLinkIndex(candidate.box())).thenComparingInt(candidate -> PackageItem.getIndex(candidate.box()));

    private GasRepackagerPlanner() {
    }

    public static boolean isOrderComplete(List<Candidate> candidates) {
        if (candidates.isEmpty()) {
            return false;
        }

        List<Candidate> sortedCandidates = new ArrayList<>(candidates);
        sortedCandidates.sort(ORDER_POSITION);
        ItemStack firstPackage = sortedCandidates.getFirst().box();
        if (!PackageItem.hasOrderData(firstPackage)) {
            return false;
        }

        int expectedLinkIndex = 0;
        int expectedPackageIndex = 0;
        boolean firstPackageInLink = true;
        boolean currentLinkIsFinal = false;
        int orderId = PackageItem.getOrderId(firstPackage);
        for (int packagePosition = 0; packagePosition < sortedCandidates.size(); packagePosition++) {
            ItemStack packageStack = sortedCandidates.get(packagePosition).box();
            if (!PackageItem.hasOrderData(packageStack) || PackageItem.getOrderId(packageStack) != orderId || PackageItem.getLinkIndex(packageStack) != expectedLinkIndex || PackageItem.getIndex(packageStack) != expectedPackageIndex) {
                return false;
            }

            boolean isFinalLink = PackageItem.isFinalLink(packageStack);
            if (firstPackageInLink) {
                currentLinkIsFinal = isFinalLink;
                firstPackageInLink = false;
            }
            else if (isFinalLink != currentLinkIsFinal) {
                return false;
            }

            if (!PackageItem.isFinal(packageStack)) {
                expectedPackageIndex++;
                continue;
            }

            if (currentLinkIsFinal) {
                return packagePosition == sortedCandidates.size() - 1;
            }

            expectedLinkIndex++;
            expectedPackageIndex = 0;
            firstPackageInLink = true;
        }
        return false;
    }

    static List<Candidate> sortByOrderPosition(List<Candidate> candidates) {
        List<Candidate> sortedCandidates = new ArrayList<>(candidates);
        sortedCandidates.sort(ORDER_POSITION);
        return List.copyOf(sortedCandidates);
    }

    static boolean isRepackUseful(GasGroupCandidates group, List<BigItemStack> output) {
        if (output.isEmpty()) {
            return false;
        }

        List<Candidate> candidates = group.candidates();
        if (output.size() != candidates.size()) {
            return true;
        }

        for (int index = 0; index < output.size(); index++) {
            Candidate candidate = candidates.get(index);
            ItemStack expected = output.get(index).stack;
            GasStack expectedGas = BalloonItem.getGas(expected);
            if (GasStack.matches(candidate.gas(), expectedGas) && PackageItem.getAddress(candidate.box()).equals(PackageItem.getAddress(expected))) {
                continue;
            }

            return true;
        }

        return false;
    }

    static boolean needsLocalGasRepack(List<Candidate> candidates, long ambientPressurePa) {
        long localPackingLimit = BalloonPackingLimits.getLocalPackingLimit(ambientPressurePa);
        return localPackingLimit > 0 && candidates.stream().filter(Candidate::isGasPackage).anyMatch(candidate -> candidate.gas().getAmount() > localPackingLimit);
    }
}
