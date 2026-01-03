package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.foundation.damageTypes.DamageTypeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import org.jetbrains.annotations.NotNull;

public class CCBDamageTypes {
    public static final ResourceKey<DamageType> CINDER_FIRE = key("cinder_fire");
    public static final ResourceKey<DamageType> BRIMSTONE_FIRE = key("brimstone_fire");
    public static final ResourceKey<DamageType> BRIMSTONE = key("brimstone");
    public static final ResourceKey<DamageType> REACTOR_KETTLE_MIXER = key("reactor_kettle_mixer");

    private static @NotNull ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, CreateCraftedBeginning.asResource(name));
    }

    public static void bootstrap(BootstrapContext<DamageType> ctx) {
        new DamageTypeBuilder(CINDER_FIRE).effects(DamageEffects.BURNING).register(ctx);
        new DamageTypeBuilder(BRIMSTONE_FIRE).effects(DamageEffects.BURNING).register(ctx);
        new DamageTypeBuilder(BRIMSTONE).effects(DamageEffects.HURT).register(ctx);
        new DamageTypeBuilder(REACTOR_KETTLE_MIXER).register(ctx);
    }

    public static @NotNull DamageSource source(ResourceKey<DamageType> key, @NotNull Level level, Entity entity) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key), entity);
    }
}
