package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.LaserBehaviour;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerConsumerBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetworkManager;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LaserEmitterBlockEntity extends SmartBlockEntity implements OpticalPowerConsumerBlockEntity, IHaveGoggleInformation {
    private LaserBehaviour laserBehaviour;
    private LaserEmitterRangeBehaviour rangeBehaviour;
    private final LaserEmitterController controller;

    public LaserEmitterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        controller = new LaserEmitterController(this);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        rangeBehaviour = new LaserEmitterRangeBehaviour(this);
        behaviours.add(rangeBehaviour);
        laserBehaviour = new LaserBehaviour(this, this::getLaserDirection, this::isLaserActive, this::getLaserRange);
        behaviours.add(laserBehaviour);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level == null || level.isClientSide) {
            return;
        }

        OpticalPowerNetworkManager.registerConsumer(level, worldPosition);
    }

    @Override
    public void tick() {
        super.tick();
        controller.tickServer(laserBehaviour);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        OpticalPowerNetworkManager.ensureConsumer(level, worldPosition);
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        controller.write(compoundTag, clientPacket);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        controller.read(compoundTag, clientPacket);
    }

    @Override
    public void applyOpticalPowerAllocation(int powerLp) {
        controller.applyAllocation(powerLp);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CCBLang.translate("gui.laser_emitter.header").forGoggles(tooltip);
        CCBLang.translate("gui.laser_emitter.power").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.number(controller.getAllocatedPowerLp()).translate("gui.unit.optical_power").style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
        return true;
    }

    @Override
    public AABB getRenderBoundingBox() {
        Vec3 extension = Vec3.atLowerCornerOf(getLaserDirection().getNormal()).scale(getLaserRange());
        return new AABB(worldPosition).expandTowards(extension).inflate(0.125);
    }

    public boolean isLaserActive() {
        return controller.getAllocatedPowerLp() > 0;
    }

    public float getBeamLength() {
        if (laserBehaviour == null) {
            return 0;
        }

        return laserBehaviour.getBeamLength();
    }

    public Direction getLaserDirection() {
        return getBlockState().getValue(DirectionalBlock.FACING);
    }

    public int getLaserRange() {
        return rangeBehaviour.getValue();
    }
}
