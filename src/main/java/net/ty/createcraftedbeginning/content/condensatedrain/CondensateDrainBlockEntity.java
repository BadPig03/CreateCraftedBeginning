package net.ty.createcraftedbeginning.content.condensatedrain;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ty.createcraftedbeginning.content.airtightengine.GasControllerData;
import net.ty.createcraftedbeginning.content.airtighttank.AirtightTankBlock;
import net.ty.createcraftedbeginning.content.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import java.lang.ref.WeakReference;
import java.util.List;

import static net.ty.createcraftedbeginning.content.airtightengine.GasControllerData.MAX_LEVEL;

public class CondensateDrainBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int PENALTY_COOLDOWN = 100;
    private static final int MAX_CAPACITY = 2000;

    private SmartFluidTankBehaviour tankBehaviour;
    private WeakReference<AirtightTankBlockEntity> source;

    public CondensateDrainBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        source = new WeakReference<>(null);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CCBBlockEntities.CONDENSATE_DRAIN.get(), (be, context) -> be.tankBehaviour.getCapability());
    }

    private AirtightTankBlockEntity getTank() {
        if (level == null) {
            return null;
        }

        AirtightTankBlockEntity tank = source.get();
        if (tank == null || tank.isRemoved()) {
            source = new WeakReference<>(null);
            Direction facing = CondensateDrainBlock.getFacing(getBlockState());
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

    public GasControllerData getGasController() {
        AirtightTankBlockEntity controller = getTankController();
        if (controller == null) {
            return null;
        }

        return controller.gasController;
    }

    private AirtightTankBlockEntity getTankController() {
        AirtightTankBlockEntity tank = getTank();
        if (tank == null) {
            return null;
        }

        return tank.getControllerBE();
    }

    private int getCondensationRate() {
        AirtightTankBlockEntity controller = getTankController();
        GasControllerData gasController = getGasController();
        if (gasController == null || controller == null || level == null) {
            return 0;
        }

        boolean isPassive = gasController.getPassive();
        int currentLevel = gasController.getCurrentLevel(controller.getTotalTankSize());
        if (currentLevel == 0 || isPassive) {
            return 0;
        }

        int drains = gasController.getAttachedDrains();
        int engines = gasController.getAttachedEngines();
        if (drains == 0 || engines == 0) {
            return 0;
        }

        return Mth.ceil(4f * currentLevel / drains);
    }

    private int getCondensationAmount() {
        SmartFluidTank tank = tankBehaviour.getPrimaryHandler();
        return tank.getFluidAmount();
    }

    private void updateCondensate() {
        AirtightTankBlockEntity controller = getTankController();
        GasControllerData gasController = getGasController();
        if (gasController == null || controller == null || level == null) {
            return;
        }

        boolean isPassive = gasController.getPassive();
        if (isPassive) {
            return;
        }

        SmartFluidTankBehaviour.InternalFluidHandler fluidHandler = (SmartFluidTankBehaviour.InternalFluidHandler) tankBehaviour.getCapability();
        SmartFluidTank tank = tankBehaviour.getPrimaryHandler();

        int currentLevel = gasController.getCurrentLevel(controller.getTotalTankSize());
        int currentCondensation = gasController.getCondensationLevel();
        if (currentLevel == 0) {
            if (!tank.isEmpty()) {
                return;
            }
            if (currentCondensation == 0) {
                int cooldown = gasController.getCondensatePenaltyCooldown();
                if (cooldown == 0) {
                    gasController.setCondensatePenaltyCooldown(PENALTY_COOLDOWN);
                    gasController.setCondensationLevel(1);
                    gasController.setCondensateSuccessCount(0);

                    controller.notifyUpdate();
                }
            }
            return;
        }

        int amount = getCondensationRate() * 10;
        FluidStack waterFluidStack = new FluidStack(Fluids.WATER, amount);

        if (tank.getSpace() < amount) {
            if (gasController.getCondensatePenaltyCooldown() == 0) {
                gasController.setCondensationLevel(Mth.clamp(currentCondensation - 1, 0, MAX_LEVEL));
                gasController.setCondensatePenaltyCooldown(PENALTY_COOLDOWN);
                gasController.setCondensateSuccessCount(0);
            }
        } else {
            int successCount = gasController.getCondensateSuccessCount();
            gasController.setCondensateSuccessCount(successCount + 1);
            if (successCount >= PENALTY_COOLDOWN / 5) {
                gasController.setCondensateSuccessCount(0);
                gasController.setCondensationLevel(Mth.clamp(currentCondensation + 1, 0, MAX_LEVEL));
            }
        }

        fluidHandler.forceFill(waterFluidStack, IFluidHandler.FluidAction.EXECUTE);

        setChanged();
        controller.notifyUpdate();
    }

    public SmartFluidTankBehaviour getTankBehaviour() {
        return tankBehaviour;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CCBLang.translate("gui.goggles.condensate_drain").forGoggles(tooltip);
        CCBLang.translate("gui.goggles.condensate_drain.condensation_capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.builder().add(CCBLang.number(getCondensationAmount()).translate("gui.goggles.unit.milli_buckets").style(ChatFormatting.BLUE)).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(MAX_CAPACITY).add(CCBLang.translate("gui.goggles.unit.milli_buckets")).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.goggles.condensate_drain.condensation_rate").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate("gui.goggles.condensate_drain.milli_buckets_per_second", CCBLang.number(getCondensationRate() * 20)).style(ChatFormatting.BLUE).forGoggles(tooltip, 1);
        return true;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartFluidTankBehaviour.single(this, MAX_CAPACITY).forbidInsertion();
        behaviours.add(tankBehaviour);
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null) {
            return;
        }

        if (level.getGameTime() % 10 == 0) {
            updateCondensate();
        }

        if (!level.isClientSide && level.getGameTime() % 20 == 0) {
            AirtightTankBlock.updateTankState(level, worldPosition.relative(CondensateDrainBlock.getFacing(getBlockState())));
        }
    }
}