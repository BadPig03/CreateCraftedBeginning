package net.ty.createcraftedbeginning.mixin.compat.sable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.neoforge.platform.SableSubLevelRenderPlatformImpl", remap = false)
public abstract class SableSubLevelRenderPlatformMixin {
    @SuppressWarnings("MethodMayBeStatic")
    @ModifyExpressionValue(method = "tesselateBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getModelData(Lnet/minecraft/core/BlockPos;)Lnet/neoforged/neoforge/client/model/data/ModelData;"))
    private ModelData ccb$tesselateBlock(ModelData original, @Coerce BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos) {
        return ccb$preparePipeModelData(original, level, model, state, pos);
    }

    @SuppressWarnings("MethodMayBeStatic")
    @ModifyExpressionValue(method = "getRenderLayers", at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/sublevel/render/vanilla/SingleBlockSubLevelWrapper;getModelData(Lnet/minecraft/core/BlockPos;)Lnet/neoforged/neoforge/client/model/data/ModelData;"))
    private ModelData ccb$getRenderLayers(ModelData original, @Coerce BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos) {
        return ccb$preparePipeModelData(original, level, model, state, pos);
    }

    private static ModelData ccb$preparePipeModelData(ModelData original, BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos) {
        if (!state.is(CCBBlocks.AIRTIGHT_PIPE_BLOCK.get()) && !state.is(CCBBlocks.AIRTIGHT_CHECK_VALVE_BLOCK.get()) && !state.is(CCBBlocks.SMART_AIRTIGHT_PIPE_BLOCK.get()) && !state.is(CCBBlocks.AIRTIGHT_PUMP_BLOCK.get()) && !state.is(CCBBlocks.SHAFT_DRIVEN_AIRTIGHT_PUMP_BLOCK.get()) && !state.is(CCBBlocks.AIRTIGHT_REGULATOR_PUMP_BLOCK.get()) && !state.is(CCBBlocks.AIRTIGHT_VALVE_BLOCK.get())) {
            return original;
        }

        return model.getModelData(level, pos, state, original);
    }
}
