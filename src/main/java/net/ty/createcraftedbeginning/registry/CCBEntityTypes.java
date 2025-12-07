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
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge.AirtightCannonWindChargeProjectileRenderer;
import net.ty.createcraftedbeginning.data.CCBEntityBuilder;
import org.jetbrains.annotations.NotNull;

public class CCBEntityTypes {
    public static final EntityEntry<AirtightCannonWindChargeProjectileEntity> AIRTIGHT_CANNON_WIND_CHARGE_PROJECTILE = register("airtight_cannon_wind_charge_projectile", AirtightCannonWindChargeProjectileEntity::new, () -> AirtightCannonWindChargeProjectileRenderer::new, MobCategory.MISC, 4, 10, true, false, AirtightCannonWindChargeProjectileEntity::build).register();

    @SuppressWarnings("SameParameterValue")
    private static <T extends Entity> @NotNull CCBEntityBuilder<T, ?> register(String name, EntityFactory<T> factory, NonNullSupplier<NonNullFunction<Context, EntityRenderer<? super T>>> renderer, MobCategory group, int range, int updateFrequency, boolean sendVelocity, boolean immuneToFire, NonNullConsumer<Builder<T>> propertyBuilder) {
        return (CCBEntityBuilder<T, ?>) CreateCraftedBeginning.registrate().entity(Lang.asId(name), factory, group).properties(b -> b.setTrackingRange(range).setUpdateInterval(updateFrequency).setShouldReceiveVelocityUpdates(sendVelocity)).properties(propertyBuilder).properties(b -> {
            if (immuneToFire) {
                b.fireImmune();
            }
        }).renderer(renderer);
    }

    public static void register() {
    }
}
