package net.ty.createcraftedbeginning.api.gas;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
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
    private float engineEfficiency;
    private float teslaEfficiency;

    @Nullable
    private String pressurizedGasName;
    @Nullable
    private String depressurizedGasName;
    @Nullable
    private String energizedGasName;
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

    public GasBuilder pressurizedGas(String pressurizedGasName) {
        this.pressurizedGasName = pressurizedGasName;
        return this;
    }

    @SuppressWarnings("unused")
    public GasBuilder depressurizedGas(String depressurizedGasName) {
        this.depressurizedGasName = depressurizedGasName;
        return this;
    }

    public GasBuilder energizedGas(String energizedGasName) {
        this.energizedGasName = energizedGasName;
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

    @Nullable
    public String getPressurizedGasName() {
        return pressurizedGasName;
    }

    @Nullable
    public String getDepressurizedGasName() {
        return depressurizedGasName;
    }

    @Nullable
    public String getEnergizedGasName() {
        return energizedGasName;
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
