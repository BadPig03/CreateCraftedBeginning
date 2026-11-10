package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.GameEventTags;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public final class AmethystSoundSuppressorGameEvents {
    private static final ThreadLocal<Dispatch> CURRENT_DISPATCH = new ThreadLocal<>();

    private AmethystSoundSuppressorGameEvents() {
    }

    public static @Nullable Dispatch beginDispatch(ServerLevel level, Holder<GameEvent> gameEvent, Vec3 sourcePosition, Context context) {
        if (!gameEvent.is(GameEventTags.VIBRATIONS) || !AmethystSoundSuppressorManager.hasCoverage(level.dimension().location())) {
            return null;
        }

        Dispatch dispatch = new Dispatch(level, gameEvent, sourcePosition, context, CURRENT_DISPATCH.get());
        CURRENT_DISPATCH.set(dispatch);
        return dispatch;
    }

    public static boolean suppressRecipient(ServerLevel level, Holder<GameEvent> gameEvent, GameEventListener listener) {
        Dispatch dispatch = CURRENT_DISPATCH.get();
        if (dispatch == null || dispatch.level != level || dispatch.gameEvent != gameEvent) {
            return false;
        }

        Optional<Vec3> position = listener.getListenerSource().getPosition(dispatch.level);
        return position.filter(dispatch::suppressAt).isPresent();
    }

    public static final class Dispatch implements AutoCloseable {
        private final ServerLevel level;
        private final Holder<GameEvent> gameEvent;
        private final Vec3 sourcePosition;
        private final Context context;
        private final @Nullable Dispatch previous;
        private boolean notified;
        private boolean closed;

        private Dispatch(ServerLevel level, Holder<GameEvent> gameEvent, Vec3 sourcePosition, Context context, @Nullable Dispatch previous) {
            this.level = level;
            this.gameEvent = gameEvent;
            this.sourcePosition = sourcePosition;
            this.context = context;
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }

            closed = true;
            if (previous == null) {
                CURRENT_DISPATCH.remove();
                return;
            }

            CURRENT_DISPATCH.set(previous);
        }

        public boolean suppressAt(Position position) {
            BlockPos worldPos = SubLevelBridge.resolve(level, position).blockPos();
            AmethystSoundSuppressorInstance suppressor = AmethystSoundSuppressorManager.findCoveringSuppressor(worldPos, level.dimension().location());
            if (suppressor == null) {
                return false;
            }

            if (notified) {
                return true;
            }

            notified = true;
            NeoForge.EVENT_BUS.post(new AmethystSoundSuppressionEvent(level, suppressor, gameEvent, sourcePosition, context));
            return true;
        }
    }
}
