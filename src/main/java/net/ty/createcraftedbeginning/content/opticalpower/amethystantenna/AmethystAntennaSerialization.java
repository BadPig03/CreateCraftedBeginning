package net.ty.createcraftedbeginning.content.opticalpower.amethystantenna;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkBinding;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystAntennaSerialization {
    private static final String COMPOUND_KEY_IDENTITY = "AntennaId";

    private AmethystAntennaSerialization() {
    }

    public static @Nullable UUID readIdentity(@Nullable CompoundTag compoundTag) {
        if (compoundTag == null || !compoundTag.hasUUID(COMPOUND_KEY_IDENTITY)) {
            return null;
        }

        return compoundTag.getUUID(COMPOUND_KEY_IDENTITY);
    }

    static StoredState read(CompoundTag compoundTag, UUID currentIdentity) {
        UUID identity = readIdentity(compoundTag);
        if (identity == null) {
            identity = currentIdentity;
        }

        return new StoredState(identity, LoadingNetworkBinding.read(compoundTag));
    }

    static void write(AmethystAntennaBlockEntity antenna, CompoundTag compoundTag) {
        compoundTag.putUUID(COMPOUND_KEY_IDENTITY, antenna.identity());
        LoadingNetworkBinding current = antenna.binding();
        if (current == null) {
            return;
        }

        current.write(compoundTag);
    }

    record StoredState(UUID identity, @Nullable LoadingNetworkBinding binding) {}
}
