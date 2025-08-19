package net.ty.createcraftedbeginning.data;

import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.core.Holder;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

import java.util.function.Function;
import java.util.stream.Stream;

public class CCBTagGen {
    public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> axeOrPickaxe() {
        return b -> b.tag(BlockTags.MINEABLE_WITH_AXE).tag(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> axeOnly() {
        return b -> b.tag(BlockTags.MINEABLE_WITH_AXE);
    }

    public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> pickaxeOnly() {
        return b -> b.tag(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, ItemBuilder<BlockItem, BlockBuilder<T, P>>> tagBlockAndItem(String... path) {
        return b -> {
            for (String p : path) {
                b.tag(CCBTags.commonBlockTag(p));
            }
            ItemBuilder<BlockItem, BlockBuilder<T, P>> item = b.item();
            for (String p : path) {
                item.tag(CCBTags.commonItemTag(p));
            }
            return item;
        };
    }

    public static class CCBTagsProvider<T> {
        private final RegistrateTagsProvider<T> provider;
        private final Function<T, ResourceKey<T>> keyExtractor;

        public CCBTagsProvider(RegistrateTagsProvider<T> provider, Function<T, Holder.Reference<T>> refExtractor) {
            this.provider = provider;
            this.keyExtractor = refExtractor.andThen(Holder.Reference::key);
        }

        public CCBTagAppender<T> tag(TagKey<T> tag) {
            TagBuilder tagbuilder = getOrCreateRawBuilder(tag);
            return new CCBTagAppender<>(tagbuilder, keyExtractor);
        }

        public TagBuilder getOrCreateRawBuilder(TagKey<T> tag) {
            return provider.addTag(tag).getInternalBuilder();
        }
    }

    public static class CCBTagAppender<T> extends TagsProvider.TagAppender<T> {

        private final Function<T, ResourceKey<T>> keyExtractor;

        public CCBTagAppender(TagBuilder pBuilder, Function<T, ResourceKey<T>> pKeyExtractor) {
            super(pBuilder);
            this.keyExtractor = pKeyExtractor;
        }

        public CCBTagAppender<T> add(T entry) {
            this.add(this.keyExtractor.apply(entry));
            return this;
        }

        @SafeVarargs
        public final CCBTagAppender<T> add(T... entries) {
            Stream.of(entries).map(this.keyExtractor).forEach(this::add);
            return this;
        }
    }
}
