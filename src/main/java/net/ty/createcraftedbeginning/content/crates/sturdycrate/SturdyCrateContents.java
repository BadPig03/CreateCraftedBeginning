package net.ty.createcraftedbeginning.content.crates.sturdycrate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record SturdyCrateContents(ItemStack content, int count, ItemStack filterItem) {
    public static final Codec<SturdyCrateContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(ItemStack.OPTIONAL_CODEC.fieldOf("content").forGetter(contents -> contents.content), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("count").forGetter(contents -> contents.count), ItemStack.OPTIONAL_CODEC.fieldOf("filterItem").forGetter(contents -> contents.filterItem)).apply(instance, SturdyCrateContents::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SturdyCrateContents> STREAM_CODEC = StreamCodec.composite(ItemStack.OPTIONAL_STREAM_CODEC, SturdyCrateContents::content, ByteBufCodecs.VAR_INT, SturdyCrateContents::count, ItemStack.OPTIONAL_STREAM_CODEC, SturdyCrateContents::filterItem, SturdyCrateContents::new);

    @Contract(" -> new")
    public static @NotNull SturdyCrateContents empty() {
        return new SturdyCrateContents(ItemStack.EMPTY, 0, ItemStack.EMPTY);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof SturdyCrateContents(ItemStack content1, int count1, ItemStack filterItem1) && ItemStack.matches(content, content1) && count == count1 && ItemStack.matches(filterItem, filterItem1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ItemStack.hashItemAndComponents(content), count, ItemStack.hashItemAndComponents(filterItem));
    }
}
