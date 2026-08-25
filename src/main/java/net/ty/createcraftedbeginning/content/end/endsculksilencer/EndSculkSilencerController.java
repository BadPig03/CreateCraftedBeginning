package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.Projection;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class EndSculkSilencerController {
    private final EndSculkSilencerBlockEntity silencer;
    private boolean isInSubLevel;

    EndSculkSilencerController(EndSculkSilencerBlockEntity silencer) {
        this.silencer = silencer;
    }

    void refresh() {
        if (!(silencer.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        Projection projection = SubLevelBridge.resolve(serverLevel, silencer.getBlockPos());
        isInSubLevel = projection.inSubLevel();
        refresh(serverLevel, projection.blockPos());
    }

    void tickServer(ServerLevel serverLevel) {
        if (!isInSubLevel) {
            return;
        }

        Projection projection = SubLevelBridge.resolve(serverLevel, silencer.getBlockPos());
        isInSubLevel = projection.inSubLevel();
        refresh(serverLevel, projection.blockPos());
    }

    void remove() {
        if (!(silencer.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        GlobalEndSculkSilencerManager.remove(serverLevel, silencer.getBlockPos());
    }

    private void refresh(ServerLevel serverLevel, BlockPos effectCenter) {
        GlobalEndSculkSilencerManager.update(serverLevel, silencer.getBlockPos(), effectCenter, silencer.getActiveWorkingRange());
    }
}
