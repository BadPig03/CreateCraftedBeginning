package net.ty.createcraftedbeginning.mixin;

import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBMixinConfigPlugin implements IMixinConfigPlugin {
    private static final String BASIN_TRANSACTION_ACCESS_MIXIN = "net.ty.createcraftedbeginning.mixin.server.create.BasinTransactionAccessMixin";
    private static final boolean BASIN_TRANSACTION_ACCESS_AVAILABLE = hasFieldResource("com.simibubi.create.content.processing.basin.BasinBlockEntity", "spoutputFluidBuffer", "Ljava/util/List;");
    private static final boolean JEI_RECIPE_TRANSFER_AVAILABLE = hasClassResource("mezz.jei.api.recipe.transfer.IRecipeTransferError");
    private static final boolean FUNCTIONAL_STORAGE_AVAILABLE = hasClassResource("com.buuz135.functionalstorage.FunctionalStorage");

    private static boolean hasClassResource(String className) {
        ClassLoader classLoader = CCBMixinConfigPlugin.class.getClassLoader();
        return classLoader != null && classLoader.getResource(className.replace('.', '/') + ".class") != null;
    }

    @SuppressWarnings("SameParameterValue")
    private static boolean hasFieldResource(String className, String fieldName, String descriptor) {
        ClassLoader classLoader = CCBMixinConfigPlugin.class.getClassLoader();
        if (classLoader == null) {
            return false;
        }

        String resourceName = className.replace('.', '/') + ".class";
        try (InputStream inputStream = classLoader.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                return false;
            }

            ClassNode classNode = new ClassNode();
            new ClassReader(inputStream).accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return classNode.fields.stream().anyMatch(field -> field.name.equals(fieldName) && field.desc.equals(descriptor));
        }
        catch (IOException | RuntimeException exception) {
            return false;
        }
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
        if (mixinClassName.equals(BASIN_TRANSACTION_ACCESS_MIXIN)) {
            return BASIN_TRANSACTION_ACCESS_AVAILABLE;
        }

        if (mixinClassName.startsWith("net.ty.createcraftedbeginning.mixin.compat.jei.")) {
            return JEI_RECIPE_TRANSFER_AVAILABLE;
        }
        else if (mixinClassName.startsWith("net.ty.createcraftedbeginning.mixin.compat.functionalstorage.")) {
            return FUNCTIONAL_STORAGE_AVAILABLE;
        }
        return true;
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
