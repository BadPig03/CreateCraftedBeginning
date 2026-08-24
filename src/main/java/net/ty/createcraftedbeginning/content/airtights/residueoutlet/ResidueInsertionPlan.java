package net.ty.createcraftedbeginning.content.airtights.residueoutlet;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.core.Participant;
import net.ty.createcraftedbeginning.core.ResourceTransaction;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record ResidueInsertionPlan(int plannedAmount, Participant<?> participant) {
    public ResidueInsertionPlan {
        if (plannedAmount <= 0) {
            throw new IllegalArgumentException("A residue insertion plan must contain a positive amount");
        }
    }

    public void addTo(ResourceTransaction transaction) {
        transaction.add(participant);
    }
}
