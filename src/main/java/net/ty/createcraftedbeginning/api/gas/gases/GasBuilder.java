package net.ty.createcraftedbeginning.api.gas.gases;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasBuilder {
    private static final ResourceLocation DEFAULT_TEXTURE = CreateCraftedBeginning.asResource("gas/icon");

    private ResourceLocation texture;
    private int tint = 0xFFFFFF;
    private int alpha = 0xFF;
    @Nullable
    private Set<TagKey<Gas>> tags;

    private GasBuilder() {
    }

    public static GasBuilder builder() {
        return new GasBuilder().texture(null);
    }

    public static GasBuilder builder(ResourceLocation texture) {
        return new GasBuilder().texture(texture);
    }

    public GasBuilder texture(@Nullable ResourceLocation texture) {
        this.texture = texture == null ? DEFAULT_TEXTURE : texture;
        return this;
    }

    public GasBuilder tint(int tint) {
        this.tint = tint;
        return this;
    }

    public GasBuilder alpha(int alpha) {
        this.alpha = alpha;
        return this;
    }

    public GasBuilder tag(TagKey<Gas> tag) {
        if (tags == null) {
            tags = new HashSet<>();
        }
        tags.add(tag);
        return this;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public int getTint() {
        return tint;
    }

    public int getAlpha() {
        return alpha;
    }

    @Nullable
    public Set<TagKey<Gas>> getTags() {
        return tags;
    }
}
