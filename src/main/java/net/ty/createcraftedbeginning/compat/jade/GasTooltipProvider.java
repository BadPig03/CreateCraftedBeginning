package net.ty.createcraftedbeginning.compat.jade;

import net.createmod.catnip.data.Iterate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.IGasHandler;
import net.ty.createcraftedbeginning.compat.jade.gas.GasConstants;
import net.ty.createcraftedbeginning.compat.jade.gas.GasDataProvider;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.ICreativeGasContainer;
import net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle.TeslaTurbineNozzleBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import org.jetbrains.annotations.NotNull;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.IElementHelper;

import java.util.HashSet;
import java.util.Set;

public enum GasTooltipProvider implements IServerDataProvider<BlockAccessor>, IComponentProvider<BlockAccessor> {
    INSTANCE;

    private static final ItemStack ILL_ICON = new ItemStack(Items.POISONOUS_POTATO);
    private static final ItemStack GALE_ICON = new ItemStack(Items.WIND_CHARGE);

    private static final String COMPOUND_KEY_WIND_LEVEL = "WindLevel";
    private static final String COMPOUND_KEY_WIND_TIME_REMAINING = "WindTimeRemaining";
    private static final String COMPOUND_KEY_IS_CREATIVE = "IsCreative";

    @Override
    public void appendTooltip(ITooltip tooltip, @NotNull BlockAccessor accessor, IPluginConfig config) {
        CompoundTag compoundTag = accessor.getServerData();
        if (compoundTag.contains(COMPOUND_KEY_WIND_LEVEL) && compoundTag.contains(COMPOUND_KEY_WIND_TIME_REMAINING) && compoundTag.contains(COMPOUND_KEY_IS_CREATIVE)) {
            IElementHelper helper = IElementHelper.get();
            float tickRate = accessor.tickRate();
            int windTimeRemaining = Mth.abs(compoundTag.getInt(COMPOUND_KEY_WIND_TIME_REMAINING));
            WindLevel windLevel = WindLevel.values()[compoundTag.getInt(COMPOUND_KEY_WIND_LEVEL)];
            boolean isCreative = compoundTag.getBoolean(COMPOUND_KEY_IS_CREATIVE);
            if (windLevel == WindLevel.ILL) {
                tooltip.add(helper.smallItem(ILL_ICON));
                tooltip.append(isCreative ? IThemeHelper.get().info(Component.translatable("jade.gas.infinity_mark")) : IThemeHelper.get().seconds(windTimeRemaining, tickRate).withStyle(ChatFormatting.RED));
            }
            else if (windLevel == WindLevel.GALE) {
                tooltip.add(helper.smallItem(GALE_ICON));
                tooltip.append(isCreative ? IThemeHelper.get().info(Component.translatable("jade.gas.infinity_mark")) : IThemeHelper.get().seconds(windTimeRemaining, tickRate));
            }
        }

        if (!compoundTag.contains(GasConstants.STORAGE_KEY) || !compoundTag.contains(GasConstants.STORAGE_UID_KEY)) {
            return;
        }
        if (!JadePlugin.GAS_BLOCK_TOOLTIP.toString().equals(compoundTag.getString(GasConstants.STORAGE_UID_KEY))) {
            return;
        }

        GasDataProvider.appendData(tooltip, compoundTag, accessor.showDetails());
    }

    @Override
    public void appendServerData(CompoundTag data, @NotNull BlockAccessor blockAccessor) {
        Level level = blockAccessor.getLevel();
        BlockPos pos = blockAccessor.getPosition();
        Set<IGasHandler> gasHandlers = new HashSet<>();
        for (Direction direction : Iterate.directions) {
            IGasHandler capability = level.getCapability(GasHandler.BLOCK, pos, direction);
            if (capability == null) {
                continue;
            }

            gasHandlers.add(capability);
        }
        if (gasHandlers.isEmpty()) {
            return;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof AirtightTankBlockEntity tank) {
            AirtightTankBlockEntity controller = tank.getControllerBE();
            if (controller != null && controller.getCore().getStructureManager().isActive()) {
                return;
            }
        }
        else if (be instanceof TeslaTurbineNozzleBlockEntity) {
            return;
        }

        boolean creative = be instanceof ICreativeGasContainer creativeGasContainer && creativeGasContainer.isCreative(level, level.getBlockState(pos), pos);
        GasDataProvider.readData(data, gasHandlers, JadePlugin.GAS_BLOCK_TOOLTIP, creative);
    }

    @Override
    public ResourceLocation getUid() {
        return JadePlugin.GAS_BLOCK_TOOLTIP;
    }
}
