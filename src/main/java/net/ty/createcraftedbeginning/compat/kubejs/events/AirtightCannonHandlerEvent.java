package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlerUtils;
import net.ty.createcraftedbeginning.api.cannonhandlers.DefaultCannonHandler;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * KubeJS event used to register Airtight Cannon Handlers for gases.
 * <p>
 * {@link AirtightCannonHandler} is not a functional interface, so KubeJS scripts
 * cannot conveniently implement it as a single JavaScript callback. This event
 * exposes a KubeJS-friendly registration method by splitting cannon behavior
 * into several smaller functional interfaces, then wrapping them in a
 * {@link DefaultCannonHandler}.
 * <p>
 * The generated handler inherits the default cannon model layer from
 * {@link DefaultCannonHandler}. KubeJS can customize the render icon, trail
 * particles, explosion behavior, projectile texture, animation speed, gas
 * consumption multiplier, and tooltip text, but not the model geometry itself.
 * <p>
 * Example usage in KubeJS:
 * <pre>{@code
 * const ParticleTypes = Java.loadClass('net.minecraft.core.particles.ParticleTypes')
 * const ExplosionInteraction = Java.loadClass('net.minecraft.world.level.Level$ExplosionInteraction')
 *
 * CCBEvents.airtightCannonHandler((event) => {
 *     event.add(
 *         'kubejs:oxygen',
 *         (level) => {
 *             return 'minecraft:fire_charge'
 *         },
 *         (level, pos) => {
 *             level.addParticle(ParticleTypes.FLAME, pos.x(), pos.y(), pos.z(), 0, 0, 0)
 *         },
 *         (level, pos, source, multiplier) => {
 *             level.explode(source, pos.x(), pos.y(), pos.z(), 1.2 * multiplier, false, ExplosionInteraction.TRIGGER)
 *         },
 *         'createcraftedbeginning:textures/entity/projectiles/creative_wind_charge.png',
 *         1.0,
 *         1.0,
 *         (cannon, context, tooltip, flag) => {
 *             tooltip.add(Component.literal('Custom Oxygen'))
 *         }
 *     )
 * })
 * }</pre>
 * <p>
 * Resource location strings passed from KubeJS must not contain leading or
 * trailing spaces.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightCannonHandlerEvent implements KubeEvent {
    /**
     * Registers a custom Airtight Cannon Handler for the given gas type.
     * <p>
     * This overload is hidden from JavaScript and is intended for internal
     * Java-side usage. The gas is converted to its {@link ResourceLocation}, then
     * delegated to
     * {@link AirtightCannonHandlerUtils#register(ResourceLocation, AirtightCannonHandler)}.
     *
     * @param gasType the gas type to register
     * @param handler the Airtight Cannon Handler to associate with the gas
     * @see Gas#getResourceLocation()
     * @see AirtightCannonHandlerUtils#register(ResourceLocation, AirtightCannonHandler)
     */
    @HideFromJS
    public static void add(Gas gasType, AirtightCannonHandler handler) {
        AirtightCannonHandlerUtils.register(gasType.getResourceLocation(), handler);
    }

    /**
     * Registers an Airtight Cannon Handler for the gas identified by the given
     * resource location.
     * <p>
     * This method is the KubeJS-facing registration entry point. It wraps the
     * provided JavaScript callbacks in a {@link DefaultCannonHandler}, allowing
     * scripts to provide only the behavior that is practical to define from
     * KubeJS.
     * <p>
     * The {@code icon} callback must return the resource location of an item. If
     * the item does not exist, the render icon falls back to {@link Items#BARRIER}.
     * <p>
     * The {@code texture} parameter is used directly as the projectile texture
     * location. It is not checked against a registry, so missing texture resources
     * will only become visible during rendering.
     * <p>
     * The {@code speed} parameter is a multiplier applied to the default rotation
     * speed of {@code 24} degrees per tick. A value of {@code 1.0} keeps the
     * default speed, while {@code 0.0} stops the rotation.
     *
     * @param location    the resource location of the gas to register
     * @param icon        the handler used to choose the item rendered as the cannon icon
     * @param particles   the handler used to render trail particles
     * @param explode     the handler used to execute explosion behavior
     * @param texture     the projectile texture location
     * @param speed       the projectile animation speed multiplier
     * @param consumption the gas consumption multiplier
     * @param text        the handler used to append tooltip text
     * @see AirtightCannonHandlerUtils#register(ResourceLocation, AirtightCannonHandler)
     * @see DefaultCannonHandler
     */
    public void add(ResourceLocation location, IconCannonHandler icon, ParticlesCannonHandler particles, ExplodeCannonHandler explode, ResourceLocation texture, float speed, float consumption, TextCannonHandler text) {
        AirtightCannonHandlerUtils.register(location, new DefaultCannonHandler() {
            @Override
            public ItemStack getRenderIcon(Level level) {
                return new ItemStack(BuiltInRegistries.ITEM.getOptional(icon.apply(level)).orElse(Items.BARRIER));
            }

            @Override
            public void renderTrailParticles(Level level, Vec3 pos) {
                particles.apply(level, pos);
            }

            @Override
            public void explode(Level level, Vec3 pos, Entity source, float multiplier) {
                explode.apply(level, pos, source, multiplier);
            }

            @Override
            public ResourceLocation getTextureLocation() {
                return texture;
            }

            @Override
            public float[] getSetupAnim(float ageInTicks) {
                return new float[]{0, -ageInTicks * 24 * Mth.PI / 180 * speed, 0, 0, 0, 0, 0, 0, 0};
            }

            @Override
            public float getGasConsumptionMultiplier() {
                return consumption;
            }

            @Override
            public void appendHoverText(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                text.apply(cannon, context, tooltip, flag);
            }
        });
    }

    /**
     * Functional interface used by KubeJS scripts to define the cannon render icon.
     * <p>
     * The returned {@link ResourceLocation} is resolved from
     * {@link BuiltInRegistries#ITEM}. If the item is not registered, the event
     * wrapper falls back to {@link Items#BARRIER}.
     */
    @FunctionalInterface
    public interface IconCannonHandler {
        /**
         * Returns the item id used as the cannon render icon.
         *
         * @param level the level where the icon is requested
         * @return the resource location of the item to render
         */
        ResourceLocation apply(Level level);
    }

    /**
     * Functional interface used by KubeJS scripts to define cannon trail particles.
     * <p>
     * This handler is normally called from rendering-side projectile logic. Avoid
     * server-only operations here.
     */
    @FunctionalInterface
    public interface ParticlesCannonHandler {
        /**
         * Renders trail particles at the given projectile position.
         *
         * @param level the level where particles should be spawned
         * @param pos   the projectile position
         */
        void apply(Level level, Vec3 pos);
    }

    /**
     * Functional interface used by KubeJS scripts to define cannon explosion
     * behavior.
     * <p>
     * This handler receives the explosion position, source entity, and runtime
     * multiplier. The implementation is responsible for performing any damage,
     * explosion, knockback, particle, or world interaction logic.
     */
    @FunctionalInterface
    public interface ExplodeCannonHandler {
        /**
         * Executes the cannon explosion behavior.
         *
         * @param level      the level where the explosion occurs
         * @param pos        the explosion position
         * @param source     the entity responsible for the explosion
         * @param multiplier the effect multiplier applied by the cannon projectile
         */
        void apply(Level level, Vec3 pos, Entity source, float multiplier);
    }

    /**
     * Functional interface used by KubeJS scripts to append cannon tooltip text.
     * <p>
     * Implementations should add {@link Component} instances to the tooltip list.
     * Adding raw strings is not recommended because later tooltip rendering code
     * expects components.
     */
    @FunctionalInterface
    public interface TextCannonHandler {
        /**
         * Appends tooltip text for the cannon item.
         *
         * @param cannon  the cannon item stack
         * @param context the tooltip context
         * @param tooltip the mutable tooltip component list
         * @param flag    the tooltip flag
         */
        void apply(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag flag);
    }
}
