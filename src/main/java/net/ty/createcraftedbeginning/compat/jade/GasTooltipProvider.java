package net.ty.createcraftedbeginning.compat.jade;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.compat.jade.gas.GasDataProvider;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.ICreativeGasContainer;
import net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle.TeslaTurbineNozzleBlockEntity;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum GasTooltipProvider implements IServerDataProvider<BlockAccessor>, IComponentProvider<BlockAccessor> {
    INSTANCE;

    private static Set<IGasHandler> getGasHandlers(Level level, BlockPos pos) {
        Set<IGasHandler> gasHandlers = new HashSet<>();
        for (Direction direction : Iterate.directions) {
            IGasHandler gasHandler = level.getCapability(GasHandler.BLOCK, pos, direction);
            if (gasHandler == null) {
                continue;
            }

            gasHandlers.add(gasHandler);
        }
        return gasHandlers;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        if (!CCBNbtUtils.contains(serverData, GasDataProvider.STORAGE_KEY) || !CCBNbtUtils.contains(serverData, GasDataProvider.STORAGE_UID_KEY)) {
            return;
        }

        if (!JadePlugin.GAS_BLOCK_TOOLTIP.toString().equals(CCBNbtUtils.getString(serverData, GasDataProvider.STORAGE_UID_KEY))) {
            return;
        }

        GasDataProvider.appendData(tooltip, serverData, accessor.showDetails());
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor blockAccessor) {
        Level level = blockAccessor.getLevel();
        BlockPos pos = blockAccessor.getPosition();
        Set<IGasHandler> gasHandlers = getGasHandlers(level, pos);
        if (gasHandlers.isEmpty()) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TeslaTurbineNozzleBlockEntity) {
            return;
        }

        boolean isCreative = blockEntity instanceof ICreativeGasContainer container && container.isCreative(level, level.getBlockState(pos), pos);
        GasDataProvider.readData(data, gasHandlers, JadePlugin.GAS_BLOCK_TOOLTIP, isCreative);
    }

    @Override
    public ResourceLocation getUid() {
        return JadePlugin.GAS_BLOCK_TOOLTIP;
    }
}
