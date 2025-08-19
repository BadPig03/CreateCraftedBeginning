package net.ty.createcraftedbeginning.content.gasinjectionchamber;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.ty.createcraftedbeginning.content.compressedair.CompressedAirOnlyFluidTank;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GasInjectionChamberBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private final GasFluidHandler gasFluidHandler;
    protected FluidTank gasInventory;

    public GasInjectionChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        gasInventory = new CompressedAirOnlyFluidTank(4000, this::onFluidStackChanged);
        gasFluidHandler = new GasFluidHandler(gasInventory);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CCBBlockEntities.GAS_INJECTION_CHAMBER.get(), (be, context) -> context == Direction.UP ? be.gasFluidHandler : null);
    }

    protected void onFluidStackChanged(FluidStack newFluidStack) {
        if (level == null || level.isClientSide) {
            return;
        }

        setChanged();
    }

    public GasFluidHandler getGasFluidHandler() {
        return gasFluidHandler;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        sendData();
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.put("GasTank", gasInventory.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (compound.contains("GasTank")) {
            gasInventory.readFromNBT(registries, compound.getCompound("GasTank"));
        }
    }

    public record GasFluidHandler(IFluidHandler handler) implements IFluidHandler {
        @Override
        public int getTanks() {
            return handler.getTanks();
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return handler.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return handler.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return handler.isFluidValid(tank, stack);
        }

        @Override
        public int fill(@NotNull FluidStack resource, @NotNull FluidAction action) {
            return handler.fill(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(@NotNull FluidStack resource, @NotNull FluidAction action) {
            return handler.drain(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, @NotNull FluidAction action) {
            return handler.drain(maxDrain, action);
        }
    }

}
