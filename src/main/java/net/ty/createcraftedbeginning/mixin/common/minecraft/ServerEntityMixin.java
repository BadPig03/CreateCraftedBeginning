package net.ty.createcraftedbeginning.mixin.common.minecraft;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.ty.createcraftedbeginning.gas.flow.GasFlowForces;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin {
    @Shadow
    @Final
    private Entity entity;

    @ModifyConstant(method = "sendChanges", constant = @Constant(intValue = 60))
    private int ccb$sendChanges(int interval) {
        if (!GasFlowForces.shouldSyncItemPosition(entity)) {
            return interval;
        }

        return 1;
    }
}
