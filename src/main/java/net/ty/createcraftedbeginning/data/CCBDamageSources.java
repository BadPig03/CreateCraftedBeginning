package net.ty.createcraftedbeginning.data;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.NotNull;

public class CCBDamageSources {
    public static @NotNull DamageSource cinderNozzleFire(Level level) {
        return source(CCBDamageTypes.CINDER_INCINERATION_BLOWER_FIRE_FIRE, level);
    }

    @SuppressWarnings("SameParameterValue")
    private static @NotNull DamageSource source(ResourceKey<DamageType> key, @NotNull LevelReader level) {
        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        return new DamageSource(registry.getHolderOrThrow(key));
    }
}
