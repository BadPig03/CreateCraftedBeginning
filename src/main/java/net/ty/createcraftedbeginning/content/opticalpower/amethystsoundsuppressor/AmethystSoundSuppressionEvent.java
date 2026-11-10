package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public final class AmethystSoundSuppressionEvent extends Event {
    private final ServerLevel level;
    private final AmethystSoundSuppressorIdentity suppressorIdentity;
    private final BlockPos effectCenter;
    private final Holder<GameEvent> gameEvent;
    private final Vec3 sourcePosition;
    private final Context context;

    AmethystSoundSuppressionEvent(ServerLevel level, AmethystSoundSuppressorInstance suppressor, Holder<GameEvent> gameEvent, Vec3 sourcePosition, Context context) {
        this.level = level;
        suppressorIdentity = suppressor.identity();
        effectCenter = suppressor.effectCenter();
        this.gameEvent = gameEvent;
        this.sourcePosition = sourcePosition;
        this.context = context;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public AmethystSoundSuppressorIdentity getSuppressorIdentity() {
        return suppressorIdentity;
    }

    public BlockPos getEffectCenter() {
        return effectCenter;
    }

    public Holder<GameEvent> getGameEvent() {
        return gameEvent;
    }

    public Vec3 getSourcePosition() {
        return sourcePosition;
    }

    public Context getContext() {
        return context;
    }
}
