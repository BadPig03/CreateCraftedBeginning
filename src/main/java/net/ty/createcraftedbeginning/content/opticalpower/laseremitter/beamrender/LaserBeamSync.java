package net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorControls;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRigidDisplay;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserStaticDependencies;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserStaticDependencies.Sample;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserDisplaySnapshot;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamSync {
    private LaserBeamSync() {}

    static @Nullable LaserDisplaySnapshot current(LaserEmitterBlockEntity owner, Level level, BeamState source, float partialTick) {
        LaserDisplaySnapshot snapshot = owner.getDisplaySnapshot();
        if (level != Minecraft.getInstance().level || owner.isRemoved() || owner.isVirtual() || snapshot == null || owner.getLaserRange() != snapshot.range()) {
            return null;
        }

        BeamFrame frame = source.frame();
        if (source.fluxLm() != snapshot.fluxLm() || !frame.origin().equals(snapshot.origin()) || !frame.direction().equals(snapshot.direction())) {
            return null;
        }

        LaserStaticDependencies dependencies = snapshot.dependencies();
        for (Sample sample : dependencies.blocks()) {
            if (sample.optical() == null) {
                continue;
            }

            BlockPos pos = sample.pos();
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

        if (SubLevelBridge.resolve(level, emitterPos).inSubLevel() || !dependencies.matches(level, partialTick)) {
            return null;
        }

        return snapshot;
    }
}
