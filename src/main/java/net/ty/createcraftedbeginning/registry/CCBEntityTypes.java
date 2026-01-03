package net.ty.createcraftedbeginning.registry;

import com.tterrag.registrate.util.entry.EntityEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.createmod.catnip.lang.Lang;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.EntityType.EntityFactory;
import net.minecraft.world.entity.MobCategory;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge.AirtightCannonWindChargeProjectileEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge.AirtightCannonWindChargeProjectileEntityRenderer;
import net.ty.createcraftedbeginning.content.airtights.weatherflares.WeatherFlareProjectileEntity;
import net.ty.createcraftedbeginning.content.airtights.weatherflares.WeatherFlareProjectileRenderer;
import net.ty.createcraftedbeginning.data.CCBEntityBuilder;
import org.jetbrains.annotations.NotNull;

public class CCBEntityTypes {
    public static final EntityEntry<AirtightCannonWindChargeProjectileEntity> AIRTIGHT_CANNON_WIND_CHARGE_PROJECTILE = register("airtight_cannon_wind_charge_projectile", AirtightCannonWindChargeProjectileEntity::new, () -> AirtightCannonWindChargeProjectileEntityRenderer::new, false, AirtightCannonWindChargeProjectileEntity::build).register();
    public static final EntityEntry<WeatherFlareProjectileEntity> WEATHER_FLARE_PROJECTILE = register("weather_flare_projectile", WeatherFlareProjectileEntity::new, () -> WeatherFlareProjectileRenderer::new, true, WeatherFlareProjectileEntity::build).register();

    private static <T extends Entity> @NotNull CCBEntityBuilder<T, ?> register(String name, EntityFactory<T> factory, NonNullSupplier<NonNullFunction<Context, EntityRenderer<? super T>>> renderer, boolean immuneToFire, NonNullConsumer<Builder<T>> propertyBuilder) {
        return (CCBEntityBuilder<T, ?>) CreateCraftedBeginning.registrate().entity(Lang.asId(name), factory, MobCategory.MISC).properties(b -> b.setTrackingRange(4).setUpdateInterval(10).setShouldReceiveVelocityUpdates(true)).properties(propertyBuilder).properties(b -> {
            if (immuneToFire) {
                b.fireImmune();
            }
        }).renderer(renderer);
    }

    public static void register() {
    }
}
