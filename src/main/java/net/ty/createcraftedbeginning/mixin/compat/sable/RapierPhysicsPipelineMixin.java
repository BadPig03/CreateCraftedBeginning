package net.ty.createcraftedbeginning.mixin.compat.sable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.ty.createcraftedbeginning.compat.sable.AmethystMirrorCollisionWorld;
import net.ty.createcraftedbeginning.compat.sable.AmethystMirrorRapierAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline", remap = false)
public abstract class RapierPhysicsPipelineMixin {
    @Shadow
    @Final
    private ServerLevel level;
    @Unique
    private @Nullable AmethystMirrorCollisionWorld ccb$mirrors;

    @Shadow
    public abstract void handleBlockChange(SectionPos sectionPos, LevelChunkSection chunk, int x, int y, int z, BlockState oldState, BlockState newState);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ccb$init(ServerLevel level, CallbackInfo callback) {
        ccb$mirrors = new AmethystMirrorCollisionWorld(level);
    }

    @Inject(method = "prePhysicsTicks", at = @At("HEAD"))
    private void ccb$prePhysicsTicks(CallbackInfo callback) {
        AmethystMirrorCollisionWorld mirrors = ccb$mirrors;
        if (mirrors == null) {
            return;
        }

        Set<BlockPos> affected = mirrors.refresh();
        if (affected.isEmpty()) {
            return;
        }

        Set<SubLevelAccess> bodies = new HashSet<>();
        for (BlockPos pos : affected) {
            if (!level.isLoaded(pos)) {
                continue;
            }

            SubLevelAccess body = SableCompanion.INSTANCE.getContaining(level, pos);
            if (body != null) {
                bodies.add(body);
            }
            int y = pos.getY();
            LevelChunk chunk = level.getChunkAt(pos);
            LevelChunkSection section = chunk.getSection(level.getSectionIndex(y));
            BlockState state = level.getBlockState(pos);
            handleBlockChange(SectionPos.of(pos), section, pos.getX() & 15, y & 15, pos.getZ() & 15, state, state);
        }
        for (SubLevelAccess body : bodies) {
            AmethystMirrorRapierAccess.refreshBody(this, body);
        }
    }

    @Inject(method = "dispose", at = @At("HEAD"))
    private void ccb$dispose(CallbackInfo callback) {
        AmethystMirrorCollisionWorld mirrors = ccb$mirrors;
        if (mirrors == null) {
            return;
        }

        mirrors.close();
        ccb$mirrors = null;
    }

    @WrapOperation(method = "onStatsChanged", at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/physics/impl/rapier/Rapier3D;setLocalBounds(JIIIIIII)V"))
    private void ccb$onStatsChanged(long scene, int body, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Operation<Void> original, @Coerce Object subLevel) {
        int[] bounds = {minX, minY, minZ, maxX, maxY, maxZ};
        AmethystMirrorCollisionWorld mirrors = ccb$mirrors;
        if (mirrors != null) {
            mirrors.expandBounds((SubLevelAccess) subLevel, bounds);
        }
        original.call(scene, body, bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
    }

    @WrapOperation(method = "handleBlockChange", at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/physics/impl/rapier/Rapier3D;changeBlock(JIIII)V"))
    private void ccb$handleBlockChange(long scene, int x, int y, int z, int packed, Operation<Void> original) {
        AmethystMirrorCollisionWorld mirrors = ccb$mirrors;
        if (mirrors != null) {
            packed = mirrors.pack(new BlockPos(x, y, z), packed);
        }
        original.call(scene, x, y, z, packed);
    }

    @WrapOperation(method = "handleChunkSectionAddition", at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/physics/impl/rapier/Rapier3D;addChunk(JIII[IZI)V"))
    private void ccb$handleChunkSectionAddition(long scene, int x, int y, int z, int[] data, boolean global, int body, Operation<Void> original) {
        AmethystMirrorCollisionWorld mirrors = ccb$mirrors;
        if (mirrors != null) {
            mirrors.packSection(x, y, z, data);
        }
        original.call(scene, x, y, z, data, global, body);
    }
}
