package net.ty.createcraftedbeginning.content.airtightcannon;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.airtightcannon.AirtightCannonProjectileType;
import net.ty.createcraftedbeginning.registry.CCBRegistries;

public class CCBAirtightCannonProjectileTypes {
    public static final ResourceKey<AirtightCannonProjectileType> FALLBACK = ResourceKey.create(CCBRegistries.AIRTIGHT_CANNON_PROJECTILE_TYPE, CreateCraftedBeginning.asResource("fallback"));

    public static void bootstrap(BootstrapContext<AirtightCannonProjectileType> ctx) {
        register(ctx, "fallback", new AirtightCannonProjectileType.Builder().damage(0).build());

        register(ctx, "gold_nugget", new AirtightCannonProjectileType.Builder().damage(20).reloadTicks(15).velocity(3f).knockback(1.5f).gravity(0).renderTumbling().addItems(Items.GOLD_NUGGET).build());

        register(ctx, "iron_nugget", new AirtightCannonProjectileType.Builder().damage(5).reloadTicks(15).velocity(1.25f).knockback(1.5f).renderTumbling().addItems(Items.IRON_NUGGET).build());
    }

    private static void register(BootstrapContext<AirtightCannonProjectileType> ctx, String name, AirtightCannonProjectileType type) {
		ctx.register(ResourceKey.create(CCBRegistries.AIRTIGHT_CANNON_PROJECTILE_TYPE, CreateCraftedBeginning.asResource(name)), type);
	}
}
