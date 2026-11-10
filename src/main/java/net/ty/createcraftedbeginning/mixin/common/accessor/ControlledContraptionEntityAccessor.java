package net.ty.createcraftedbeginning.mixin.common.accessor;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(ControlledContraptionEntity.class)
public interface ControlledContraptionEntityAccessor {
    @Accessor("controllerPos")
    @Nullable BlockPos ccb$getControllerPos();
}
