package net.ty.createcraftedbeginning.compat.kubejs;

import dev.latvian.mods.kubejs.block.state.BlockStatePredicate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandlerUtils;
import net.ty.createcraftedbeginning.api.coolantshandlers.AirtightCoolantHandler;
import net.ty.createcraftedbeginning.api.coolantshandlers.AirtightCoolantHandlerUtils;
import net.ty.createcraftedbeginning.api.coolantshandlers.CoolantEfficiency;
import net.ty.createcraftedbeginning.api.drainagehandlers.AirtightDrainageHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.AirtightDrainageHandlerUtils;
import net.ty.createcraftedbeginning.api.fillhandlers.AirtightFillHandler;
import net.ty.createcraftedbeginning.api.fillhandlers.AirtightFillHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasRegistries;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandler;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandlerUtils;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightArmorsHandlerEvent.ArmorsHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightCoolantHandlerEvent.EfficiencyCoolantHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightCoolantHandlerEvent.MeltCoolantHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightDrainageHandlerEvent.DrainageHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightFillHandlerEvent.FillHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightThermoregulatorHandlerEvent.ThermoregulatorHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBKubeJSHandlerUtils {
    private CCBKubeJSHandlerUtils() {
    }

    /**
     * Registers a KubeJS fill handler for a specific block.
     *
     * @param block   the block to associate with the fill handler
     * @param handler the KubeJS fill callback
     */
    public static void registerFill(Block block, FillHandler handler) {
        AirtightFillHandlerUtils.register(block, createFillHandler(handler));
    }

    /**
     * Registers a KubeJS fill handler for blocks matching a predicate.
     *
     * @param predicate the predicate used to select matching block states
     * @param handler   the KubeJS fill callback
     */
    public static void registerFill(BlockStatePredicate predicate, FillHandler handler) {
        AirtightFillHandler adaptedHandler = createFillHandler(handler);
        AirtightFillHandler.REGISTRY.registerProvider(block -> predicate.testBlock(block) ? adaptedHandler : null);
    }

    /**
     * Registers KubeJS coolant callbacks for a specific block.
     *
     * @param block      the coolant block
     * @param efficiency the callback that supplies coolant efficiency
     * @param melt       the callback that supplies the melted block state id
     */
    public static void registerCoolant(Block block, EfficiencyCoolantHandler efficiency, MeltCoolantHandler melt) {
        AirtightCoolantHandlerUtils.register(block, createCoolantHandler(efficiency, melt));
    }

    /**
     * Registers KubeJS coolant callbacks for blocks matching a predicate.
     *
     * @param predicate  the predicate used to select matching block states
     * @param efficiency the callback that supplies coolant efficiency
     * @param melt       the callback that supplies the melted block state id
     */
    public static void registerCoolant(BlockStatePredicate predicate, EfficiencyCoolantHandler efficiency, MeltCoolantHandler melt) {
        AirtightCoolantHandler adaptedHandler = createCoolantHandler(efficiency, melt);
        AirtightCoolantHandler.REGISTRY.registerProvider(block -> predicate.testBlock(block) ? adaptedHandler : null);
    }

    /**
     * Registers a KubeJS thermoregulator handler for a specific block.
     *
     * @param block   the thermoregulator block
     * @param handler the KubeJS thermoregulator callback
     */
    public static void registerThermoregulator(Block block, ThermoregulatorHandler handler) {
        AirtightThermoregulatorHandlerUtils.register(block, handler::apply);
    }

    /**
     * Registers a KubeJS thermoregulator handler for blocks matching a predicate.
     *
     * @param predicate the predicate used to select matching block states
     * @param handler   the KubeJS thermoregulator callback
     */
    public static void registerThermoregulator(BlockStatePredicate predicate, ThermoregulatorHandler handler) {
        AirtightThermoregulatorHandler adaptedHandler = handler::apply;
        AirtightThermoregulatorHandler.REGISTRY.registerProvider(block -> predicate.testBlock(block) ? adaptedHandler : null);
    }

    /**
     * Registers KubeJS airtight-armor behavior and per-slot consumption multipliers for a gas.
     *
     * @param location   the resource location of the gas
     * @param handler    the callback used to decide whether an effect can be cured
     * @param helmet     the helmet gas consumption multiplier
     * @param chestplate the chestplate gas consumption multiplier
     * @param leggings   the leggings gas consumption multiplier
     * @param boots      the boots gas consumption multiplier
     * @param elytra     the elytra boosting multiplier
     */
    public static void registerArmors(ResourceLocation location, ArmorsHandler handler, float helmet, float chestplate, float leggings, float boots, float elytra) {
        AirtightArmorsHandlerUtils.register(location, new AirtightArmorsHandler() {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean canCureEffect(MobEffectInstance effectInstance) {
                return handler.apply(effectInstance);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public float getConsumptionMultiplier(EquipmentSlot slot) {
                return switch (slot) {
                    case HEAD -> helmet;
                    case CHEST -> chestplate;
                    case LEGS -> leggings;
                    case FEET -> boots;
                    default -> 1;
                };
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public float getMultiplierForBoostingElytra() {
                return elytra;
            }
        });
    }

    /**
     * Registers KubeJS drainage behavior for a gas.
     *
     * @param location    the resource location of the gas
     * @param inflation   the outline inflation applied by the handler
     * @param showOutline whether drainage interactions should show an outline
     * @param handler     the KubeJS drainage callback
     */
    public static void registerDrainage(ResourceLocation location, float inflation, boolean showOutline, DrainageHandler handler) {
        AirtightDrainageHandlerUtils.register(location, new AirtightDrainageHandler() {
            /**
             * {@inheritDoc}
             */
            @Override
            public float getInflation() {
                return inflation;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean shouldShowOutline() {
                return showOutline;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void apply(Level level, BlockPos pos, Direction direction, Gas gasType) {
                if (showOutline) {
                    showOutline(level, pos, direction, inflation, gasType.getTint());
                }
                handler.apply(level, pos, direction, gasType);
            }
        });
    }

    private static AirtightFillHandler createFillHandler(FillHandler handler) {
        return (level, pos, state) -> GasRegistries.GAS_REGISTRY.getOptional(handler.apply(level, pos, state)).orElse(Gas.EMPTY_GAS_HOLDER.value());
    }

    private static AirtightCoolantHandler createCoolantHandler(EfficiencyCoolantHandler efficiency, MeltCoolantHandler melt) {
        return new AirtightCoolantHandler() {
            /**
             * {@inheritDoc}
             */
            @Override
            public CoolantEfficiency getCoolantEfficiency(Level level, BlockPos pos, BlockState blockState) {
                return CoolantEfficiency.fromInt(efficiency.apply(level, pos, blockState));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public BlockState getMeltBlockState(Level level, BlockPos pos, BlockState blockState) {
                return BuiltInRegistries.BLOCK.getOptional(melt.apply(level, pos, blockState)).orElse(Blocks.AIR).defaultBlockState();
            }
        };
    }
}
