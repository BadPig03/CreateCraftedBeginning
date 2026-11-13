package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightPipeBlockEntity extends AbstractAirtightPipeBlockEntity implements TransformableBlockEntity, IHaveHoveringInformation {
    private @Nullable UUID pendingPlacer;

    public AirtightPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return level != null && getBlockState().getValue(AirtightPipeBlock.CASED) && AirtightPipeTooltip.addTo(tooltip, level, worldPosition, getBlockState());
    }

    @Override
    public void transform(BlockEntity entity, StructureTransform transform) {
        BracketedBlockEntityBehaviour bracket = getBehaviour(BracketedBlockEntityBehaviour.TYPE);
        if (bracket == null) {
            return;
        }

        bracket.transformBracket(transform);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level == null || level.isClientSide || isVirtual()) {
            return;
        }

        level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
    }

    @Override
    protected void addPipeBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new AirtightPipeBracketBehaviour(this));
    }

    @Override
    protected GasTransportBehaviour createTransportBehaviour() {
        return new AirtightPipeTransportBehaviour(this);
    }

    @Override
    public boolean allowsGasTransport(Level level, BlockState blockState, BlockPos blockPos, Direction direction) {
        return AirtightPipeConnections.isOpen(blockState, direction);
    }

    void place(ServerLevel level, UUID placer) {
        pendingPlacer = placer;
        if (level.captureBlockSnapshots) {
            return;
        }

        commitPlacement(level);
    }

    void commitPlacement(ServerLevel level) {
        UUID placer = pendingPlacer;
        pendingPlacer = null;
        if (placer == null) {
            return;
        }

        AirtightPipeAdvancement.awardCasing(level, worldPosition, level.getPlayerByUUID(placer));
    }
}
