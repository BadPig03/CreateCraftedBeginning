package net.ty.createcraftedbeginning.mixin.compat.functionalstorage;

import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.buuz135.functionalstorage.util.ConnectedDrawers;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.compat.functionalstorage.GasDrawerConnections;
import net.ty.createcraftedbeginning.compat.functionalstorage.access.GasConnectedDrawersAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;
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
    private final GasDrawerConnections ccb$connections = new GasDrawerConnections();

    @Override
    public List<GasHandler> ccb$getGasHandlers() {
        return ccb$connections.getHandlers();
    }

    @Inject(method = "rebuild", at = @At("HEAD"))
    private void ccb$beginRebuild(CallbackInfo callback) {
        ccb$connections.beginRebuild(connectedDrawers);
    }

    @WrapOperation(method = "rebuild", at = @At(value = "INVOKE", target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"))
    private void ccb$includeGasDrawers(List<Long> validDrawers, Comparator<? super Long> comparator, Operation<Void> original) {
        ccb$connections.include(level, controllerTile, validDrawers);
        original.call(validDrawers, comparator);
    }

    @Inject(method = "rebuild", at = @At("TAIL"))
    private void ccb$rebuild(CallbackInfo callback) {
        ccb$connections.finishRebuild(level, controllerTile, connectedDrawers);
    }
}
