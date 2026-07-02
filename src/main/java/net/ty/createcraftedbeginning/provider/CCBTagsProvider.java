package net.ty.createcraftedbeginning.provider;

import com.tterrag.registrate.providers.RegistrateTagsProvider;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder.Reference;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBTagsProvider<T> {
    private final RegistrateTagsProvider<T> provider;
    private final Function<T, ResourceKey<T>> keyExtractor;

    @Contract(pure = true)
    public CCBTagsProvider(RegistrateTagsProvider<T> provider, Function<T, Reference<T>> refExtractor) {
        this.provider = provider;
        keyExtractor = refExtractor.andThen(Reference::key);
    }

    public CCBTagAppender<T> tag(TagKey<T> tag) {
        TagBuilder tagbuilder = getOrCreateRawBuilder(tag);
        return new CCBTagAppender<>(tagbuilder, keyExtractor);
    }

    public TagBuilder getOrCreateRawBuilder(TagKey<T> tag) {
        return provider.addTag(tag).getInternalBuilder();
    }
}
