package net.ty.createcraftedbeginning.mixin.compat.functionalstorage;

import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.buuz135.functionalstorage.util.ConnectedDrawers;
import com.hrznstudio.titanium.block.BasicTileBlock;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.compat.functionalstorage.ControllerGasHandler;
import net.ty.createcraftedbeginning.compat.functionalstorage.access.GasConnectedDrawersAccess;
import net.ty.createcraftedbeginning.compat.functionalstorage.access.GasControllerAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = StorageControllerTile.class, remap = false)
public abstract class StorageControllerTileMixin implements GasControllerAccess {
    @Shadow
    protected ConnectedDrawers connectedDrawers;

    @Unique
    private ControllerGasHandler ccb$gasHandler;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ccb$init(BasicTileBlock<?> base, BlockEntityType<?> entityType, BlockPos pos, BlockState state, CallbackInfo ci) {
        ccb$gasHandler = new ControllerGasHandler();
    }

    @ModifyExpressionValue(method = "serverTick", at = @At(value = "INVOKE", target = "Lcom/buuz135/functionalstorage/util/ConnectedDrawers;getExtensions()I"))
    private int ccb$serverTick(int original) {
        return original + ((GasConnectedDrawersAccess) connectedDrawers).ccb$getGasHandlers().size();
    }

    @Override
    public IGasHandler ccb$getGasHandler() {
        return ccb$gasHandler;
    }
}
