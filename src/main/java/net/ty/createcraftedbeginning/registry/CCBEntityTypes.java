package net.ty.createcraftedbeginning.registry;

import com.tterrag.registrate.util.entry.EntityEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.createmod.catnip.lang.Lang;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtightcannon.AirtightCannonProjectileEntity;
import net.ty.createcraftedbeginning.content.airtightcannon.AirtightCannonProjectileRenderer;
import net.ty.createcraftedbeginning.data.CCBEntityBuilder;

public class CCBEntityTypes {
    public static final EntityEntry<AirtightCannonProjectileEntity> AIRTIGHT_CANNON_PROJECTILE = register("airtight_cannon_projectile", AirtightCannonProjectileEntity::new, () -> AirtightCannonProjectileRenderer::new, MobCategory.MISC, 4, 20, true, false, AirtightCannonProjectileEntity::build).register();

    private static <T extends Entity> CCBEntityBuilder<T, ?> register(String name, EntityType.EntityFactory<T> factory, NonNullSupplier<NonNullFunction<EntityRendererProvider.Context, EntityRenderer<? super T>>> renderer, MobCategory group, int range, int updateFrequency, boolean sendVelocity, boolean immuneToFire, NonNullConsumer<EntityType.Builder<T>> propertyBuilder) {
        String id = Lang.asId(name);
        return (CCBEntityBuilder<T, ?>) CreateCraftedBeginning.registrate().entity(id, factory, group).properties(b -> b.setTrackingRange(range).setUpdateInterval(updateFrequency).setShouldReceiveVelocityUpdates(sendVelocity)).properties(propertyBuilder).properties(b -> {
            if (immuneToFire) {
                b.fireImmune();
            }
        }).renderer(renderer);
    }

    public static void register() {
	}
}
