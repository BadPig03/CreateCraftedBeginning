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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class Gas {
    public static final Codec<Holder<Gas>> HOLDER_CODEC = CCBGasRegistries.GAS_REGISTRY.holderByNameCodec();
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Gas>> HOLDER_STREAM_CODEC = ByteBufCodecs.holderRegistry(CCBRegistries.GAS_REGISTRY_KEY);
    public static final StreamCodec<RegistryFriendlyByteBuf, Gas> GAS_STREAM_CODEC = ByteBufCodecs.registry(CCBRegistries.GAS_REGISTRY_KEY);
    public static final Holder<Gas> EMPTY_GAS_HOLDER = DeferredHolder.create(CCBGasRegistries.EMPTY_GAS_KEY);

    private final Reference<Gas> builtInRegistryHolder = CCBGasRegistries.GAS_REGISTRY.createIntrusiveHolder(this);
    private final ResourceLocation texture;
    private final int tint;
    private final int alpha;
    private final Set<TagKey<Gas>> tags;

    @Nullable
    private String translationKey;

    /**
     * Creates a new {@code Gas} instance.
     *
     * @param builder the builder to configure
     */
    public Gas(GasBuilder builder) {
        texture = builder.getTexture();
        tint = builder.getTint();
        alpha = builder.getAlpha();
        tags = builder.getTags() != null ? Set.copyOf(builder.getTags()) : Collections.emptySet();
    }

    /**
     * Parses a gas holder from the supplied serialized tag.
     *
     * @param lookupProvider the lookup provider to use
     * @param tag            the tag to inspect or process
     * @return an optional containing the parsed value, or an empty optional when parsing fails
     */
    public static Optional<Holder<Gas>> parseHolder(Provider lookupProvider, Tag tag) {
        return HOLDER_CODEC.parse(lookupProvider.createSerializationContext(NbtOps.INSTANCE), tag).resultOrPartial(error -> CreateCraftedBeginning.LOGGER.error("Tried to load invalid gas: '{}'", error));
    }

    /**
     * Resolves a gas holder from its serialized identifier, falling back to the empty gas holder.
     *
     * @param lookupProvider the lookup provider to use
     * @param tag            the tag to inspect or process
     * @return the resolved gas holder, or the empty gas holder when the identifier is absent or invalid
     */
    public static Holder<Gas> parseOptionalHolder(Provider lookupProvider, String tag) {
        if (tag.isEmpty()) {
            return EMPTY_GAS_HOLDER;
        }

        Optional<RegistryLookup<Gas>> lookup = lookupProvider.lookup(CCBRegistries.GAS_REGISTRY_KEY);
        if (lookup.isEmpty()) {
            return EMPTY_GAS_HOLDER;
        }

        ResourceLocation location = ResourceLocation.tryParse(tag);
        if (location == null) {
            return EMPTY_GAS_HOLDER;
        }

        ResourceKey<Gas> key = ResourceKey.create(CCBRegistries.GAS_REGISTRY_KEY, location);
        Optional<Reference<Gas>> reference = lookup.get().get(key);
        return reference.isPresent() ? reference.get() : EMPTY_GAS_HOLDER;
    }

    /**
     * Resolves a registered gas from its resource location, falling back to the empty gas.
     *
     * @param location the resource location identifying the target value
     * @return the registered gas, or the empty gas when the identifier is unknown
     */
    public static Gas getGasTypeByName(ResourceLocation location) {
        return CCBGasRegistries.GAS_REGISTRY.getOptional(location).orElse(EMPTY_GAS_HOLDER.value());
    }

    /**
     * Checks whether this gas belongs to the supplied tag.
     *
     * @param tag the tag to inspect or process
     * @return {@code true} if this gas belongs to the supplied tag; otherwise {@code false}
     */
    public boolean is(TagKey<Gas> tag) {
        return getHolder().is(tag);
    }

    /**
     * Checks whether this value is empty.
     *
     * @return {@code true} if this value is empty; otherwise {@code false}
     */
    public boolean isEmpty() {
        return getHolder().is(CCBGasRegistries.EMPTY_GAS_KEY);
    }

    /**
     * Returns the holder.
     *
     * @return the holder
     */
    public Holder<Gas> getHolder() {
        return builtInRegistryHolder;
    }

    /**
     * Returns the texture.
     *
     * @return the texture
     */
    public ResourceLocation getTexture() {
        return texture;
    }

    /**
     * Returns the translation key.
     *
     * @return the translation key
     */
    public String getTranslationKey() {
        if (translationKey != null) {
            return translationKey;
        }

        ResourceLocation id = CCBGasRegistries.GAS_REGISTRY.getKeyOrNull(this);
        if (id == null) {
            translationKey = "gas." + CreateCraftedBeginning.MOD_ID + ".unknown";
        }
        else {
            translationKey = "gas." + id.getNamespace() + '.' + id.getPath();
        }
        return translationKey;
    }

    /**
     * Returns the tint.
     *
     * @return the combined ARGB tint color
     */
    public int getTint() {
        return ARGB32.color(alpha, tint);
    }

    /**
     * Returns the tags.
     *
     * @return the tags
     */
    public Set<TagKey<Gas>> getTags() {
        return tags;
    }

    /**
     * Checks whether this gas belongs to the supplied tag.
     *
     * @param tag the tag to inspect or process
     * @return {@code true} if this gas belongs to the supplied tag; otherwise {@code false}
     */
    public boolean hasTag(TagKey<Gas> tag) {
        return tags.contains(tag);
    }

    /**
     * Returns the resource location.
     *
     * @return the registry resource location of this gas
     */
    public ResourceLocation getResourceLocation() {
        return builtInRegistryHolder.key().location();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return CCBGasRegistries.GAS_REGISTRY.wrapAsHolder(this).getRegisteredName();
    }
}
