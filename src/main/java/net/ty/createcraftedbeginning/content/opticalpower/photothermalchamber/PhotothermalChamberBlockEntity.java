package net.ty.createcraftedbeginning.content.opticalpower.photothermalchamber;

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
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.opticalpower.photothermalchamber.PhotothermalChamberSerialization.ClientSnapshot;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class PhotothermalChamberBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private static final double RENDER_BOUNDS_PADDING = 0.001;

    private final PhotothermalChamberController controller;
    private boolean clientStateRejected;

    public PhotothermalChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        controller = new PhotothermalChamberController(this);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new CCBAdvancementBehaviour(this, CCBAdvancements.RENEWABLE_BLAZE, CCBAdvancements.SOLAR_COOKER));
    }

    @Override
    public void tick() {
        super.tick();
        controller.tickServer();
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        PhotothermalChamberSerialization.write(compoundTag, clientPacket, controller);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (!clientPacket) {
            super.read(compoundTag, provider, false);
            controller.getHeatState().readSaved(compoundTag, worldPosition);
            return;
        }

        ClientSnapshot snapshot = PhotothermalChamberSerialization.readClient(compoundTag, worldPosition, !clientStateRejected);
        if (snapshot == null) {
            clientStateRejected = true;
            return;
        }

        super.read(compoundTag, provider, true);
        snapshot.restore(compoundTag, controller);
        clientStateRejected = false;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        PhotothermalChamberTooltip.addToGoggleTooltip(tooltip, controller.getOfferedFluxLm(), controller.getHeatLevel(), controller.getHeatProgress(), clientStateRejected);
        return true;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return PhotothermalChamberPort.getLaserShape().bounds().move(worldPosition).inflate(RENDER_BOUNDS_PADDING);
    }

    boolean isPortIlluminated(PhotothermalChamberPort port) {
        return !clientStateRejected && controller.isPortIlluminated(port);
    }
}
