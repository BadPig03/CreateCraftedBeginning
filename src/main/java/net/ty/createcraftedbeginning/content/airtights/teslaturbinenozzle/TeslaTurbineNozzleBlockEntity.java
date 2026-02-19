package net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock.TeslaTurbineStructuralPosition;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class TeslaTurbineNozzleBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private final Direction lastDirection;
    private TeslaTurbineBlockEntity turbine;

    public TeslaTurbineNozzleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        turbine = null;
        lastDirection = state.getValue(TeslaTurbineNozzleBlock.FACING);
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.TESLA_TURBINE_NOZZLE.get(), TeslaTurbineNozzleBlockEntity::getGasCapability);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CCBLang.translate("gui.goggles.tesla_turbine_nozzle.header").forGoggles(tooltip);
        CCBLang.translate("gui.goggles.tesla_turbine_nozzle.flow_direction").style(ChatFormatting.GRAY).forGoggles(tooltip);
        if (getBlockState().getValue(TeslaTurbineNozzleBlock.CLOCKWISE)) {
            CCBLang.translate("gui.goggles.tesla_turbine_nozzle.flow_direction.clockwise").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        }
        else {
            CCBLang.translate("gui.goggles.tesla_turbine_nozzle.flow_direction.counter_clockwise").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        }
        return true;
    }

    private @Nullable IGasHandler getGasCapability(Direction direction) {
        if (direction != getBlockState().getValue(TeslaTurbineNozzleBlock.FACING)) {
            return null;
        }

        if (turbine == null || turbine.isRemoved()) {
            turbine = getTurbine();
            if (turbine == null) {
                return null;
            }
        }

        return turbine.getCore().createGasHandler(getBlockState().getValue(TeslaTurbineNozzleBlock.CLOCKWISE));
    }

    private @Nullable TeslaTurbineBlockEntity getTurbine() {
        if (level == null) {
            return null;
        }

        Direction nozzleFacing = getBlockState().getValue(TeslaTurbineNozzleBlock.FACING).getOpposite();
        BlockPos structuralPos = worldPosition.relative(nozzleFacing);
        BlockState structuralState = level.getBlockState(structuralPos);
        if (!(structuralState.getBlock() instanceof TeslaTurbineStructuralBlock)) {
            return null;
        }

        Axis structuralAxis = structuralState.getValue(TeslaTurbineStructuralBlock.AXIS);
        if (nozzleFacing.getAxis() == structuralAxis) {
            return null;
        }

        TeslaTurbineStructuralPosition structuralPosition = structuralState.getValue(TeslaTurbineStructuralBlock.STRUCTURAL_POSITION);
        if (TeslaTurbineStructuralPosition.isMid(structuralPosition)) {
            return null;
        }

        Set<Direction> directionsToCheck = TeslaTurbineStructuralPosition.getPossiblePosition(structuralPosition, structuralAxis);
        for (Direction direction : directionsToCheck) {
            BlockPos candidatePos = structuralPos.relative(direction);
            if (candidatePos.equals(worldPosition)) {
                continue;
            }

            BlockState candidateState = level.getBlockState(candidatePos);
            if (!(candidateState.getBlock() instanceof TeslaTurbineNozzleBlock)) {
                continue;
            }

            return null;
        }

        BlockPos masterPos = TeslaTurbineStructuralBlock.getMaster(structuralPos, structuralState);
        return level.getBlockEntity(masterPos) instanceof TeslaTurbineBlockEntity master ? master : null;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        if (turbine == null || turbine.isRemoved()) {
            turbine = getTurbine();
        }
        if (turbine != null && lastDirection == getBlockState().getValue(TeslaTurbineNozzleBlock.FACING)) {
            return;
        }

        level.destroyBlock(worldPosition, true);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }
}
