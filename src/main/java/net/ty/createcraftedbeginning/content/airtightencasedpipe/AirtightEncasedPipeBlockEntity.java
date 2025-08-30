package net.ty.createcraftedbeginning.content.airtightencasedpipe;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.advancement.AdvancementBehaviour;
import net.ty.createcraftedbeginning.advancement.CCBAdvancement;
import net.ty.createcraftedbeginning.advancement.CCBAdvancements;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.data.CCBTags;
import net.ty.createcraftedbeginning.util.Helpers;

import java.util.List;
import java.util.Map;

public class AirtightEncasedPipeBlockEntity extends SmartBlockEntity {
    private CompressAirPipeTransportBehaviour transportBehaviour;

    public AirtightEncasedPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        transportBehaviour = new CompressAirPipeTransportBehaviour(this);
        behaviours.add(transportBehaviour);
        registerAwardables(behaviours, CCBAdvancements.AIRTIGHT_ENCASED_PIPE_SEALED);
        registerAwardables(behaviours, CCBAdvancements.AIRTIGHT_ENCASED_PIPE_EXPLOSION);
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null) {
            return;
        }

        if (level.getGameTime() % 20 == 0) {
            int count = 0;
            for (Direction direction : Iterate.directions) {
                if (!getBlockState().getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(direction))) {
                    count++;
                }
            }
            if (count == 6) {
                awardSealed();
            } else if (count == 0) {
                transportBehaviour.testIfOverloaded();
            }
        }
    }

    private void registerAwardables(List<BlockEntityBehaviour> behaviours, CCBAdvancement... advancements) {
        for (BlockEntityBehaviour behaviour : behaviours) {
            if (behaviour instanceof AdvancementBehaviour ab) {
                ab.add(advancements);
                return;
            }
        }
        behaviours.add(new AdvancementBehaviour(this, advancements));
    }

    private void awardSealed() {
        AdvancementBehaviour behaviour = getBehaviour(AdvancementBehaviour.TYPE);
        if (behaviour != null) {
            behaviour.awardPlayer(CCBAdvancements.AIRTIGHT_ENCASED_PIPE_SEALED);
        }
    }

    private void awardExplosion() {
        AdvancementBehaviour behaviour = getBehaviour(AdvancementBehaviour.TYPE);
        if (behaviour != null) {
            behaviour.awardPlayer(CCBAdvancements.AIRTIGHT_ENCASED_PIPE_EXPLOSION);
        }
    }

    class CompressAirPipeTransportBehaviour extends FluidTransportBehaviour {
        public CompressAirPipeTransportBehaviour(SmartBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canPullFluidFrom(FluidStack fluid, BlockState state, Direction direction) {
            return fluid.is(CCBTags.commonFluidTag("compressed_air"));
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            BlockState otherState = getWorld().getBlockState(worldPosition.relative(direction));
            Block otherBlock = otherState.getBlock();

            if (!(otherBlock instanceof AirBlock || Helpers.isValidAirtightComponents(otherBlock))) {
                return false;
            }

            return AirtightEncasedPipeBlock.isPipe(state) && state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(direction));
        }

        @Override
        public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction direction) {
            return AttachmentTypes.NONE;
        }

        private void testIfOverloaded() {
            if (level == null) {
                return;
            }

            float fastSpeed = AllConfigs.server().kinetics.fastSpeed.getF();
            float totalPressure = 0;
            float explosionPower = CCBConfig.server().compressedAir.explosionPower.getF();

            for (Map.Entry<Direction, PipeConnection> entry : interfaces.entrySet()) {
                Couple<Float> pressure = entry.getValue().getPressure();
                float absPressure = Mth.abs(pressure.getFirst() - pressure.getSecond());
                totalPressure += absPressure;
            }

            if (fastSpeed * 2 <= totalPressure * 5 / 6f) {
                BlockPos pos = blockEntity.getBlockPos();
                level.destroyBlock(pos, false);
                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, explosionPower * explosionPower, Level.ExplosionInteraction.NONE);

                awardExplosion();
            }
        }
    }
}
