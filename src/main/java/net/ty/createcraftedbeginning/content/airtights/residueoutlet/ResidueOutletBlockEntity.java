package net.ty.createcraftedbeginning.content.airtights.residueoutlet;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ResidueOutletBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private static final int LAZY_TICK_RATE = 20;
    private static final String COMPOUND_KEY_INVENTORY = "Inventory";

    private final IItemHandler itemCapability;
    private final ResidueOutletInventory inventory;

    private SmartFluidTankBehaviour fluidTankBehaviour;

    public ResidueOutletBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inventory = new ResidueOutletInventory(this);
        itemCapability = inventory.getExtractionCapability();
        setLazyTickRate(LAZY_TICK_RATE);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(FluidHandler.BLOCK, CCBBlockEntities.RESIDUE_OUTLET.get(), (outlet, context) -> outlet.fluidTankBehaviour.getCapability());
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.RESIDUE_OUTLET.get(), (outlet, context) -> outlet.itemCapability);
    }

    public static int getMaxCapacity() {
        return CCBConfig.server().airtights.residueOutletCapacity.get() * FluidType.BUCKET_VOLUME;
    }

    private static void addItemTooltip(List<Component> tooltip, ItemStack item) {
        CCBLang.text("").add(Component.translatable(item.getDescriptionId()).withStyle(ChatFormatting.GRAY)).add(CCBLang.text(" x" + item.getCount()).style(ChatFormatting.GREEN)).forGoggles(tooltip, 1);
    }

    private static void addFluidTooltip(List<Component> tooltip, FluidStack fluid) {
        LangBuilder unit = CCBLang.translate("gui.unit.milli_buckets");
        CCBLang.fluidName(fluid).add(CCBLang.text(" ")).style(ChatFormatting.GRAY).add(CCBLang.number(fluid.getAmount()).add(unit).style(ChatFormatting.BLUE)).forGoggles(tooltip, 1);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ItemStack item = inventory.getStackInSlot(0);
        FluidStack fluid = fluidTankBehaviour.getPrimaryHandler().getFluidInTank(0);
        if (item.isEmpty() && fluid.isEmpty()) {
            return false;
        }

        CCBLang.translate("gui.residue_outlet.header").forGoggles(tooltip);
        if (!item.isEmpty()) {
            addItemTooltip(tooltip, item);
        }
        if (!fluid.isEmpty()) {
            addFluidTooltip(tooltip, fluid);
        }
        return true;
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

        BlockState state = getBlockState();
        if (state.getBlock() instanceof ResidueOutletBlock outlet && outlet.canSurvive(state, level, getBlockPos())) {
            return;
        }

        level.destroyBlock(worldPosition, true);
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.put(COMPOUND_KEY_INVENTORY, inventory.serializeNBT(provider));
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (!compoundTag.contains(COMPOUND_KEY_INVENTORY)) {
            return;
        }

        inventory.deserializeNBT(provider, compoundTag.getCompound(COMPOUND_KEY_INVENTORY));
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    public @Nullable ResidueInsertionPlan createResidueInsertionPlan(FluidStack fluidStack, ItemStack itemStack, int maxAmount) {
        boolean hasFluid = !fluidStack.isEmpty();
        boolean hasItem = !itemStack.isEmpty();
        if (maxAmount <= 0 || hasFluid == hasItem) {
            return null;
        }

        return hasFluid ? createFluidInsertionPlan(fluidStack, maxAmount) : createItemInsertionPlan(itemStack, maxAmount);
    }

    private @Nullable ResidueInsertionPlan createFluidInsertionPlan(FluidStack fluid, int maxAmount) {
        int plannedAmount = insertResidueFluid(fluid.copyWithAmount(maxAmount), FluidAction.SIMULATE);
        if (plannedAmount <= 0) {
            return null;
        }

        FluidStack plannedFluid = fluid.copyWithAmount(plannedAmount);
        return new ResidueInsertionPlan(plannedAmount, () -> insertResidueFluid(plannedFluid, FluidAction.EXECUTE));
    }

    private @Nullable ResidueInsertionPlan createItemInsertionPlan(ItemStack item, int maxUnits) {
        int plannedUnits = Math.min(maxUnits, inventory.getItemInsertionCapacityUnits(item));
        if (plannedUnits <= 0) {
            return null;
        }

        ItemStack plannedItem = item.copyWithCount(1);
        return new ResidueInsertionPlan(plannedUnits, () -> inventory.addPartialItemUnits(plannedUnits, plannedItem));
    }

    public int insertResidueFluid(FluidStack fluidStack, FluidAction action) {
        return fluidTankBehaviour.getPrimaryHandler().fill(fluidStack, action);
    }

    ResidueOutletInventory getInventory() {
        return inventory;
    }

    public record ResidueInsertionPlan(int plannedAmount, IntSupplier insertion) {
        public ResidueInsertionPlan {
            if (plannedAmount <= 0) {
                throw new IllegalArgumentException("A residue insertion plan must contain a positive amount.");
            }
            Objects.requireNonNull(insertion, "insertion");
        }

        public int commit() {
            return Math.clamp(insertion.getAsInt(), 0, plannedAmount);
        }
    }
}
