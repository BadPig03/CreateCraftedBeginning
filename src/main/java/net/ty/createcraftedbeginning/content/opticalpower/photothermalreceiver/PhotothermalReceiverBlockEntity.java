package net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class PhotothermalReceiverBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private static final double RENDER_BOUNDS_PADDING = 0.001;

    private final PhotothermalReceiverController controller;

    public PhotothermalReceiverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        controller = new PhotothermalReceiverController(this);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        controller.tickServer();
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
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return PhotothermalReceiverTooltip.addToGoggleTooltip(tooltip, controller);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return PhotothermalReceiverPort.getLaserShape().bounds().move(worldPosition).inflate(RENDER_BOUNDS_PADDING);
    }

    public void receiveLaser(BlockPos emitterPos, PhotothermalReceiverPort port, int powerLp) {
        controller.receiveLaser(emitterPos, port, powerLp);
    }

    public boolean isPortIlluminated(PhotothermalReceiverPort port) {
        return controller.isPortIlluminated(port);
    }
}
