package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkEvent.Load;
import net.neoforged.neoforge.event.level.ChunkTicketLevelUpdatedEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
public final class AirtightPipeEvents {
    private static final Map<ServerLevel, Set<ChunkPos>> PENDING = new HashMap<>();

    private AirtightPipeEvents() {
    }

    @SubscribeEvent
    public static void onChunkLoad(Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        PENDING.computeIfAbsent(level, ignored -> new HashSet<>()).add(event.getChunk().getPos());
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        PENDING.computeIfAbsent(level, ignored -> new HashSet<>()).add(event.getChunk().getPos());
    }

    @SubscribeEvent
    public static void onChunkTicketLevelUpdated(ChunkTicketLevelUpdatedEvent event) {
        int accessible = ChunkLevel.byStatus(ChunkStatus.FULL);
        boolean wasAccessible = event.getOldTicketLevel() <= accessible;
        boolean isAccessible = event.getNewTicketLevel() <= accessible;
        if (wasAccessible == isAccessible) {
            return;
        }

        PENDING.computeIfAbsent(event.getLevel(), ignored -> new HashSet<>()).add(new ChunkPos(event.getChunkPos()));
    }

    @SubscribeEvent
    public static void onLevelTick(Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Set<ChunkPos> changed = PENDING.remove(level);
        if (changed == null) {
            return;
        }

        Set<BlockPos> pipes = new HashSet<>();
        for (ChunkPos pos : changed) {
            collectPipes(level, pos, pos, pipes);
            for (Direction direction : Iterate.horizontalDirections) {
                collectPipes(level, new ChunkPos(pos.x + direction.getStepX(), pos.z + direction.getStepZ()), pos, pipes);
            }
        }
        for (BlockPos pos : pipes) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof AirtightPipeBlock block)) {
                continue;
            }

            block.tick(state, level, pos, level.random);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        PENDING.remove(level);
    }

    private static void collectPipes(ServerLevel level, ChunkPos pos, ChunkPos changed, Set<BlockPos> pipes) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null) {
            return;
        }

        for (BlockEntity entity : List.copyOf(chunk.getBlockEntities().values())) {
            if (!(entity instanceof AirtightPipeBlockEntity)) {
                continue;
            }

            BlockPos pipePos = entity.getBlockPos();
            if (pos.equals(changed)) {
                pipes.add(pipePos);
                continue;
            }

            for (Direction direction : Iterate.horizontalDirections) {
                if (new ChunkPos(pipePos.relative(direction)).equals(changed)) {
                    pipes.add(pipePos);
                    break;
                }
            }
        }
    }
}
