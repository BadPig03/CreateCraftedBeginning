package net.ty.createcraftedbeginning.content.airtights.gas;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.data.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

public class GasBlock extends Block implements IBE<GasBlockEntity> {
    public GasBlock(Properties properties) {
        super(properties);
    }

    public static boolean canDiffuseTo(@NotNull BlockState state) {
        return state.isAir() || state.getBlock() instanceof GasBlock;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return CCBShapes.GAS;
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity entity, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        if (level.isClientSide) {
            return;
        }
        if (!(entity instanceof Player)) {
            return;
        }

        withBlockEntityDo(level, pos, be -> be.changeGasAmount(1000L, false));
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        //GasWorldContentsDataManager dataManager = CreateCraftedBeginning.GAS_WORLD_CONTENTS_DATA_MANAGER;
        //dataManager.addContents(pos, new GasWorldContents(pos, CCBGases.NATURAL_AIR.get(), MAX_GAS_AMOUNT));
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        //GasWorldContentsDataManager dataManager = CreateCraftedBeginning.GAS_WORLD_CONTENTS_DATA_MANAGER;
        //dataManager.removeContents(pos);
    }

    @Override
    public Class<GasBlockEntity> getBlockEntityClass() {
        return GasBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GasBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.GAS.get();
    }
}
