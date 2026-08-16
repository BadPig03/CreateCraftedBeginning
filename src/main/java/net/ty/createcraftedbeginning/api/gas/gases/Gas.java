package net.ty.createcraftedbeginning.api.gas.gases;

import com.mojang.serialization.Codec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FastColor.ARGB32;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.ty.createcraftedbeginning.api.CCBAPI;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Gas {
    public static final Codec<Holder<Gas>> HOLDER_CODEC = GasRegistries.GAS_REGISTRY.holderByNameCodec();
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Gas>> HOLDER_STREAM_CODEC = ByteBufCodecs.holderRegistry(GasRegistries.GAS_REGISTRY_KEY);
    @SuppressWarnings("unused")
    public static final StreamCodec<RegistryFriendlyByteBuf, Gas> GAS_STREAM_CODEC = ByteBufCodecs.registry(GasRegistries.GAS_REGISTRY_KEY);
    public static final Holder<Gas> EMPTY_GAS_HOLDER = DeferredHolder.create(GasRegistries.EMPTY_GAS_KEY);

    private final Reference<Gas> builtInRegistryHolder = GasRegistries.GAS_REGISTRY.createIntrusiveHolder(this);
    private final ResourceLocation texture;
    private final int tint;
    private final int alpha;
    private final Set<TagKey<Gas>> tags;

    @Nullable
    private String translationKey;

    public Gas(GasBuilder builder) {
        texture = builder.getTexture();
        tint = builder.getTint();
        alpha = builder.getAlpha();
        tags = builder.getTags() != null ? Set.copyOf(builder.getTags()) : Collections.emptySet();
    }

    @SuppressWarnings("unused")
    public static Optional<Holder<Gas>> parseHolder(Provider lookupProvider, Tag tag) {
        return HOLDER_CODEC.parse(lookupProvider.createSerializationContext(NbtOps.INSTANCE), tag).resultOrPartial(error -> CCBAPI.LOGGER.error("Tried to load invalid gas: '{}'", error));
    }

    @SuppressWarnings("unused")
    public static Holder<Gas> parseOptionalHolder(Provider lookupProvider, String tag) {
        if (tag.isEmpty()) {
            return EMPTY_GAS_HOLDER;
        }

        Optional<RegistryLookup<Gas>> lookup = lookupProvider.lookup(GasRegistries.GAS_REGISTRY_KEY);
        if (lookup.isEmpty()) {
            return EMPTY_GAS_HOLDER;
        }

        ResourceLocation location = ResourceLocation.tryParse(tag);
        if (location == null) {
            return EMPTY_GAS_HOLDER;
        }

        ResourceKey<Gas> key = ResourceKey.create(GasRegistries.GAS_REGISTRY_KEY, location);
        Optional<Reference<Gas>> reference = lookup.get().get(key);
        if (reference.isEmpty()) {
            return EMPTY_GAS_HOLDER;
        }
        return reference.get();
    }

    public static Gas getGasTypeByName(ResourceLocation location) {
        return GasRegistries.GAS_REGISTRY.getOptional(location).orElse(EMPTY_GAS_HOLDER.value());
    }

    public boolean is(TagKey<Gas> tag) {
        return getHolder().is(tag);
    }

    public boolean isEmpty() {
        return getHolder().is(GasRegistries.EMPTY_GAS_KEY);
    }

    public Holder<Gas> getHolder() {
        return builtInRegistryHolder;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public String getTranslationKey() {
        if (translationKey != null) {
            return translationKey;
        }

        ResourceLocation id = GasRegistries.GAS_REGISTRY.getKeyOrNull(this);
        if (id == null) {
            translationKey = "gas." + CCBAPI.MOD_ID + ".unknown";
        }
        else {
            translationKey = "gas." + id.getNamespace() + '.' + id.getPath();
        }
        return translationKey;
    }

    public int getTint() {
        return ARGB32.color(alpha, tint);
    }

    public Set<TagKey<Gas>> getTags() {
        return tags;
    }

    @SuppressWarnings("unused")
    public boolean hasTag(TagKey<Gas> tag) {
        return tags.contains(tag);
    }

    public ResourceLocation getResourceLocation() {
        return builtInRegistryHolder.key().location();
    }

    @Override
    public String toString() {
        return GasRegistries.GAS_REGISTRY.wrapAsHolder(this).getRegisteredName();
    }
}
