package net.ty.createcraftedbeginning.content.airtightpump;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.data.CCBTags;

import java.util.List;
import java.util.Map;

public class AirtightPumpBlockEntity extends PumpBlockEntity {
    public AirtightPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.removeIf(b -> b instanceof FluidTransportBehaviour);
        behaviours.add(new CompressedAirTransferBehaviour(this));
    }

    class CompressedAirTransferBehaviour extends FluidTransportBehaviour {
        public CompressedAirTransferBehaviour(AirtightPumpBlockEntity be) {
            super(be);
        }

        @Override
		public void tick() {
			super.tick();
			for (Map.Entry<Direction, PipeConnection> entry : interfaces.entrySet()) {
				boolean pull = isPullingOnSide(isFront(entry.getKey()));
				Couple<Float> pressure = entry.getValue().getPressure();
				pressure.set(pull, Math.abs(getSpeed()));
				pressure.set(!pull, 0f);
			}
		}

        @Override
        public boolean canPullFluidFrom(FluidStack fluid, BlockState state, Direction direction) {
            return fluid.is(CCBTags.commonFluidTag("compressed_air"));
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            return isSideAccessible(direction);
        }

        @Override
        public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction direction) {
            AttachmentTypes attachment = super.getRenderedRimAttachment(world, pos, state, direction);
            return attachment == AttachmentTypes.RIM ? AttachmentTypes.NONE : attachment;
        }
    }
}
