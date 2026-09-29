package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabLayout.CCBCreativeTabSection;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBCreativeSectionTracker {
    private static final Map<RegistryEntry<?, ?>, CCBCreativeTabSection> SECTION_LOOKUP = new IdentityHashMap<>();
    private static final Set<RegistryEntry<?, ?>> SEEN = Collections.newSetFromMap(new IdentityHashMap<>());
    private static CreateRegistrate registrate;
    private static CCBCreativeTabSection currentSection;

    private CCBCreativeSectionTracker() {
    }

    public static synchronized void set(CreateRegistrate registrate, CCBCreativeTabSection section) {
        CCBCreativeSectionTracker.registrate = registrate;
        capture(registrate);
        currentSection = section;
    }

    public static synchronized boolean isOutOfSection(RegistryEntry<?, ?> entry, CCBCreativeTabSection section) {
        CreateRegistrate activeRegistrate = registrate;
        if (activeRegistrate != null) {
            capture(activeRegistrate);
        }
        return SECTION_LOOKUP.get(entry) != section;
    }

    private static void capture(CreateRegistrate registrate) {
        capture(registrate.getAll(Registries.BLOCK));
        capture(registrate.getAll(Registries.ITEM));
    }

    private static void capture(Collection<? extends RegistryEntry<?, ?>> entries) {
        for (RegistryEntry<?, ?> entry : entries) {
            if (!SEEN.add(entry) || currentSection == null) {
                continue;
            }

            SECTION_LOOKUP.put(entry, currentSection);
        }
    }
}
