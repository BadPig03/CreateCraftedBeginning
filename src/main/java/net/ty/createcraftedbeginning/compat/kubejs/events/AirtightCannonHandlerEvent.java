package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.rhino.Context;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlers;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonShotContext;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.compat.kubejs.KubeJSCannonHandler;
import net.ty.createcraftedbeginning.compat.kubejs.KubeJSToolHandlerPatches;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightCannonHandlerEvent implements KubeEvent {
    public void add(Context context, ResourceLocation location, ResourceLocation profileId, @Nullable Object options) {
        try {
            AirtightCannonHandlers.register(location, GameplayPressureProfiles.require(profileId), new KubeJSCannonHandler(context, options));
        }
        catch (IllegalArgumentException | IllegalStateException failure) {
            CCBAPI.LOGGER.error("Failed to register configured airtight cannon handler for gas '{}' and gameplay pressure profile '{}': {}", location, profileId, failure.getMessage());
        }
    }

    public void modify(ResourceLocation location, ResourceLocation profileId, @Nullable Object changes) {
        try {
            AirtightCannonHandlers.modify(location, GameplayPressureProfiles.require(profileId), KubeJSToolHandlerPatches.readCannon(changes));
        }
        catch (IllegalArgumentException | IllegalStateException failure) {
            CCBAPI.LOGGER.error("Failed to modify airtight cannon handler for gas '{}' and gameplay pressure profile '{}': {}", location, profileId, failure.getMessage());
        }
    }

    @FunctionalInterface
    public interface IconCannonHandler {
        ResourceLocation apply(Level level);
    }

    @FunctionalInterface
    public interface ParticlesCannonHandler {
        void apply(Level level, Vec3 pos, Vec3 velocity);
    }

    @FunctionalInterface
    public interface ExplodeCannonHandler {
        void apply(Level level, Vec3 pos, AirtightCannonShotContext context);
    }

    @FunctionalInterface
    public interface TextCannonHandler {
        void apply(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag flag);
    }
}
