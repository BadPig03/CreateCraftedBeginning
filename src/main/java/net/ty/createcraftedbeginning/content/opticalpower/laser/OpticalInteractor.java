package net.ty.createcraftedbeginning.content.opticalpower.laser;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface OpticalInteractor {
    OpticalInteractor ABSORBING = (incident, hit) -> new OpticalInteraction(null, incident.fluxLm());

    OpticalInteraction interact(BeamState incident, OpticalHit hit);

    default boolean supportsDisplayMeshReuse() {
        return false;
    }
}
