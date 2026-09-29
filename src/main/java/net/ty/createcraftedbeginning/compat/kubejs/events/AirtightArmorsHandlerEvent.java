package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.compat.kubejs.KubeJSHandlerAdapters;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightArmorsHandlerEvent implements KubeEvent {
    public void add(ResourceLocation location, ResourceLocation profileId, ArmorsHandler handler, float helmet, float chestplate, float leggings, float boots, float elytra) {
        KubeJSHandlerAdapters.registerArmors(location, GameplayPressureProfiles.require(profileId), handler, helmet, chestplate, leggings, boots, elytra);
    }

    @FunctionalInterface
    public interface ArmorsHandler {
        boolean apply(MobEffectInstance effectInstance);
    }
}
