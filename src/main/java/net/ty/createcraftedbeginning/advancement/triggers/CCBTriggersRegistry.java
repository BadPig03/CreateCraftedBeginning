package net.ty.createcraftedbeginning.advancement.triggers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBTriggersRegistry {
    private static final List<CriterionTriggerBase<?>> TRIGGERS = new ArrayList<>();

    public static SimpleCCBTrigger add(String id) {
        return add(new SimpleCCBTrigger(id));
    }

    private static <T extends CriterionTriggerBase<?>> T add(T trigger) {
        TRIGGERS.add(trigger);
        return trigger;
    }

    public static void register() {
        for (CriterionTriggerBase<?> trigger : TRIGGERS) {
            Registry.register(BuiltInRegistries.TRIGGER_TYPES, trigger.getId(), trigger);
        }
    }
}
