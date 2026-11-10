package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.apache.commons.lang3.tuple.MutablePair;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystSoundSuppressorMovementBehaviour implements MovementBehaviour {
    static void refreshContraption(AbstractContraptionEntity entity) {
        Contraption contraption = entity.getContraption();
        if (contraption == null) {
            return;
        }

        for (MutablePair<StructureBlockInfo, MovementContext> actor : contraption.getActors()) {
            MovementContext context = actor.getRight();
            if (!actor.getLeft().state().is(CCBBlocks.AMETHYST_SOUND_SUPPRESSOR_BLOCK.get()) || context == null) {
                continue;
            }

            updateRegistration(context);
        }
    }

    private static void updateRegistration(MovementContext context) {
        AbstractContraptionEntity entity = context.contraption.entity;
        if (entity == null || !(context.world instanceof ServerLevel serverLevel)) {
            return;
        }

        AmethystSoundSuppressorIdentity identity = resolveIdentity(context, entity);
        if (context.disabled || !entity.isAlive()) {
            AmethystSoundSuppressorManager.removeMoving(serverLevel, identity);
            return;
        }

        Vec3 position = entity.toGlobalVector(Vec3.atCenterOf(context.localPos), 1);
        BlockPos effectCenter = SubLevelBridge.resolve(serverLevel, position).blockPos();
        AmethystSoundSuppressorManager.updateMoving(serverLevel, identity, effectCenter);
    }

    private static void removeRegistration(MovementContext context) {
        AbstractContraptionEntity entity = context.contraption.entity;
        if (entity == null || !(context.world instanceof ServerLevel serverLevel)) {
            return;
        }

        AmethystSoundSuppressorManager.removeMoving(serverLevel, resolveIdentity(context, entity));
    }

    private static AmethystSoundSuppressorIdentity resolveIdentity(MovementContext context, AbstractContraptionEntity entity) {
        BlockPos localPos = context.localPos;
        UUID contraptionId = entity.getUUID();
        if (context.temporaryData instanceof AmethystSoundSuppressorIdentity identity && identity.blockPos().equals(localPos) && contraptionId.equals(identity.contraptionId())) {
            return identity;
        }

        AmethystSoundSuppressorIdentity identity = new AmethystSoundSuppressorIdentity(localPos, contraptionId);
        context.temporaryData = identity;
        return identity;
    }

    @Override
    public void startMoving(MovementContext context) {
        updateRegistration(context);
    }

    @Override
    public void tick(MovementContext context) {
        if (context.world.isClientSide) {
            AmethystSoundSuppressorClient.tick(context);
            return;
        }

        updateRegistration(context);
    }

    @Override
    public void onDisabledByControls(MovementContext context) {
        MovementBehaviour.super.onDisabledByControls(context);
        removeRegistration(context);
    }

    @Override
    public boolean mustTickWhileDisabled() {
        return true;
    }

    @Override
    public void stopMoving(MovementContext context) {
        removeRegistration(context);
    }
}
