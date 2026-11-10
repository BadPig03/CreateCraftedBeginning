package net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonFactory;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonPackingLimits;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerScan.Candidate;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerScan.GasGroupCandidates;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasRepackagerOutputs {
    private GasRepackagerOutputs() {
    }

    public static OutputResult createBalloons(GasStack inputGas, String address, long ambientPressurePa) {
        long localPackingLimit = BalloonPackingLimits.getLocalPackingLimit(ambientPressurePa);
        if (inputGas.isEmpty() || localPackingLimit <= 0) {
            return OutputResult.EMPTY;
        }

        if (!new GasRepackagerOutputBudget().reserveGas(inputGas.getAmount(), localPackingLimit)) {
            return OutputResult.LIMIT_EXCEEDED;
        }

        List<BigItemStack> outputPackages = new ArrayList<>();
        long remainingAmount = inputGas.getAmount();
        while (remainingAmount > 0) {
            long packedAmount = Math.min(remainingAmount, localPackingLimit);
            GasStack packedGas = inputGas.copyWithAmount(packedAmount);
            ItemStack balloon = BalloonFactory.create(packedGas, address);
            if (!GasStack.matches(BalloonItem.getGas(balloon), packedGas)) {
                return OutputResult.EMPTY;
            }

            outputPackages.add(new BigItemStack(balloon, 1));

            remainingAmount -= packedAmount;
        }
        return new OutputResult(outputPackages, false);
    }

    public static OutputResult createMixedOrderOutput(int orderId, List<Candidate> candidates, long ambientPressurePa, RandomSource random) {
        List<Candidate> sortedCandidates = new ArrayList<>(candidates);
        sortedCandidates.sort(GasRepackagerPlanner.ORDER_POSITION);
        List<Candidate> gasCandidates = sortedCandidates.stream().filter(Candidate::isGasPackage).toList();
        if (gasCandidates.isEmpty()) {
            return OutputResult.EMPTY;
        }

        String finalAddress = PackageItem.getAddress(sortedCandidates.getLast().box());
        List<BigItemStack> gasPackages = new ArrayList<>();
        List<GasGroupCandidates> groups = new ArrayList<>();
        gasCandidates.forEach(candidate -> GasRepackagerScan.addToGroup(groups, candidate, finalAddress));
        long packingLimit = BalloonPackingLimits.getLocalPackingLimit(ambientPressurePa);
        if (packingLimit <= 0) {
            return OutputResult.EMPTY;
        }

        GasRepackagerOutputBudget budget = new GasRepackagerOutputBudget();
        for (GasGroupCandidates group : groups) {
            if (!budget.reserveGas(group.gas().getAmount(), packingLimit)) {
                return OutputResult.LIMIT_EXCEEDED;
            }
        }
        List<Candidate> itemCandidates = sortedCandidates.stream().filter(candidate -> !candidate.isGasPackage()).toList();
        List<Candidate> reversedCandidates = new ArrayList<>(candidates);
        reversedCandidates.sort(GasRepackagerPlanner.ORDER_POSITION.reversed());
        PackageOrderWithCrafts orderContext = reversedCandidates.stream().map(candidate -> PackageItem.getOrderContext(candidate.box())).filter(context -> context != null && !context.isEmpty()).findFirst().orElse(null);
        if (!itemCandidates.isEmpty() && !budget.reserveItems(itemCandidates, orderContext)) {
            return OutputResult.LIMIT_EXCEEDED;
        }

        for (GasGroupCandidates group : groups) {
            OutputResult generated = createBalloons(group.gas(), group.address(), ambientPressurePa);
            if (generated.packages().isEmpty()) {
                return generated;
            }

            gasPackages.addAll(generated.packages());
        }
        List<BigItemStack> itemPackages = List.of();
        if (!itemCandidates.isEmpty()) {
            List<Candidate> sortedItemCandidates = new ArrayList<>(itemCandidates);
            sortedItemCandidates.sort(GasRepackagerPlanner.ORDER_POSITION);
            PackageRepackageHelper helper = new PackageRepackageHelper();
            for (int candidateIndex = 0; candidateIndex < sortedItemCandidates.size(); candidateIndex++) {
                Candidate candidate = sortedItemCandidates.get(candidateIndex);
                ItemStack fragment = candidate.box().copyWithCount(1);
                if (candidateIndex != sortedItemCandidates.size() - 1) {
                    helper.addPackageFragment(fragment);
                    continue;
                }

                PackageItem.clearAddress(fragment);
                if (!finalAddress.isBlank()) {
                    PackageItem.addAddress(fragment, finalAddress);
                }
                if (orderContext != null) {
                    PackageItem.setOrder(fragment, PackageItem.getOrderId(fragment), PackageItem.getLinkIndex(fragment), PackageItem.isFinalLink(fragment), PackageItem.getIndex(fragment), PackageItem.isFinal(fragment), orderContext);
                }
                helper.addPackageFragment(fragment);
            }
            itemPackages = helper.repack(orderId, random);
        }
        List<BigItemStack> outputPackages = new ArrayList<>(itemPackages.size() + gasPackages.size());
        outputPackages.addAll(itemPackages);
        outputPackages.addAll(gasPackages);
        if (outputPackages.isEmpty()) {
            return OutputResult.EMPTY;
        }

        List<ItemStack> outerOrderPackages = new ArrayList<>();
        for (BigItemStack output : itemPackages) {
            if (PackageItem.getOrderId(output.stack) != orderId) {
                continue;
            }

            outerOrderPackages.add(output.stack);
        }
        for (BigItemStack output : gasPackages) {
            outerOrderPackages.add(output.stack);
        }
        for (int packageIndex = 0; packageIndex < outerOrderPackages.size(); packageIndex++) {
            ItemStack outputPackage = outerOrderPackages.get(packageIndex);
            boolean finalPackage = packageIndex == outerOrderPackages.size() - 1;
            PackageItem.setOrder(outputPackage, orderId, 0, true, packageIndex, finalPackage, finalPackage ? orderContext : null);
        }
        return new OutputResult(outputPackages, false);
    }

    public record OutputResult(List<BigItemStack> packages, boolean limitExceeded) {
        private static final OutputResult EMPTY = new OutputResult(List.of(), false);
        private static final OutputResult LIMIT_EXCEEDED = new OutputResult(List.of(), true);

        public OutputResult {
            packages = List.copyOf(packages);
        }
    }
}
