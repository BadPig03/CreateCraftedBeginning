package net.ty.createcraftedbeginning.content.airtightintakeport;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ty.createcraftedbeginning.advancement.AdvancementBehaviour;
import net.ty.createcraftedbeginning.advancement.CCBAdvancement;
import net.ty.createcraftedbeginning.advancement.CCBAdvancements;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtightpump.AirtightPumpBlockEntity;
import net.ty.createcraftedbeginning.content.compressedair.CompressedAirTankBehaviour;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBFluids;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;

import java.util.List;

public class AirtightIntakePortBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    protected CompressedAirTankBehaviour tankBehaviour;

    public AirtightIntakePortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CCBBlockEntities.AIRTIGHT_INTAKE_PORT.get(), (be, context) -> be.tankBehaviour.getCapability());
    }

    private boolean canProduceCompressedAir() {
        if (level == null || isWaterlogged()) {
            return false;
        }

        BlockPos pumpPos = worldPosition.relative(getBlockState().getValue(AirtightIntakePortBlock.FACING).getOpposite());
        BlockState pumpState = level.getBlockState(pumpPos);
        Block pumpBlock = pumpState.getBlock();

        Block airBlock = level.getBlockState(worldPosition.relative(getBlockState().getValue(AirtightIntakePortBlock.FACING))).getBlock();
        if (!(airBlock instanceof AirBlock) || !(pumpBlock instanceof AirtightPumpBlock)) {
            return false;
        }

        return isConnectionValid(pumpPos) && isPumpFastEnough(pumpPos);
    }

    private boolean intakePortTooltip(List<Component> tooltip, IFluidHandler handler) {
        if (level == null || handler == null || handler.getTanks() == 0) {
            return false;
        }

        BlockPos pos = worldPosition.relative(getBlockState().getValue(AirtightIntakePortBlock.FACING).getOpposite());
        boolean invalidConnection = !isConnectionValid(pos);
        boolean notFastEnough = !isPumpFastEnough(pos);
        boolean isBlockInvalid = !isBlockValid(pos);
        boolean isFrontObstructed = isFrontObstructed();
        boolean isWaterlogged = isWaterlogged();

        CCBLang.translate("gui.goggles.intake_port").forGoggles(tooltip);

        CCBLang.builder().add(CCBLang.translate("gui.goggles.intake_port.production_rate")).style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.builder().add(CCBLang.number(getProductionRate(pos) * 20).add(CCBLang.translate("gui.goggles.unit.milli_buckets_per_second")).style(ChatFormatting.AQUA)).forGoggles(tooltip, 1);

        if (invalidConnection || notFastEnough || isBlockInvalid || isFrontObstructed || isWaterlogged) {
            tooltip.add(CommonComponents.EMPTY);
            CCBLang.translate("gui.goggles.warning").style(ChatFormatting.GOLD).forGoggles(tooltip);
        }

        if (isBlockInvalid && invalidConnection) {
            MutableComponent hint = CCBLang.translateDirect("gui.goggles.intake_port.no_connection");
            List<Component> cutString = TooltipHelper.cutTextComponent(hint, FontHelper.Palette.GRAY_AND_WHITE);
            for (Component component : cutString) {
                CCBLang.builder().add(component.copy()).forGoggles(tooltip);
            }
            return true;
        }

        if (isWaterlogged) {
            MutableComponent hint = CCBLang.translateDirect("gui.goggles.intake_port.waterlogged");
            List<Component> cutString = TooltipHelper.cutTextComponent(hint, FontHelper.Palette.GRAY_AND_WHITE);
            for (Component component : cutString) {
                CCBLang.builder().add(component.copy()).forGoggles(tooltip);
            }
            return true;
        }

        if (isFrontObstructed) {
            MutableComponent hint = CCBLang.translateDirect("gui.goggles.intake_port.face_obstructed");
            List<Component> cutString = TooltipHelper.cutTextComponent(hint, FontHelper.Palette.GRAY_AND_WHITE);
            for (Component component : cutString) {
                CCBLang.builder().add(component.copy()).forGoggles(tooltip);
            }
            return true;
        }

        if (invalidConnection) {
            MutableComponent hint = CCBLang.translateDirect("gui.goggles.intake_port.invalid_connection", I18n.get(level.getBlockState(pos).getBlock().getDescriptionId()));
            List<Component> cutString = TooltipHelper.cutTextComponent(hint, FontHelper.Palette.GRAY_AND_WHITE);
            for (Component component : cutString) {
                CCBLang.builder().add(component.copy()).forGoggles(tooltip);
            }
            return true;
        }

        if (notFastEnough) {
            MutableComponent hint = CCBLang.translateDirect("gui.goggles.intake_port.not_fast_enough", I18n.get(level.getBlockState(pos).getBlock().getDescriptionId()));
            List<Component> cutString = TooltipHelper.cutTextComponent(hint, FontHelper.Palette.GRAY_AND_WHITE);
            for (Component component : cutString) {
                CCBLang.builder().add(component.copy()).forGoggles(tooltip);
            }
        }

        return true;
    }

    private boolean isBlockValid(BlockPos pos) {
        if (level == null) {
            return false;
        }

        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof PumpBlockEntity;
    }

    private boolean isConnectionValid(BlockPos pos) {
        if (level == null) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AirtightPumpBlock)) {
            return false;
        }

        return state.getValue(AirtightPumpBlock.FACING).getOpposite() == level.getBlockState(worldPosition).getValue(AirtightIntakePortBlock.FACING);
    }

    private boolean isFrontObstructed() {
        if (level == null) {
            return true;
        }

        Block airBlock = level.getBlockState(worldPosition.relative(getBlockState().getValue(AirtightIntakePortBlock.FACING))).getBlock();
        return !(airBlock instanceof AirBlock);
    }

    private boolean isPumpFastEnough(BlockPos pos) {
        if (level == null || !(level.getBlockEntity(pos) instanceof AirtightPumpBlockEntity pump)) {
            return false;
        }
        return Mth.abs(pump.getSpeed()) >= IRotate.SpeedLevel.MEDIUM.getSpeedValue();
    }

    private boolean isWaterlogged() {
        return getBlockState().getValue(AirtightIntakePortBlock.WATERLOGGED);
    }

    private float getAmountPerTick() {
        return CCBConfig.server().compressedAir.airAmountFromIntakePort.getF();
    }

    private int getProductionRate(BlockPos pos) {
        if (level == null || !(level.getBlockEntity(pos) instanceof AirtightPumpBlockEntity pump)) {
            return 0;
        }
        if (!isPumpFastEnough(pos) || !isConnectionValid(pos) || isFrontObstructed() || isWaterlogged()) {
            return 0;
        }
        return Math.round((Mth.abs(pump.getSpeed()) * getAmountPerTick()));
    }

    private void generateCompressedAir() {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockPos pumpPos = worldPosition.relative(getBlockState().getValue(AirtightIntakePortBlock.FACING).getOpposite());
        int amount = getProductionRate(pumpPos);
        if (amount == 0) {
            return;
        }

        FluidStack airStack = new FluidStack(CCBFluids.MEDIUM_PRESSURE_COMPRESSED_AIR.get(), amount);
        CompressedAirTankBehaviour.InternalFluidHandler fluidHandler = (CompressedAirTankBehaviour.InternalFluidHandler) tankBehaviour.getCapability();

        int space = fluidHandler.forceFill(airStack, IFluidHandler.FluidAction.SIMULATE);
        if (space <= 0) {
            return;
        }

        fluidHandler.forceFill(airStack.copyWithAmount(space), IFluidHandler.FluidAction.EXECUTE);
        award();

        if (!(level.getBlockEntity(pumpPos) instanceof AirtightPumpBlockEntity be)) {
            return;
        }

        if (Mth.abs(be.getSpeed()) < AllConfigs.server().kinetics.maxRotationSpeed.get()) {
            return;
        }
        awardMaxed();
    }

    private void spawnAirParticle() {
        if (level == null || !level.isClientSide) {
            return;
        }

        BlockState state = getBlockState();
        Direction facing = state.getValue(AirtightIntakePortBlock.FACING).getOpposite();
        Vec3 center = VecHelper.getCenterOf(worldPosition);
        Vec3 spawnPos = center.add(Vec3.atLowerCornerOf(facing.getOpposite().getNormal()).scale(0.8));
        Vec3 motion = Vec3.atLowerCornerOf(facing.getNormal()).scale(0.05).add((level.random.nextDouble() - 0.5) * 0.02, (level.random.nextDouble() - 0.5) * 0.02, (level.random.nextDouble() - 0.5) * 0.02);

        level.addParticle((SimpleParticleType) CCBParticleTypes.COMPRESSED_AIR_INTAKE.get(), spawnPos.x, spawnPos.y, spawnPos.z, motion.x, motion.y, motion.z);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) {
            return false;
        }
        return intakePortTooltip(tooltip, level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition, null));
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        int capacity = CCBConfig.server().compressedAir.airtightTankCapacity.get() * 500;
        tankBehaviour = CompressedAirTankBehaviour.single(this, capacity).forbidInsertion();
        behaviours.add(tankBehaviour);
        registerAwardables(behaviours, CCBAdvancements.AIRTIGHT_INTAKE_PORT);
        registerAwardables(behaviours, CCBAdvancements.AIRTIGHT_INTAKE_PORT_MAXED);
    }

    @Override
    public void initialize() {
        super.initialize();
        sendData();
        if (level != null && level.isClientSide) {
            invalidateRenderBoundingBox();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        if (!canProduceCompressedAir()) {
            return;
        }

        if (tankBehaviour.getPrimaryHandler().getSpace() > 0) {
            spawnAirParticle();
        }

        generateCompressedAir();
        sendData();
    }

    private void registerAwardables(List<BlockEntityBehaviour> behaviours, CCBAdvancement... advancements) {
        for (BlockEntityBehaviour behaviour : behaviours) {
            if (behaviour instanceof AdvancementBehaviour ab) {
                ab.add(advancements);
                return;
            }
        }
        behaviours.add(new AdvancementBehaviour(this, advancements));
    }

    private void award() {
        AdvancementBehaviour behaviour = getBehaviour(AdvancementBehaviour.TYPE);
        if (behaviour != null) {
            behaviour.awardPlayer(CCBAdvancements.AIRTIGHT_INTAKE_PORT);
        }
    }

    private void awardMaxed() {
        AdvancementBehaviour behaviour = getBehaviour(AdvancementBehaviour.TYPE);
        if (behaviour != null) {
            behaviour.awardPlayer(CCBAdvancements.AIRTIGHT_INTAKE_PORT_MAXED);
        }
    }
}
