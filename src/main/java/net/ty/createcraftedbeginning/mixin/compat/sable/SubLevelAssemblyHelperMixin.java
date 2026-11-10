package net.ty.createcraftedbeginning.mixin.compat.sable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.ty.createcraftedbeginning.compat.sable.AssemblyTransformAccess;
import net.ty.createcraftedbeginning.compat.sable.OpticalFiberAssemblyCompat;
import net.ty.createcraftedbeginning.compat.sable.SableLoadingNetwork;
import net.ty.createcraftedbeginning.compat.sable.SableLoadingNetwork.Move;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorSerialization;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.api.SubLevelAssemblyHelper", remap = false)
public abstract class SubLevelAssemblyHelperMixin {
    private static final String COMPOUND_KEY_ID = "id";

    @WrapMethod(method = "moveBlocks")
    private static void ccb$moveBlocks$network(ServerLevel level, @Coerce Object transform, Iterable<BlockPos> positions, Operation<Void> original) {
        AssemblyTransformAccess access = (AssemblyTransformAccess) transform;
        List<Move> moves = SableLoadingNetwork.prepareMove(level, access, positions);
        try {
            original.call(level, transform, positions);
        }
        finally {
            SableLoadingNetwork.finishMove(level, access.ccb$getResultingLevel(), moves);
        }
    }

    @Inject(method = "moveBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;loadWithComponents(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V"), require = 1)
    private static void ccb$moveBlocks$loadWithComponents(ServerLevel level, @Coerce Object transform, Iterable<BlockPos> positions, CallbackInfo callback, @Local CompoundTag compoundTag) {
        String type = compoundTag.getString(COMPOUND_KEY_ID);
        if (CCBBlockEntities.AMETHYST_MIRROR.getId().toString().equals(type)) {
            AmethystMirrorSerialization.transformMirrorNbt(compoundTag, ((AssemblyTransformAccess) transform)::ccb$transform);
            return;
        }

        if (CCBBlockEntities.AIRTIGHT_FRACTIONATION_TOWER.getId().toString().equals(type)) {
            AirtightFractionationTowerBlockEntity.transformStructureNbt(compoundTag, ((AssemblyTransformAccess) transform)::ccb$transform);
            return;
        }

        if (!CCBBlockEntities.AIR_VENT.getId().toString().equals(type)) {
            return;
        }

        AirVentBlockEntity.transformLouverNbt(compoundTag, ((AssemblyTransformAccess) transform)::ccb$transform);
    }

    @Inject(method = "moveBlocks", at = @At("TAIL"), require = 1)
    private static void ccb$moveBlocks$tail(ServerLevel level, @Coerce Object transform, Iterable<BlockPos> positions, CallbackInfo callback) {
        AssemblyTransformAccess access = (AssemblyTransformAccess) transform;
        OpticalFiberAssemblyCompat.refreshAfterMove(level, access.ccb$getResultingLevel(), positions, access::ccb$transform);
    }
}
