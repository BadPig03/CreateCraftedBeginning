package net.ty.createcraftedbeginning.mixin;

import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ty.createcraftedbeginning.advancement.AdvancementBehaviour;
import net.ty.createcraftedbeginning.advancement.CCBAdvancements;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.data.CCBTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

@Mixin(FluidTankBlockEntity.class)
public abstract class FluidTankBlockEntityMixin {
    @Unique
    private static final Map<ResourceLocation, Queue<DelayedExplosion>> DIMENSION_EXPLOSION_QUEUES = new HashMap<>();

    @Shadow
    public abstract boolean isController();

    @SuppressWarnings("all")
    @Inject(method = "onFluidStackChanged", at = @At("TAIL"), remap = false)
    private void onFluidChanged(FluidStack newFluidStack, CallbackInfo ci) {
        FluidTankBlockEntity tank = (FluidTankBlockEntity) (Object) this;

        Level level = tank.getLevel();

        float explosionPower = getExplosionPower();

        if (level == null || level.isClientSide() || explosionPower == 0 || !isController() || isCreativeTank(level, tank) || !isCompressedAir(newFluidStack) || !isOverloaded(newFluidStack)) {
            return;
        }

        tank.getTankInventory().drain(tank.getTankInventory().getFluidAmount(), IFluidHandler.FluidAction.EXECUTE);
        BlockPos centerPos = tank.getBlockPos();
        if (level instanceof ServerLevel serverLevel) {
            Queue<DelayedExplosion> queue = getDimensionQueue(serverLevel);
            queue.add(new DelayedExplosion(centerPos, explosionPower));
        }
    }

    @SuppressWarnings("all")
    @Inject(method = "tick", at = @At("TAIL"))
    private void onServerTick(CallbackInfo ci) {
        FluidTankBlockEntity tank = (FluidTankBlockEntity) (Object) this;
        Level level = tank.getLevel();

        if (!(level instanceof ServerLevel) || level.isClientSide()) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        Queue<DelayedExplosion> queue = getDimensionQueue(serverLevel);
        processExplosions(serverLevel, queue);
    }

    @Unique
    private Queue<DelayedExplosion> getDimensionQueue(ServerLevel level) {
        return DIMENSION_EXPLOSION_QUEUES.computeIfAbsent(level.dimension().location(), k -> new ArrayDeque<>());
    }

    @Unique
    private void processExplosions(ServerLevel level, Queue<DelayedExplosion> queue) {
        int processed = 0;
        while (!queue.isEmpty() && processed < 3) {
            DelayedExplosion task = queue.poll();
            BlockPos pos = task.pos();

            if (level.isLoaded(pos) && level.getWorldBorder().isWithinBounds(pos)) {
                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, task.power(), Level.ExplosionInteraction.NONE);
                level.destroyBlock(pos, true);

                AdvancementBehaviour.tryAwardToNearbyPlayersWithLooking(level, pos, CCBAdvancements.COMPRESSED_AIR_EXPLOSION, 64, 15);
            }
            processed++;
        }

        if (queue.isEmpty()) {
            DIMENSION_EXPLOSION_QUEUES.remove(level.dimension().location());
        }
    }

    @Unique
    private boolean isCompressedAir(FluidStack fluidStack) {
        return fluidStack.is(CCBTags.commonFluidTag("compressed_air"));
    }

    @Unique
    private boolean isOverloaded(FluidStack fluidStack) {
        return fluidStack.getAmount() > CCBConfig.server().compressedAir.safeAirAmount.get() * 1000;
    }

    @Unique
    private boolean isCreativeTank(Level level, FluidTankBlockEntity tank) {
        IFluidHandler tankCapability = level.getCapability(Capabilities.FluidHandler.BLOCK, tank.getBlockPos(), null);
        if (tankCapability == null) {
            return false;
        }
        return tankCapability instanceof CreativeFluidTankBlockEntity.CreativeSmartFluidTank;
    }

    @Unique
    private float getExplosionPower() {
        return CCBConfig.server().compressedAir.explosionPower.getF();
    }

    @Unique
    private record DelayedExplosion(BlockPos pos, float power) {
    }
}