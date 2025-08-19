package net.ty.createcraftedbeginning.content.aircompressor;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.ty.createcraftedbeginning.advancement.AdvancementBehaviour;
import net.ty.createcraftedbeginning.advancement.CCBAdvancement;
import net.ty.createcraftedbeginning.advancement.CCBAdvancements;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.compressedair.CompressedAirOnlyFluidTank;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.data.CCBTags;
import net.ty.createcraftedbeginning.recipe.PressurizationRecipe;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AirCompressorBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {
    private static final float EXPLOSION_POWER = 8.0f;
    private static boolean canPressurization;
    private final InputFluidHandler inputFluidHandler;
    private final OutputFluidHandler outputFluidHandler;
    protected FluidTank inputFluidInventory;
    protected FluidTank outputFluidInventory;

    public AirCompressorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inputFluidInventory = new CompressedAirOnlyFluidTank(4000, this::onFluidStackChanged);
        inputFluidHandler = new InputFluidHandler(inputFluidInventory);

        outputFluidInventory = new CompressedAirOnlyFluidTank(1000, this::onFluidStackChanged);
        outputFluidHandler = new OutputFluidHandler(outputFluidInventory);

        canPressurization = false;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CCBBlockEntities.AIR_COMPRESSOR.get(), (be, context) -> {
            Direction inputDir = be.getBlockState().getValue(AirCompressorBlock.HORIZONTAL_FACING).getClockWise();
            Direction outputDir = inputDir.getOpposite();
            if (context == inputDir) {
                return be.inputFluidHandler;
            } else if (context == outputDir) {
                return be.outputFluidHandler;
            }
            return null;
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        if (level.getGameTime() % 20 == 0) {
            pressurizeTheFluid();
        }

        sendData();
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.put("InputTank", inputFluidInventory.writeToNBT(registries, new CompoundTag()));
        compound.put("OutputTank", outputFluidInventory.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (compound.contains("InputTank")) {
            inputFluidInventory.readFromNBT(registries, compound.getCompound("InputTank"));
        }
        if (compound.contains("OutputTank")) {
            outputFluidInventory.readFromNBT(registries, compound.getCompound("OutputTank"));
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        registerAwardables(behaviours, CCBAdvancements.AIR_COMPRESSOR);
        registerAwardables(behaviours, CCBAdvancements.AIR_COMPRESSOR_EXPLOSION);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) {
            return false;
        }
        return airCompressorTooltip(tooltip);
    }

    private boolean isBeneathNotBreezeChamber() {
        if (level == null) {
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(getBlockPos().below());
        return !(blockEntity instanceof BreezeChamberBlockEntity);
    }

    private boolean isCompressorNotFastEnough() {
        if (level == null) {
            return true;
        }
        return !(Mth.abs(speed) >= IRotate.SpeedLevel.MEDIUM.getSpeedValue());
    }

    private float getCoolingEfficiency() {
        if (level == null) {
            return 0;
        }
        if (!(level.getBlockEntity(worldPosition.below()) instanceof BreezeChamberBlockEntity bcbe)) {
            return 0;
        }
        return switch (bcbe.getFrostLevelFromBlock()) {
            case RIMING -> .25f;
            case WANING, CHILLED -> 1;
            case GALLING -> 2;
        };
    }

    private int getPressurizeAmountPerSecond() {
        return CCBConfig.server().pressurizationAmount.get();
    }

    private int getConvertAmount() {
        if (isCompressorNotFastEnough() || isBeneathNotBreezeChamber()) {
            return 0;
        }
        return Math.round((Mth.abs(speed) * getPressurizeAmountPerSecond() * getCoolingEfficiency()));
    }

    private boolean isInputEmpty() {
        int amount = getConvertAmount();
        return inputFluidInventory.isEmpty() || inputFluidInventory.getFluidAmount() < amount;
    }

    private boolean isOutputFull() {
        if (level == null) {
            return false;
        }

        FluidStack resultFluidStack = PressurizationRecipe.getResultingFluidStack(level, inputFluidInventory.getFluid());

        if (resultFluidStack == FluidStack.EMPTY || resultFluidStack.getAmount() == 0) {
            return false;
        }

        int amount = Math.round((float) getConvertAmount() / resultFluidStack.getAmount());
        return outputFluidInventory.getSpace() == 0 || outputFluidInventory.getSpace() < amount;
    }

    private void pressurizeTheFluid() {
        if (isInputEmpty() || isOutputFull() || level == null) {
            return;
        }

        FluidStack fluidStack = inputFluidInventory.getFluid();

        if (fluidStack.is(CCBTags.CCBFluidTags.HIGH_PRESSURE_COMPRESSED_AIR.tag)) {
            awardExplosion();

            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, EXPLOSION_POWER, Level.ExplosionInteraction.NONE);
            level.destroyBlock(worldPosition, true);
            return;
        }

        FluidStack resultFluidStack = PressurizationRecipe.getResultingFluidStack(level, fluidStack);
        if (resultFluidStack.getFluid() == Fluids.EMPTY || resultFluidStack.getAmount() == 0) {
            return;
        }

        int amount = getConvertAmount();
        int convertedAmount = Math.round((float) amount / resultFluidStack.getAmount());

        setCanPressurization(true);
        inputFluidInventory.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        outputFluidInventory.fill(new FluidStack(resultFluidStack.getFluid(), convertedAmount), IFluidHandler.FluidAction.EXECUTE);
        setCanPressurization(false);

        award();
    }

    public InputFluidHandler getInputFluidHandler() {
        return inputFluidHandler;
    }

    public OutputFluidHandler getOutputFluidHandler() {
        return outputFluidHandler;
    }

    private void setCanPressurization(boolean state) {
        canPressurization = state;
    }

    protected void onFluidStackChanged(FluidStack newFluidStack) {
        if (level == null || level.isClientSide) {
            return;
        }

        setChanged();
    }

    private boolean airCompressorTooltip(List<Component> tooltip) {
        if (level == null) {
            return false;
        }

        boolean notFastEnough = isCompressorNotFastEnough();
        boolean noBreezeChamber = isBeneathNotBreezeChamber();
        boolean inputEmpty = isInputEmpty();
        boolean outputFull = isOutputFull();

        CCBLang.translate("gui.goggles.air_compressor").forGoggles(tooltip);
        CCBLang.translate("gui.goggles.stress_impact").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.number(Mth.abs(calculateStressApplied() * speed)).translate("gui.goggles.unit.stress").style(ChatFormatting.AQUA).space().add(CCBLang.translate("gui.goggles.at_current_speed").style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        CCBLang.builder().add(CCBLang.translate("gui.goggles.air_compressor.pressurization_rate")).style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.builder().add(CCBLang.number(getConvertAmount()).add(CCBLang.translate("gui.goggles.unit.milli_buckets_per_second")).style(ChatFormatting.AQUA)).forGoggles(tooltip, 1);

        if (noBreezeChamber || notFastEnough || inputEmpty || outputFull) {
            CCBLang.text("").forGoggles(tooltip);
            CCBLang.translate("gui.goggles.warning").style(ChatFormatting.GOLD).forGoggles(tooltip);
        }

        if (notFastEnough) {
            MutableComponent hint = CCBLang.translateDirect("gui.goggles.air_compressor.not_fast_enough");
            List<Component> cutString = TooltipHelper.cutTextComponent(hint, FontHelper.Palette.GRAY_AND_WHITE);
            for (Component component : cutString) {
                CCBLang.builder().add(component.copy()).forGoggles(tooltip);
            }
            return true;
        }

        if (noBreezeChamber) {
            MutableComponent hint = CCBLang.translateDirect("gui.goggles.air_compressor.no_breeze_chamber");
            List<Component> cutString = TooltipHelper.cutTextComponent(hint, FontHelper.Palette.GRAY_AND_WHITE);
            for (Component component : cutString) {
                CCBLang.builder().add(component.copy()).forGoggles(tooltip);
            }
        }

        if (inputEmpty) {
            MutableComponent hint = CCBLang.translateDirect("gui.goggles.air_compressor.no_input");
            List<Component> cutString = TooltipHelper.cutTextComponent(hint, FontHelper.Palette.GRAY_AND_WHITE);
            for (Component component : cutString) {
                CCBLang.builder().add(component.copy()).forGoggles(tooltip);
            }
        }

        if (outputFull) {
            MutableComponent hint = CCBLang.translateDirect("gui.goggles.air_compressor.full_output");
            List<Component> cutString = TooltipHelper.cutTextComponent(hint, FontHelper.Palette.GRAY_AND_WHITE);
            for (Component component : cutString) {
                CCBLang.builder().add(component.copy()).forGoggles(tooltip);
            }
        }

        return true;
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
            behaviour.awardPlayer(CCBAdvancements.AIR_COMPRESSOR);
        }
    }

    private void awardExplosion() {
        AdvancementBehaviour behaviour = getBehaviour(AdvancementBehaviour.TYPE);
        if (behaviour != null) {
            behaviour.awardPlayer(CCBAdvancements.AIR_COMPRESSOR_EXPLOSION);
        }
    }

    public record InputFluidHandler(IFluidHandler handler) implements IFluidHandler {
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
            return canPressurization ? handler.drain(resource, action) : FluidStack.EMPTY;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, @NotNull FluidAction action) {
            return canPressurization ? handler.drain(maxDrain, action) : FluidStack.EMPTY;
        }
    }

    public record OutputFluidHandler(IFluidHandler handler) implements IFluidHandler {
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
            return canPressurization ? handler.fill(resource, action) : 0;
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