package net.ty.createcraftedbeginning.api.gas.gases;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class GasBuilder {
    private static final ResourceLocation TEXTURE = CreateCraftedBeginning.asResource("gas/icon");

    private final ResourceLocation texture;
    private int tint = 0xFFFFFF;
    private float inflation;
    private float engineEfficiency;
    private float teslaEfficiency;

    @Nullable
    private Set<TagKey<Gas>> tags;

    protected GasBuilder(ResourceLocation texture) {
        this.texture = texture;
    }

    @Contract(" -> new")
    public static @NotNull GasBuilder builder() {
        return builder(TEXTURE);
    }

    @Contract("_ -> new")
    public static @NotNull GasBuilder builder(ResourceLocation texture) {
        return new GasBuilder(Objects.requireNonNull(texture));
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public GasBuilder tint(int tint) {
        this.tint = tint;
        return this;
    }

    public GasBuilder inflation(float inflation) {
        this.inflation = inflation;
        return this;
    }

    public GasBuilder engineEfficiency(float engineEfficiency) {
        this.engineEfficiency = engineEfficiency;
        return this;
    }

    public GasBuilder teslaEfficiency(float teslaEfficiency) {
        this.teslaEfficiency = teslaEfficiency;
        return this;
    }

    public GasBuilder tag(TagKey<Gas> tag) {
        if (tags == null) {
            tags = new HashSet<>();
        }
        tags.add(tag);
        return this;
    }

    public int getTint() {
        return tint;
    }

    public float getInflation() {
        return inflation;
    }

    public float getEngineEfficiency() {
        return engineEfficiency;
    }

    public float getTeslaEfficiency() {
        return teslaEfficiency;
    }

    @Nullable
    public Set<TagKey<Gas>> getTags() {
        return tags;
    }
}
