package net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorControls;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRenderGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRigidDisplay;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserStaticDependencies.Sample;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserStaticDependencies;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserDisplaySnapshot;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamSync {
    private @Nullable LaserStaticDependencies bound;
    private @Nullable LaserRenderGeometry geometry;
    private List<BlockPos> optics = List.of();

    @Nullable LaserDisplaySnapshot current(LaserEmitterBlockEntity owner, Level level, BeamState source, float partialTick) {
        LaserDisplaySnapshot snapshot = owner.getDisplaySnapshot();
        if (level != Minecraft.getInstance().level || owner.isRemoved() || owner.isVirtual() || snapshot == null || owner.getLaserRange() != snapshot.range()) {
            return null;
        }

        BeamFrame frame = source.frame();
        if (source.fluxLm() != snapshot.fluxLm() || !frame.origin().equals(snapshot.origin()) || !frame.direction().equals(snapshot.direction())) {
            return null;
        }

        LaserStaticDependencies dependencies = snapshot.dependencies();
        if (bound != dependencies || geometry == null) {
            bound = dependencies;
            geometry = new LaserRenderGeometry(level, dependencies);
            optics = dependencies.blocks().stream().filter(sample -> sample.optical() != null).map(Sample::pos).toList();
        }
        for (BlockPos pos : optics) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof AmethystMirrorBlockEntity mirror && AmethystMirrorControls.hasDisplayPreview(mirror)) {
                return null;
            }
        }
        BlockPos emitterPos = owner.getBlockPos();
        LaserRigidDisplay rigid = snapshot.rigid();
        if (rigid != null) {
            if (!rigid.matches(level, emitterPos, dependencies, partialTick)) {
                return null;
            }

            return snapshot;
        }

        if (SubLevelBridge.resolve(level, emitterPos).inSubLevel() || !geometry.matches(level, partialTick)) {
            return null;
        }

        return snapshot;
    }

    void clear() {
        bound = null;
        geometry = null;
        optics = List.of();
    }
}
