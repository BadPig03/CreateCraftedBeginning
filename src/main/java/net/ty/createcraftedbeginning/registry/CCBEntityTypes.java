package net.ty.createcraftedbeginning.registry;

import com.tterrag.registrate.builders.EntityBuilder;
import com.tterrag.registrate.util.entry.EntityEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.EntityType.EntityFactory;
import net.minecraft.world.entity.MobCategory;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge.AirtightCannonWindChargeProjectileEntity;
import net.ty.createcraftedbeginning.content.airtights.weatherflares.projectile.WeatherFlareProjectileEntity;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBEntityTypes {
    public static final EntityEntry<AirtightCannonWindChargeProjectileEntity> AIRTIGHT_CANNON_WIND_CHARGE_PROJECTILE = CCBEntityTypes.<AirtightCannonWindChargeProjectileEntity>register("airtight_cannon_wind_charge_projectile", AirtightCannonWindChargeProjectileEntity::new, false, AirtightCannonWindChargeProjectileEntity::build).register();
    public static final EntityEntry<WeatherFlareProjectileEntity> WEATHER_FLARE_PROJECTILE = CCBEntityTypes.<WeatherFlareProjectileEntity>register("weather_flare_projectile", WeatherFlareProjectileEntity::new, true, WeatherFlareProjectileEntity::build).register();

    private static <T extends Entity> @NotNull EntityBuilder<T, ?> register(String name, EntityFactory<T> factory, boolean immuneToFire, NonNullConsumer<Builder<T>> propertyBuilder) {
        return CreateCraftedBeginning.registrate().entity(Lang.asId(name), factory, MobCategory.MISC).defaultLang().properties(b -> b.setTrackingRange(4).setUpdateInterval(10).setShouldReceiveVelocityUpdates(true)).properties(propertyBuilder).properties(b -> {
            if (immuneToFire) {
                b.fireImmune();
            }
        });
    }

    public static void register() {
    }
}
