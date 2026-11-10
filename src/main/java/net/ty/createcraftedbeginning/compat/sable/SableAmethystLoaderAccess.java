package net.ty.createcraftedbeginning.compat.sable;

import com.mojang.serialization.Codec;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.ty.createcraftedbeginning.api.CCBAPI;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class SableAmethystLoaderAccess {
    private static final MethodHandle CONTAINER;
    private static final MethodHandle ALL_TICKETS;
    private static final MethodHandle SUB_LEVEL;
    private static final MethodHandle TICKETS;
    private static final MethodHandle TYPE;
    private static final MethodHandle KEY;
    private static final MethodHandle ADD;
    private static final MethodHandle REMOVE;
    private static final MethodHandle HOLDING_MAP;
    private static final MethodHandle RECOVER;
    private static final MethodHandle POINTER;
    private static final MethodHandle HOLDING_POS;
    private static final MethodHandle HOLDING_DATA;
    private static final MethodHandle DATA_ID;
    private static final MethodHandle POINTER_POS;
    private static final Object LOADER_TYPE;
    private static final Object RECOVERY_TYPE;

    static {
        try {
            Class<?> containers = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
            Class<?> serverContainer = Class.forName("dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer");
            Class<?> subLevel = Class.forName("dev.ryanhcode.sable.sublevel.ServerSubLevel");
            Class<?> ticketType = Class.forName("dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType");
            Class<?> ticket = Class.forName("dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicket");
            Class<?> baseSubLevel = Class.forName("dev.ryanhcode.sable.sublevel.SubLevel");
            Class<?> info = Class.forName("dev.ryanhcode.sable.api.sublevel.ticket.SubLevelTicketInfo");
            Class<?> holdingMap = Class.forName("dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap");
            Class<?> pointer = Class.forName("dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer");
            Class<?> holdingChunk = Class.forName("dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunk");
            Class<?> holding = Class.forName("dev.ryanhcode.sable.sublevel.storage.HoldingSubLevel");
            Class<?> data = Class.forName("dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData");
            Lookup lookup = MethodHandles.publicLookup();
            CONTAINER = lookup.findStatic(containers, "getContainer", MethodType.methodType(serverContainer, ServerLevel.class));
            ALL_TICKETS = lookup.findVirtual(serverContainer, "getAllTickets", MethodType.methodType(Map.class));
            SUB_LEVEL = lookup.findVirtual(containers, "getSubLevel", MethodType.methodType(baseSubLevel, UUID.class));
            TICKETS = lookup.findVirtual(info, "tickets", MethodType.methodType(ObjectSet.class));
            TYPE = lookup.findVirtual(ticket, "getType", MethodType.methodType(ticketType));
            KEY = lookup.findVirtual(ticket, "getKey", MethodType.methodType(Object.class));
            ADD = lookup.findVirtual(serverContainer, "addForceLoadTicket", MethodType.methodType(boolean.class, subLevel, ticketType, Object.class));
            REMOVE = lookup.findVirtual(serverContainer, "removeForceLoadTicket", MethodType.methodType(boolean.class, subLevel, ticketType, Object.class));
            HOLDING_MAP = lookup.findVirtual(serverContainer, "getHoldingChunkMap", MethodType.methodType(holdingMap));
            RECOVER = lookup.findVirtual(holdingMap, "snatchAndLoad", MethodType.methodType(void.class, pointer, UUID.class));
            POINTER = lookup.findConstructor(pointer, MethodType.methodType(void.class, ChunkPos.class, short.class, short.class));
            HOLDING_POS = lookup.findVirtual(holdingChunk, "getChunkPos", MethodType.methodType(ChunkPos.class));
            HOLDING_DATA = lookup.findVirtual(holding, "data", MethodType.methodType(data));
            DATA_ID = lookup.findVirtual(data, "uuid", MethodType.methodType(UUID.class));
            POINTER_POS = lookup.findVirtual(pointer, "chunkPos", MethodType.methodType(ChunkPos.class));
            RECOVERY_TYPE = lookup.findStatic(ticketType, "create", MethodType.methodType(ticketType, ResourceLocation.class, Codec.class)).invoke(CCBAPI.asResource("amethyst_loader_recovery"), BlockPos.CODEC);
            LOADER_TYPE = lookup.findStatic(ticketType, "create", MethodType.methodType(ticketType, ResourceLocation.class, Codec.class)).invoke(CCBAPI.asResource("amethyst_loader"), BlockPos.CODEC);
        }
        catch (Throwable exception) {
            throw new IllegalStateException("Failed to bind Sable Amethyst Loader tickets.", exception);
        }
    }

    private SableAmethystLoaderAccess() {
    }

    static void initialize() {
    }

    static @Nullable Object container(ServerLevel level) {
        return invoke(CONTAINER, level);
    }

    @SuppressWarnings("unchecked")
    static Map<UUID, Object> tickets(Object container) {
        return Objects.requireNonNull((Map<UUID, Object>) invoke(ALL_TICKETS, container), "Sable sub-level container ticket map must not be null.");
    }

    static @Nullable SubLevelAccess subLevel(Object container, UUID id) {
        return (SubLevelAccess) invoke(SUB_LEVEL, container, id);
    }

    static List<BlockPos> loaders(Object info) {
        List<BlockPos> positions = new ArrayList<>();
        ObjectSet<?> tickets = Objects.requireNonNull((ObjectSet<?>) invoke(TICKETS, info), "Sable sub-level ticket set must not be null when reading Amethyst Loader positions.");
        for (Object ticket : tickets) {
            if (invoke(TYPE, ticket) != LOADER_TYPE) {
                continue;
            }

            positions.add((BlockPos) invoke(KEY, ticket));
        }
        return positions;
    }

    static List<BlockPos> recoveries(Object info) {
        List<BlockPos> positions = new ArrayList<>();
        ObjectSet<?> tickets = Objects.requireNonNull((ObjectSet<?>) invoke(TICKETS, info), "Sable sub-level ticket set must not be null when reading Amethyst Loader recovery positions.");
        for (Object ticket : tickets) {
            if (invoke(TYPE, ticket) != RECOVERY_TYPE) {
                continue;
            }

            positions.add((BlockPos) invoke(KEY, ticket));
        }
        return positions;
    }

    static void setRecovery(Object container, SubLevelAccess subLevel, BlockPos pos, boolean forced) {
        invoke(forced ? ADD : REMOVE, container, subLevel, RECOVERY_TYPE, pos);
    }

    static void recover(Object container, UUID host, ChunkPos chunk) {
        Object pointer = invoke(POINTER, chunk, (short) 0, (short) 0);
        invoke(RECOVER, invoke(HOLDING_MAP, container), pointer, host);
    }

    static UUID holdingId(Object holding) {
        return Objects.requireNonNull((UUID) invoke(DATA_ID, invoke(HOLDING_DATA, holding)), "Sable held sub-level UUID must not be null.");
    }

    static UUID savedId(Object data) {
        return Objects.requireNonNull((UUID) invoke(DATA_ID, data), "Sable saved sub-level UUID must not be null.");
    }

    static ChunkPos holdingPos(Object chunk) {
        return Objects.requireNonNull((ChunkPos) invoke(HOLDING_POS, chunk), "Sable holding chunk position must not be null.");
    }

    static ChunkPos pointerPos(Object pointer) {
        return Objects.requireNonNull((ChunkPos) invoke(POINTER_POS, pointer), "Sable saved sub-level pointer must have a chunk position.");
    }

    static void setForced(Object container, SubLevelAccess subLevel, BlockPos pos, boolean forced) {
        invoke(forced ? ADD : REMOVE, container, subLevel, LOADER_TYPE, pos);
    }

    private static @Nullable Object invoke(MethodHandle method, Object... arguments) {
        try {
            return method.invokeWithArguments(arguments);
        }
        catch (Throwable exception) {
            throw new IllegalStateException("Failed to invoke Sable loader API " + method + '.', exception);
        }
    }
}
