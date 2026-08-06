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
public final class GasRepackagerUtils {
    private static final Comparator<Candidate> ORDER_POSITION = Comparator.comparingInt((Candidate candidate) -> PackageItem.getLinkIndex(candidate.box())).thenComparingInt(candidate -> PackageItem.getIndex(candidate.box()));

    private GasRepackagerUtils() {
    }

    private static @Nullable PackageOrderWithCrafts findOrderContext(List<Candidate> candidates) {
        List<Candidate> sorted = new ArrayList<>(candidates);
        sorted.sort(ORDER_POSITION.reversed());
        return sorted.stream().map(candidate -> PackageItem.getOrderContext(candidate.box())).filter(context -> context != null && !context.isEmpty()).findFirst().orElse(null);
    }

    private static boolean isAlreadyCanonical(GasGroupCandidates group, List<BigItemStack> canonical) {
        if (canonical.size() != group.candidates().size()) {
            return false;
        }

        for (int i = 0; i < canonical.size(); i++) {
            Candidate candidate = group.candidates().get(i);
            ItemStack expected = canonical.get(i).stack;
            BalloonGasContents expectedContents = BalloonUtils.getGasContents(expected);
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

        ItemStack balloon = BalloonUtils.containingLike(outputTemplate, normalized);
        if (balloon.isEmpty()) {
            return;
        }

        PackageItem.clearAddress(balloon);
        if (!address.isBlank()) {
            PackageItem.addAddress(balloon, address);
        }

        output.add(new BigItemStack(balloon, 1));
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

    public static ExtractionResult extractCandidates(IItemHandler targetInv, List<Candidate> candidates) {
        Set<Integer> candidateSlots = new HashSet<>();
        for (Candidate candidate : candidates) {
            if (candidate.slot() < 0 || candidate.slot() >= targetInv.getSlots() || !candidateSlots.add(candidate.slot())) {
                return ExtractionResult.failed(List.of());
            }

            ItemStack simulated = targetInv.extractItem(candidate.slot(), 1, true);
            if (!isSamePackage(simulated, candidate.box())) {
                return ExtractionResult.failed(List.of());
            }
        }

        List<Candidate> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingInt(Candidate::slot).reversed());
        List<ExtractedItem> extractedItems = new ArrayList<>(sorted.size());
        for (Candidate candidate : sorted) {
            ItemStack extracted = targetInv.extractItem(candidate.slot(), 1, false);
            if (!extracted.isEmpty()) {
                extractedItems.add(new ExtractedItem(candidate.slot(), extracted.copy()));
            }
            if (!isSamePackage(extracted, candidate.box())) {
                return rollbackExtraction(targetInv, extractedItems);
            }
        }
        return ExtractionResult.success();
    }

    private static ExtractionResult rollbackExtraction(IItemHandler targetInv, List<ExtractedItem> extractedItems) {
        List<ItemStack> rollbackRemainders = new ArrayList<>();
        for (int i = extractedItems.size() - 1; i >= 0; i--) {
            ExtractedItem extracted = extractedItems.get(i);
            ItemStack remainder = targetInv.insertItem(extracted.slot(), extracted.stack().copy(), false);
            for (int slot = 0; slot < targetInv.getSlots() && !remainder.isEmpty(); slot++) {
                if (slot == extracted.slot()) {
                    continue;
                }

                remainder = targetInv.insertItem(slot, remainder, false);
            }
            if (!remainder.isEmpty()) {
                rollbackRemainders.add(remainder.copy());
            }
        }
        return ExtractionResult.failed(rollbackRemainders);
    }

    public static boolean isOrderComplete(List<Candidate> candidates) {
        if (candidates.isEmpty()) {
            return false;
        }

        List<Candidate> sorted = new ArrayList<>(candidates);
        sorted.sort(ORDER_POSITION);
        ItemStack firstBox = sorted.getFirst().box();
        if (!PackageItem.hasOrderData(firstBox)) {
            return false;
        }

        int expectedLinkIndex = 0;
        int expectedPackageIndex = 0;
        boolean firstPackageInLink = true;
        boolean currentLinkIsFinal = false;
        int orderId = PackageItem.getOrderId(firstBox);
        for (int i = 0; i < sorted.size(); i++) {
            ItemStack box = sorted.get(i).box();
            if (!PackageItem.hasOrderData(box) || PackageItem.getOrderId(box) != orderId || PackageItem.getLinkIndex(box) != expectedLinkIndex || PackageItem.getIndex(box) != expectedPackageIndex) {
                return false;
            }

            boolean finalLink = PackageItem.isFinalLink(box);
            if (firstPackageInLink) {
                currentLinkIsFinal = finalLink;
                firstPackageInLink = false;
            }
            else if (finalLink != currentLinkIsFinal) {
                return false;
            }

            if (!PackageItem.isFinal(box)) {
                expectedPackageIndex++;
                continue;
            }

            if (currentLinkIsFinal) {
                return i == sorted.size() - 1;
            }

            expectedLinkIndex++;
            expectedPackageIndex = 0;
            firstPackageInLink = true;
        }
        return false;
    }

    public static boolean isRepackUseful(GasGroupCandidates group, List<BigItemStack> output) {
        int inputCount = group.candidates().size();
        if (inputCount < 2) {
            return false;
        }

        int outputCount = output.size();
        return outputCount > 0 && outputCount <= inputCount && (outputCount < inputCount || !isAlreadyCanonical(group, output));
    }

    public static boolean isStandaloneFinalOrderPackage(ItemStack box) {
        return PackageItem.hasOrderData(box) && PackageItem.getLinkIndex(box) == 0 && PackageItem.getIndex(box) == 0 && PackageItem.isFinalLink(box) && PackageItem.isFinal(box);
    }

    public static List<BigItemStack> createBalloons(ItemStack outputTemplate, BalloonGasContents inputContents, String address) {
        List<BigItemStack> output = new ArrayList<>();
        long capacity = BalloonUtils.getCapacity();
        BalloonGasContents contents = inputContents.normalized();
        if (!BalloonUtils.isBalloon(outputTemplate) || contents.isEmpty() || capacity <= 0) {
            return List.of();
        }

        List<GasStack> currentGases = new ArrayList<>();
        long currentAmount = 0;
        for (GasEntry sourceGas : contents.gases()) {
            long remaining = sourceGas.getAmount();
            while (remaining > 0) {
                if (currentGases.size() >= BalloonGasContents.MAX_GAS_TYPES) {
                    addOutputBalloon(output, outputTemplate, new BalloonGasContents(List.copyOf(currentGases)), address);
                    currentGases.clear();
                    currentAmount = 0;
                }

                long availableSpace = capacity - currentAmount;
                long inserted = Math.min(remaining, availableSpace);
                if (inserted <= 0) {
                    break;
                }

                currentGases.add(sourceGas.toStack(inserted));
                currentAmount += inserted;
                remaining -= inserted;
                if (currentAmount < capacity) {
                    continue;
                }

                addOutputBalloon(output, outputTemplate, new BalloonGasContents(List.copyOf(currentGases)), address);
                currentGases.clear();
                currentAmount = 0;
            }
        }

        if (currentGases.isEmpty()) {
            return List.copyOf(output);
        }

        addOutputBalloon(output, outputTemplate, new BalloonGasContents(List.copyOf(currentGases)), address);
        return List.copyOf(output);
    }

    public static List<BigItemStack> createItemOrderPassThroughOutput(List<Candidate> candidates) {
        if (candidates.isEmpty() || candidates.stream().anyMatch(Candidate::isGasPackage) || !isOrderComplete(candidates)) {
            return List.of();
        }

        List<Candidate> sorted = new ArrayList<>(candidates);
        sorted.sort(ORDER_POSITION);
        return sorted.stream().map(candidate -> new BigItemStack(candidate.box().copyWithCount(1), 1)).toList();
    }

    public static List<BigItemStack> createMixedOrderOutput(int orderId, List<Candidate> candidates) {
        List<Candidate> sorted = new ArrayList<>(candidates);
        sorted.sort(ORDER_POSITION);
        List<Candidate> gasCandidates = sorted.stream().filter(Candidate::isGasPackage).toList();
        if (gasCandidates.isEmpty()) {
            return List.of();
        }

        Map<Integer, List<BigItemStack>> generatedByAnchorSlot = new LinkedHashMap<>();
        Set<Integer> gasSlots = new HashSet<>();
        for (GasGroupCandidates group : groupCandidates(gasCandidates)) {
            List<BigItemStack> generated = createBalloons(group.outputTemplate(), group.contents(), group.address());
            if (generated.isEmpty() || generated.size() > group.candidates().size()) {
                return List.of();
            }

            Candidate anchor = group.candidates().stream().min(ORDER_POSITION).orElseThrow();
            generatedByAnchorSlot.put(anchor.slot(), generated);
            group.candidates().forEach(candidate -> gasSlots.add(candidate.slot()));
        }

        List<BigItemStack> output = new ArrayList<>();
        for (Candidate candidate : sorted) {
            if (!gasSlots.contains(candidate.slot())) {
                output.add(new BigItemStack(candidate.box().copyWithCount(1), 1));
                continue;
            }

            List<BigItemStack> generated = generatedByAnchorSlot.get(candidate.slot());
            if (generated == null) {
                continue;
            }

            output.addAll(generated);
        }

        if (output.isEmpty()) {
            return List.of();
        }

        PackageOrderWithCrafts orderContext = findOrderContext(candidates);
        for (int packageIndex = 0; packageIndex < output.size(); packageIndex++) {
            boolean finalPackage = packageIndex == output.size() - 1;
            PackageOrderWithCrafts context = finalPackage ? orderContext : null;
            PackageItem.setOrder(output.get(packageIndex).stack, orderId, 0, true, packageIndex, finalPackage, context);
        }
        return List.copyOf(output);
    }

    public static ScanResult scanPackages(IItemHandler targetInv) {
        List<GasGroupCandidates> simpleGroups = new ArrayList<>();
        Map<Integer, List<Candidate>> orderedPackagesByOrder = new LinkedHashMap<>();
        Candidate firstPassThroughPackage = null;
        for (int slot = 0; slot < targetInv.getSlots(); slot++) {
            ItemStack extracted = targetInv.extractItem(slot, 1, true);
            if (extracted.isEmpty()) {
                continue;
            }

            ItemStack box = extracted.copyWithCount(1);
            BalloonGasContents contents = BalloonUtils.getGasContents(box);
            boolean gasBalloon = !contents.isEmpty();
            Candidate candidate = new Candidate(slot, box, contents);
            if (PackageItem.isPackage(box) && PackageItem.hasOrderData(box)) {
                orderedPackagesByOrder.computeIfAbsent(PackageItem.getOrderId(box), ignored -> new ArrayList<>()).add(candidate);
                if (gasBalloon && firstPassThroughPackage == null && isStandaloneFinalOrderPackage(box)) {
                    firstPassThroughPackage = candidate;
                }
                continue;
            }

            if (!candidate.isGasPackage()) {
                if (PackageItem.isPackage(box) && firstPassThroughPackage == null) {
                    firstPassThroughPackage = candidate;
                }
                continue;
            }

            addToGroup(simpleGroups, candidate, PackageItem.getAddress(box));
            if (firstPassThroughPackage == null) {
                firstPassThroughPackage = candidate;
            }
        }

        return new ScanResult(simpleGroups, orderedPackagesByOrder, firstPassThroughPackage);
    }

    private record ExtractedItem(int slot, ItemStack stack) {}

    public record ExtractionResult(boolean committed, List<ItemStack> rollbackRemainders) {
        public ExtractionResult {
            rollbackRemainders = rollbackRemainders.stream().map(ItemStack::copy).toList();
        }

        private static ExtractionResult success() {
            return new ExtractionResult(true, List.of());
        }

        private static ExtractionResult failed(List<ItemStack> rollbackRemainders) {
            return new ExtractionResult(false, rollbackRemainders);
        }
    }

    public record Candidate(int slot, ItemStack box, BalloonGasContents contents) {
        public Candidate {
            box = box.copyWithCount(1);
        }

        public boolean isGasPackage() {
            return !contents.isEmpty();
        }

    }

    public static final class GasGroupCandidates {
        private final String address;
        private final List<Candidate> candidates = new ArrayList<>();
        private final List<Candidate> candidateView = Collections.unmodifiableList(candidates);
        private final List<GasStack> gases = new ArrayList<>();
        private final ItemStack outputTemplate;
        private final boolean rare;

        @Nullable
        private BalloonGasContents cachedContents;

        private GasGroupCandidates(Candidate first, String address) {
            outputTemplate = first.box().copyWithCount(1);
            rare = BalloonStyleUtils.isRareBalloon(first.box());
            this.address = address;
            add(first);
        }

        private void add(Candidate candidate) {
            candidates.add(candidate);
            BalloonGasContents contents = candidate.contents();
            contents.gases().stream().map(GasEntry::toStack).forEach(gases::add);
            cachedContents = null;
        }

        private boolean accepts(Candidate candidate, String address) {
            ItemStack box = candidate.box();
            if (!this.address.equals(address)) {
                return false;
            }

            if (rare) {
                return BalloonStyleUtils.isRareBalloon(box) && box.is(outputTemplate.getItem());
            }
            return BalloonStyleUtils.isRegularBalloon(box);
        }

        public BalloonGasContents contents() {
            if (cachedContents != null) {
                return cachedContents;
            }

            cachedContents = new BalloonGasContents(gases);
            return cachedContents;
        }

        public String address() {
            return address;
        }

        public ItemStack outputTemplate() {
            return outputTemplate.copyWithCount(1);
        }

        public List<Candidate> candidates() {
            return candidateView;
        }
    }

    public record ScanResult(List<GasGroupCandidates> simpleGroups, Map<Integer, List<Candidate>> orderedPackagesByOrder, @Nullable Candidate firstPassThroughPackage) {
        public ScanResult {
            simpleGroups = List.copyOf(simpleGroups);
            Map<Integer, List<Candidate>> immutableOrders = new LinkedHashMap<>();
            orderedPackagesByOrder.forEach((orderId, candidates) -> immutableOrders.put(orderId, List.copyOf(candidates)));
            orderedPackagesByOrder = Collections.unmodifiableMap(immutableOrders);
        }
    }
}
