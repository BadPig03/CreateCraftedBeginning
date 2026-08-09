package net.ty.createcraftedbeginning.content.end.endincinerationblower;

import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.platform.CCBSubLevelBridge.EntityArea;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class EndIncinerationBlowerTargetCache {
    private static final int ITEM_ENTITY_CACHE_INTERVAL = 5;
    private static final int TRANSPORTED_HANDLER_CACHE_INTERVAL = 20;

    private final BlockPos origin;
    private final List<ItemEntity> affectedItems = new ArrayList<>();
    private final List<TransportedItemStackHandlerBehaviour> transportedHandlers = new ArrayList<>();

    private int cachedBlockRadius = -1;
    private long nextItemEntityScanTime = Long.MIN_VALUE;
    private long nextTransportedHandlerScanTime = Long.MIN_VALUE;

    EndIncinerationBlowerTargetCache(BlockPos origin) {
        this.origin = origin;
    }

    List<ItemEntity> getAffectedItems(ServerLevel level, AABB area, EntityArea entityArea) {
        long gameTime = level.getGameTime();
        if (gameTime < nextItemEntityScanTime) {
            return affectedItems;
        }

        affectedItems.clear();
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            if (entityArea.intersects(itemEntity)) {
                affectedItems.add(itemEntity);
            }
        }
        nextItemEntityScanTime = gameTime + ITEM_ENTITY_CACHE_INTERVAL;
        return affectedItems;
    }

    List<TransportedItemStackHandlerBehaviour> getTransportedHandlers(Level level, float speed) {
        int blockRadius = EndIncinerationBlowerRange.calculateBlockRadius(speed);
        long gameTime = level.getGameTime();
        if (cachedBlockRadius == blockRadius && gameTime < nextTransportedHandlerScanTime) {
            return transportedHandlers;
        }

        transportedHandlers.clear();
        BlockPos min = origin.offset(-blockRadius, -blockRadius, -blockRadius);
        BlockPos max = origin.offset(blockRadius, blockRadius, blockRadius);
        for (BlockPos blockPos : BlockPos.betweenClosed(min, max)) {
            TransportedItemStackHandlerBehaviour behaviour = BlockEntityBehaviour.get(level, blockPos, TransportedItemStackHandlerBehaviour.TYPE);
            if (behaviour != null) {
                transportedHandlers.add(behaviour);
            }
        }

        cachedBlockRadius = blockRadius;
        nextTransportedHandlerScanTime = gameTime + TRANSPORTED_HANDLER_CACHE_INTERVAL;
        return transportedHandlers;
    }
}
