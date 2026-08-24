package net.ty.createcraftedbeginning.core;

public interface Participant<S> {
    boolean validate();

    S snapshot();

    boolean execute();

    void restore(S snapshot);
}
