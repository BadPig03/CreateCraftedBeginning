package net.ty.createcraftedbeginning.data;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.ty.createcraftedbeginning.registry.CCBDamageTypes;

public class CCBDamageSources {
    public static DamageSource cinderNozzleFire(Level level) {
        return source(CCBDamageTypes.CINDER_NOZZLE_FIRE, level);
    }

    private static DamageSource source(ResourceKey<DamageType> key, LevelReader level) {
        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        return new DamageSource(registry.getHolderOrThrow(key));
    }
}
