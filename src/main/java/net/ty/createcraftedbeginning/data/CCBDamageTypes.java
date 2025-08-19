package net.ty.createcraftedbeginning.data;

import com.simibubi.create.foundation.damageTypes.DamageTypeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageType;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

public class CCBDamageTypes {
    public static final ResourceKey<DamageType> CINDER_INCINERATION_BLOWER_FIRE_FIRE = key("cinder_incineration_blower_fire");

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, CreateCraftedBeginning.asResource(name));
    }

    public static void bootstrap(BootstrapContext<DamageType> ctx) {
        new DamageTypeBuilder(CINDER_INCINERATION_BLOWER_FIRE_FIRE).effects(DamageEffects.BURNING).register(ctx);
    }
}
