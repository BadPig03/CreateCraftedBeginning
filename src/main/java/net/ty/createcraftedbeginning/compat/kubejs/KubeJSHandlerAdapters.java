package net.ty.createcraftedbeginning.compat.kubejs;

import dev.latvian.mods.kubejs.block.state.BlockStatePredicate;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Function;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandlerPatch;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandlers;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandlers;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfile;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseHandler;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseHandlers;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandler;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandlers;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightArmorsHandlerEvent.ArmorsHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightDrillHandlerEvent.HitDrillHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightDrillHandlerEvent.TooltipDrillHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightThermoregulatorHandlerEvent.ThermoregulatorHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.GasReleaseHandlerEvent.ReleaseHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class KubeJSHandlerAdapters {
    private static final long MAX_SAFE_INTEGER = 9007199254740991L;

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

    public static void modifyArmors(Context context, ResourceLocation location, GameplayPressureProfile profile, @Nullable Object changes) {
        if (!(changes instanceof Map<?, ?> fields)) {
            throw new IllegalArgumentException("Armor handler changes must be an object.");
        }

        Map<EquipmentSlot, Float> consumption = new EnumMap<>(EquipmentSlot.class);
        Float elytraBoostMultiplier = null;
        Predicate<MobEffectInstance> canCureEffect = null;
        for (Entry<?, ?> field : fields.entrySet()) {
            Object value = field.getValue();
            switch (String.valueOf(field.getKey())) {
                case "consumption" -> {
                    if (!(value instanceof Map<?, ?> slots)) {
                        throw new IllegalArgumentException("Armor handler field 'consumption' must be an object.");
                    }

                    for (Entry<?, ?> slot : slots.entrySet()) {
                        String name = String.valueOf(slot.getKey());
                        EquipmentSlot equipmentSlot = switch (name) {
                            case "helmet" -> EquipmentSlot.HEAD;
                            case "chestplate" -> EquipmentSlot.CHEST;
                            case "leggings" -> EquipmentSlot.LEGS;
                            case "boots" -> EquipmentSlot.FEET;
                            default -> throw new IllegalArgumentException("Unknown armor handler field 'consumption." + name + "'.");
                        };
                        consumption.put(equipmentSlot, readArmorsMultiplier("consumption." + name, slot.getValue()));
                    }
                }
                case "elytraBoostMultiplier" -> elytraBoostMultiplier = readArmorsMultiplier("elytraBoostMultiplier", value);
                case "canCureEffect" -> {
                    if (!(value instanceof Function)) {
                        throw new IllegalArgumentException("Armor handler field 'canCureEffect' must be a function.");
                    }

                    ArmorsHandler handler = (ArmorsHandler) context.jsToJava(value, TypeInfo.of(ArmorsHandler.class));
                    canCureEffect = handler::apply;
                }
                default -> throw new IllegalArgumentException("Unknown armor handler field '" + field.getKey() + "'.");
            }
        }

        AirtightArmorsHandlers.modify(location, profile, new AirtightArmorsHandlerPatch(consumption, elytraBoostMultiplier, canCureEffect));
    }

    public static void registerDrill(Context context, ResourceLocation location, GameplayPressureProfile profile, @Nullable Object options) {
        if (!(options instanceof Map<?, ?> fields)) {
            throw new IllegalArgumentException("Drill handler options must be an object.");
        }

        for (Object key : fields.keySet()) {
            switch (String.valueOf(key)) {
                case "damageBonus", "consumptionMultiplier", "onHit", "appendTooltip" -> {}
                default -> throw new IllegalArgumentException("Unknown drill handler field '" + key + "'.");
            }
        }

        Object damageValue = fields.get("damageBonus");
        if (!(damageValue instanceof Number damage)) {
            throw new IllegalArgumentException("Drill handler field 'damageBonus' must be a number; got " + damageValue + '.');
        }

        double damageAmount = damage.doubleValue();
        if (!GasConsumptionMath.isNonNegativeFinite(damageAmount) || damageAmount != Math.rint(damageAmount) || damageAmount > Integer.MAX_VALUE - AirtightDrillHandler.BASE_DAMAGE_AMOUNT) {
            throw new IllegalArgumentException("Drill handler field 'damageBonus' must be an integer in [0, " + (Integer.MAX_VALUE - AirtightDrillHandler.BASE_DAMAGE_AMOUNT) + "]; got " + damageValue + '.');
        }

        Object consumptionValue = fields.get("consumptionMultiplier");
        if (!(consumptionValue instanceof Number consumption) || !GasConsumptionMath.isNonNegativeFinite(consumption.doubleValue())) {
            throw new IllegalArgumentException("Drill handler field 'consumptionMultiplier' must be a finite, non-negative number; got " + consumptionValue + '.');
        }

        HitDrillHandler hitHandler = null;
        if (fields.containsKey("onHit")) {
            Object callback = fields.get("onHit");
            if (!(callback instanceof Function)) {
                throw new IllegalArgumentException("Drill handler field 'onHit' must be a function; got " + callback + '.');
            }

            hitHandler = (HitDrillHandler) context.jsToJava(callback, TypeInfo.of(HitDrillHandler.class));
        }

        TooltipDrillHandler tooltipHandler = null;
        if (fields.containsKey("appendTooltip")) {
            Object callback = fields.get("appendTooltip");
            if (!(callback instanceof Function)) {
                throw new IllegalArgumentException("Drill handler field 'appendTooltip' must be a function; got " + callback + '.');
            }

            tooltipHandler = (TooltipDrillHandler) context.jsToJava(callback, TypeInfo.of(TooltipDrillHandler.class));
        }

        int damageBonus = (int) damageAmount;
        double consumptionMultiplier = consumption.doubleValue();
        HitDrillHandler onHit = hitHandler;
        TooltipDrillHandler appendTooltip = tooltipHandler;
        AirtightDrillHandlers.register(location, profile, new AirtightDrillHandler() {
            @Override
            public int getDamageAddition() {
                return damageBonus;
            }

            @Override
            public double getConsumptionMultiplier() {
                return consumptionMultiplier;
            }

            @Override
            public void appendAdditionalHoverText(ItemStack drill, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag flag) {
                if (appendTooltip == null) {
                    return;
                }

                List<Component> additions = new ArrayList<>();
                appendTooltip.apply(drill, tooltipContext, additions, flag);
                tooltip.addAll(additions);
            }

            @Override
            public void extraBehaviour(LivingEntity target, Player player, ServerLevel level) {
                AirtightDrillHandler.super.extraBehaviour(target, player, level);
                if (onHit == null) {
                    return;
                }

                onHit.apply(target, player, level);
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

    public static void registerGasRelease(Context context, ResourceLocation location, GameplayPressureProfile profile, @Nullable Object options) {
        if (!(options instanceof Map<?, ?> fields)) {
            throw new IllegalArgumentException("Gas release handler options must be an object.");
        }

        for (Object key : fields.keySet()) {
            switch (String.valueOf(key)) {
                case "amountPerEffectGU", "cooldownTicks", "showOutline", "apply" -> {}
                default -> throw new IllegalArgumentException("Unknown gas release handler field '" + key + "'.");
            }
        }

        long amountPerEffectGU = fields.containsKey("amountPerEffectGU") ? readGasReleaseInteger("amountPerEffectGU", fields.get("amountPerEffectGU"), MAX_SAFE_INTEGER) : 1000;
        int cooldownTicks = fields.containsKey("cooldownTicks") ? (int) readGasReleaseInteger("cooldownTicks", fields.get("cooldownTicks"), Integer.MAX_VALUE) : 1;
        boolean showOutline;
        if (fields.containsKey("showOutline")) {
            Object value = fields.get("showOutline");
            if (!(value instanceof Boolean outline)) {
                throw new IllegalArgumentException("Gas release handler field 'showOutline' must be a boolean; got " + value + '.');
            }

            showOutline = outline;
        }
        else {
            showOutline = true;
        }

        Object callback = fields.get("apply");
        if (!(callback instanceof Function)) {
            throw new IllegalArgumentException("Gas release handler field 'apply' must be a function; got " + callback + '.');
        }

        ReleaseHandler handler = (ReleaseHandler) context.jsToJava(callback, TypeInfo.of(ReleaseHandler.class));
        GasReleaseHandlers.register(location, profile, new GasReleaseHandler() {
            @Override
            public long getEffectInterval(GasStack gas) {
                return amountPerEffectGU;
            }

            @Override
            public int getEffectCooldown(GasStack gas) {
                return cooldownTicks;
            }

            @Override
            public boolean shouldShowOutline() {
                return showOutline;
            }

            @Override
            public void apply(GasReleaseContext releaseContext) {
                handler.apply(releaseContext);
            }
        });
    }

    private static float readArmorsMultiplier(String field, @Nullable Object value) {
        if (!(value instanceof Number number) || !GasConsumptionMath.isNonNegativeFinite(number.doubleValue()) || !Float.isFinite(number.floatValue())) {
            throw new IllegalArgumentException("Armor handler field '" + field + "' must be a finite, non-negative number representable as a float, got " + value + '.');
        }

        return number.floatValue();
    }

    private static long readGasReleaseInteger(String field, @Nullable Object value, long maximum) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Gas release handler field '" + field + "' must be a finite integer in [1, " + maximum + "]; got " + value + '.');
        }

        double amount = number.doubleValue();
        if (!Double.isFinite(amount) || amount < 1 || amount > maximum || amount != Math.rint(amount)) {
            throw new IllegalArgumentException("Gas release handler field '" + field + "' must be a finite integer in [1, " + maximum + "]; got " + value + '.');
        }

        return (long) amount;
    }
}
