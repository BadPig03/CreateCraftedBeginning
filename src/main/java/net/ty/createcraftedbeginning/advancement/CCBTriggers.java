package net.ty.createcraftedbeginning.advancement;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBTriggers {
    private static final List<CriterionTriggerBase<?>> triggers = new ArrayList<>();

    public static SimpleCCBTrigger addSimple(String id) {
        return add(new SimpleCCBTrigger(id));
    }

    private static <T extends CriterionTriggerBase<?>> T add(T trigger) {
        triggers.add(trigger);
        return trigger;
    }

    public static void register() {
        for (CriterionTriggerBase<?> trigger : triggers) {
            Registry.register(BuiltInRegistries.TRIGGER_TYPES, trigger.getId(), trigger);
        }
    }
}
