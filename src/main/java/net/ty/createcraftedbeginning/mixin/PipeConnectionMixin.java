package net.ty.createcraftedbeginning.mixin;

import com.simibubi.create.content.fluids.PipeConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.advancement.AdvancementBehaviour;
import net.ty.createcraftedbeginning.advancement.CCBAdvancements;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtightpipe.AirtightPipeBlockEntity;
import net.ty.createcraftedbeginning.content.airtightpump.AirtightPumpBlockEntity;
import net.ty.createcraftedbeginning.data.CCBTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(PipeConnection.class)
public abstract class PipeConnectionMixin {
    @Unique
    private static final float EXPLOSION_POWER = 4.0f;

    @SuppressWarnings("all")
    @Inject(method = "manageFlows", at = @At("HEAD"), cancellable = true)
    private void checkForExplosion(Level level, BlockPos pos, FluidStack internalFluid, Predicate<FluidStack> extractionPredicate, CallbackInfoReturnable<Boolean> ci) {
        PipeConnection pipe = (PipeConnection) (Object) this;

        if (!isValidBlockEntity(level, pos)) {
            return;
        }

        if (!isOverloaded(Mth.abs(pipe.comparePressure()))) {
            return;
        }

        if (!internalFluid.is(CCBTags.commonFluidTag("compressed_air"))) {
            return;
        }

        if (!level.isClientSide) {
            BlockPos pipePos = pos.relative(pipe.side);
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, EXPLOSION_POWER, Level.ExplosionInteraction.NONE);
            level.destroyBlock(pos, true);

            AdvancementBehaviour.tryAwardToNearbyPlayersWithLooking(level, pos, CCBAdvancements.COMPRESSED_AIR_EXPLOSION, 64, 15);
        }

        pipe.wipePressure();
        ci.setReturnValue(true);
    }

    @Unique
    private boolean isValidBlockEntity(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return !(be instanceof AirtightPipeBlockEntity || be instanceof AirtightPumpBlockEntity);
    }

    @Unique
    private boolean isOverloaded(float pressure) {
        return pressure > CCBConfig.server().safeRotationSpeed.get();
    }
}
