package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.ty.createcraftedbeginning.platform.CCBSubLevelBridge;
import net.ty.createcraftedbeginning.platform.CCBSubLevelBridge.Projection;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class EndSculkSilencerController {
    private final EndSculkSilencerBlockEntity silencer;
    private boolean isInSubLevel;

    public EndSculkSilencerController(EndSculkSilencerBlockEntity silencer) {
        this.silencer = silencer;
    }

    public void refresh() {
        if (!(silencer.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        Projection projection = CCBSubLevelBridge.resolve(serverLevel, silencer.getBlockPos());
        isInSubLevel = projection.inSubLevel();
        refresh(serverLevel, projection.blockPos());
    }

    public void tickServer(ServerLevel serverLevel) {
        if (!isInSubLevel) {
            return;
        }

        Projection projection = CCBSubLevelBridge.resolve(serverLevel, silencer.getBlockPos());
        isInSubLevel = projection.inSubLevel();
        refresh(serverLevel, projection.blockPos());
    }

    public void remove() {
        if (!(silencer.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        GlobalEndSculkSilencerManager.remove(serverLevel, silencer.getBlockPos());
    }

    private void refresh(ServerLevel serverLevel, BlockPos effectCenter) {
        GlobalEndSculkSilencerManager.update(serverLevel, silencer.getBlockPos(), effectCenter, silencer.getActiveWorkingRange());
    }
}
