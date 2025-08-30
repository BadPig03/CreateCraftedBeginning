package net.ty.createcraftedbeginning.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBBuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

@Mixin(BuiltInRegistries.class)
public class BuiltInRegistriesMixin {
    static {
        CCBBuiltInRegistries.init();
    }

    @WrapOperation(method = "validate", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;forEach(Ljava/util/function/Consumer;)V"))
    private static <T extends Registry<?>> void create$ourRegistriesAreNotEmpty(Registry<T> instance, Consumer<T> consumer, Operation<Void> original) {
        Consumer<T> callback = (t) -> {
            if (!t.key().location().getNamespace().equals(CreateCraftedBeginning.MOD_ID)) {
                consumer.accept(t);
            }
        };

        original.call(instance, callback);
    }
}
