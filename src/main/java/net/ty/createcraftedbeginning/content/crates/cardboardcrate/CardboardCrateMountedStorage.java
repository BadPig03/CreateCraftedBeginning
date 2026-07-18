package net.ty.createcraftedbeginning.content.crates.cardboardcrate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.crates.CrateItemStackHandler;
import net.ty.createcraftedbeginning.content.crates.CrateMountedItemStorage;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CardboardCrateMountedStorage extends CrateMountedItemStorage<CardboardCrateBlockEntity> {
    public static final MapCodec<CardboardCrateMountedStorage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(ItemStack.OPTIONAL_CODEC.fieldOf("content").forGetter(CardboardCrateMountedStorage::getStoredItem), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("count").forGetter(CardboardCrateMountedStorage::getStoredCount), Codec.BOOL.optionalFieldOf("discardedPackage", false).forGetter(CardboardCrateMountedStorage::hasDiscardedPackage)).apply(instance, CardboardCrateMountedStorage::new));

    private boolean discardedPackage;

    public CardboardCrateMountedStorage(ItemStack content, int count) {
        this(content, count, false);
    }

    public CardboardCrateMountedStorage(ItemStack content, int count, boolean discardedPackage) {
        this(CCBMountedStorage.CARDBOARD_CRATE.get(), content, count, discardedPackage);
    }

    protected CardboardCrateMountedStorage(MountedItemStorageType<?> type, ItemStack content, int count, boolean discardedPackage) {
        super(type, CardboardCrateBlockEntity.class, content, count, () -> CCBConfig.server().crates.maxCardboardCapacity.get(), CardboardCrateBlockEntity::isPackage);
        this.discardedPackage = discardedPackage;
    }

    public static CardboardCrateMountedStorage fromBlockEntity(CardboardCrateBlockEntity crate) {
        CrateItemStackHandler handler = crate.getHandler();
        return new CardboardCrateMountedStorage(handler.getStoredItem(0), handler.getCountInSlot(0));
    }

    public boolean hasDiscardedPackage() {
        return discardedPackage;
    }

    @Override
    protected void afterUnmount(CardboardCrateBlockEntity crate) {
        if (!discardedPackage) {
            return;
        }

        crate.awardPackageDisposal();
    }

    @Override
    protected void onTrackedItemDiscarded() {
        discardedPackage = true;
    }
}
