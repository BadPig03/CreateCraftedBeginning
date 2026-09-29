package net.ty.createcraftedbeginning.foundation.transaction;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface TransactionParticipant<S> {
    boolean validate();

    S snapshot();

    boolean execute();

    void restore(S snapshot);
}
