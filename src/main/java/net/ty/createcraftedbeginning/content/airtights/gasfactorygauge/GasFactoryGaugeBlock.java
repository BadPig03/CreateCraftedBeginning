package net.ty.createcraftedbeginning.content.airtights.gasfactorygauge;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockItem;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasFactoryGaugeBlock extends FactoryPanelBlock {
    public GasFactoryGaugeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        PanelSlot slot = getTargetedSlot(pos, state, context.getClickLocation());
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return onBlockEntityUse(level, pos, be -> {
            FactoryPanelBehaviour behaviour = be.panels.get(slot);
            if (behaviour == null || !behaviour.isActive()) {
                return InteractionResult.SUCCESS;
            }

            BreakEvent event = new BreakEvent(level, pos, level.getBlockState(pos), player);
            NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled() || !be.removePanel(slot)) {
                return InteractionResult.SUCCESS;
            }

            if (!player.isCreative()) {
                player.getInventory().placeItemBackInInventory(new ItemStack(CCBBlocks.GAS_FACTORY_GAUGE_BLOCK.asItem()));
            }
            IWrenchable.playRemoveSound(level, pos);
            if (be.activePanels() == 0) {
                level.destroyBlock(pos, false);
            }
            return InteractionResult.SUCCESS;
        });
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide() || !stack.is(CCBBlocks.GAS_FACTORY_GAUGE_BLOCK.asItem())) {
            return ItemInteractionResult.SUCCESS;
        }

        Vec3 location = hitResult.getLocation();
        if (!FactoryPanelBlockItem.isTuned(stack)) {
            AllSoundEvents.DENY.playOnServer(level, pos);
            player.displayClientMessage(CCBLang.translate("gui.gas_factory_gauge.tune_before_placing").style(ChatFormatting.RED).component(), true);
            return ItemInteractionResult.FAIL;
        }

        PanelSlot slot = getTargetedSlot(pos, state, location);
        withBlockEntityDo(level, pos, blockEntity -> {
            ItemStack panelStack = FactoryPanelBlockItem.fixCtrlCopiedStack(stack);
            if (!blockEntity.addPanel(slot, LogisticallyLinkedBlockItem.networkFromStack(panelStack))) {
                return;
            }

            player.displayClientMessage(CCBLang.translateDirect("gui.gas_factory_gauge.logistically_linked.connected"), true);
            level.playSound(null, pos, state.getSoundType(level, pos, player).getPlaceSound(), SoundSource.BLOCKS);
            if (player.isCreative()) {
                return;
            }

            stack.shrink(1);
            if (stack.isEmpty()) {
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
        });
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        return !tryDestroyGasSubPanelFirst(state, level, pos, player) && super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (!context.getItemInHand().is(CCBBlocks.GAS_FACTORY_GAUGE_BLOCK.asItem())) {
            return false;
        }

        FactoryPanelBlockEntity blockEntity = getBlockEntity(context.getLevel(), context.getClickedPos());
        PanelSlot slot = getTargetedSlot(context.getClickedPos(), state, context.getClickLocation());
        return blockEntity != null && !blockEntity.panels.get(slot).isActive();
    }

    @Override
    public BlockEntityType<? extends FactoryPanelBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.GAS_FACTORY_GAUGE.get();
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return simpleCodec(GasFactoryGaugeBlock::new);
    }

    private boolean tryDestroyGasSubPanelFirst(BlockState state, Level level, BlockPos pos, Player player) {
        double range = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 1;
        Vec3 location = player.pick(range, 1, false).getLocation();
        PanelSlot destroyedSlot = getTargetedSlot(pos, state, location);
        InteractionResult result = onBlockEntityUse(level, pos, blockEntity -> {
            if (blockEntity.activePanels() < 2 || !blockEntity.removePanel(destroyedSlot)) {
                return InteractionResult.FAIL;
            }

            if (!player.isCreative()) {
                popResource(level, pos, CCBBlocks.GAS_FACTORY_GAUGE_BLOCK.asStack());
            }
            return InteractionResult.SUCCESS;
        });
        return result == InteractionResult.SUCCESS;
    }
}
