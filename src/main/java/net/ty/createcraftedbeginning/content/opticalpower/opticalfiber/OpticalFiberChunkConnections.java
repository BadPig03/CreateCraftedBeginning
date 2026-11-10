package net.ty.createcraftedbeginning.content.opticalpower.opticalfiber;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent.Load;
import net.neoforged.neoforge.event.level.ChunkTicketLevelUpdatedEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import net.ty.createcraftedbeginning.api.CCBAPI;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
final class OpticalFiberChunkConnections {
    private static final int ACCESSIBLE_LEVEL = ChunkLevel.byStatus(ChunkStatus.FULL);
    private static final Predicate<BlockState> IS_FIBER = state -> state.getBlock() instanceof OpticalFiberBlock;
    private static final Map<ServerLevel, Set<ChunkPos>> PENDING = new IdentityHashMap<>();

    private OpticalFiberChunkConnections() {
    }

    @SubscribeEvent
    private static void onChunkLoad(Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        enqueue(level, event.getChunk().getPos());
    }

    @SubscribeEvent
    private static void onChunkTicketLevelUpdated(ChunkTicketLevelUpdatedEvent event) {
        if (event.getOldTicketLevel() <= ACCESSIBLE_LEVEL || event.getNewTicketLevel() > ACCESSIBLE_LEVEL) {
            return;
        }

        enqueue(event.getLevel(), new ChunkPos(event.getChunkPos()));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    private static void onLevelTick(Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Set<ChunkPos> pending = PENDING.remove(level);
        if (pending == null) {
            return;
        }

        for (ChunkPos pos : pending) {
            LevelChunk chunk = findAccessibleChunk(level, pos);
            if (chunk == null) {
                continue;
            }

            for (Direction direction : Iterate.horizontalDirections) {
                ChunkPos neighborPos = new ChunkPos(pos.x + direction.getStepX(), pos.z + direction.getStepZ());
                if (direction.getAxisDirection() == AxisDirection.NEGATIVE && pending.contains(neighborPos)) {
                    continue;
                }

                LevelChunk neighbor = findAccessibleChunk(level, neighborPos);
                if (neighbor == null) {
                    continue;
                }

                refreshEdge(level, chunk, neighbor, direction);
                refreshEdge(level, neighbor, chunk, direction.getOpposite());
            }
        }
    }

    @SubscribeEvent
    private static void onLevelUnload(Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        PENDING.remove(level);
    }

    private static void enqueue(ServerLevel level, ChunkPos pos) {
        PENDING.computeIfAbsent(level, ignored -> new HashSet<>()).add(pos);
    }

    private static @Nullable LevelChunk findAccessibleChunk(ServerLevel level, ChunkPos pos) {
        BlockPos anchor = new BlockPos(pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ());
        if (!level.isLoaded(anchor)) {
            return null;
        }

        return level.getChunkSource().getChunkNow(pos.x, pos.z);
    }

    private static void refreshEdge(ServerLevel level, LevelChunk chunk, LevelChunk neighbor, Direction direction) {
        boolean xEdge = direction.getAxis() == Axis.X;
        int edge = direction.getAxisDirection() == AxisDirection.POSITIVE ? SectionPos.SECTION_MAX_INDEX : 0;
        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        LevelChunkSection[] sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection section = sections[index];
            if (!section.maybeHas(IS_FIBER)) {
                continue;
            }

            int minY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
            for (int offset = 0; offset < SectionPos.SECTION_SIZE; offset++) {
                int x = xEdge ? edge : offset;
                int z = xEdge ? offset : edge;
                for (int y = 0; y < SectionPos.SECTION_SIZE; y++) {
                    BlockState state = section.getBlockState(x, y, z);
                    if (!IS_FIBER.test(state)) {
                        continue;
                    }

                    BlockPos pos = new BlockPos(minX + x, minY + y, minZ + z);
                    BlockPos neighborPos = pos.relative(direction);
                    BlockState updated = state.updateShape(direction, neighbor.getBlockState(neighborPos), level, pos, neighborPos);
                    if (updated == state) {
                        continue;
                    }

                    level.setBlock(pos, updated, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }
        }
    }
}
