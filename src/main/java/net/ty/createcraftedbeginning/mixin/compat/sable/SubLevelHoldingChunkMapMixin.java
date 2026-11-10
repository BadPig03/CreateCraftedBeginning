package net.ty.createcraftedbeginning.mixin.compat.sable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.ty.createcraftedbeginning.compat.sable.SableLoadingNetworkControl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap", remap = false)
public abstract class SubLevelHoldingChunkMapMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @WrapOperation(method = {"processUnload", "moveToUnloaded", "getOrLoadHoldingChunk"}, at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/sublevel/storage/holding/SubLevelHoldingChunk;acceptHoldingSubLevel(Ldev/ryanhcode/sable/sublevel/storage/HoldingSubLevel;)V"))
    private void ccb$acceptHoldingSubLevel(@Coerce Object chunk, @Coerce Object holding, Operation<Void> original) {
        original.call(chunk, holding);
        SableLoadingNetworkControl.rememberHolding(level, chunk, holding);
    }

    @Inject(method = "moveAndSaveSubLevel(Ldev/ryanhcode/sable/sublevel/ServerSubLevel;Ldev/ryanhcode/sable/sublevel/storage/serialization/SubLevelData;Ldev/ryanhcode/sable/sublevel/storage/holding/GlobalSavedSubLevelPointer;Lnet/minecraft/world/level/ChunkPos;)Ldev/ryanhcode/sable/sublevel/storage/holding/GlobalSavedSubLevelPointer;", at = @At("RETURN"))
    private void ccb$moveAndSaveSubLevel(@Coerce @Nullable Object subLevel, @Coerce Object data, @Coerce @Nullable Object previous, ChunkPos chunk, CallbackInfoReturnable<Object> cir) {
        Object pointer = cir.getReturnValue();
        if (pointer == null) {
            return;
        }

        SableLoadingNetworkControl.rememberSaved(level, data, pointer);
    }
}
