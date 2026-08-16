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
    private final EndSculkSilencerBlockEntity blockEntity;
    private boolean inSableSubLevel;

    public EndSculkSilencerController(EndSculkSilencerBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    public void refresh() {
        if (!(blockEntity.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        Projection projection = CCBSubLevelBridge.resolve(serverLevel, blockEntity.getBlockPos());
        inSableSubLevel = projection.inSubLevel();
        refresh(serverLevel, projection.blockPos());
    }

    public void tickServer(ServerLevel serverLevel) {
        if (!inSableSubLevel) {
            return;
        }

        Projection projection = CCBSubLevelBridge.resolve(serverLevel, blockEntity.getBlockPos());
        inSableSubLevel = projection.inSubLevel();
        refresh(serverLevel, projection.blockPos());
    }

    public void remove() {
        if (!(blockEntity.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        GlobalEndSculkSilencerManager.remove(serverLevel, blockEntity.getBlockPos());
    }

    private void refresh(ServerLevel serverLevel, BlockPos effectCenter) {
        GlobalEndSculkSilencerManager.update(serverLevel, blockEntity.getBlockPos(), effectCenter, blockEntity.getActiveWorkingRange());
    }
}
