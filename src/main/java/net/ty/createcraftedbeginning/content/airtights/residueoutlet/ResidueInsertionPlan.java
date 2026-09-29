package net.ty.createcraftedbeginning.content.airtights.residueoutlet;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.foundation.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.foundation.transaction.TransactionParticipant;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record ResidueInsertionPlan(int plannedAmount, TransactionParticipant<?> participant) {
    public ResidueInsertionPlan {
        if (plannedAmount <= 0) {
            throw new IllegalArgumentException("Residue insertion plan amount must be positive; got " + plannedAmount + '.');
        }
    }

    public void addTo(ResourceTransaction transaction) {
        transaction.add(participant);
    }
}
