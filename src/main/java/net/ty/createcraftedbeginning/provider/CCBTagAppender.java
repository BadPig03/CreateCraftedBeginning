package net.ty.createcraftedbeginning.provider;

import net.minecraft.data.tags.TagsProvider.TagAppender;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;

import java.util.function.Function;
import java.util.stream.Stream;

public class CCBTagAppender<T> extends TagAppender<T> {
    private final Function<T, ResourceKey<T>> keyExtractor;

    public CCBTagAppender(TagBuilder builder, Function<T, ResourceKey<T>> pKeyExtractor) {
        super(builder);
        keyExtractor = pKeyExtractor;
    }

    public CCBTagAppender<T> add(T entry) {
        add(keyExtractor.apply(entry));
        return this;
    }

    @SafeVarargs
    public final CCBTagAppender<T> add(T... entries) {
        Stream.of(entries).map(keyExtractor).forEach(this::add);
        return this;
    }
}
