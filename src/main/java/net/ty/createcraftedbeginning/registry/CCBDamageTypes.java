package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.foundation.damageTypes.DamageTypeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageType;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

public class CCBDamageTypes {
    public static final ResourceKey<DamageType>
        CINDER_NOZZLE_FIRE = key("cinder_nozzle_fire");

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, CreateCraftedBeginning.asResource(name));
    }

    public static void bootstrap(BootstrapContext<DamageType> ctx) {
        new DamageTypeBuilder(CINDER_NOZZLE_FIRE).effects(DamageEffects.BURNING).register(ctx);
    }
}
