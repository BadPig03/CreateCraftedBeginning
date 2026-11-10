package net.ty.createcraftedbeginning.mixin.compat.sable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.api.sublevel.SubLevelContainer", remap = false)
public abstract class SubLevelContainerMixin {
    @Shadow
    public abstract Level getLevel();

    @WrapOperation(method = "removeSubLevel(IILdev/ryanhcode/sable/sublevel/storage/SubLevelRemovalReason;)V", at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/sublevel/SubLevel;onRemove()V"))
    private void ccb$removeSubLevel(@Coerce Object subLevel, Operation<Void> original, int x, int z, @Coerce Object reason) {
        original.call(subLevel);
        if (!(getLevel() instanceof ServerLevel level) || !((Enum<?>) reason).name().equals("REMOVED")) {
            return;
        }

        LoadingNetworkSavedData.get(level).removeSubLevel(level.dimension(), ((SubLevelAccess) subLevel).getUniqueId());
    }
}
