package net.ty.createcraftedbeginning.content.fluids;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public final class CCBFluidClientExtensions {
    private static final List<Registration> REGISTRATIONS = new ArrayList<>();

    private CCBFluidClientExtensions() {
    }

    public static void track(FluidType fluidType, Supplier<IClientFluidTypeExtensions> factory) {
        REGISTRATIONS.add(new Registration(fluidType, factory));
    }

    public static void register(RegisterClientExtensionsEvent event) {
        for (Registration registration : REGISTRATIONS) {
            event.registerFluidType(registration.factory().get(), registration.fluidType());
        }
    }

    private record Registration(FluidType fluidType, Supplier<IClientFluidTypeExtensions> factory) {}
}
