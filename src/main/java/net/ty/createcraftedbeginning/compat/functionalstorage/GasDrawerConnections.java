package net.ty.createcraftedbeginning.compat.functionalstorage;

import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.compat.functionalstorage.access.GasControllerAccess;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasDrawerConnections {
    private List<Long> candidates = List.of();
    private List<GasHandler> handlers = List.of();

    public List<GasHandler> getHandlers() {
        return handlers;
    }

    public void beginRebuild(List<Long> connected) {
        candidates = List.copyOf(connected);
    }

    public void include(@Nullable Level level, StorageControllerTile<?> controller, List<Long> valid) {
        if (level == null || level.isClientSide()) {
            return;
        }

        AABB area = new AABB(controller.getBlockPos()).inflate(controller.getStorageMultiplier());
        for (long packedPos : candidates) {
            if (valid.contains(packedPos)) {
                continue;
            }

            BlockPos pos = BlockPos.of(packedPos);
            if (!area.contains(Vec3.atCenterOf(pos)) || !level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof GasDrawerBlockEntity)) {
                continue;
            }

            valid.add(packedPos);
        }
    }

    public void finishRebuild(@Nullable Level level, StorageControllerTile<?> controller, List<Long> connected) {
        candidates = List.of();
        if (level == null || level.isClientSide()) {
            handlers = List.of();
            return;
        }

        List<GasHandler> handlers = new ArrayList<>();
        for (long packedPos : connected) {
            BlockPos pos = BlockPos.of(packedPos);
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof GasDrawerBlockEntity drawer)) {
                continue;
            }

            handlers.add(drawer.getGasHandler());
        }
        this.handlers = List.copyOf(handlers);
        GasHandler handler = ((GasControllerAccess) controller).ccb$getGasHandler();
        if (!(handler instanceof ControllerGasHandler gasController)) {
            return;
        }

        gasController.refresh(this.handlers);
    }
}
