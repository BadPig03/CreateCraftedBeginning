package net.ty.createcraftedbeginning.mixin.common.minecraft;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.tags.TagsProvider;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.datagen.tag.TagGenerationState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(TagsProvider.class)
public abstract class TagsProviderMixin {
    @Shadow
    @Final
    protected String modId;

    @Unique
    private final TagGenerationState ccb$generation = new TagGenerationState();

    @WrapMethod(method = "createContentsProvider")
    private CompletableFuture<Provider> ccb$createContentsProvider(Operation<CompletableFuture<Provider>> original) {
        if (!CCBAPI.MOD_ID.equals(modId) || !(this instanceof RegistrateTagsProvider<?>)) {
            return original.call();
        }

        return ccb$generation.getOrCreate(original::call);
    }
}
