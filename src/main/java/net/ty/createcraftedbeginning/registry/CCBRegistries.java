package net.ty.createcraftedbeginning.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.airtightcannon.AirtightCannonProjectileBlockHitAction;
import net.ty.createcraftedbeginning.api.airtightcannon.AirtightCannonProjectileEntityHitAction;
import net.ty.createcraftedbeginning.api.airtightcannon.AirtightCannonProjectileRenderMode;
import net.ty.createcraftedbeginning.api.airtightcannon.AirtightCannonProjectileType;

public class CCBRegistries {
	public static final ResourceKey<Registry<AirtightCannonProjectileType>> AIRTIGHT_CANNON_PROJECTILE_TYPE = key("airtight_cannon_projectile/type");
    public static final ResourceKey<Registry<MapCodec<? extends AirtightCannonProjectileRenderMode>>> AIRTIGHT_CANNON_PROJECTILE_RENDER_MODE = key("airtight_cannon_projectile/render_mode");
	public static final ResourceKey<Registry<MapCodec<? extends AirtightCannonProjectileEntityHitAction>>> AIRTIGHT_CANNON_PROJECTILE_ENTITY_HIT_ACTION = key("airtight_cannon_projectile/entity_hit_action");
	public static final ResourceKey<Registry<MapCodec<? extends AirtightCannonProjectileBlockHitAction>>> AIRTIGHT_CANNON_PROJECTILE_BLOCK_HIT_ACTION = key("airtight_cannon_projectile/block_hit_action");

	private static <T> ResourceKey<Registry<T>> key(String name) {
		return ResourceKey.createRegistryKey(CreateCraftedBeginning.asResource(name));
	}
}
