package net.ty.createcraftedbeginning.datagen.tag;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class TagGenerationState {
    private @Nullable CompletableFuture<Provider> contents;

    public synchronized CompletableFuture<Provider> getOrCreate(Supplier<CompletableFuture<Provider>> generator) {
        if (contents == null) {
            contents = generator.get();
        }

        return contents;
    }
}
