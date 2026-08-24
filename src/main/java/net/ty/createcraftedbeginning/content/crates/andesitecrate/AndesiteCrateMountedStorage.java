package net.ty.createcraftedbeginning.content.crates.andesitecrate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.crates.CrateMountedItemStorage;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AndesiteCrateMountedStorage extends CrateMountedItemStorage<AndesiteCrateBlockEntity> {
    public static final MapCodec<AndesiteCrateMountedStorage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(ItemStack.OPTIONAL_CODEC.fieldOf("content").forGetter(AndesiteCrateMountedStorage::getStoredItem), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("count").forGetter(AndesiteCrateMountedStorage::getStoredCount)).apply(instance, AndesiteCrateMountedStorage::new));

    private AndesiteCrateMountedStorage(ItemStack content, int count) {
        this(CCBMountedStorage.ANDESITE_CRATE.get(), content, count);
    }

    private AndesiteCrateMountedStorage(MountedItemStorageType<?> type, ItemStack content, int count) {
        super(type, AndesiteCrateBlockEntity.class, content, count, () -> CCBConfig.server().crates.maxAndesiteCapacity.get());
    }

    public static AndesiteCrateMountedStorage fromBlockEntity(AndesiteCrateBlockEntity crate) {
        return new AndesiteCrateMountedStorage(crate.getStoredItem(), crate.getStoredCount());
    }
}
