package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorBlock;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Reason;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Termination;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserEmitterAdvancements {
    private static final int REQUIRED_REFLECTIONS = 4;

    private LaserEmitterAdvancements() {
    }

    static void award(LaserEmitterBlockEntity emitter, BeamPropagation propagation) {
        CCBAdvancementBehaviour advancement = emitter.getBehaviour(CCBAdvancementBehaviour.TYPE);
        if (advancement == null || advancement.getPlayer() == null) {
            return;
        }

        boolean emitted = false;
        int reflections = 0;
        List<Segment> segments = propagation.segments();
        List<Termination> terminations = propagation.terminations();
        for (int index = 0; index < segments.size(); index++) {
            Segment segment = segments.get(index);
            if (segment.length() <= 0 || segment.incident().fluxLm() <= 0) {
                continue;
            }

            emitted = true;
            OpticalHit hit = segment.hit();
            if (hit == null || !AmethystMirrorBlock.isReflectiveSurface(hit.surface())) {
                continue;
            }

            boolean validReflection = true;
            for (Termination termination : terminations) {
                Reason reason = termination.reason();
                if (termination.parentIndex() != index || termination.state().fluxLm() > 0 && reason != Reason.REPEATED_STATE && reason != Reason.NO_PROGRESS) {
                    continue;
                }

                validReflection = false;
                break;
            }
            if (!validReflection) {
                continue;
            }

            reflections++;
        }
        if (emitted) {
            advancement.awardPlayer(CCBAdvancements.LIGHT_ON_THE_MOTION_OF_PHOTONS);
        }
        if (reflections < REQUIRED_REFLECTIONS) {
            return;
        }

        advancement.awardPlayer(CCBAdvancements.RECURSIVE_GLOW);
    }
}
