package net.ty.createcraftedbeginning.content.crates.sturdycrate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.crates.CrateInventoryState;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record SturdyCrateContents(ItemStack content, int count, ItemStack filterItem) {
    public static final Codec<SturdyCrateContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(ItemStack.OPTIONAL_CODEC.fieldOf("content").forGetter(SturdyCrateContents::content), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("count").forGetter(SturdyCrateContents::count), ItemStack.OPTIONAL_CODEC.fieldOf("filterItem").forGetter(SturdyCrateContents::filterItem)).apply(instance, SturdyCrateContents::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SturdyCrateContents> STREAM_CODEC = StreamCodec.composite(ItemStack.OPTIONAL_STREAM_CODEC, SturdyCrateContents::content, ByteBufCodecs.VAR_INT, SturdyCrateContents::count, ItemStack.OPTIONAL_STREAM_CODEC, SturdyCrateContents::filterItem, SturdyCrateContents::new);

    public SturdyCrateContents {
        CrateInventoryState normalizedInventory = CrateInventoryState.normalize(content, count, Integer.MAX_VALUE);
        content = normalizedInventory.content();
        count = normalizedInventory.count();
        filterItem = filterItem.isEmpty() ? ItemStack.EMPTY : filterItem.copyWithCount(1);
    }

    @Contract(" -> new")
    public static SturdyCrateContents empty() {
        return new SturdyCrateContents(ItemStack.EMPTY, 0, ItemStack.EMPTY);
    }

    @Override
    public ItemStack content() {
        return content.isEmpty() ? ItemStack.EMPTY : content.copy();
    }

    @Override
    public ItemStack filterItem() {
        return filterItem.isEmpty() ? ItemStack.EMPTY : filterItem.copy();
    }

    public boolean hasInventory() {
        return !content.isEmpty() && count > 0;
    }

    private boolean hasFilter() {
        return !filterItem.isEmpty();
    }

    public boolean hasData() {
        return hasInventory() || hasFilter();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof SturdyCrateContents(ItemStack otherContent, int otherCount, ItemStack otherFilter) && ItemStack.matches(content, otherContent) && count == otherCount && ItemStack.matches(filterItem, otherFilter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ItemStack.hashItemAndComponents(content), count, ItemStack.hashItemAndComponents(filterItem));
    }
}
