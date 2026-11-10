package net.ty.createcraftedbeginning.mixin.compat.sable;

import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;", at = @At("HEAD"), cancellable = true)
    private void ccb$getChunk(int x, int z, ChunkStatus status, boolean requireChunk, CallbackInfoReturnable<ChunkAccess> callback) {
        if (SableCompanion.INSTANCE.getContaining(level, new BlockPos(x << 4, 0, z << 4)) == null) {
            return;
        }

        LevelChunk chunk = ((ServerChunkCache) (Object) this).getChunkNow(x, z);
        if (chunk == null) {
            return;
        }

        callback.setReturnValue(chunk);
    }
}
