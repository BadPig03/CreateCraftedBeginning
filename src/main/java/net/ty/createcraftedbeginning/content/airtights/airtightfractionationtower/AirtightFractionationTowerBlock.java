package net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.HitResult;
import net.ty.createcraftedbeginning.gas.network.GasConnectable;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightFractionationTowerBlock extends Block implements IBE<AirtightFractionationTowerBlockEntity>, IWrenchable, GasConnectable {
    static final BooleanProperty TOP = BooleanProperty.create("top");
    static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");

    static final int WIDTH = 3;
    static final int MIN_HEIGHT = 3;
    static final int MAX_HEIGHT = 9;

    public AirtightFractionationTowerBlock(Properties properties) {
        super(properties.pushReaction(PushReaction.BLOCK));
        registerDefaultState(defaultBlockState().setValue(TOP, true).setValue(BOTTOM, true));
    }

    @Override
    public Class<AirtightFractionationTowerBlockEntity> getBlockEntityClass() {
        return AirtightFractionationTowerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightFractionationTowerBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_FRACTIONATION_TOWER.get();
    }

    @Override
    public boolean canConnectOnFace(BlockPos currentPos, BlockState currentState, Direction localFace) {
        return true;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.asItem());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!moving && !state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof AirtightFractionationTowerBlockEntity tower) {
            AirtightFractionationTowerStructure.disassemble(level, pos, tower);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(TOP, BOTTOM);
        super.createBlockStateDefinition(builder);
    }
}
