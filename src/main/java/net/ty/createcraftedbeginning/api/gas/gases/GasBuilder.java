package net.ty.createcraftedbeginning.api.gas.gases;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public class GasBuilder {
    private static final ResourceLocation TEXTURE = CreateCraftedBeginning.asResource("gas/icon");

    private final ResourceLocation texture;
    private int tint = 0xFFFFFF;
    private float inflation;
    private float engineEfficiency;
    private float teslaEfficiency;

    @Nullable
    private ResourceLocation pressurizedGasType;
    @Nullable
    private ResourceLocation energizedGasType;
    @Nullable
    private Supplier<FluidStack> outputFluidStackSupplier;
    @Nullable
    private Supplier<ItemStack> outputItemStackSupplier;
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

    public GasBuilder pressurizedGas(ResourceLocation pressurizedGasType) {
        this.pressurizedGasType = pressurizedGasType;
        return this;
    }

    public GasBuilder energizedGas(ResourceLocation energizedGasType) {
        this.energizedGasType = energizedGasType;
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

    public GasBuilder outputItemStack(Supplier<ItemStack> outputItemStackSupplier) {
        this.outputItemStackSupplier = outputItemStackSupplier;
        return this;
    }

    public GasBuilder outputFluidStack(Supplier<FluidStack> outputFluidStackSupplier) {
        this.outputFluidStackSupplier = outputFluidStackSupplier;
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

    @Nullable
    public ResourceLocation getPressurizedGasType() {
        return pressurizedGasType;
    }

    @Nullable
    public ResourceLocation getEnergizedGasType() {
        return energizedGasType;
    }

    @Nullable
    public FluidStack getOutputFluidStack() {
        return outputFluidStackSupplier != null ? outputFluidStackSupplier.get() : null;
    }

    @Nullable
    public ItemStack getOutputItemStack() {
        return outputItemStackSupplier != null ? outputItemStackSupplier.get() : null;
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
