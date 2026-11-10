package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamGeometry.Section;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.DrawSection;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.Face;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.SurfaceReason;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record LaserBeamDisplayInput(LaserBeamContext context, Base base, Mapping mapping) {
    public record Base(List<DrawSection> sections, List<Face> faces, SurfaceReason reason, int remainingBudget, Set<Integer> invalidSegments) {
        public Base {
            sections = List.copyOf(sections);
            faces = List.copyOf(faces);
            invalidSegments = Set.copyOf(invalidSegments);
        }

        public boolean isComplete() {
            return reason == SurfaceReason.COMPLETE || reason == SurfaceReason.EMPTY;
        }
    }

    public record Mapping(MappingState state, String reason, Set<Integer> selected, List<MappedFamily> families) {
        public Mapping {
            selected = Set.copyOf(selected);
            families = List.copyOf(families);
        }
    }

    public enum MappingState {
        READY,
        NO_FAMILY,
        UNSUPPORTED
    }

    public record MappedFamily(List<Integer> displayIds, int representative, double pathDistance, LaserBeamAppearance appearance, List<Tile> tiles, boolean polygonal) {
        public MappedFamily {
            displayIds = List.copyOf(displayIds);
            tiles = List.copyOf(tiles);
        }
    }

    public record Tile(Section geometry, Section support, List<Point> polygon, Vec3 startCenter, Vec3 endCenter, LaserBeamBrightness field, LaserBeamBrightness haloField, double pathOffset, List<Boolean> exposedEdges, List<Vec3> outward, @Nullable List<OpticalPlane> endPlanes, @Nullable Vec3 interior) {
        public Tile {
            polygon = List.copyOf(polygon);
            exposedEdges = List.copyOf(exposedEdges);
            outward = List.copyOf(outward);
            endPlanes = endPlanes == null ? null : List.copyOf(endPlanes);
        }
    }
}
