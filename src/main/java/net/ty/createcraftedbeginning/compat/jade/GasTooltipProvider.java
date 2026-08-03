package net.ty.createcraftedbeginning.compat.jade;

import net.createmod.catnip.data.Iterate;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.compat.jade.gas.GasConstants;
import net.ty.createcraftedbeginning.compat.jade.gas.GasDataProvider;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.ICreativeGasContainer;
import net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle.TeslaTurbineNozzleBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.IElementHelper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum GasTooltipProvider implements IServerDataProvider<BlockAccessor>, IComponentProvider<BlockAccessor> {
    INSTANCE;

    private static final ItemStack ILL_ICON = new ItemStack(Items.POISONOUS_POTATO);
    private static final ItemStack GALE_ICON = new ItemStack(Items.WIND_CHARGE);

    private static final String COMPOUND_KEY_WIND_LEVEL = "WindLevel";
    private static final String COMPOUND_KEY_WIND_TIME_REMAINING = "WindTimeRemaining";
    private static final String COMPOUND_KEY_IS_CREATIVE = "IsCreative";

    private static void appendWindTooltip(ITooltip tooltip, BlockAccessor accessor, CompoundTag data) {
        if (!data.contains(COMPOUND_KEY_WIND_LEVEL) || !data.contains(COMPOUND_KEY_WIND_TIME_REMAINING) || !data.contains(COMPOUND_KEY_IS_CREATIVE)) {
            return;
        }

        IElementHelper helper = IElementHelper.get();
        float tickRate = accessor.tickRate();
        int remainingTicks = Mth.abs(data.getInt(COMPOUND_KEY_WIND_TIME_REMAINING));
        WindLevel windLevel = WindLevel.values()[data.getInt(COMPOUND_KEY_WIND_LEVEL)];
        boolean isCreative = data.getBoolean(COMPOUND_KEY_IS_CREATIVE);
        if (windLevel == WindLevel.ILL) {
            tooltip.add(helper.smallItem(ILL_ICON));
            tooltip.append(isCreative ? IThemeHelper.get().info(Component.translatable("jade.gas.infinity_mark")) : IThemeHelper.get().seconds(remainingTicks, tickRate).withStyle(ChatFormatting.RED));
            return;
        }

        if (windLevel != WindLevel.GALE) {
            return;
        }

        tooltip.add(helper.smallItem(GALE_ICON));
        tooltip.append(isCreative ? IThemeHelper.get().info(Component.translatable("jade.gas.infinity_mark")) : IThemeHelper.get().seconds(remainingTicks, tickRate));
    }

    private static Set<IGasHandler> getGasHandlers(Level level, BlockPos pos) {
        Set<IGasHandler> gasHandlers = new HashSet<>();
        for (Direction direction : Iterate.directions) {
            IGasHandler handler = level.getCapability(GasHandler.BLOCK, pos, direction);
            if (handler == null) {
                continue;
            }

            gasHandlers.add(handler);
        }
        return gasHandlers;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        appendWindTooltip(tooltip, accessor, data);

        if (!data.contains(GasConstants.STORAGE_KEY) || !data.contains(GasConstants.STORAGE_UID_KEY)) {
            return;
        }

        if (!JadePlugin.GAS_BLOCK_TOOLTIP.toString().equals(data.getString(GasConstants.STORAGE_UID_KEY))) {
            return;
        }

        GasDataProvider.appendData(tooltip, data, accessor.showDetails());
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
