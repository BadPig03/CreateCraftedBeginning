package net.ty.createcraftedbeginning.api.events;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.Event;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandler;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandlerUtils;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandlerUtils;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlerUtils;
import net.ty.createcraftedbeginning.api.coolantshandlers.AirtightCoolantHandler;
import net.ty.createcraftedbeginning.api.coolantshandlers.AirtightCoolantHandlerUtils;
import net.ty.createcraftedbeginning.api.drainagehandlers.AirtightDrainageHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.AirtightDrainageHandlerUtils;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandlerUtils;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandlerUtils;
import net.ty.createcraftedbeginning.api.fillhandlers.AirtightFillHandler;
import net.ty.createcraftedbeginning.api.fillhandlers.AirtightFillHandlerUtils;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandler;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandlerUtils;
import net.ty.createcraftedbeginning.api.turbinehandlers.AirtightTurbineHandlerUtils;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Fired once on {@code NeoForge.EVENT_BUS} during common setup after CCB has
 * registered its built-in airtight handlers. Addons should register their
 * handlers from this event instead of relying on incidental setup ordering.
 * <p>
 * The event is posted synchronously from CCB's enqueued common-setup work, so
 * registrations performed by listeners are complete before normal gameplay.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public final class RegisterAirtightHandlersEvent extends Event {
    /**
     * Registers an airtight arm handler for a gas.
     *
     * @param gas     the resource location of the gas
     * @param handler the arm handler to register
     */
    public void registerArm(ResourceLocation gas, AirtightArmHandler handler) {
        AirtightArmHandlerUtils.register(gas, handler);
    }

    /**
     * Registers an airtight arm handler using fixed stat values.
     *
     * @param gas         the resource location of the gas
     * @param consumption the gas consumption multiplier
     * @param blockRange  the block interaction range bonus
     * @param entityRange the entity interaction range bonus
     * @param knockback   the attack knockback bonus
     */
    public void registerArm(ResourceLocation gas, float consumption, float blockRange, float entityRange, float knockback) {
        AirtightArmHandlerUtils.register(gas, consumption, blockRange, entityRange, knockback);
    }

    /**
     * Registers an airtight armor handler for a gas.
     *
     * @param gas     the resource location of the gas
     * @param handler the armor handler to register
     */
    public void registerArmors(ResourceLocation gas, AirtightArmorsHandler handler) {
        AirtightArmorsHandlerUtils.register(gas, handler);
    }

    /**
     * Registers an airtight cannon handler for a gas.
     *
     * @param gas     the resource location of the gas
     * @param handler the cannon handler to register
     */
    public void registerCannon(ResourceLocation gas, AirtightCannonHandler handler) {
        AirtightCannonHandlerUtils.register(gas, handler);
    }

    /**
     * Registers an airtight coolant handler for a block.
     *
     * @param block   the coolant block
     * @param handler the coolant handler to register
     */
    public void registerCoolant(Block block, AirtightCoolantHandler handler) {
        AirtightCoolantHandlerUtils.register(block, handler);
    }

    /**
     * Registers an airtight drainage handler for a gas.
     *
     * @param gas     the resource location of the gas
     * @param handler the drainage handler to register
     */
    public void registerDrainage(ResourceLocation gas, AirtightDrainageHandler handler) {
        AirtightDrainageHandlerUtils.register(gas, handler);
    }

    /**
     * Registers an airtight drill handler for a gas.
     *
     * @param gas     the resource location of the gas
     * @param handler the drill handler to register
     */
    public void registerDrill(ResourceLocation gas, AirtightDrillHandler handler) {
        AirtightDrillHandlerUtils.register(gas, handler);
    }

    /**
     * Registers an airtight drill handler using fixed damage and consumption values.
     *
     * @param gas         the resource location of the gas
     * @param damage      the drill damage value
     * @param consumption the gas consumption multiplier
     */
    public void registerDrill(ResourceLocation gas, int damage, float consumption) {
        AirtightDrillHandlerUtils.register(gas, damage, consumption);
    }

    /**
     * Registers an airtight engine handler using the normal maximum engine level.
     *
     * @param gas        the resource location of the gas
     * @param workFactor the work factor contributed by the gas
     */
    public void registerEngine(ResourceLocation gas, double workFactor) {
        AirtightEngineHandlerUtils.register(gas, workFactor);
    }

    /**
     * Registers an airtight engine handler with an explicit maximum engine level.
     *
     * @param gas        the resource location of the gas
     * @param workFactor the work factor contributed by the gas
     * @param maxLevel   the maximum engine level supported by the gas
     */
    public void registerEngine(ResourceLocation gas, double workFactor, int maxLevel) {
        AirtightEngineHandlerUtils.register(gas, workFactor, maxLevel);
    }

    /**
     * Registers an airtight fill handler for a block.
     *
     * @param block   the block to associate with the handler
     * @param handler the fill handler to register
     */
    public void registerFill(Block block, AirtightFillHandler handler) {
        AirtightFillHandlerUtils.register(block, handler);
    }

    /**
     * Registers an airtight thermoregulator handler for a block.
     *
     * @param block   the thermoregulator block
     * @param handler the thermoregulator handler to register
     */
    public void registerThermoregulator(Block block, AirtightThermoregulatorHandler handler) {
        AirtightThermoregulatorHandlerUtils.register(block, handler);
    }

    /**
     * Registers an airtight turbine handler for a gas.
     *
     * @param gas      the resource location of the gas
     * @param maxLevel the maximum turbine level supported by the gas
     */
    public void registerTurbine(ResourceLocation gas, int maxLevel) {
        AirtightTurbineHandlerUtils.register(gas, maxLevel);
    }
}
