package net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasContents;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasContents.GasEntry;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonStyleUtils;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasRepackagerUtils {
    private static final Comparator<Candidate> ORDER_POSITION = Comparator.comparingInt((Candidate candidate) -> PackageItem.getLinkIndex(candidate.box())).thenComparingInt(candidate -> PackageItem.getIndex(candidate.box()));

    private GasRepackagerUtils() {
    }

    private static @Nullable PackageOrderWithCrafts findOrderContext(List<Candidate> candidates) {
        List<Candidate> sortedCandidates = new ArrayList<>(candidates);
        sortedCandidates.sort(ORDER_POSITION.reversed());
        return sortedCandidates.stream().map(candidate -> PackageItem.getOrderContext(candidate.box())).filter(context -> context != null && !context.isEmpty()).findFirst().orElse(null);
    }

    private static boolean isAlreadyCanonical(GasGroupCandidates group, List<BigItemStack> canonical) {
        if (canonical.size() != group.candidates().size()) {
            return false;
        }

        for (int packageIndex = 0; packageIndex < canonical.size(); packageIndex++) {
            Candidate candidate = group.candidates().get(packageIndex);
            ItemStack expectedPackage = canonical.get(packageIndex).stack;
            BalloonGasContents expectedContents = BalloonUtils.getGasContents(expectedPackage);
            if (!candidate.box().is(group.outputTemplate().getItem()) || !candidate.contents().equals(expectedContents)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSamePackage(ItemStack actual, ItemStack expected) {
        return !actual.isEmpty() && !expected.isEmpty() && ItemStack.isSameItemSameComponents(actual.copyWithCount(1), expected.copyWithCount(1));
    }

    private static @Unmodifiable List<GasGroupCandidates> groupCandidates(List<Candidate> candidates) {
        List<GasGroupCandidates> groups = new ArrayList<>();
        candidates.forEach(candidate -> addToGroup(groups, candidate, PackageItem.getAddress(candidate.box())));
        return List.copyOf(groups);
    }

    private static void addOutputBalloon(List<BigItemStack> output, ItemStack outputTemplate, BalloonGasContents contents, String address) {
        BalloonGasContents normalized = contents.normalized();
        if (normalized.isEmpty()) {
            return;
        }

        ItemStack outputBalloon = BalloonUtils.containingLike(outputTemplate, normalized);
        if (outputBalloon.isEmpty()) {
            return;
        }

        PackageItem.clearAddress(outputBalloon);
        if (!address.isBlank()) {
            PackageItem.addAddress(outputBalloon, address);
        }

        output.add(new BigItemStack(outputBalloon, 1));
    }

    private static void addToGroup(List<GasGroupCandidates> groups, Candidate candidate, String address) {
        for (GasGroupCandidates group : groups) {
            if (!group.accepts(candidate, address)) {
                continue;
            }

            group.add(candidate);
            return;
        }

        groups.add(new GasGroupCandidates(candidate, address));
    }

    static ExtractionResult extractCandidates(IItemHandler targetInv, List<Candidate> candidates) {
        Set<Integer> candidateSlots = new HashSet<>();
        for (Candidate candidate : candidates) {
            if (candidate.slot() < 0 || candidate.slot() >= targetInv.getSlots() || !candidateSlots.add(candidate.slot())) {
                return ExtractionResult.failed(List.of());
            }

            ItemStack simulatedExtraction = targetInv.extractItem(candidate.slot(), 1, true);
            if (!isSamePackage(simulatedExtraction, candidate.box())) {
                return ExtractionResult.failed(List.of());
            }
        }

        List<Candidate> sortedCandidates = new ArrayList<>(candidates);
        sortedCandidates.sort(Comparator.comparingInt(Candidate::slot).reversed());
        List<ExtractedItem> extractedItems = new ArrayList<>(sortedCandidates.size());
        for (Candidate candidate : sortedCandidates) {
            ItemStack extractedStack = targetInv.extractItem(candidate.slot(), 1, false);
            if (!extractedStack.isEmpty()) {
                extractedItems.add(new ExtractedItem(candidate.slot(), extractedStack.copy()));
            }
            if (!isSamePackage(extractedStack, candidate.box())) {
                return rollbackExtraction(targetInv, extractedItems);
            }
        }
        return ExtractionResult.success();
    }

    private static ExtractionResult rollbackExtraction(IItemHandler targetInv, List<ExtractedItem> extractedItems) {
        List<ItemStack> rollbackRemainders = new ArrayList<>();
        for (int extractedIndex = extractedItems.size() - 1; extractedIndex >= 0; extractedIndex--) {
            ExtractedItem extractedItem = extractedItems.get(extractedIndex);
            ItemStack rollbackRemainder = targetInv.insertItem(extractedItem.slot(), extractedItem.stack().copy(), false);
            for (int targetSlot = 0; targetSlot < targetInv.getSlots() && !rollbackRemainder.isEmpty(); targetSlot++) {
                if (targetSlot == extractedItem.slot()) {
                    continue;
                }

                rollbackRemainder = targetInv.insertItem(targetSlot, rollbackRemainder, false);
            }
            if (!rollbackRemainder.isEmpty()) {
                rollbackRemainders.add(rollbackRemainder.copy());
            }
        }
        return ExtractionResult.failed(rollbackRemainders);
    }

    static boolean isOrderComplete(List<Candidate> candidates) {
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

    static boolean isRepackUseful(GasGroupCandidates group, List<BigItemStack> output) {
        int inputCount = group.candidates().size();
        if (inputCount < 2) {
            return false;
        }

        int outputCount = output.size();
        return outputCount > 0 && outputCount <= inputCount && (outputCount < inputCount || !isAlreadyCanonical(group, output));
    }

    static boolean isStandaloneFinalOrderPackage(ItemStack box) {
        return PackageItem.hasOrderData(box) && PackageItem.getLinkIndex(box) == 0 && PackageItem.getIndex(box) == 0 && PackageItem.isFinalLink(box) && PackageItem.isFinal(box);
    }

    static List<BigItemStack> createBalloons(ItemStack outputTemplate, BalloonGasContents inputContents, String address) {
        List<BigItemStack> outputPackages = new ArrayList<>();
        long capacity = BalloonUtils.getCapacity();
        BalloonGasContents normalizedContents = inputContents.normalized();
        if (!BalloonUtils.isBalloon(outputTemplate) || normalizedContents.isEmpty() || capacity <= 0) {
            return List.of();
        }

        List<GasStack> currentGases = new ArrayList<>();
        long currentAmount = 0;
        for (GasEntry sourceGas : normalizedContents.gases()) {
            long remainingAmount = sourceGas.getAmount();
            while (remainingAmount > 0) {
                if (currentGases.size() >= BalloonGasContents.MAX_GAS_TYPES) {
                    addOutputBalloon(outputPackages, outputTemplate, new BalloonGasContents(List.copyOf(currentGases)), address);
                    currentGases.clear();
                    currentAmount = 0;
                }

                long availableSpace = capacity - currentAmount;
                long insertedAmount = Math.min(remainingAmount, availableSpace);
                if (insertedAmount <= 0) {
                    break;
                }

                currentGases.add(sourceGas.toStack(insertedAmount));
                currentAmount += insertedAmount;
                remainingAmount -= insertedAmount;
                if (currentAmount < capacity) {
                    continue;
                }

                addOutputBalloon(outputPackages, outputTemplate, new BalloonGasContents(List.copyOf(currentGases)), address);
                currentGases.clear();
                currentAmount = 0;
            }
        }

        if (currentGases.isEmpty()) {
            return List.copyOf(outputPackages);
        }

        addOutputBalloon(outputPackages, outputTemplate, new BalloonGasContents(List.copyOf(currentGases)), address);
        return List.copyOf(outputPackages);
    }

    static List<BigItemStack> createItemOrderPassThroughOutput(List<Candidate> candidates) {
        if (candidates.isEmpty() || candidates.stream().anyMatch(Candidate::isGasPackage) || !isOrderComplete(candidates)) {
            return List.of();
        }

        List<Candidate> sortedCandidates = new ArrayList<>(candidates);
        sortedCandidates.sort(ORDER_POSITION);
        return sortedCandidates.stream().map(candidate -> new BigItemStack(candidate.box().copyWithCount(1), 1)).toList();
    }

    static List<BigItemStack> createMixedOrderOutput(int orderId, List<Candidate> candidates) {
        List<Candidate> sortedCandidates = new ArrayList<>(candidates);
        sortedCandidates.sort(ORDER_POSITION);
        List<Candidate> gasCandidates = sortedCandidates.stream().filter(Candidate::isGasPackage).toList();
        if (gasCandidates.isEmpty()) {
            return List.of();
        }

        Map<Integer, List<BigItemStack>> generatedByAnchorSlot = new LinkedHashMap<>();
        Set<Integer> gasSlots = new HashSet<>();
        for (GasGroupCandidates group : groupCandidates(gasCandidates)) {
            List<BigItemStack> generatedPackages = createBalloons(group.outputTemplate(), group.contents(), group.address());
            if (generatedPackages.isEmpty() || generatedPackages.size() > group.candidates().size()) {
                return List.of();
            }

            Candidate anchorCandidate = group.candidates().stream().min(ORDER_POSITION).orElseThrow();
            generatedByAnchorSlot.put(anchorCandidate.slot(), generatedPackages);
            group.candidates().forEach(candidate -> gasSlots.add(candidate.slot()));
        }

        List<BigItemStack> outputPackages = new ArrayList<>();
        for (Candidate candidate : sortedCandidates) {
            if (!gasSlots.contains(candidate.slot())) {
                outputPackages.add(new BigItemStack(candidate.box().copyWithCount(1), 1));
                continue;
            }

            List<BigItemStack> generatedPackages = generatedByAnchorSlot.get(candidate.slot());
            if (generatedPackages == null) {
                continue;
            }

            outputPackages.addAll(generatedPackages);
        }

        if (outputPackages.isEmpty()) {
            return List.of();
        }

        PackageOrderWithCrafts orderContext = findOrderContext(candidates);
        for (int packageIndex = 0; packageIndex < outputPackages.size(); packageIndex++) {
            boolean finalPackage = packageIndex == outputPackages.size() - 1;
            PackageOrderWithCrafts context = finalPackage ? orderContext : null;
            PackageItem.setOrder(outputPackages.get(packageIndex).stack, orderId, 0, true, packageIndex, finalPackage, context);
        }
        return List.copyOf(outputPackages);
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
            BalloonGasContents gasContents = BalloonUtils.getGasContents(packageStack);
            boolean isGasBalloon = !gasContents.isEmpty();
            Candidate candidate = new Candidate(slot, packageStack, gasContents);
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
            if (firstPassThroughPackage == null) {
                firstPassThroughPackage = candidate;
            }
        }

        return new ScanResult(simpleGroups, orderedPackagesByOrder, firstPassThroughPackage);
    }

    private record ExtractedItem(int slot, ItemStack stack) {}

    record ExtractionResult(boolean committed, List<ItemStack> rollbackRemainders) {
        ExtractionResult {
            rollbackRemainders = rollbackRemainders.stream().map(ItemStack::copy).toList();
        }

        private static ExtractionResult success() {
            return new ExtractionResult(true, List.of());
        }

        private static ExtractionResult failed(List<ItemStack> rollbackRemainders) {
            return new ExtractionResult(false, rollbackRemainders);
        }
    }

    record Candidate(int slot, ItemStack box, BalloonGasContents contents) {
        Candidate {
            box = box.copyWithCount(1);
        }

        boolean isGasPackage() {
            return !contents.isEmpty();
        }

    }

    static final class GasGroupCandidates {
        private final String address;
        private final List<Candidate> candidates = new ArrayList<>();
        private final List<Candidate> candidateView = Collections.unmodifiableList(candidates);
        private final List<GasStack> gases = new ArrayList<>();
        private final ItemStack outputTemplate;
        private final boolean rare;

        @Nullable
        private BalloonGasContents cachedContents;

        private GasGroupCandidates(Candidate firstCandidate, String address) {
            outputTemplate = firstCandidate.box().copyWithCount(1);
            rare = BalloonStyleUtils.isRareBalloon(firstCandidate.box());
            this.address = address;
            add(firstCandidate);
        }

        private void add(Candidate candidate) {
            candidates.add(candidate);
            BalloonGasContents candidateContents = candidate.contents();
            candidateContents.gases().stream().map(GasEntry::toStack).forEach(gases::add);
            cachedContents = null;
        }

        private boolean accepts(Candidate candidate, String address) {
            ItemStack candidateBox = candidate.box();
            if (!this.address.equals(address)) {
                return false;
            }

            if (rare) {
                return BalloonStyleUtils.isRareBalloon(candidateBox) && candidateBox.is(outputTemplate.getItem());
            }
            return BalloonStyleUtils.isRegularBalloon(candidateBox);
        }

        BalloonGasContents contents() {
            if (cachedContents != null) {
                return cachedContents;
            }

            cachedContents = new BalloonGasContents(gases);
            return cachedContents;
        }

        String address() {
            return address;
        }

        ItemStack outputTemplate() {
            return outputTemplate.copyWithCount(1);
        }

        List<Candidate> candidates() {
            return candidateView;
        }
    }

    record ScanResult(List<GasGroupCandidates> simpleGroups, Map<Integer, List<Candidate>> orderedPackagesByOrder, @Nullable Candidate firstPassThroughPackage) {
        ScanResult {
            simpleGroups = List.copyOf(simpleGroups);
            Map<Integer, List<Candidate>> immutableOrders = new LinkedHashMap<>();
            orderedPackagesByOrder.forEach((orderId, candidates) -> immutableOrders.put(orderId, List.copyOf(candidates)));
            orderedPackagesByOrder = Collections.unmodifiableMap(immutableOrders);
        }
    }
}
