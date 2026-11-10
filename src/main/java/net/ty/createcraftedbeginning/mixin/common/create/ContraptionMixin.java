package net.ty.createcraftedbeginning.mixin.common.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.contraptions.StructureTransform;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.crates.FilteredCrateMountedItemStorage;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkContraptions;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.Node;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(Contraption.class)
public abstract class ContraptionMixin {
    @Shadow
    protected MountedStorageManager storage;

    @Inject(method = "readNBT", at = @At("RETURN"))
    private void ccb$readNBT(Level world, CompoundTag nbt, boolean spawnData, CallbackInfo callback) {
        FilteredCrateMountedItemStorage.bindLevel(storage, world);
    }

    @Inject(method = "removeBlocksFromWorld", at = @At("HEAD"))
    @SuppressWarnings("DataFlowIssue")
    private void ccb$removeBlocksFromWorld(Level world, BlockPos offset, CallbackInfo ci) {
        if (!(world instanceof ServerLevel level)) {
            return;
        }

        LoadingNetworkContraptions.assemble(level, (Contraption) (Object) this, offset);
    }

    @WrapMethod(method = "addBlocksToWorld")
    @SuppressWarnings("DataFlowIssue")
    private void ccb$addBlocksToWorld(Level world, StructureTransform transform, Operation<Void> original) {
        if (!(world instanceof ServerLevel level)) {
            original.call(world, transform);
            return;
        }

        Map<GlobalPos, Node> placements = LoadingNetworkContraptions.preparePlacement(level, (Contraption) (Object) this, transform);
        try {
            original.call(world, transform);
        }
        finally {
            LoadingNetworkContraptions.finishPlacement(level, placements);
        }
    }
}
