package net.ty.createcraftedbeginning.content.crates.sturdycrate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.crates.CrateItemStackHandler;
import net.ty.createcraftedbeginning.content.crates.FilteredCrateMountedItemStorage;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SturdyCrateMountedStorage extends FilteredCrateMountedItemStorage<SturdyCrateBlockEntity> {
    public static final MapCodec<SturdyCrateMountedStorage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(ItemStack.OPTIONAL_CODEC.fieldOf("content").forGetter(SturdyCrateMountedStorage::getStoredItem), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("count").forGetter(SturdyCrateMountedStorage::getStoredCount), ItemStack.OPTIONAL_CODEC.fieldOf("filterItem").forGetter(SturdyCrateMountedStorage::getFilterItem)).apply(instance, SturdyCrateMountedStorage::new));

    public SturdyCrateMountedStorage(ItemStack content, int count, ItemStack filterItem) {
        this(CCBMountedStorage.STURDY_CRATE.get(), content, count, filterItem);
    }

    protected SturdyCrateMountedStorage(MountedItemStorageType<?> type, ItemStack content, int count, ItemStack filterItem) {
        super(type, SturdyCrateBlockEntity.class, content, count, filterItem, () -> CCBConfig.server().crates.maxSturdyCapacity.get());
    }

    public static SturdyCrateMountedStorage fromBlockEntity(SturdyCrateBlockEntity crate) {
        CrateItemStackHandler handler = crate.getHandler();
        return new SturdyCrateMountedStorage(handler.getStoredItem(0), handler.getCountInSlot(0), crate.getFilterItem());
    }

    @Override
    protected boolean canStoreItem(ItemStack stack) {
        return stack.canFitInsideContainerItems() && super.canStoreItem(stack);
    }
}
