package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ChamberGasTank extends IMultiBlockEntityContainer {
    @Override
    <T extends BlockEntity & IMultiBlockEntityContainer> @Nullable T getControllerBE();

    @Override
    boolean isController();

    @Override
    int getWidth();

    GasStorageHandler getTankInventory();

    GasStorageHandler getCapability();

    boolean isRemoved();
}
