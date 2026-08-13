package net.ty.createcraftedbeginning.compat.functionalstorage.client;

import com.buuz135.functionalstorage.item.FSAttachments;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.compat.functionalstorage.registry.CCBFunctionalStorageBlocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class FunctionalStorageJEICompat {
    private static final ISubtypeInterpreter<ItemStack> DRAWER_SUBTYPE = new ISubtypeInterpreter<>() {
        @Override
        public @Nullable Object getSubtypeData(ItemStack ingredient, UidContext context) {
            return ingredient.get(FSAttachments.TILE);
        }

        @Override
        @SuppressWarnings("deprecated")
        public String getLegacyStringSubtypeInfo(ItemStack ingredient, UidContext context) {
            return ingredient.getOrDefault(FSAttachments.TILE, new CompoundTag()).toString();
        }
    };

    private FunctionalStorageJEICompat() {
    }

    public static void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(CCBFunctionalStorageBlocks.GAS_DRAWER_1_BLOCK.asItem(), DRAWER_SUBTYPE);
        registration.registerSubtypeInterpreter(CCBFunctionalStorageBlocks.GAS_DRAWER_2_BLOCK.asItem(), DRAWER_SUBTYPE);
        registration.registerSubtypeInterpreter(CCBFunctionalStorageBlocks.GAS_DRAWER_4_BLOCK.asItem(), DRAWER_SUBTYPE);
    }
}
