package net.ty.createcraftedbeginning.advancement;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBTriggers {
    private static final List<CriterionTriggerBase<?>> triggers = new LinkedList<>();

    public static SimpleCCBTrigger addSimple(String id) {
        return add(new SimpleCCBTrigger(id));
    }

    private static <T extends CriterionTriggerBase<?>> T add(T instance) {
        triggers.add(instance);
        return instance;
    }

    public static void register() {
        triggers.forEach(trigger -> Registry.register(BuiltInRegistries.TRIGGER_TYPES, trigger.getId(), trigger));
    }
}
