package net.ty.createcraftedbeginning.api.gas.pressure;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GameplayPressureProfileCompoundTags {
    private GameplayPressureProfileCompoundTags() {
    }

    public static void write(CompoundTag compoundTag, String key, GameplayPressureProfile profile) {
        requireKnownProfile(profile);
        compoundTag.putString(key, profile.id().toString());
    }

    public static GameplayPressureProfile read(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_STRING)) {
            throw new IllegalArgumentException("Gameplay pressure profile key '" + key + "' must contain a string id; the key is missing or has a different tag type.");
        }

        ResourceLocation id = ResourceLocation.tryParse(tag.getString(key));
        if (id == null) {
            throw new IllegalArgumentException("Gameplay pressure profile key '" + key + "' contains an invalid id '" + tag.getString(key) + "'.");
        }

        return GameplayPressureProfiles.require(id);
    }

    private static void requireKnownProfile(GameplayPressureProfile profile) {
        if (GameplayPressureProfiles.orderedProfiles().contains(profile)) {
            return;
        }

        throw new IllegalArgumentException("Gameplay pressure profile '" + profile.id() + "' with minimum pressure " + profile.minimumPressurePa() + " Pa is not registered.");
    }
}
