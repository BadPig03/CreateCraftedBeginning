package net.ty.createcraftedbeginning.api.gas;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class GasBuilder {
    private final ResourceLocation texture;
    private int tint = 0xFFFFFF;
    private float pressure = 0f;
    private float energy = 0f;

    @Nullable
    private String pressurizedGasName;
    @Nullable
    private String depressurizedGasName;
    @Nullable
    private String vortexedGasName;
    @Nullable
    private FluidStack condensate;

    protected GasBuilder(ResourceLocation texture) {
        this.texture = texture;
    }

    @Contract("_ -> new")
    public static @NotNull GasBuilder builder(ResourceLocation texture) {
        return new GasBuilder(Objects.requireNonNull(texture));
    }

    @Contract(" -> new")
    public static @NotNull GasBuilder builder() {
        return builder(ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "gas/gas"));
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public GasBuilder tint(int tint) {
        this.tint = tint;
        return this;
    }

    public GasBuilder pressurizedGas(String name) {
        this.pressurizedGasName = name;
        return this;
    }

    public GasBuilder depressurizedGas(String name) {
        this.depressurizedGasName = name;
        return this;
    }

    public GasBuilder vortexedGas(String name) {
        this.vortexedGasName = name;
        return this;
    }

    public GasBuilder pressure(float pressure) {
        this.pressure = pressure;
        return this;
    }

    public GasBuilder energy(float energy) {
        this.energy = energy;
        return this;
    }

    public GasBuilder condensate(FluidStack condensate) {
        this.condensate = condensate;
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
    public String getVortexedGasName() {
        return vortexedGasName;
    }

    @Nullable
    public FluidStack getCondensate() {
        return condensate;
    }

    public float getPressure() {
        return pressure;
    }

    public float getEnergy() {
        return energy;
    }
}
