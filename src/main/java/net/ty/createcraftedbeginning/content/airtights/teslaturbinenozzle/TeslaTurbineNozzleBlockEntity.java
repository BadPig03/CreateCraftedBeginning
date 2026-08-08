package net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock.TeslaTurbineStructuralPosition;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineNozzleBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private TeslaTurbineBlockEntity turbine;

    public TeslaTurbineNozzleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.TESLA_TURBINE_NOZZLE.get(), TeslaTurbineNozzleBlockEntity::getGasCapability);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CCBLang.translate("gui.tesla_turbine_nozzle.header").forGoggles(tooltip);
        CCBLang.translate("gui.tesla_turbine_nozzle.flow_direction").style(ChatFormatting.GRAY).forGoggles(tooltip);

        boolean isClockwise = getBlockState().getValue(TeslaTurbineNozzleBlock.CLOCKWISE);
        String directionKey = isClockwise ? "gui.tesla_turbine_nozzle.flow_direction.clockwise" : "gui.tesla_turbine_nozzle.flow_direction.counter_clockwise";
        CCBLang.translate(directionKey).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        return true;
    }

    private @Nullable IGasHandler getGasCapability(@Nullable Direction direction) {
        if (direction != getBlockState().getValue(TeslaTurbineNozzleBlock.FACING)) {
            return null;
        }

        if (turbine == null || turbine.isRemoved()) {
            turbine = getTurbine();
        }
        if (turbine == null) {
            return null;
        }

        boolean isClockwise = getBlockState().getValue(TeslaTurbineNozzleBlock.CLOCKWISE);
        return turbine.getCore().createGasHandler(isClockwise);
    }

    private @Nullable TeslaTurbineBlockEntity getTurbine() {
        if (level == null) {
            return null;
        }

        Direction inwardDirection = getBlockState().getValue(TeslaTurbineNozzleBlock.FACING).getOpposite();
        BlockPos structurePos = worldPosition.relative(inwardDirection);
        BlockState structureState = level.getBlockState(structurePos);
        if (!(structureState.getBlock() instanceof TeslaTurbineStructuralBlock)) {
            return null;
        }

        Axis structureAxis = structureState.getValue(TeslaTurbineStructuralBlock.AXIS);
        if (inwardDirection.getAxis() == structureAxis) {
            return null;
        }

        TeslaTurbineStructuralPosition structurePosition = structureState.getValue(TeslaTurbineStructuralBlock.STRUCTURAL_POSITION);
        if (TeslaTurbineStructuralPosition.isMid(structurePosition)) {
            return null;
        }

        if (TeslaTurbineNozzleBlock.hasOtherNozzle(level, structurePos, worldPosition, structureAxis, structurePosition)) {
            return null;
        }

        BlockPos masterPos = TeslaTurbineStructuralBlock.getMaster(structurePos, structureState);
        return level.getBlockEntity(masterPos) instanceof TeslaTurbineBlockEntity master ? master : null;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof TeslaTurbineNozzleBlock nozzle)) {
            return;
        }

        if (level.getBlockTicks().hasScheduledTick(worldPosition, nozzle)) {
            return;
        }

        level.scheduleTick(worldPosition, nozzle, 1);
    }
}
