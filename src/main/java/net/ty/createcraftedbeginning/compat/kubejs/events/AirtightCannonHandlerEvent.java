package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlers;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonShotContext;
import net.ty.createcraftedbeginning.api.cannonhandlers.DefaultCannonHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightCannonHandlerEvent implements KubeEvent {
    public void add(ResourceLocation location, ResourceLocation profileId, IconCannonHandler icon, ParticlesCannonHandler particles, ExplodeCannonHandler explode, ResourceLocation texture, float speed, float consumption, TextCannonHandler text) {
        AirtightCannonHandlers.register(location, GameplayPressureProfiles.require(profileId), createHandler(icon, particles, explode, texture, speed, consumption, text));
    }

    private static AirtightCannonHandler createHandler(IconCannonHandler icon, ParticlesCannonHandler particles, ExplodeCannonHandler explode, ResourceLocation texture, float speed, float consumption, TextCannonHandler text) {
        return new DefaultCannonHandler() {
            @Override
            public ItemStack getRenderIcon(Level level) {
                return new ItemStack(BuiltInRegistries.ITEM.getOptional(icon.apply(level)).orElse(Items.BARRIER));
            }

            @Override
            public void renderTrailParticles(Level level, Vec3 pos, Vec3 velocity) {
                particles.apply(level, pos, velocity);
            }

            @Override
            public ResourceLocation getTextureLocation() {
                return texture;
            }

            @Override
            public float getRotationSpeed() {
                return 24 * speed;
            }

            @Override
            public void explode(Level level, Vec3 pos, AirtightCannonShotContext context) {
                explode.apply(level, pos, context);
            }

            @Override
            public float getGasConsumptionMultiplier() {
                return consumption;
            }

            @Override
            public void appendHoverText(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                text.apply(cannon, context, tooltip, flag);
            }
        };
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
