package net.ty.createcraftedbeginning.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.RegistryBuilder;
import net.ty.createcraftedbeginning.api.airtightcannon.AirtightCannonProjectileBlockHitAction;
import net.ty.createcraftedbeginning.api.airtightcannon.AirtightCannonProjectileEntityHitAction;
import net.ty.createcraftedbeginning.api.airtightcannon.AirtightCannonProjectileRenderMode;
import org.jetbrains.annotations.ApiStatus;

public class CCBBuiltInRegistries {
    public static final Registry<MapCodec<? extends AirtightCannonProjectileRenderMode>> POTATO_PROJECTILE_RENDER_MODE = simple(CCBRegistries.AIRTIGHT_CANNON_PROJECTILE_RENDER_MODE);
    public static final Registry<MapCodec<? extends AirtightCannonProjectileEntityHitAction>> POTATO_PROJECTILE_ENTITY_HIT_ACTION = simple(CCBRegistries.AIRTIGHT_CANNON_PROJECTILE_ENTITY_HIT_ACTION);
    public static final Registry<MapCodec<? extends AirtightCannonProjectileBlockHitAction>> POTATO_PROJECTILE_BLOCK_HIT_ACTION = simple(CCBRegistries.AIRTIGHT_CANNON_PROJECTILE_BLOCK_HIT_ACTION);

    private static <T> Registry<T> simple(ResourceKey<Registry<T>> key) {
        return register(key, false, () -> {
        });
    }

    private static <T> Registry<T> simpleWithFreezeCallback(ResourceKey<Registry<T>> key, Runnable onBakeCallback) {
		return register(key, false, onBakeCallback);
	}

	private static <T> Registry<T> withIntrusiveHolders(ResourceKey<Registry<T>> key) {
		return register(key, true, () -> {});
	}

    @SuppressWarnings({"deprecation", "unchecked", "rawtypes"})
    private static <T> Registry<T> register(ResourceKey<Registry<T>> key, boolean hasIntrusiveHolders, Runnable onBakeCallback) {
        RegistryBuilder<T> builder = new RegistryBuilder<>(key).sync(true);

        if (hasIntrusiveHolders) {
            builder.withIntrusiveHolders();
        }

        builder.onBake(r -> onBakeCallback.run());

        Registry<T> registry = builder.create();
        ((WritableRegistry) BuiltInRegistries.REGISTRY).register(key, registry, RegistrationInfo.BUILT_IN);
        return registry;
    }

    @ApiStatus.Internal
    public static void init() {
    }
}
