package net.ty.createcraftedbeginning.content.opticalpower.amethystantenna;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.ChunkStabilizerBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.ChunkStabilizerNetwork;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkBinding;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystAntennaBlockItem extends BlockItem {
    public AmethystAntennaBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(CCBDataComponents.LOADING_NETWORK) || super.isFoil(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (player == null || !level.isLoaded(pos)) {
            return InteractionResult.FAIL;
        }

        BlockEntity target = level.getBlockEntity(pos);
        LoadingNetworkBinding binding = null;
        if (target instanceof ChunkStabilizerBlockEntity loader) {
            binding = ChunkStabilizerNetwork.binding(loader);
        }
        else if (target instanceof AmethystAntennaBlockEntity antenna) {
            binding = antenna.binding();
        }
        if (binding == null || player.isShiftKeyDown()) {
            return super.useOn(context);
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(serverLevel);
        UUID network = binding.id();
        if (!data.owns(network, player.getUUID())) {
            player.displayClientMessage(CCBLang.translateDirect("loading_network.protected").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        LoadingNetworkBinding current = data.binding(network);
        if (current == null) {
            return InteractionResult.FAIL;
        }

        context.getItemInHand().set(CCBDataComponents.LOADING_NETWORK, current);
        player.displayClientMessage(CCBLang.translateDirect("loading_network.tuned"), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        LoadingNetworkBinding binding = context.getItemInHand().get(CCBDataComponents.LOADING_NETWORK);
        if (player == null) {
            return InteractionResult.FAIL;
        }

        if (level instanceof ServerLevel serverLevel && binding != null && !LoadingNetworkSavedData.get(serverLevel).owns(binding.id(), player.getUUID())) {
            player.displayClientMessage(CCBLang.translateDirect("loading_network.protected").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        InteractionResult result = super.place(context);
        if (!(level instanceof ServerLevel) || !result.consumesAction()) {
            return result;
        }

        if (binding == null) {
            return result;
        }

        player.displayClientMessage(CCBLang.translateDirect("loading_network.connected"), true);
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!stack.has(CCBDataComponents.LOADING_NETWORK)) {
            return;
        }

        tooltip.add(CCBLang.translateDirect("loading_network.tooltip").withStyle(ChatFormatting.GOLD));
        tooltip.add(CCBLang.translateDirect("loading_network.tooltip_clear").withStyle(ChatFormatting.GRAY));
    }
}
