package net.ty.createcraftedbeginning.content.airtights.residueoutlet;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlock;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ResidueOutletBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private static final int LAZY_TICK_RATE = 20;
    private static final String COMPOUND_KEY_INVENTORY = "Inventory";

    private final IItemHandlerModifiable itemCapability;
    private final ResidueOutletInventory inventory;

    private SmartFluidTankBehaviour fluidTankBehaviour;

    public ResidueOutletBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inventory = new ResidueOutletInventory(this);
        itemCapability = new CombinedInvWrapper(inventory);
        setLazyTickRate(LAZY_TICK_RATE);
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(FluidHandler.BLOCK, CCBBlockEntities.RESIDUE_OUTLET.get(), (be, context) -> be.fluidTankBehaviour.getCapability());
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.RESIDUE_OUTLET.get(), (be, context) -> be.itemCapability);
    }

    public static int getMaxCapacity() {
        return AllConfigs.server().fluids.fluidTankCapacity.get() * 500;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CCBLang.translate("gui.goggles.residue_outlet.header").forGoggles(tooltip);
        ItemStack itemStack = inventory.getStackInSlot(0);
        if (itemStack.isEmpty()) {
            CCBLang.translate("gui.goggles.residue_outlet.item.capacity").style(ChatFormatting.GRAY).add(CCBLang.number(64).style(ChatFormatting.GOLD)).forGoggles(tooltip);
        }
        else {
            CCBLang.translate("gui.goggles.residue_outlet.item.capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CCBLang.text(Component.translatable(itemStack.getDescriptionId()).getString()).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            CCBLang.number(itemStack.getCount()).style(ChatFormatting.GOLD).add(CCBLang.text(" / ").style(ChatFormatting.GRAY)).add(CCBLang.number(itemStack.getMaxStackSize()).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        }

        tooltip.add(CommonComponents.EMPTY);
        SmartFluidTank tank = fluidTankBehaviour.getPrimaryHandler();
        FluidStack fluidStack = tank.getFluidInTank(0);
        LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
        int capacity = getMaxCapacity();
        if (fluidStack.isEmpty()) {
            CCBLang.translate("gui.goggles.residue_outlet.fluid.capacity").style(ChatFormatting.GRAY).add(CCBLang.number(capacity).add(mb).style(ChatFormatting.GOLD)).forGoggles(tooltip);
        }
        else {
            CCBLang.translate("gui.goggles.residue_outlet.fluid.capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CCBLang.fluidName(fluidStack).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            CCBLang.number(fluidStack.getAmount()).add(mb).style(ChatFormatting.GOLD).add(CCBLang.text(" / ").style(ChatFormatting.GRAY)).add(CCBLang.number(capacity).add(mb).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        }
        return true;
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        fluidTankBehaviour = SmartFluidTankBehaviour.single(this, getMaxCapacity()).forbidInsertion();
        behaviours.add(fluidTankBehaviour);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof ResidueOutletBlock outlet) || !outlet.canSurvive(state, level, getBlockPos())) {
            level.destroyBlock(worldPosition, true);
            return;
        }

        AirtightTankBlock.updateTankState(level, worldPosition.relative(ResidueOutletBlock.getFacing(state)));
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

    public SmartFluidTankBehaviour getFluidTankBehaviour() {
        return fluidTankBehaviour;
    }

    public ResidueOutletInventory getInventory() {
        return inventory;
    }
}