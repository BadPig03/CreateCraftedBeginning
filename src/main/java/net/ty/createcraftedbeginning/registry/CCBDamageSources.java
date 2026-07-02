package net.ty.createcraftedbeginning.registry;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBDamageSources {
    public static DamageSource brimstoneFire(Level level) {
        return register(CCBDamageTypes.BRIMSTONE_FIRE, level);
    }

    public static DamageSource brimstone(Level level) {
        return register(CCBDamageTypes.BRIMSTONE, level);
    }

    public static DamageSource reactorKettleMixer(Level level) {
		return register(CCBDamageTypes.REACTOR_KETTLE_MIXER, level);
	}

    private static DamageSource register(ResourceKey<DamageType> key, LevelReader level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
    }
}
