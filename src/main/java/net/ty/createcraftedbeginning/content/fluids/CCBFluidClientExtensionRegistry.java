package net.ty.createcraftedbeginning.content.fluids;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBFluidClientExtensionRegistry {
    private static final List<FluidRegistration> REGISTRATIONS = new ArrayList<>();

    private CCBFluidClientExtensionRegistry() {
    }

    public static void registerSimple(FluidType fluidType, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
        REGISTRATIONS.add(new SimpleRegistration(fluidType, stillTexture, flowingTexture));
    }

    @Contract(pure = true)
    public static @Unmodifiable List<FluidRegistration> registrations() {
        return List.copyOf(REGISTRATIONS);
    }

    static void registerTinted(TintedFluidType fluidType) {
        REGISTRATIONS.add(new TintedRegistration(fluidType));
    }

    public sealed interface FluidRegistration permits SimpleRegistration, TintedRegistration {}

    public record SimpleRegistration(FluidType fluidType, ResourceLocation stillTexture, ResourceLocation flowingTexture) implements FluidRegistration {}

    public record TintedRegistration(TintedFluidType fluidType) implements FluidRegistration {}
}
