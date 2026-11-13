package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ChunkStabilizerMapChanges {
    private final List<WeakReference<BitSet>> listeners = new ArrayList<>();

    public void changed(int x, int z) {
        Iterator<WeakReference<BitSet>> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            BitSet target = iterator.next().get();
            if (target == null) {
                iterator.remove();
                continue;
            }

            target.set(z * 16 + x);
        }
    }

    void watch(BitSet target) {
        listeners.removeIf(reference -> reference.get() == null);
        listeners.add(new WeakReference<>(target));
    }
}
