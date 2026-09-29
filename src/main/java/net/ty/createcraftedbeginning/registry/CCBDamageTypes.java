package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.foundation.damageTypes.DamageTypeBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.CCBAPI;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBDamageTypes {
    public static final ResourceKey<DamageType> BRIMSTONE_FIRE = key("brimstone_fire");
    public static final ResourceKey<DamageType> BRIMSTONE = key("brimstone");
    public static final ResourceKey<DamageType> REACTOR_KETTLE_MIXER = key("reactor_kettle_mixer");

    public static void bootstrap(BootstrapContext<DamageType> context) {
        new DamageTypeBuilder(BRIMSTONE_FIRE).effects(DamageEffects.BURNING).register(context);
        new DamageTypeBuilder(BRIMSTONE).effects(DamageEffects.HURT).register(context);
        new DamageTypeBuilder(REACTOR_KETTLE_MIXER).register(context);
    }

    public static DamageSource source(ResourceKey<DamageType> key, Level level, Entity entity) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key), entity);
    }

    public static DamageSource source(ResourceKey<DamageType> key, Level level, Entity directEntity, @Nullable Entity causingEntity) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key), directEntity, causingEntity);
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, CCBAPI.asResource(name));
    }
}
