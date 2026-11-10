package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.rhino.Context;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.compat.kubejs.KubeJSHandlerAdapters;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightArmorsHandlerEvent implements KubeEvent {
    public void add(ResourceLocation location, ResourceLocation profileId, ArmorsHandler handler, float helmet, float chestplate, float leggings, float boots, float elytra) {
        KubeJSHandlerAdapters.registerArmors(location, GameplayPressureProfiles.require(profileId), handler, helmet, chestplate, leggings, boots, elytra);
    }

    public void modify(Context context, ResourceLocation location, ResourceLocation profileId, @Nullable Object changes) {
        try {
            KubeJSHandlerAdapters.modifyArmors(context, location, GameplayPressureProfiles.require(profileId), changes);
        }
        catch (IllegalArgumentException | IllegalStateException failure) {
            CCBAPI.LOGGER.error("Failed to modify airtight armor handler for gas '{}' and gameplay pressure profile '{}': {}", location, profileId, failure.getMessage());
        }
    }

    @FunctionalInterface
    public interface ArmorsHandler {
        boolean apply(MobEffectInstance effectInstance);
    }
}
