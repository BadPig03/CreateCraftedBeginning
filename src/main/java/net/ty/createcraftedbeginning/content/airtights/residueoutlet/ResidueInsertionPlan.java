package net.ty.createcraftedbeginning.content.airtights.residueoutlet;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ResidueInsertionPlan {
    int plannedAmount();

    void addTo(ResourceTransaction transaction);
}
