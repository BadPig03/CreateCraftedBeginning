package net.ty.createcraftedbeginning.init;

import com.simibubi.create.api.equipment.potatoCannon.PotatoCannonProjectileType;
import com.simibubi.create.api.equipment.potatoCannon.PotatoCannonProjectileType.Builder;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.equipment.potatoCannon.AllPotatoProjectileEntityHitActions;
import com.simibubi.create.content.equipment.potatoCannon.AllPotatoProjectileEntityHitActions.PotionEffect;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

public class CCBPotatoProjectileTypes {
    public static void bootstrap(BootstrapContext<PotatoCannonProjectileType> context) {
        register(context, "amethyst_ice_cream", new Builder().damage(6).addItems(CCBItems.AMETHYST_ICE_CREAM.asItem()).knockback(0.25f).reloadTicks(20).renderTowardMotion(140, 2).velocity(1.5f).soundPitch(1.0f).onEntityHit(new PotionEffect(MobEffects.MOVEMENT_SLOWDOWN, 1, 300, false)).build());
        register(context, "chocolate_ice_cream", new Builder().damage(8).addItems(CCBItems.CHOCOLATE_ICE_CREAM.asItem()).knockback(0.25f).reloadTicks(20).renderTowardMotion(140, 2).velocity(1.5f).soundPitch(1.0f).onEntityHit(new PotionEffect(MobEffects.MOVEMENT_SLOWDOWN, 1, 300, false)).build());
        register(context, "creative_ice_cream", new Builder().damage(Integer.MAX_VALUE).addItems(CCBItems.CREATIVE_ICE_CREAM.asItem()).knockback(0).reloadTicks(200).renderTowardMotion(140, 2).velocity(1.5f).soundPitch(1.0f).build());
        register(context, "honey_ice_cream", new Builder().damage(8).addItems(CCBItems.HONEY_ICE_CREAM.asItem()).knockback(0.25f).reloadTicks(20).renderTowardMotion(140, 2).velocity(1.5f).soundPitch(1.0f).onEntityHit(new PotionEffect(MobEffects.MOVEMENT_SLOWDOWN, 1, 300, false)).build());
        register(context, "ice_cream", new Builder().damage(5).addItems(CCBItems.ICE_CREAM.asItem()).knockback(0.25f).reloadTicks(20).renderTowardMotion(140, 2).velocity(1.5f).soundPitch(1.0f).onEntityHit(new PotionEffect(MobEffects.MOVEMENT_SLOWDOWN, 1, 300, false)).build());
        register(context, "ice_cream_cone", new Builder().damage(4).addItems(CCBItems.ICE_CREAM_CONE.asItem()).knockback(0.2f).reloadTicks(12).renderTowardMotion(140, 2).velocity(1.5f).soundPitch(1.0f).build());
        register(context, "milk_ice_cream", new Builder().damage(5).addItems(CCBItems.MILK_ICE_CREAM.asItem()).knockback(0.25f).reloadTicks(20).renderTowardMotion(140, 2).velocity(1.5f).soundPitch(1.0f).onEntityHit(new PotionEffect(MobEffects.MOVEMENT_SLOWDOWN, 1, 300, false)).build());
    }

    private static void register(@NotNull BootstrapContext<PotatoCannonProjectileType> context, String name, PotatoCannonProjectileType type) {
        context.register(ResourceKey.create(CreateRegistries.POTATO_PROJECTILE_TYPE, ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, name)), type);
    }
}
