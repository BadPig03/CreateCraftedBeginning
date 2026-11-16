package net.ty.createcraftedbeginning.mixin.common.minecraft;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorSectionAccess;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorSectionIndex;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapChanges;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapSectionAccess;
import net.ty.createcraftedbeginning.content.opticalpower.experiencecrystal.ExperienceCrystalSectionAccess;
import net.ty.createcraftedbeginning.content.opticalpower.experiencecrystal.ExperienceCrystalSectionIndex;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserSectionAccess;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserSectionCandidates;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserSectionChanges;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(LevelChunkSection.class)
public abstract class LevelChunkSectionMixin implements ExperienceCrystalSectionAccess, AmethystMirrorSectionAccess, ChunkStabilizerMapSectionAccess, LaserSectionAccess {
    @Unique
    @Nullable
    private LaserSectionCandidates ccb$laserCandidates;
    @Unique
    @Nullable
    private WeakReference<LaserSectionChanges> ccb$laserChanges;
    @Unique
    @Nullable
    private ExperienceCrystalSectionIndex ccb$crystalIndex;
    @Unique
    @Nullable
    private AmethystMirrorSectionIndex ccb$mirrorIndex;
    @Unique
    @Nullable
    private WeakReference<ChunkStabilizerMapChanges> ccb$mapChanges;

    @Override
    public LaserSectionCandidates ccb$getLaserCandidates() {
        LaserSectionCandidates candidates = ccb$laserCandidates;
        if (candidates == null) {
            candidates = new LaserSectionCandidates((LevelChunkSection) (Object) this);
            ccb$laserCandidates = candidates;
        }
        return candidates;
    }

    @Override
    public LaserSectionChanges ccb$getLaserChanges() {
        LaserSectionChanges changes = ccb$laserChanges == null ? null : ccb$laserChanges.get();
        if (changes == null) {
            changes = new LaserSectionChanges();
            ccb$laserChanges = new WeakReference<>(changes);
        }
        return changes;
    }

    @Override
    public ChunkStabilizerMapChanges ccb$getMapChanges() {
        ChunkStabilizerMapChanges changes = ccb$mapChanges == null ? null : ccb$mapChanges.get();
        if (changes == null) {
            changes = new ChunkStabilizerMapChanges();
            ccb$mapChanges = new WeakReference<>(changes);
        }
        return changes;
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public AmethystMirrorSectionIndex ccb$getMirrorIndex() {
        AmethystMirrorSectionIndex index = ccb$mirrorIndex;
        if (index == null) {
            index = new AmethystMirrorSectionIndex((LevelChunkSection) (Object) this);
            ccb$mirrorIndex = index;
        }
        return index;
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public ExperienceCrystalSectionIndex ccb$getCrystalIndex() {
        if (ccb$crystalIndex == null) {
            ccb$crystalIndex = new ExperienceCrystalSectionIndex((LevelChunkSection) (Object) this);
        }

        return ccb$crystalIndex;
    }

    @Inject(method = "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("RETURN"))
    private void ccb$setBlockState(int x, int y, int z, BlockState state, boolean useLocks, CallbackInfoReturnable<BlockState> callback) {
        if (callback.getReturnValue() == state) {
            return;
        }

        LaserSectionCandidates candidates = ccb$laserCandidates;
        if (candidates != null) {
            candidates.changed(x, y, z, state);
        }

        LaserSectionChanges laserChanges = ccb$laserChanges == null ? null : ccb$laserChanges.get();
        if (laserChanges != null) {
            laserChanges.changed(x, y, z);
        }

        if (ccb$crystalIndex != null) {
            ccb$crystalIndex.changed(x, y, z, state);
        }
        ChunkStabilizerMapChanges changes = ccb$mapChanges == null ? null : ccb$mapChanges.get();
        if (changes != null) {
            changes.changed(x, z);
        }
        if (ccb$mirrorIndex == null) {
            return;
        }

        ccb$mirrorIndex.changed(x, y, z, state);
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void ccb$read(FriendlyByteBuf buffer, CallbackInfo callback) {
        ccb$laserChanges = null;
        ccb$laserCandidates = null;
        ccb$crystalIndex = null;
        ccb$mirrorIndex = null;
        ccb$mapChanges = null;
    }

    @Inject(method = "recalcBlockCounts", at = @At("RETURN"))
    private void ccb$recalcBlockCounts(CallbackInfo callback) {
        ccb$laserChanges = null;
        ccb$laserCandidates = null;
        ccb$crystalIndex = null;
        ccb$mirrorIndex = null;
        ccb$mapChanges = null;
    }

    @Inject(method = "readBiomes", at = @At("RETURN"))
    private void ccb$readBiomes(CallbackInfo callback) {
        ccb$mapChanges = null;
    }

    @Inject(method = "fillBiomesFromNoise", at = @At("RETURN"))
    private void ccb$fillBiomesFromNoise(CallbackInfo callback) {
        ccb$mapChanges = null;
    }
}
