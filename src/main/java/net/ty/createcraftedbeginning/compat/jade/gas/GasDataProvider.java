package net.ty.createcraftedbeginning.compat.jade.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import org.jetbrains.annotations.NotNull;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public class GasDataProvider implements IServerDataProvider<BlockAccessor> {
    public static final GasDataProvider INSTANCE = new GasDataProvider();

    @Override
    public ResourceLocation getUid() {
        return GasConstants.DATA_PROVIDER;
    }

    @Override
    public void appendServerData(CompoundTag data, @NotNull BlockAccessor blockAccessor) {
        Level level = blockAccessor.getLevel();
        BlockPos pos = blockAccessor.getPosition();
        IGasHandler gasHandler = level.getCapability(GasCapabilities.GasHandler.BLOCK, pos, blockAccessor.getSide());
        if (gasHandler == null) {
            return;
        }

        BlockEntity be = level.getBlockEntity(pos);
        boolean creative = false;
        if (be instanceof AirtightTankBlockEntity tank) {
            AirtightTankBlockEntity controller = tank.getControllerBE();
            if (controller != null && controller.gasController.isActive()) {
                return;
            }
        }
        else if (be instanceof CreativeAirtightTankBlockEntity) {
            creative = true;
        }

        DataProviderHelper.getDataFromIGasHandler(data, gasHandler, GasConstants.TOOLTIP_RENDERER, creative);
    }
}
