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
@SuppressWarnings("unused")
public final class CCBFluidClientExtensionRegistry {
    private static final List<Registration> REGISTRATIONS = new ArrayList<>();

    private CCBFluidClientExtensionRegistry() {
    }

    public static void registerSimple(FluidType fluidType, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
        REGISTRATIONS.add(new SimpleRegistration(fluidType, stillTexture, flowingTexture));
    }

    public static void registerTinted(TintedFluidType fluidType) {
        REGISTRATIONS.add(new TintedRegistration(fluidType));
    }

    @Contract(pure = true)
    public static @Unmodifiable List<Registration> registrations() {
        return List.copyOf(REGISTRATIONS);
    }

    public sealed interface Registration permits SimpleRegistration, TintedRegistration {
        FluidType fluidType();
    }

    public record SimpleRegistration(FluidType fluidType, ResourceLocation stillTexture, ResourceLocation flowingTexture) implements Registration {}

    public record TintedRegistration(TintedFluidType fluidType) implements Registration {}
}
