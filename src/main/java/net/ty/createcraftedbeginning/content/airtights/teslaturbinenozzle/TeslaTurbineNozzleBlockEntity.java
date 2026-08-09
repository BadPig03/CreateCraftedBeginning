package net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineNozzleBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private final TeslaTurbineNozzleConnection connection;
    private final TeslaTurbineNozzleTooltip tooltip;

    public TeslaTurbineNozzleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        connection = new TeslaTurbineNozzleConnection(this);
        tooltip = new TeslaTurbineNozzleTooltip(this);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.TESLA_TURBINE_NOZZLE.get(), TeslaTurbineNozzleBlockEntity::getGasCapability);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return this.tooltip.addToGoggleTooltip(tooltip);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void invalidate() {
        connection.invalidate();
        super.invalidate();
        invalidateCapabilities();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        connection.scheduleValidation();
    }

    private @Nullable IGasHandler getGasCapability(@Nullable Direction direction) {
        return connection.getGasCapability(direction);
    }
}
