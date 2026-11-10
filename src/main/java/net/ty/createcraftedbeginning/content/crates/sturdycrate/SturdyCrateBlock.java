package net.ty.createcraftedbeginning.content.crates.sturdycrate;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.ty.createcraftedbeginning.content.crates.CrateBlock;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SturdyCrateBlock extends CrateBlock<SturdyCrateBlockEntity> {
    public SturdyCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(SturdyCrateBlock::new);
    }

    @Override
    public Class<SturdyCrateBlockEntity> getBlockEntityClass() {
        return SturdyCrateBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SturdyCrateBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.STURDY_CRATE.get();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        if (!(level.getBlockEntity(pos) instanceof SturdyCrateBlockEntity crate)) {
            return;
        }

        crate.loadFromItem(stack);
    }

    @Override
    protected void onCrateRemoved(Level level, BlockPos pos, SturdyCrateBlockEntity crate, boolean isMoving) {
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack crateItem = new ItemStack(this);
        if (!(level.getBlockEntity(pos) instanceof SturdyCrateBlockEntity crate) || !crate.hasStoredData() || !player.isShiftKeyDown()) {
            return crateItem;
        }

        crate.saveToItem(crateItem);
        return crateItem;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (player.isCreative() && !level.isClientSide && level.getBlockEntity(pos) instanceof SturdyCrateBlockEntity crate && crate.hasStoredData()) {
            ItemStack crateItem = new ItemStack(this);
            crate.saveToItem(crateItem);
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, crateItem);
        }
        super.playerWillDestroy(level, pos, state, player);
        return state;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isCreative()) {
            return super.onSneakWrenched(state, context);
        }

        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        BreakEvent breakEvent = new BreakEvent(level, pos, state, player);
        NeoForge.EVENT_BUS.post(breakEvent);
        if (breakEvent.isCanceled() || level.getBlockState(pos) != state) {
            return InteractionResult.SUCCESS;
        }

        ItemStack crateItem = ItemStack.EMPTY;
        if (level.getBlockEntity(pos) instanceof SturdyCrateBlockEntity crate && crate.hasStoredData()) {
            crateItem = new ItemStack(this);
            crate.saveToItem(crateItem);
        }
        state.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY, true);
        if (!level.destroyBlock(pos, false)) {
            return InteractionResult.SUCCESS;
        }

        if (!crateItem.isEmpty()) {
            player.getInventory().placeItemBackInInventory(crateItem);
        }
        IWrenchable.playRemoveSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack crateStack, TooltipContext context, List<Component> tooltips, TooltipFlag flag) {
        SturdyCrateTooltip.append(crateStack, tooltips);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, Builder params) {
        if (!(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof SturdyCrateBlockEntity crate)) {
            return super.getDrops(state, params);
        }

        ItemStack crateItem = new ItemStack(this);
        crate.saveToItem(crateItem);
        return Collections.singletonList(crateItem);
    }

}
