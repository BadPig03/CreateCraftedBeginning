package net.ty.createcraftedbeginning.content.crates.brasscrate;

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
public class BrassCrateMountedStorage extends FilteredCrateMountedItemStorage<BrassCrateBlockEntity> {
    public static final MapCodec<BrassCrateMountedStorage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(ItemStack.OPTIONAL_CODEC.fieldOf("content").forGetter(BrassCrateMountedStorage::getStoredItem), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("count").forGetter(BrassCrateMountedStorage::getStoredCount), ItemStack.OPTIONAL_CODEC.fieldOf("filterItem").forGetter(BrassCrateMountedStorage::getFilterItem)).apply(instance, BrassCrateMountedStorage::new));

    public BrassCrateMountedStorage(ItemStack content, int count, ItemStack filterItem) {
        this(CCBMountedStorage.BRASS_CRATE.get(), content, count, filterItem);
    }

    protected BrassCrateMountedStorage(MountedItemStorageType<?> type, ItemStack content, int count, ItemStack filterItem) {
        super(type, BrassCrateBlockEntity.class, content, count, filterItem, () -> CCBConfig.server().crates.maxBrassCapacity.get());
    }

    public static BrassCrateMountedStorage fromBlockEntity(BrassCrateBlockEntity crate) {
        CrateItemStackHandler handler = crate.getHandler();
        return new BrassCrateMountedStorage(handler.getStoredItem(0), handler.getCountInSlot(0), crate.getFilterItem());
    }
}
