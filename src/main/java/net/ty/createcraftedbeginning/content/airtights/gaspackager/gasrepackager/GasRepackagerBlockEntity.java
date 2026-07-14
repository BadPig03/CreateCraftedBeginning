package net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager;

import com.simibubi.create.compat.computercraft.events.RepackageEvent;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasContents;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonStyleUtils;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonUtils;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasRepackagerBlockEntity extends RepackagerBlockEntity {
    private static final Comparator<Candidate> ORDER_POSITION = Comparator.comparingInt((Candidate candidate) -> PackageItem.getLinkIndex(candidate.box())).thenComparingInt(candidate -> PackageItem.getIndex(candidate.box()));

    public GasRepackagerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.GAS_REPACKAGER.get(), (be, context) -> be.inventory);
    }

    private static List<GasGroupCandidates> groupCandidates(List<Candidate> candidates) {
        List<GasGroupCandidates> groups = new ArrayList<>();
        candidates.forEach(candidate -> addToGroup(groups, candidate, PackageItem.getAddress(candidate.box())));
        return groups;
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

    private static boolean isRepackUseful(GasGroupCandidates group) {
        int inputCount = group.candidates().size();
        if (inputCount < 2) {
            return false;
        }

        long outputCount = countOutputPackages(group.totalAmount());
        return outputCount > 0 && outputCount <= inputCount && (outputCount < inputCount || !isAlreadyCanonical(group));
    }

    private static boolean isAlreadyCanonical(GasGroupCandidates group) {
        long capacity = BalloonUtils.getCapacity();
        if (capacity <= 0) {
            return true;
        }

        long remaining = group.totalAmount();
        for (Candidate candidate : group.candidates()) {
            if (remaining <= 0 || !candidate.box().is(group.outputTemplate().getItem())) {
                return false;
            }

            long expectedAmount = Math.min(capacity, remaining);
            if (candidate.totalAmount() != expectedAmount) {
                return false;
            }

            remaining -= expectedAmount;
        }
        return remaining == 0;
    }

    private static long countOutputPackages(long amount) {
        long capacity = BalloonUtils.getCapacity();
        if (amount <= 0 || capacity <= 0) {
            return 0;
        }

        return (amount - 1) / capacity + 1;
    }

    private static List<BigItemStack> createBalloons(ItemStack outputTemplate, BalloonGasContents inputContents, String address) {
        List<BigItemStack> output = new ArrayList<>();
        long capacity = BalloonUtils.getCapacity();
        BalloonGasContents contents = inputContents.normalized();
        if (!BalloonUtils.isBalloon(outputTemplate) || contents.isEmpty() || capacity <= 0) {
            return output;
        }

        List<GasStack> currentGases = new ArrayList<>();
        long currentAmount = 0;
        for (GasStack sourceGas : contents.gases()) {
            if (sourceGas.isEmpty()) {
                continue;
            }

            long remaining = sourceGas.getAmount();
            while (remaining > 0) {
                long availableSpace = capacity - currentAmount;
                long inserted = Math.min(remaining, availableSpace);
                if (inserted <= 0) {
                    break;
                }

                currentGases.add(sourceGas.copyWithAmount(inserted));
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

        if (!currentGases.isEmpty()) {
            addOutputBalloon(output, outputTemplate, new BalloonGasContents(List.copyOf(currentGases)), address);
        }
        return output;
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

    private static @Nullable PackageOrderWithCrafts findOrderContext(List<Candidate> candidates) {
        List<Candidate> sorted = new ArrayList<>(candidates);
        sorted.sort(ORDER_POSITION.reversed());
        return sorted.stream().map(candidate -> PackageItem.getOrderContext(candidate.box())).filter(context -> context != null && !context.isEmpty()).findFirst().orElse(null);
    }

    private static boolean isOrderComplete(List<Candidate> candidates) {
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

    private static boolean isStandaloneFinalOrderPackage(ItemStack box) {
        return PackageItem.hasOrderData(box) && PackageItem.getLinkIndex(box) == 0 && PackageItem.getIndex(box) == 0 && PackageItem.isFinalLink(box) && PackageItem.isFinal(box);
    }

    private static boolean extractCandidates(IItemHandler targetInv, List<Candidate> candidates) {
        for (Candidate candidate : candidates) {
            ItemStack simulated = targetInv.extractItem(candidate.slot(), 1, true);
            if (isSamePackage(simulated, candidate.box())) {
                continue;
            }

            return false;
        }

        List<Candidate> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingInt(Candidate::slot).reversed());
        for (Candidate candidate : sorted) {
            ItemStack extracted = targetInv.extractItem(candidate.slot(), 1, false);
            if (isSamePackage(extracted, candidate.box())) {
                continue;
            }

            return false;
        }

        return true;
    }

    private static boolean isSamePackage(ItemStack actual, ItemStack expected) {
        return !actual.isEmpty() && !expected.isEmpty() && ItemStack.isSameItemSameComponents(actual.copyWithCount(1), expected.copyWithCount(1));
    }

    private static ScanResult scanPackages(IItemHandler targetInv) {
        ScanResult result = new ScanResult();
        for (int slot = 0; slot < targetInv.getSlots(); slot++) {
            ItemStack extracted = targetInv.extractItem(slot, 1, true);
            if (extracted.isEmpty()) {
                continue;
            }

            ItemStack box = extracted.copyWithCount(1);
            boolean gasBalloon = BalloonUtils.containsGasContents(box);
            BalloonGasContents contents = gasBalloon ? BalloonUtils.getGasContents(box).normalized() : BalloonGasContents.EMPTY;
            Candidate candidate = new Candidate(slot, box, contents);
            if (PackageItem.isPackage(box) && PackageItem.hasOrderData(box)) {
                result.orderedPackagesByOrder.computeIfAbsent(PackageItem.getOrderId(box), ignored -> new ArrayList<>()).add(candidate);
                if (gasBalloon && result.firstPassThrough == null && isStandaloneFinalOrderPackage(box)) {
                    result.firstPassThrough = candidate;
                }
                continue;
            }

            if (!candidate.isGasPackage()) {
                continue;
            }

            addToGroup(result.simpleGroups, candidate, PackageItem.getAddress(box));
            if (result.firstPassThrough != null) {
                continue;
            }

            result.firstPassThrough = candidate;
        }

        return result;
    }

    private static List<BigItemStack> createMixedOrderOutput(int orderId, List<Candidate> candidates) {
        List<Candidate> sorted = new ArrayList<>(candidates);
        sorted.sort(ORDER_POSITION);
        List<Candidate> gasCandidates = sorted.stream().filter(Candidate::isGasPackage).toList();
        if (gasCandidates.isEmpty()) {
            return List.of();
        }

        Map<Integer, List<BigItemStack>> generatedByAnchorSlot = new LinkedHashMap<>();
        Set<Integer> gasSlots = new HashSet<>();
        for (GasGroupCandidates group : groupCandidates(gasCandidates)) {
            long expectedOutputCount = countOutputPackages(group.totalAmount());
            if (expectedOutputCount <= 0 || expectedOutputCount > group.candidates().size()) {
                return List.of();
            }

            List<BigItemStack> generated = createBalloons(group.outputTemplate(), group.contents(), group.address());
            if (generated.size() != expectedOutputCount) {
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
        return output;
    }

    @Override
    public boolean unwrapBox(ItemStack box, boolean simulate) {
        return BalloonUtils.containsGasContents(box) && super.unwrapBox(box, simulate);
    }

    @Override
    protected void attemptToRepackage(IItemHandler targetInv) {
        ScanResult scan = scanPackages(targetInv);
        if (tryRepackageCompletedGasOrder(targetInv, scan) || tryRepackageSimpleGasGroup(targetInv, scan)) {
            return;
        }

        passThroughFirstReadyGasPackage(targetInv, scan);
    }

    private boolean tryRepackageCompletedGasOrder(IItemHandler targetInv, ScanResult scan) {
        for (Entry<Integer, List<Candidate>> entry : scan.orderedPackagesByOrder.entrySet()) {
            int orderId = entry.getKey();
            List<Candidate> candidates = entry.getValue();
            if (candidates.isEmpty() || candidates.stream().noneMatch(Candidate::isGasPackage) || candidates.stream().allMatch(candidate -> isStandaloneFinalOrderPackage(candidate.box()))) {
                continue;
            }

            if (!isOrderComplete(candidates)) {
                continue;
            }

            List<BigItemStack> output = createMixedOrderOutput(orderId, candidates);
            if (output.isEmpty()) {
                continue;
            }

            if (!extractCandidates(targetInv, candidates)) {
                return false;
            }

            queueRepackaged(output);
            return true;
        }

        return false;
    }

    private boolean tryRepackageSimpleGasGroup(IItemHandler targetInv, ScanResult scan) {
        for (GasGroupCandidates group : scan.simpleGroups) {
            if (!isRepackUseful(group)) {
                continue;
            }

            String address = resolveSimpleOutputAddress(group.address());
            List<BigItemStack> output = createBalloons(group.outputTemplate(), group.contents(), address);
            if (output.isEmpty()) {
                continue;
            }

            if (!extractCandidates(targetInv, group.candidates())) {
                return false;
            }

            queueRepackaged(output);
            return true;
        }

        return false;
    }

    private void passThroughFirstReadyGasPackage(IItemHandler targetInv, ScanResult scan) {
        Candidate candidate = scan.firstPassThrough;
        if (candidate == null || !extractCandidates(targetInv, List.of(candidate))) {
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

    private String resolveSimpleOutputAddress(String originalAddress) {
        updateSignAddress();
        return signBasedAddress.isBlank() ? originalAddress : signBasedAddress;
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

    private static final class ScanResult {
        private final List<GasGroupCandidates> simpleGroups = new ArrayList<>();
        private final Map<Integer, List<Candidate>> orderedPackagesByOrder = new LinkedHashMap<>();
        private Candidate firstPassThrough;
    }

    private record Candidate(int slot, ItemStack box, BalloonGasContents contents) {
        private boolean isGasPackage() {
            return !contents.isEmpty();
        }

        private long totalAmount() {
            return contents.totalAmount();
        }
    }

    private static final class GasGroupCandidates {
        private final String address;
        private final List<Candidate> candidates = new ArrayList<>();
        private final List<GasStack> gases = new ArrayList<>();
        private final ItemStack outputTemplate;
        private final boolean rare;

        private long totalAmount;

        private GasGroupCandidates(Candidate first, String address) {
            outputTemplate = first.box().copyWithCount(1);
            rare = BalloonStyleUtils.isRareBalloon(first.box());
            this.address = address;
            add(first);
        }

        private void add(Candidate candidate) {
            candidates.add(candidate);
            BalloonGasContents contents = candidate.contents().normalized();
            contents.gases().stream().filter(gas -> !gas.isEmpty()).map(GasStack::copy).forEach(gases::add);
            long added = contents.totalAmount();
            if (added <= 0) {
                return;
            }

            totalAmount = totalAmount > Long.MAX_VALUE - added ? Long.MAX_VALUE : totalAmount + added;
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

        private BalloonGasContents contents() {
            return new BalloonGasContents(gases.stream().map(GasStack::copy).toList()).normalized();
        }

        private String address() {
            return address;
        }

        private ItemStack outputTemplate() {
            return outputTemplate;
        }

        private List<Candidate> candidates() {
            return candidates;
        }

        private long totalAmount() {
            return totalAmount;
        }
    }
}
