package net.ty.createcraftedbeginning.platform.client;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip.Section;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface GoggleItemStorageSource {
    static @Nullable Section section(BlockEntity source, @Nullable BlockEntity observed) {
        if (source instanceof GoggleItemStorageSource storageSource) {
            return storageSource.getItemStorageSection(observed);
        }

        return Section.ITEM_STORAGE;
    }

    @Nullable Section getItemStorageSection(@Nullable BlockEntity observed);
}
