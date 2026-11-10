package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.rhino.Context;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandlers;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.compat.kubejs.KubeJSHandlerAdapters;
import net.ty.createcraftedbeginning.compat.kubejs.KubeJSToolHandlerPatches;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightDrillHandlerEvent implements KubeEvent {
    public void add(ResourceLocation location, ResourceLocation profileId, int damage, double consumption) {
        AirtightDrillHandlers.register(location, GameplayPressureProfiles.require(profileId), damage, consumption);
    }

    public void add(Context context, ResourceLocation location, ResourceLocation profileId, @Nullable Object options) {
        try {
            KubeJSHandlerAdapters.registerDrill(context, location, GameplayPressureProfiles.require(profileId), options);
        }
        catch (IllegalArgumentException | IllegalStateException failure) {
            CCBAPI.LOGGER.error("Failed to register configured airtight drill handler for gas '{}' and gameplay pressure profile '{}': {}", location, profileId, failure.getMessage());
        }
    }

    public void modify(ResourceLocation location, ResourceLocation profileId, @Nullable Object changes) {
        try {
            AirtightDrillHandlers.modify(location, GameplayPressureProfiles.require(profileId), KubeJSToolHandlerPatches.readDrill(changes));
        }
        catch (IllegalArgumentException | IllegalStateException failure) {
            CCBAPI.LOGGER.error("Failed to modify airtight drill handler for gas '{}' and gameplay pressure profile '{}': {}", location, profileId, failure.getMessage());
        }
    }

    @FunctionalInterface
    public interface HitDrillHandler {
        void apply(LivingEntity target, Player player, ServerLevel level);
    }

    @FunctionalInterface
    public interface TooltipDrillHandler {
        void apply(ItemStack drill, TooltipContext context, List<Component> tooltip, TooltipFlag flag);
    }
}
