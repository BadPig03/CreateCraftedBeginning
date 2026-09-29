package net.ty.createcraftedbeginning.compat.kubejs;

import dev.latvian.mods.kubejs.block.state.BlockStatePredicate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandlers;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfile;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseHandler;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseHandlers;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandler;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandlers;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightArmorsHandlerEvent.ArmorsHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightThermoregulatorHandlerEvent.ThermoregulatorHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.GasReleaseHandlerEvent.ReleaseHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class KubeJSHandlerAdapters {
    private KubeJSHandlerAdapters() {
    }

    public static void registerThermoregulator(Block block, ThermoregulatorHandler handler) {
        AirtightThermoregulatorHandlers.register(block, handler::apply);
    }

    public static void registerThermoregulator(BlockStatePredicate predicate, ThermoregulatorHandler handler) {
        AirtightThermoregulatorHandler thermoregulatorHandler = handler::apply;
        AirtightThermoregulatorHandler.REGISTRY.registerProvider(block -> predicate.testBlock(block) ? thermoregulatorHandler : null);
    }

    public static void registerArmors(ResourceLocation location, GameplayPressureProfile profile, ArmorsHandler handler, float helmet, float chestplate, float leggings, float boots, float elytra) {
        AirtightArmorsHandlers.register(location, profile, new AirtightArmorsHandler() {
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

    public static void registerGasRelease(ResourceLocation location, GameplayPressureProfile profile, boolean showOutline, ReleaseHandler handler) {
        GasReleaseHandlers.register(location, profile, new GasReleaseHandler() {
            @Override
            public boolean shouldShowOutline() {
                return showOutline;
            }

            @Override
            public void apply(GasReleaseContext context) {
                handler.apply(context);
            }
        });
    }
}
