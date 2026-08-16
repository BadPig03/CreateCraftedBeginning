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

    public static void registerFill(Block block, FillHandler handler) {
        AirtightFillHandlerUtils.register(block, createFillHandler(handler));
    }

    public static void registerFill(BlockStatePredicate predicate, FillHandler handler) {
        AirtightFillHandler adaptedHandler = createFillHandler(handler);
        AirtightFillHandler.REGISTRY.registerProvider(block -> predicate.testBlock(block) ? adaptedHandler : null);
    }

    public static void registerCoolant(Block block, EfficiencyCoolantHandler efficiency, MeltCoolantHandler melt) {
        AirtightCoolantHandlerUtils.register(block, createCoolantHandler(efficiency, melt));
    }

    public static void registerCoolant(BlockStatePredicate predicate, EfficiencyCoolantHandler efficiency, MeltCoolantHandler melt) {
        AirtightCoolantHandler adaptedHandler = createCoolantHandler(efficiency, melt);
        AirtightCoolantHandler.REGISTRY.registerProvider(block -> predicate.testBlock(block) ? adaptedHandler : null);
    }

    public static void registerThermoregulator(Block block, ThermoregulatorHandler handler) {
        AirtightThermoregulatorHandlerUtils.register(block, handler::apply);
    }

    public static void registerThermoregulator(BlockStatePredicate predicate, ThermoregulatorHandler handler) {
        AirtightThermoregulatorHandler adaptedHandler = handler::apply;
        AirtightThermoregulatorHandler.REGISTRY.registerProvider(block -> predicate.testBlock(block) ? adaptedHandler : null);
    }

    public static void registerArmors(ResourceLocation location, ArmorsHandler handler, float helmet, float chestplate, float leggings, float boots, float elytra) {
        AirtightArmorsHandlerUtils.register(location, new AirtightArmorsHandler() {
            @Override
            public boolean canCureEffect(MobEffectInstance effectInstance) {
                return handler.apply(effectInstance);
            }

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

            @Override
            public float getMultiplierForBoostingElytra() {
                return elytra;
            }
        });
    }

    public static void registerDrainage(ResourceLocation location, float inflation, boolean showOutline, DrainageHandler handler) {
        AirtightDrainageHandlerUtils.register(location, new AirtightDrainageHandler() {
            @Override
            public float getInflation() {
                return inflation;
            }

            @Override
            public boolean shouldShowOutline() {
                return showOutline;
            }

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
            @Override
            public CoolantEfficiency getCoolantEfficiency(Level level, BlockPos pos, BlockState blockState) {
                return CoolantEfficiency.fromInt(efficiency.apply(level, pos, blockState));
            }

            @Override
            public BlockState getMeltBlockState(Level level, BlockPos pos, BlockState blockState) {
                return BuiltInRegistries.BLOCK.getOptional(melt.apply(level, pos, blockState)).orElse(Blocks.AIR).defaultBlockState();
            }
        };
    }
}
