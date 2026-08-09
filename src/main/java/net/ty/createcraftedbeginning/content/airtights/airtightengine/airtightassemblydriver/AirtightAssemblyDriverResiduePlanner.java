package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletInsertionTarget;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueInsertionPlan;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.recipe.ResidueGenerationRecipe.ResidueOutput;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightAssemblyDriverResiduePlanner {
    private AirtightAssemblyDriverResiduePlanner() {
    }

    static @Nullable GenerationPlan create(Level level, List<BlockPos> outletPositions, ResidueOutput output, int requiredAmount, int startIndex) {
        int outletCount = outletPositions.size();
        int remainingAmount = requiredAmount;
        int lastOutletIndex = -1;
        List<ResidueInsertionPlan> insertions = new ArrayList<>();
        for (int offset = 0; offset < outletCount && remainingAmount > 0; offset++) {
            int outletIndex = (startIndex + offset) % outletCount;
            ResidueInsertionPlan insertion = createOutletInsertionPlan(outletPositions.get(outletIndex), level, output, remainingAmount);
            if (insertion == null) {
                continue;
            }

            insertions.add(insertion);
            remainingAmount -= insertion.plannedAmount();
            lastOutletIndex = outletIndex;
        }

        return remainingAmount == 0 ? new GenerationPlan(List.copyOf(insertions), lastOutletIndex) : null;
    }

    static boolean commit(GenerationPlan plan) {
        ResourceTransaction transaction = new ResourceTransaction();
        for (ResidueInsertionPlan insertion : plan.insertions()) {
            insertion.addTo(transaction);
        }
        return transaction.commit();
    }

    private static @Nullable ResidueInsertionPlan createOutletInsertionPlan(BlockPos pos, Level level, ResidueOutput output, int maxAmount) {
        if (!(level.getBlockEntity(pos) instanceof ResidueOutletInsertionTarget outlet)) {
            return null;
        }
        return outlet.createResidueInsertionPlan(output.fluidStack(), output.itemStack(), maxAmount);
    }

    record GenerationPlan(List<ResidueInsertionPlan> insertions, int lastOutletIndex) {}
}
