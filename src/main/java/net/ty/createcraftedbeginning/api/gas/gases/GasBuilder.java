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

    /**
     * Creates a builder for configuring a new instance.
     *
     * @return the created value
     */
    public static GasBuilder builder() {
        return new GasBuilder().texture(null);
    }

    /**
     * Creates a builder for configuring a new instance.
     *
     * @param texture the texture resource location to use
     * @return the created value
     */
    public static GasBuilder builder(ResourceLocation texture) {
        return new GasBuilder().texture(texture);
    }

    /**
     * Sets the texture used by this builder.
     *
     * @param texture the texture resource location to use
     * @return this builder for chaining
     */
    public GasBuilder texture(@Nullable ResourceLocation texture) {
        this.texture = texture == null ? DEFAULT_TEXTURE : texture;
        return this;
    }

    /**
     * Sets the tint color used by this builder.
     *
     * @param tint the tint color to use
     * @return this builder for chaining
     */
    public GasBuilder tint(int tint) {
        this.tint = tint;
        return this;
    }

    /**
     * Sets the alpha value used by this builder.
     *
     * @param alpha the alpha value to use
     * @return this builder for chaining
     */
    public GasBuilder alpha(int alpha) {
        this.alpha = alpha;
        return this;
    }

    /**
     * Creates an ingredient that matches gases in the supplied tag.
     *
     * @param tag the tag to inspect or process
     * @return the created value
     */
    public GasBuilder tag(TagKey<Gas> tag) {
        if (tags == null) {
            tags = new HashSet<>();
        }
        tags.add(tag);
        return this;
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
     * Returns the tint.
     *
     * @return the tint
     */
    public int getTint() {
        return tint;
    }

    /**
     * Returns the alpha.
     *
     * @return the alpha
     */
    public int getAlpha() {
        return alpha;
    }

    /**
     * Returns the tags.
     *
     * @return the tags
     */
    @Nullable
    public Set<TagKey<Gas>> getTags() {
        return tags;
    }
}
