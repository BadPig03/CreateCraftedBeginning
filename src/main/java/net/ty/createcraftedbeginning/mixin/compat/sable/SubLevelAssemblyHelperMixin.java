package net.ty.createcraftedbeginning.mixin.compat.sable;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.ty.createcraftedbeginning.compat.sable.AssemblyTransformAccess;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.api.SubLevelAssemblyHelper", remap = false)
public abstract class SubLevelAssemblyHelperMixin {
    @Inject(method = "moveBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;loadWithComponents(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V"), require = 1)
    private static void ccb$moveBlocks(ServerLevel level, @Coerce Object transform, Iterable<BlockPos> positions, CallbackInfo callback, @Local CompoundTag tag) {
        String type = tag.getString("id");
        if (CCBBlockEntities.AIRTIGHT_FRACTIONATION_TOWER.getId().toString().equals(type)) {
            AirtightFractionationTowerBlockEntity.transformStructureNbt(tag, ((AssemblyTransformAccess) transform)::ccb$transform);
            return;
        }

        if (!CCBBlockEntities.AIR_VENT.getId().toString().equals(type)) {
            return;
        }

        AirVentBlockEntity.transformLouverNbt(tag, ((AssemblyTransformAccess) transform)::ccb$transform);
    }
}
