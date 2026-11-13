package net.ty.createcraftedbeginning.content.opticalpower.laser.exposure;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalTarget;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OpticalExposureLedger {
    private static final Map<ServerLevel, OpticalExposureLedger> LEVELS = new HashMap<>();

    private final Map<BlockPos, SourceBatch> current = new TreeMap<>();
    private Map<OpticalTarget, List<CapturedExposure>> previous = Map.of();
    private long tick = Long.MIN_VALUE;

    private OpticalExposureLedger() {
    }

    public static void submit(BlockEntity source, BeamPropagation propagation) {
        if (!(source.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos sourcePos = source.getBlockPos();
        long gameTime = level.getGameTime();
        OpticalExposureLedger ledger = LEVELS.computeIfAbsent(level, ignored -> new OpticalExposureLedger());
        ledger.advance(gameTime);
        if (source.isRemoved() || !level.isLoaded(sourcePos) || level.getBlockEntity(sourcePos) != source) {
            withdraw(source);
            return;
        }

        List<CapturedExposure> captured = new ArrayList<>();
        ResourceKey<Level> dimension = level.dimension();
        for (OpticalExposure exposure : OpticalExposure.fromPropagation(level, gameTime, sourcePos, propagation)) {
            OpticalTarget target = exposure.hit().target();
            ResourceKey<Level> targetDimension = target.dimension();
            if (targetDimension != dimension) {
                throw new IllegalArgumentException("Exposure dimension must match source level " + dimension + "; got " + targetDimension + '.');
            }

            double receivedFluxLm = exposure.receivedFluxLm();
            if (!Double.isFinite(receivedFluxLm) || receivedFluxLm < 0 || receivedFluxLm > exposure.absorbedFluxLm()) {
                throw new IllegalArgumentException("Received exposure flux must be finite and in [0, " + exposure.absorbedFluxLm() + "] lm; got " + receivedFluxLm + " lm.");
            }

            BlockEntity recipient = null;
            BlockPos pos = target.blockPos();
            if (target.contraptionId() == null && level.isLoaded(pos)) {
                recipient = level.getBlockEntity(pos);
            }
            captured.add(new CapturedExposure(source, recipient, exposure));
        }
        ledger.current.put(sourcePos.immutable(), new SourceBatch(source, List.copyOf(captured)));
    }

    public static void withdraw(BlockEntity source) {
        if (!(source.getLevel() instanceof ServerLevel level)) {
            return;
        }

        OpticalExposureLedger ledger = LEVELS.get(level);
        if (ledger == null) {
            return;
        }

        ledger.advance(level.getGameTime());
        BlockPos pos = source.getBlockPos();
        SourceBatch batch = ledger.current.get(pos);
        if (batch == null || batch.source() != source) {
            return;
        }

        ledger.current.remove(pos);
    }

    public static List<OpticalExposure> getPrevious(BlockEntity recipient) {
        if (!(recipient.getLevel() instanceof ServerLevel level) || recipient.isRemoved()) {
            return List.of();
        }

        OpticalExposureLedger ledger = LEVELS.get(level);
        if (ledger == null) {
            return List.of();
        }

        ledger.advance(level.getGameTime());
        BlockPos pos = recipient.getBlockPos();
        if (!level.isLoaded(pos) || level.getBlockEntity(pos) != recipient) {
            return List.of();
        }

        OpticalTarget target = new OpticalTarget(level.dimension(), pos, null, SubLevelBridge.resolve(level, pos).inSubLevel());
        List<OpticalExposure> exposures = new ArrayList<>();
        for (CapturedExposure captured : ledger.previous.getOrDefault(target, List.of())) {
            BlockEntity source = captured.source();
            OpticalExposure exposure = captured.exposure();
            BlockPos sourcePos = exposure.sourcePos();
            if (captured.recipient() != recipient || source.isRemoved() || !level.isLoaded(sourcePos) || level.getBlockEntity(sourcePos) != source) {
                continue;
            }

            exposures.add(exposure);
        }
        return List.copyOf(exposures);
    }

    static void tick(ServerLevel level) {
        OpticalExposureLedger ledger = LEVELS.get(level);
        if (ledger == null) {
            return;
        }

        ledger.advance(level.getGameTime());
    }

    static void removeLevel(ServerLevel level) {
        LEVELS.remove(level);
    }

    static void clear() {
        LEVELS.clear();
    }

    private void advance(long gameTime) {
        if (tick == gameTime) {
            return;
        }

        Map<OpticalTarget, List<CapturedExposure>> completed = new HashMap<>();
        if (tick == gameTime - 1) {
            for (SourceBatch batch : current.values()) {
                for (CapturedExposure captured : batch.exposures()) {
                    OpticalTarget target = captured.exposure().hit().target();
                    completed.computeIfAbsent(target, ignored -> new ArrayList<>()).add(captured);
                }
            }
        }
        completed.replaceAll((target, exposures) -> List.copyOf(exposures));
        previous = Map.copyOf(completed);
        current.clear();
        tick = gameTime;
    }

    private record CapturedExposure(BlockEntity source, @Nullable BlockEntity recipient, OpticalExposure exposure) {
    }

    private record SourceBatch(BlockEntity source, List<CapturedExposure> exposures) {
    }
}
