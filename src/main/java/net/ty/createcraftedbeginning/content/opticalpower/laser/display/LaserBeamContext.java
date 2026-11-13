package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Boundary;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamTerminal;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRenderSnapshot;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record LaserBeamContext(double sourceRadius, List<DisplaySegment> segments, Vec3 localOrigin, Vec3 worldOrigin, Vec3 xAxis, Vec3 yAxis, Vec3 zAxis) {
    public LaserBeamContext {
        segments = List.copyOf(segments);
    }

    public static LaserBeamContext from(LaserRenderSnapshot snapshot) {
        BeamPropagation propagation = snapshot.propagation();
        List<DisplaySegment> segments = propagation.segments().stream().map(segment -> {
            OpticalHit hit = segment.hit();
            Boundary boundary = segment.boundary();
            OpticalPlane contact = null;
            if (boundary != null) {
                contact = boundary.cutoffPlane();
            }
            else if (hit != null) {
                contact = new OpticalPlane(hit.position(), hit.surface().normal());
            }
            OpticalPlane impact = null;
            BeamTerminal terminal = segment.terminal();
            if (hit != null && boundary != null && !boundary.opticalInteraction() && (terminal == null || terminal.faces().isEmpty())) {
                impact = new OpticalPlane(boundary.firstContactPoint(), hit.surface().normal());
            }
            return new DisplaySegment(segment.parentIndex(), state(segment.start()), segment.length(), segment.remainingRange(), contact, impact);
        }).toList();
        return new LaserBeamContext(propagation.source().profile().radius(), segments, snapshot.localOrigin(), snapshot.worldOrigin(), snapshot.xAxis(), snapshot.yAxis(), snapshot.zAxis());
    }

    public static DisplayState state(BeamState state) {
        BeamFrame frame = state.frame();
        return new DisplayState(new DisplayFrame(frame.origin(), frame.direction(), frame.transverseAxis()), state.profile(), state.fluxLm(), state.footprint());
    }

    public LaserBeamContext atRenderPose(Level level, float partialTick) {
        Vec3 origin = localOrigin();
        CoordinateTransform transform = SubLevelBridge.createRenderTransform(level, origin.add(0.5, 0.5, 0.5), partialTick);
        return new LaserBeamContext(sourceRadius(), segments(), origin, transform.transformPosition(origin), transform.transformNormal(new Vec3(1, 0, 0)), transform.transformNormal(new Vec3(0, 1, 0)), transform.transformNormal(new Vec3(0, 0, 1)));
    }

    public double distanceTo(Vec3 position) {
        double distanceSquared = Double.POSITIVE_INFINITY;
        for (DisplaySegment segment : segments) {
            DisplayFrame frame = segment.start().frame();
            Vec3 origin = frame.origin();
            Vec3 direction = frame.direction();
            double x = position.x - origin.x;
            double y = position.y - origin.y;
            double z = position.z - origin.z;
            double along = Math.clamp(x * direction.x + y * direction.y + z * direction.z, 0, segment.length());
            x -= direction.x * along;
            y -= direction.y * along;
            z -= direction.z * along;
            distanceSquared = Math.min(distanceSquared, x * x + y * y + z * z);
        }
        return Math.sqrt(distanceSquared);
    }

    public Vec3 toLocalPosition(Vec3 worldPosition) {
        return localOrigin.add(toLocalDirection(worldPosition.subtract(worldOrigin)));
    }

    public Vec3 toLocalDirection(Vec3 worldDirection) {
        return new Vec3(worldDirection.dot(xAxis) / xAxis.length(), worldDirection.dot(yAxis) / yAxis.length(), worldDirection.dot(zAxis) / zAxis.length());
    }

    public Vec3 toWorldPosition(Vec3 localPosition) {
        Vec3 local = localPosition.subtract(localOrigin);
        return worldOrigin.add(xAxis.scale(local.x / xAxis.length())).add(yAxis.scale(local.y / yAxis.length())).add(zAxis.scale(local.z / zAxis.length()));
    }

    public record DisplayFrame(Vec3 origin, Vec3 direction, Vec3 transverseAxis) {
        public Vec3 getSecondTransverseAxis() {
            return direction.cross(transverseAxis);
        }
    }

    public record DisplayState(DisplayFrame frame, BeamProfile profile, double fluxLm, BeamFootprint footprint) {}

    public record DisplaySegment(int parentIndex, DisplayState start, double length, double remainingRange, @Nullable OpticalPlane contact, @Nullable OpticalPlane impact) {

        public DisplaySegment(int parentIndex, BeamState start, double length, double remainingRange, @Nullable OpticalPlane contact) {
            this(parentIndex, state(start), length, remainingRange, contact, null);
        }
    }
}
