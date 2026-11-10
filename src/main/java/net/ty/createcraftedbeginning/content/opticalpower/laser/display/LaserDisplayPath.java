package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact.Vertex;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamPathReplay;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamPathReplay.Evaluation;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record LaserDisplayPath(BeamState source, List<OpticalHit> events, double range) {
    LaserDisplayPath {
        events = List.copyOf(events);
        if (!Double.isFinite(range) || range < 0) {
            throw new IllegalArgumentException("Display path range must be finite and non-negative; got " + range + " blocks.");
        }
    }

    static @Nullable PhysicalPoint intersect(Endpoint endpoint, OpticalHit terminal) {
        BeamState start = endpoint.start();
        double remaining = endpoint.remainingRange();
        BeamFootprint footprint = start.footprint();
        Vertex vertex = BeamContact.intersect(start, start.profile().spans(remaining), terminal, footprint.horizontalOffset(), footprint.verticalOffset());
        if (vertex == null) {
            return null;
        }

        double distance = vertex.distance();
        return new PhysicalPoint(start, start.advance(distance), vertex.position(), distance, remaining);
    }

    @Nullable Endpoint evaluate(double u, double v) {
        return evaluate(new BeamFootprint(1, 1, u, v), source.fluxLm());
    }

    @Nullable Endpoint evaluate(BeamFootprint footprint, double flux) {
        Evaluation endpoint = BeamPathReplay.evaluate(source, events, range, footprint, flux);
        if (endpoint == null) {
            return null;
        }

        return new Endpoint(endpoint.state(), endpoint.remainingRange());
    }

    @Nullable PhysicalPoint intersect(double u, double v, OpticalHit terminal) {
        Endpoint endpoint = evaluate(u, v);
        if (endpoint == null) {
            return null;
        }

        return intersect(endpoint, terminal);
    }

    record Endpoint(BeamState start, double remainingRange) {}

    record PhysicalPoint(BeamState start, BeamState incident, Vec3 physicalPosition, double distance, double remainingRange) {}
}
