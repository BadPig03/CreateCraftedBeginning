package net.ty.createcraftedbeginning.mixin.compat.functionalstorage;

import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.buuz135.functionalstorage.util.ConnectedDrawers;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.compat.functionalstorage.ControllerGasHandler;
import net.ty.createcraftedbeginning.compat.functionalstorage.GasDrawerBlockEntity;
import net.ty.createcraftedbeginning.compat.functionalstorage.access.GasConnectedDrawersAccess;
import net.ty.createcraftedbeginning.compat.functionalstorage.access.GasControllerAccess;
import org.jetbrains.annotations.Unmodifiable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = ConnectedDrawers.class, remap = false)
public abstract class ConnectedDrawersMixin implements GasConnectedDrawersAccess {
    @Shadow
    @Final
    private StorageControllerTile<?> controllerTile;
    @Shadow
    private List<Long> connectedDrawers;
    @Shadow
    private Level level;

    @Unique
    private List<IGasHandler> ccb$gasHandlers = List.of();

    @WrapOperation(method = "rebuild", at = @At(value = "INVOKE", target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"))
    private void ccb$includeGasDrawers(List<Long> validDrawers, Comparator<? super Long> comparator, Operation<Void> original) {
        if (level == null || level.isClientSide()) {
            original.call(validDrawers, comparator);
            return;
        }

        double range = controllerTile.getStorageMultiplier();
        AABB area = new AABB(controllerTile.getBlockPos()).inflate(range);
        for (long packedPos : connectedDrawers) {
            if (validDrawers.contains(packedPos)) {
                continue;
            }

            BlockPos pos = BlockPos.of(packedPos);
            if (!area.contains(Vec3.atCenterOf(pos)) || !level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof GasDrawerBlockEntity)) {
                continue;
            }

            validDrawers.add(packedPos);
        }
        original.call(validDrawers, comparator);
    }

    @Inject(method = "rebuild", at = @At("TAIL"))
    private void ccb$rebuild(CallbackInfo ci) {
        if (level == null || level.isClientSide()) {
            ccb$gasHandlers = List.of();
            return;
        }

        ccb$gasHandlers = ccb$collectGasHandlers();
        IGasHandler handler = ((GasControllerAccess) controllerTile).ccb$getGasHandler();
        if (!(handler instanceof ControllerGasHandler controller)) {
            return;
        }

        controller.refresh(ccb$gasHandlers);
    }

    @Unique
    private @Unmodifiable List<IGasHandler> ccb$collectGasHandlers() {
        List<IGasHandler> handlers = new ArrayList<>();
        for (long packedPos : connectedDrawers) {
            if (!(level.getBlockEntity(BlockPos.of(packedPos)) instanceof GasDrawerBlockEntity drawer)) {
                continue;
            }

            handlers.add(drawer.getGasHandler());
        }
        return List.copyOf(handlers);
    }

    @Override
    public List<IGasHandler> ccb$getGasHandlers() {
        return ccb$gasHandlers;
    }
}
