package net.ty.createcraftedbeginning.mixin;

import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBMixinConfigPlugin implements IMixinConfigPlugin {
    private static final boolean JEI_RECIPE_TRANSFER_AVAILABLE = hasClassResource();

    private static boolean hasClassResource() {
        ClassLoader classLoader = CCBMixinConfigPlugin.class.getClassLoader();
        return classLoader != null && classLoader.getResource("mezz.jei.api.recipe.transfer.IRecipeTransferError".replace('.', '/') + ".class") != null;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public @Nullable String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !mixinClassName.startsWith("net.ty.createcraftedbeginning.mixin.compat.jei.") || JEI_RECIPE_TRANSFER_AVAILABLE;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public @Nullable List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
