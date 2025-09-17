package net.ty.createcraftedbeginning.api.gas;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.data.CCBGasRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class Gas {
    public static final Codec<Holder<Gas>> HOLDER_CODEC = CCBGasRegistry.GAS_REGISTRY.holderByNameCodec();
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Gas>> HOLDER_STREAM_CODEC = ByteBufCodecs.holderRegistry(CCBGasRegistry.GAS_REGISTRY_NAME);
    public static final Holder<Gas> EMPTY_GAS_HOLDER = DeferredHolder.create(CCBGasRegistry.EMPTY_GAS_KEY);

    private final Holder.Reference<Gas> builtInRegistryHolder = CCBGasRegistry.GAS_REGISTRY.createIntrusiveHolder(this);
    private final ResourceLocation iconLocation;
    private final int tint;
    private final float pressure;
    private final float energy;

    @Nullable
    private final String pressurizedGasName;
    @Nullable
    private final String depressurizedGasName;
    @Nullable
    private final String vortexedGasName;
    private final FluidStack condensate;

    @Nullable
    private String translationKey;

    public Gas(@NotNull GasBuilder builder) {
        this.iconLocation = builder.getTexture();
        this.tint = builder.getTint();
        this.pressurizedGasName = builder.getPressurizedGasName();
        this.depressurizedGasName = builder.getDepressurizedGasName();
        this.vortexedGasName = builder.getVortexedGasName();
        this.condensate = builder.getCondensate();
        this.pressure = builder.getPressure();
        this.energy = builder.getEnergy();
    }

    public static Optional<Holder<Gas>> parseHolder(HolderLookup.@NotNull Provider lookupProvider, Tag tag) {
        return HOLDER_CODEC.parse(lookupProvider.createSerializationContext(NbtOps.INSTANCE), tag).resultOrPartial(error -> CreateCraftedBeginning.LOGGER.error("Tried to load invalid gas: '{}'", error));
    }

    public static Holder<Gas> parseOptionalHolder(HolderLookup.Provider lookupProvider, @NotNull String tag) {
        if (tag.isEmpty()) {
            return EMPTY_GAS_HOLDER;
        }

        Optional<HolderLookup.RegistryLookup<Gas>> lookup = lookupProvider.lookup(CCBGasRegistry.GAS_REGISTRY_NAME);
        if (lookup.isEmpty()) {
            return EMPTY_GAS_HOLDER;
        }

        ResourceLocation rl = ResourceLocation.tryParse(tag);
        if (rl == null) {
            return EMPTY_GAS_HOLDER;
        }

        Optional<Holder.Reference<Gas>> reference = lookup.get().get(ResourceKey.create(CCBGasRegistry.GAS_REGISTRY_NAME, rl));
        return reference.isPresent() ? reference.get() : EMPTY_GAS_HOLDER;
    }

    @SuppressWarnings("deprecation")
    public static TextureAtlasSprite getGasTexture(@NotNull Holder<Gas> holder) {
        ResourceLocation sprite = holder.value().getIcon();
        if (sprite == null) {
            sprite = MissingTextureAtlasSprite.getLocation();
        }
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(sprite);
    }

    @Override
    public final String toString() {
        return CCBGasRegistry.GAS_REGISTRY.wrapAsHolder(this).getRegisteredName();
    }

    public String getTranslationKey() {
        if (translationKey == null) {
            ResourceLocation id = CCBGasRegistry.GAS_REGISTRY.getKeyOrNull(this);
            if (id != null) {
                translationKey = "gas." + id.getNamespace() + "." + id.getPath();
            }
        }
        return translationKey;
    }

    public Component getTextComponent() {
        return Component.translatable(getTranslationKey());
    }

    public ResourceLocation getIcon() {
        return iconLocation;
    }

    public int getTint() {
        return tint;
    }

    @Nullable
    public Gas getPressurizedGas() {
        if (pressurizedGasName == null) {
            return null;
        }
        return GetGasByName(pressurizedGasName);
    }

    @Nullable
    public Gas getDepressurizedGas() {
        if (depressurizedGasName == null) {
            return null;
        }
        return GetGasByName(depressurizedGasName);
    }

    @Nullable
    public Gas getVortexedGasName() {
        if (vortexedGasName == null) {
            return null;
        }
        return GetGasByName(vortexedGasName);
    }

    public FluidStack getCondensate() {
        if (condensate == null) {
            return FluidStack.EMPTY;
        }
        return condensate;
    }

    public float getPressure() {
        return pressure;
    }

    public float getEnergy() {
        return energy;
    }

    public Gas GetGasByName(String gasName) {
        ResourceLocation currentId = builtInRegistryHolder.key().location();
        String namespace = currentId.getNamespace();
        ResourceLocation pressurizedId = ResourceLocation.fromNamespaceAndPath(namespace, gasName);
        return CCBGasRegistry.GAS_REGISTRY.get(pressurizedId);
    }

    public Holder<Gas> getHolder() {
        return builtInRegistryHolder;
    }

    public boolean is(TagKey<Gas> tag) {
        return getHolder().is(tag);
    }

    public Stream<TagKey<Gas>> getTags() {
        return getHolder().tags();
    }
}
