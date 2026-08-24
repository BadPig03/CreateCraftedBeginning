package net.ty.createcraftedbeginning.content.airtights.residueoutlet;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction.Participant;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ResidueOutletBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, ResidueOutletInsertionTarget {
    private static final int LAZY_TICK_RATE = 20;

    protected final ResidueOutletInventory inventory;
    protected final ResidueOutletInsertionPlanner insertionPlanner;
    protected final ResidueOutletSerialization serialization;
    protected final ResidueOutletTooltip tooltip;

    protected SmartFluidTankBehaviour fluidTankBehaviour;

    public ResidueOutletBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inventory = new ResidueOutletInventory(this);
        insertionPlanner = new ResidueOutletInsertionPlanner(this, inventory);
        serialization = new ResidueOutletSerialization(inventory);
        tooltip = new ResidueOutletTooltip(this, inventory);
        setLazyTickRate(LAZY_TICK_RATE);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(FluidHandler.BLOCK, CCBBlockEntities.RESIDUE_OUTLET.get(), (outlet, context) -> outlet.fluidTankBehaviour.getCapability());
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.RESIDUE_OUTLET.get(), (outlet, context) -> outlet.inventory.getExtractionCapability());
    }

    public static int getMaxCapacity() {
        return CCBConfig.server().airtights.residueOutletCapacity.get() * FluidType.BUCKET_VOLUME;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltipLines, boolean isPlayerSneaking) {
        return tooltip.addToGoggleTooltip(tooltipLines);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        fluidTankBehaviour = SmartFluidTankBehaviour.single(this, getMaxCapacity()).forbidInsertion();
        behaviours.add(fluidTankBehaviour);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState outletState = getBlockState();
        if (outletState.getBlock() instanceof ResidueOutletBlock outlet && outlet.canSurvive(outletState, level, getBlockPos())) {
            return;
        }

        level.destroyBlock(worldPosition, true);
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        serialization.write(compoundTag, provider);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        serialization.read(compoundTag, provider);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    @Override
    public @Nullable ResidueInsertionPlan createResidueInsertionPlan(FluidStack fluidStack, ItemStack itemStack, int maxAmount) {
        return insertionPlanner.create(fluidStack, itemStack, maxAmount);
    }

    public int insertResidueFluid(FluidStack fluidStack, FluidAction action) {
        return fluidTankBehaviour.getPrimaryHandler().fill(fluidStack, action);
    }

    public ResidueOutletInventory getInventory() {
        return inventory;
    }

    public SmartFluidTankBehaviour getFluidTankBehaviour() {
        return fluidTankBehaviour;
    }

    public FluidStack getStoredFluid() {
        return fluidTankBehaviour.getPrimaryHandler().getFluidInTank(0);
    }

    public record ResidueInsertionPlan(int plannedAmount, Participant<?> participant) implements net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueInsertionPlan {
        public ResidueInsertionPlan {
            if (plannedAmount <= 0) {
                throw new IllegalArgumentException("A residue insertion plan must contain a positive amount.");
            }
            Objects.requireNonNull(participant, "participant");
        }

        private static <S> void addParticipant(ResourceTransaction transaction, Participant<S> participant) {
            transaction.add(participant);
        }

        @Override
        public void addTo(ResourceTransaction transaction) {
            addParticipant(transaction, participant);
        }
    }
}
