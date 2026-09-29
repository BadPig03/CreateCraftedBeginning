package net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerScan.Candidate;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasRepackagerExtraction {

    private GasRepackagerExtraction() {
    }

    @Internal
    public static ExtractionResult extractCandidates(IItemHandler targetInv, List<Candidate> candidates) {
        Set<Integer> candidateSlots = new HashSet<>();
        for (Candidate candidate : candidates) {
            if (candidate.slot() < 0 || candidate.slot() >= targetInv.getSlots() || !candidateSlots.add(candidate.slot())) {
                return ExtractionResult.failed(List.of());
            }

            ItemStack simulatedExtraction = targetInv.extractItem(candidate.slot(), 1, true);
            if (isSamePackage(simulatedExtraction, candidate.box())) {
                continue;
            }

            return ExtractionResult.failed(List.of());
        }

        List<Candidate> sortedCandidates = new ArrayList<>(candidates);
        sortedCandidates.sort(Comparator.comparingInt(Candidate::slot).reversed());
        List<ExtractedItem> extractedItems = new ArrayList<>(sortedCandidates.size());
        for (Candidate candidate : sortedCandidates) {
            ItemStack extractedStack = targetInv.extractItem(candidate.slot(), 1, false);
            if (!extractedStack.isEmpty()) {
                extractedItems.add(new ExtractedItem(candidate.slot(), extractedStack.copy()));
            }
            if (isSamePackage(extractedStack, candidate.box())) {
                continue;
            }

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
                if (rollbackRemainder.isEmpty()) {
                    continue;
                }

                rollbackRemainders.add(rollbackRemainder.copy());
            }
            return ExtractionResult.failed(rollbackRemainders);

        }

        return new ExtractionResult(true, List.of());
    }

    @Internal
    public record ExtractionResult(boolean committed, List<ItemStack> rollbackRemainders) {
        @Internal
        public ExtractionResult {
            rollbackRemainders = rollbackRemainders.stream().map(ItemStack::copy).toList();
        }

        private static ExtractionResult failed(List<ItemStack> rollbackRemainders) {
            return new ExtractionResult(false, rollbackRemainders);
        }
    }

    private static boolean isSamePackage(ItemStack actual, ItemStack expected) {
        return !actual.isEmpty() && !expected.isEmpty() && ItemStack.isSameItemSameComponents(actual.copyWithCount(1), expected.copyWithCount(1));
    }

    private record ExtractedItem(int slot, ItemStack stack) {}
}
