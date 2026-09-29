package net.ty.createcraftedbeginning.mixin.common.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.contraptions.MountedStorageSyncPacket;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageAccess;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageState;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageSyncPacket;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = MountedStorageManager.class, remap = false)
public abstract class MountedStorageManagerMixin implements MountedGasStorageAccess {
    @Unique
    private MountedGasStorageState ccb$gasState;
    @Shadow
    private int syncCooldown;

    @Override
    @Unique
    public MountedGasStorageWrapper ccb$getGasStorage() {
        assertInitialized();
        return ccb$gasState.getStorage();
    }

    @Override
    @Unique
    public void ccb$handleGasStorageSync(MountedGasStorageSyncPacket packet, AbstractContraptionEntity entity) {
        assertInitialized();
        ccb$gasState.handleSync(packet, entity);
    }

    @Override
    @Unique
    public void ccb$setGasStorage(MountedGasStorageWrapper gases) {
        ccb$gasState.setStorage(gases);
    }

    @Shadow
    protected abstract void assertInitialized();

    @Inject(method = "initialize", at = @At("TAIL"))
    private void ccb$initialize(CallbackInfo callback) {
        ccb$gasState.initialize();
    }

    @Inject(method = "reset", at = @At("TAIL"))
    private void ccb$reset(CallbackInfo callback) {
        if (ccb$gasState == null) {
            ccb$gasState = new MountedGasStorageState();
            return;
        }

        ccb$gasState.reset();
    }

    @Inject(method = "handleSync", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/MountedStorageManager;reset()V", shift = Shift.BEFORE))
    private void ccb$captureGasesBeforeCreateSync(MountedStorageSyncPacket packet, AbstractContraptionEntity entity, CallbackInfo callback) {
        assertInitialized();
        ccb$gasState.captureBeforeSync();
    }

    @Inject(method = "handleSync", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/MountedStorageManager;reset()V", shift = Shift.AFTER))
    private void ccb$restoreGasesAfterCreateReset(MountedStorageSyncPacket packet, AbstractContraptionEntity entity, CallbackInfo callback) {
        ccb$gasState.restoreAfterReset();
    }

    @Inject(method = "addBlock", at = @At("TAIL"))
    private void ccb$addBlock(Level level, BlockState state, BlockPos globalPos, BlockPos localPos, BlockEntity blockEntity, CallbackInfo callback) {
        ccb$gasState.addBlock(level, state, globalPos, localPos, blockEntity);
    }

    @Inject(method = "unmount", at = @At("TAIL"))
    private void ccb$unmount(Level level, StructureBlockInfo info, BlockPos globalPos, BlockEntity blockEntity, CallbackInfo callback) {
        assertInitialized();
        ccb$gasState.unmount(level, info, globalPos, blockEntity);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void ccb$tick(AbstractContraptionEntity entity, CallbackInfo callback) {
        if (syncCooldown > 0 || !ccb$gasState.tick(entity)) {
            return;
        }

        syncCooldown = 8;
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void ccb$write(CompoundTag compoundTag, Provider provider, boolean clientPacket, CallbackInfo callback) {
        assertInitialized();
        ccb$gasState.write(compoundTag, clientPacket);
    }

    @Inject(method = "read", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/MountedStorageManager;initialize()V", shift = Shift.BEFORE))
    private void ccb$readOnInitialization(CompoundTag nbt, Provider provider, boolean clientPacket, Contraption contraption, CallbackInfo callback) {
        ccb$gasState.read(nbt);
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void ccb$readAtReturn(CompoundTag nbt, Provider registries, boolean clientPacket, Contraption contraption, CallbackInfo callback) {
        if (!clientPacket) {
            return;
        }

        assertInitialized();
        ccb$gasState.afterSync(contraption);
    }
}
