package net.ty.createcraftedbeginning.content.airtights.airtightengine;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.util.Helpers;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;

public class AirtightEngineBlockEntity extends GeneratingKineticBlockEntity implements IHaveGoggleInformation {
    public static final int BASE_ROTATION_SPEED = 8;
    public static final float DELTA_TIME = 0.01f;

    private WeakReference<AirtightTankBlockEntity> source;
    private boolean isClockwise;
    private float pistonPhase;

    public AirtightEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        source = new WeakReference<>(null);
        isClockwise = true;
        pistonPhase = 0;
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            pistonPhase += Mth.abs(getSpeed()) * DELTA_TIME;
            if (pistonPhase > 2 * Math.PI) {
                pistonPhase -= (float) (2 * Math.PI);
            }
        }

        if (!isOverStressed()) {
            updateGeneratedRotation();
        }

        if (!level.isClientSide && level.getGameTime() % Helpers.getActualTickRate(level) == 0) {
            AirtightTankBlock.updateTankState(level, worldPosition.relative(AirtightEngineBlock.getFacing(getBlockState())));
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CCBLang.translate("gui.goggles.airtight_engine").forGoggles(tooltip);
        CCBLang.translate("gui.goggles.stress_impact").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.number(BASE_ROTATION_SPEED * getCombinedCapacity()).translate("gui.goggles.unit.stress").style(ChatFormatting.AQUA).space().add(CCBLang.translate("gui.goggles.at_current_speed").style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);

        return true;
    }

    @Override
    public void initialize() {
        super.initialize();
        updateGeneratedRotation();
    }

    @Override
    public float calculateAddedStressCapacity() {
        float speedModifier = getSpeedModifier();
        if (speedModifier < 0) {
            float capacity = 512f;
            this.lastCapacityProvided = capacity;
            return capacity;
        }
        return super.calculateAddedStressCapacity();
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        if (clientPacket) {
            return;
        }
        compound.putBoolean("Clockwise", isClockwise);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (!clientPacket && compound.contains("Clockwise")) {
            isClockwise = compound.getBoolean("Clockwise");
        }
    }

    @Override
    public float getGeneratedSpeed() {
        int speedDirection = isClockwise ? 1 : -1;
        return BASE_ROTATION_SPEED * Mth.abs(getSpeedModifier()) * speedDirection;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
    }

    @Override
    public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
        BlockPos.betweenClosedStream(new BlockPos(-1, -1, -1), new BlockPos(1, 1, 1)).forEach(offset -> {
            if (offset.distSqr(BlockPos.ZERO) == 2) {
                neighbours.add(worldPosition.offset(offset));
            }
        });
        return neighbours;
    }

    public void toggleDirection() {
        isClockwise = !isClockwise;
        updateGeneratedRotation();
        setChanged();
    }

    private @Nullable AirtightTankBlockEntity getTank() {
        if (level == null) {
            return null;
        }

        AirtightTankBlockEntity tank = source.get();
        if (tank == null || tank.isRemoved()) {
            source = new WeakReference<>(null);
            Direction facing = AirtightEngineBlock.getFacing(getBlockState());
            BlockEntity be = level.getBlockEntity(worldPosition.relative(facing));
            if (be instanceof AirtightTankBlockEntity tankBe) {
                source = new WeakReference<>(tank = tankBe);
            }
        }

        if (tank == null) {
            return null;
        }
        return tank.getControllerBE();
    }

    private @Nullable GasControllerData getGasController() {
        AirtightTankBlockEntity controller = getTankController();
        if (controller == null) {
            return null;
        }

        return controller.gasController;
    }

    private @Nullable AirtightTankBlockEntity getTankController() {
        AirtightTankBlockEntity tank = getTank();
        if (tank == null) {
            return null;
        }

        return tank.getControllerBE();
    }

    private float getSpeedModifier() {
        GasControllerData gasController = getGasController();
        AirtightTankBlockEntity controller = getTankController();
        if (gasController == null || controller == null) {
            return 0;
        }

        int attachedEngines = gasController.getAttachedEngines();
        if (attachedEngines <= 0) {
            return 0;
        }

        int size = controller.getTotalTankSize();
        int level = gasController.getCurrentLevel(size);
        return (float) level / attachedEngines;
    }

    private float getCombinedCapacity() {
        float speedModifier = getSpeedModifier();
        if (speedModifier < 0) {
            return 512f;
        }
        double capacity = BlockStressValues.getCapacity(CCBBlocks.AIRTIGHT_ENGINE_BLOCK.get());
        return (float) (speedModifier * capacity);
    }

    public float getPistonPhase() {
        return pistonPhase;
    }
}
