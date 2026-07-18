package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IChamberGasTank extends IMultiBlockEntityContainer {
    GasTank getTankInventory();

    IGasHandler getCapability();

    int getWidth();

    @Override
    <T extends BlockEntity & IMultiBlockEntityContainer> @Nullable T getControllerBE();

    boolean isController();

    boolean isRemoved();
}
