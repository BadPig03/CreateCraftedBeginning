package net.ty.createcraftedbeginning.api.gas.gases;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("unused")
public class Gas {
    public static final Codec<Holder<Gas>> HOLDER_CODEC = CCBGasRegistries.GAS_REGISTRY.holderByNameCodec();
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Gas>> HOLDER_STREAM_CODEC = ByteBufCodecs.holderRegistry(CCBRegistries.GAS_REGISTRY_KEY);
    public static final StreamCodec<RegistryFriendlyByteBuf, Gas> GAS_STREAM_CODEC = ByteBufCodecs.registry(CCBRegistries.GAS_REGISTRY_KEY);
    public static final Holder<Gas> EMPTY_GAS_HOLDER = DeferredHolder.create(CCBGasRegistries.EMPTY_GAS_KEY);
    private static final String UNKNOWN_GAS = "gas." + CreateCraftedBeginning.MOD_ID + ".unknown";

    private final Reference<Gas> builtInRegistryHolder = CCBGasRegistries.GAS_REGISTRY.createIntrusiveHolder(this);
    private final ResourceLocation iconLocation;
    private final int tint;
    private final float inflation;
    private final float engineEfficiency;
    private final float teslaEfficiency;

    @Nullable
    private final ResourceLocation pressurizedGasType;
    @Nullable
    private final ResourceLocation energizedGasType;
    private final Set<TagKey<Gas>> tags;
    @Nullable
    private String translationKey;

    public Gas(@NotNull GasBuilder builder) {
        iconLocation = builder.getTexture();
        tint = builder.getTint();
        inflation = builder.getInflation();
        pressurizedGasType = builder.getPressurizedGasType();
        energizedGasType = builder.getEnergizedGasType();
        engineEfficiency = builder.getEngineEfficiency();
        teslaEfficiency = builder.getTeslaEfficiency();
        tags = builder.getTags() != null ? Set.copyOf(builder.getTags()) : Collections.emptySet();
    }

    public static Optional<Holder<Gas>> parseHolder(@NotNull Provider lookupProvider, Tag tag) {
        return HOLDER_CODEC.parse(lookupProvider.createSerializationContext(NbtOps.INSTANCE), tag).resultOrPartial(error -> CreateCraftedBeginning.LOGGER.error("Tried to load invalid gas: '{}'", error));
    }

    public static Holder<Gas> parseOptionalHolder(Provider lookupProvider, @NotNull String tag) {
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

        Optional<Reference<Gas>> reference = lookup.get().get(ResourceKey.create(CCBRegistries.GAS_REGISTRY_KEY, location));
        return reference.isPresent() ? reference.get() : EMPTY_GAS_HOLDER;
    }

    public static TextureAtlasSprite getGasTexture(@NotNull Holder<Gas> holder) {
        ResourceLocation sprite = holder.value().iconLocation;
        if (sprite == null) {
            sprite = MissingTextureAtlasSprite.getLocation();
        }
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(sprite);
    }

    public static @NotNull Gas getGasTypeByName(ResourceLocation location) {
        if (location == null) {
            return EMPTY_GAS_HOLDER.value();
        }

        return CCBGasRegistries.GAS_REGISTRY.get(location);
    }

    public boolean is(TagKey<Gas> tag) {
        return getHolder().is(tag);
    }

    public boolean isEmpty() {
        return getHolder().is(CCBGasRegistries.EMPTY_GAS_KEY);
    }

    public Holder<Gas> getHolder() {
        return builtInRegistryHolder;
    }

    public ResourceLocation getIcon() {
        return iconLocation;
    }

    public String getTranslationKey() {
        if (translationKey != null) {
            return translationKey;
        }

        ResourceLocation id = CCBGasRegistries.GAS_REGISTRY.getKeyOrNull(this);
        if (id == null) {
            translationKey = UNKNOWN_GAS;
        }
        else {
            translationKey = "gas." + id.getNamespace() + '.' + id.getPath();
        }
        return translationKey;
    }

    public int getTint() {
        return tint;
    }

    public float getInflation() {
        return inflation;
    }

    public @NotNull Gas getPressurizedGasType() {
        return Objects.requireNonNullElse(getGasTypeByName(pressurizedGasType), EMPTY_GAS_HOLDER.value());
    }

    public @NotNull Gas getEnergizedGasType() {
        return Objects.requireNonNullElse(getGasTypeByName(energizedGasType), EMPTY_GAS_HOLDER.value());
    }

    public float getEngineEfficiency() {
        return engineEfficiency;
    }

    public float getTeslaEfficiency() {
        return teslaEfficiency;
    }

    public Set<TagKey<Gas>> getTags() {
        return tags;
    }

    public boolean hasTag(TagKey<Gas> tag) {
        return tags.contains(tag);
    }

    public ResourceLocation getResourceLocation() {
        return builtInRegistryHolder.key().location();
    }

    @Override
    public String toString() {
        return CCBGasRegistries.GAS_REGISTRY.wrapAsHolder(this).getRegisteredName();
    }
}
