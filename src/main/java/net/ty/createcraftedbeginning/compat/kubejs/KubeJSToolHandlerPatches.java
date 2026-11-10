package net.ty.createcraftedbeginning.compat.kubejs;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandlerPatch;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlerPatch;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandlerPatch;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Map.Entry;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class KubeJSToolHandlerPatches {
    private KubeJSToolHandlerPatches() {
    }

    public static AirtightArmHandlerPatch readArm(@Nullable Object changes) {
        Map<?, ?> fields = readFields(changes);
        Float consumption = null;
        Float blockRange = null;
        Float entityRange = null;
        Float knockback = null;
        for (Entry<?, ?> entry : fields.entrySet()) {
            String field = String.valueOf(entry.getKey());
            switch (field) {
                case "consumptionMultiplier" -> consumption = readFloat(field, entry.getValue());
                case "blockRangeBonus" -> blockRange = readFloat(field, entry.getValue());
                case "entityRangeBonus" -> entityRange = readFloat(field, entry.getValue());
                case "knockbackBonus" -> knockback = readFloat(field, entry.getValue());
                default -> throw new IllegalArgumentException("Unknown arm handler field '" + field + "'.");
            }
        }

        return new AirtightArmHandlerPatch(consumption, blockRange, entityRange, knockback);
    }

    public static AirtightDrillHandlerPatch readDrill(@Nullable Object changes) {
        Map<?, ?> fields = readFields(changes);
        Integer damage = null;
        Double consumption = null;
        for (Entry<?, ?> entry : fields.entrySet()) {
            String field = String.valueOf(entry.getKey());
            switch (field) {
                case "damageBonus" -> {
                    double value = readNumber(field, entry.getValue());
                    if (value != Math.rint(value) || value > Integer.MAX_VALUE) {
                        throw new IllegalArgumentException("Drill handler field 'damageBonus' must be a non-negative integer, got " + value + '.');
                    }

                    damage = (int) value;
                }
                case "consumptionMultiplier" -> consumption = readNumber(field, entry.getValue());
                default -> throw new IllegalArgumentException("Unknown drill handler field '" + field + "'.");
            }
        }

        return new AirtightDrillHandlerPatch(damage, consumption);
    }

    public static AirtightCannonHandlerPatch readCannon(@Nullable Object changes) {
        Map<?, ?> fields = readFields(changes);
        if (fields.size() != 1 || !fields.containsKey("consumptionMultiplier")) {
            throw new IllegalArgumentException("Cannon handler changes must contain only 'consumptionMultiplier'.");
        }

        return new AirtightCannonHandlerPatch(readNumber("consumptionMultiplier", fields.get("consumptionMultiplier")));
    }

    private static Map<?, ?> readFields(@Nullable Object changes) {
        if (!(changes instanceof Map<?, ?> fields)) {
            throw new IllegalArgumentException("Tool handler changes must be an object.");
        }

        return fields;
    }

    private static float readFloat(String field, @Nullable Object input) {
        double value = readNumber(field, input);
        float result = (float) value;
        if (!Float.isFinite(result)) {
            throw new IllegalArgumentException("Tool handler field '" + field + "' must fit in a finite float, got " + value + '.');
        }

        return result;
    }

    private static double readNumber(String field, @Nullable Object input) {
        if (!(input instanceof Number number) || !GasConsumptionMath.isNonNegativeFinite(number.doubleValue())) {
            throw new IllegalArgumentException("Tool handler field '" + field + "' must be a finite, non-negative number, got " + input + '.');
        }

        return number.doubleValue();
    }
}
