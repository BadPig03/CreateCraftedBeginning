package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput.Tile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserDisplayFamilies.Family;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamMappedGeometry {
    private LaserBeamMappedGeometry() {}

    static @Unmodifiable List<FamilyGeometry> create(List<Family> families) {
        List<FamilyGeometry> geometry = new ArrayList<>();
        for (Family family : families) {
            try {
                geometry.add(LaserBeamStripDisplay.create(family));
            }
            catch (UnsupportedOperationException unsupported) {
                geometry.add(LaserBeamPolygonDisplay.create(family));
            }
        }

        if (geometry.stream().anyMatch(FamilyGeometry::polygonal)) {
            for (int index = 0; index < geometry.size(); index++) {
                if (geometry.get(index).polygonal()) {
                    continue;
                }

                geometry.set(index, LaserBeamPolygonDisplay.create(families.get(index)));
            }
        }

        return List.copyOf(geometry);
    }

    static LaserBeamWork<List<FamilyGeometry>> begin(List<Family> families) {
        return new Build(families);
    }

    private static final class Build implements LaserBeamWork<List<FamilyGeometry>> {
        private final List<Family> families;
        private final List<FamilyGeometry> geometry = new ArrayList<>();
        private @Nullable LaserBeamWork<FamilyGeometry> active;
        private boolean polygon;
        private boolean converting;
        private int index;
        private List<FamilyGeometry> result;

        private Build(List<Family> families) {
            this.families = families;
        }

        @Override
        public boolean step() {
            if (result != null) {
                return true;
            }

            if (active != null) {
                try {
                    if (!active.step()) {
                        return false;
                    }

                    FamilyGeometry completed = active.result();
                    if (converting) {
                        geometry.set(index, completed);
                    }
                    else {
                        geometry.add(completed);
                    }

                    active = null;
                    index++;
                    return false;
                }
                catch (UnsupportedOperationException unsupported) {
                    if (polygon) {
                        throw unsupported;
                    }

                    polygon = true;
                    active = LaserBeamPolygonDisplay.begin(families.get(index));
                    return false;
                }
            }

            if (index >= families.size()) {
                if (!converting && geometry.stream().anyMatch(FamilyGeometry::polygonal)) {
                    converting = true;
                    index = 0;
                    return false;
                }

                result = List.copyOf(geometry);
                return true;
            }

            if (converting && geometry.get(index).polygonal()) {
                index++;
                return false;
            }

            polygon = converting;
            try {
                active = polygon ? LaserBeamPolygonDisplay.begin(families.get(index)) : LaserBeamStripDisplay.begin(families.get(index));
            }
            catch (UnsupportedOperationException unsupported) {
                polygon = true;
                active = LaserBeamPolygonDisplay.begin(families.get(index));
            }

            return false;
        }

        @Override
        public List<FamilyGeometry> result() {
            return Objects.requireNonNull(result, "Mapped laser beam family geometry build has not completed.");
        }
    }

    record FamilyGeometry(Family family, List<Tile> tiles, boolean polygonal) {
        FamilyGeometry {
            tiles = List.copyOf(tiles);
        }
    }

}
