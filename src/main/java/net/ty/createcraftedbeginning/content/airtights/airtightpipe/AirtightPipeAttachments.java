package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.content.fluids.pipes.IAxisPipe;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.content.airtights.airtightmeters.AbstractAirtightMeterBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightregulatorpump.AirtightRegulatorPumpBlock;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectable;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightPipeAttachments {
    private AirtightPipeAttachments() {
    }

    static AirtightPipeAttachmentTypes resolve(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction face, @Nullable GasTransportBehaviour transport) {
        Block block = state.getBlock();
        BlockPos neighborPos = pos.relative(face);
        BlockState neighborState = level.getBlockState(neighborPos);
        Block neighborBlock = neighborState.getBlock();
        Direction neighborFace = face.getOpposite();
        switch (block) {
            case AirtightPumpBlock ignored -> {
                if (face.getAxis() != state.getValue(BlockStateProperties.FACING).getAxis()) {
                    return AirtightPipeAttachmentTypes.NONE;
                }

                boolean drain = neighborBlock instanceof AirtightPipeDrain pipeDrain && pipeDrain.shouldRenderDrain(level, neighborPos, neighborState, neighborFace);
                if (drain) {
                    return AirtightPipeAttachmentTypes.DRAIN;
                }

                return AirtightPipeAttachmentTypes.NONE;
            }
            case AirtightRegulatorPumpBlock ignored -> {
                Direction output = state.getValue(BlockStateProperties.FACING);
                if (face.getAxis() != output.getAxis()) {
                    return AirtightPipeAttachmentTypes.NONE;
                }

                boolean drain = neighborBlock instanceof AirtightPipeDrain pipeDrain && pipeDrain.shouldRenderDrain(level, neighborPos, neighborState, neighborFace);
                if (face == output) {
                    if (drain) {
                        return AirtightPipeAttachmentTypes.OUTLET_DRAIN;
                    }

                    return AirtightPipeAttachmentTypes.OUTLET_RIM;
                }

                if (drain) {
                    return AirtightPipeAttachmentTypes.INLET_DRAIN;
                }

                return AirtightPipeAttachmentTypes.INLET_RIM;
            }
            case AirtightPipeBlock ignored -> {
                if (!AirtightPipeConnections.isOpen(state, face)) {
                    return AirtightPipeAttachmentTypes.NONE;
                }

                boolean connected = neighborBlock instanceof GasConnectable component && component.canConnectOnFace(neighborPos, neighborState, neighborFace);
                switch (neighborBlock) {
                    case AirtightPipeBlock ignored1 when connected -> {
                        if (!state.getValue(AirtightPipeBlock.CASED) && neighborState.getValue(AirtightPipeBlock.CASED)) {
                            return AirtightPipeAttachmentTypes.RIM;
                        }

                        return AirtightPipeAttachmentTypes.NONE;
                    }
                    case IAxisPipe ignored1 when connected && !(neighborBlock instanceof AirtightRegulatorPumpBlock) -> {
                        if (state.getValue(AirtightPipeBlock.CASED) || neighborBlock instanceof AbstractAirtightMeterBlock) {
                            return AirtightPipeAttachmentTypes.RIM;
                        }

                        return AirtightPipeAttachmentTypes.NONE;
                    }
                    case AirtightPipeDrain drain when drain.shouldRenderDrain(level, neighborPos, neighborState, neighborFace) -> {
                        return AirtightPipeAttachmentTypes.DRAIN;
                    }
                    default -> {
                    }
                }

                return AirtightPipeAttachmentTypes.RIM;
            }
            default -> {
            }
        }

        Axis axis = null;
        if (block instanceof IAxisPipe pipe) {
            axis = pipe.getAxis(state);
        }
        if (axis != null) {
            if (face.getAxis() != axis) {
                return AirtightPipeAttachmentTypes.NONE;
            }

            boolean alignedPipe = neighborBlock instanceof IAxisPipe neighborPipe && !(neighborBlock instanceof AirtightRegulatorPumpBlock) && neighborPipe.getAxis(neighborState) == axis || neighborBlock instanceof AirtightPipeBlock && AirtightPipeConnections.isOpen(neighborState, neighborFace);
            if (!alignedPipe) {
                boolean drain = neighborBlock instanceof AirtightPipeDrain pipeDrain && pipeDrain.shouldRenderDrain(level, neighborPos, neighborState, neighborFace);
                if (drain) {
                    return AirtightPipeAttachmentTypes.DRAIN;
                }

                return AirtightPipeAttachmentTypes.RIM;
            }

            return AirtightPipeAttachmentTypes.NONE;
        }

        if (transport == null || !transport.canConnectOnFace(state, face)) {
            return AirtightPipeAttachmentTypes.NONE;
        }

        if (neighborBlock instanceof AirtightPumpBlock && neighborFace == neighborState.getValue(AirtightPumpBlock.FACING)) {
            return AirtightPipeAttachmentTypes.NONE;
        }

        if (GasCapabilities.hasBlockHandler(level, neighborPos, neighborFace)) {
            return AirtightPipeAttachmentTypes.DRAIN;
        }

        return AirtightPipeAttachmentTypes.RIM;
    }
}
