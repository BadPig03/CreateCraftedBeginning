package net.ty.createcraftedbeginning.content.gasinjectionchamber;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ty.createcraftedbeginning.advancement.AdvancementBehaviour;
import net.ty.createcraftedbeginning.advancement.CCBAdvancement;
import net.ty.createcraftedbeginning.advancement.CCBAdvancements;
import net.ty.createcraftedbeginning.content.compressedair.CompressedAirCanisterItem;
import net.ty.createcraftedbeginning.content.compressedair.CanisterUtil;
import net.ty.createcraftedbeginning.content.compressedair.CompressedAirTankBehaviour;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;

import java.util.ArrayList;
import java.util.List;

import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.HOLD;
import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.PASS;

public class GasInjectionChamberBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int PROCESSING_TIME = 60;
    public static final int NOZZLE_TIME = 15;
    public static final int NOZZLE_PART_TIME = 15;
    public static final int NOZZLE_IDLE_TIME = 5;
    public int processingTicks = -1;
    public boolean sendCloud;

    protected CompressedAirTankBehaviour tankBehaviour;
    protected BeltProcessingBehaviour beltProcessing;

    public GasInjectionChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CCBBlockEntities.GAS_INJECTION_CHAMBER.get(), (be, context) -> context == Direction.UP ? be.tankBehaviour.getCapability() : null);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = CompressedAirTankBehaviour.single(this, 1000);
        beltProcessing = new BeltProcessingBehaviour(this).whenItemEnters(this::onItemEntered).whileItemHeld(this::onItemHeld);
        behaviours.add(beltProcessing);
        behaviours.add(tankBehaviour);
        registerAwardables(behaviours, CCBAdvancements.GAS_INJECTION_CHAMBER);
    }

    @Override
    public void tick() {
        super.tick();

        if (processingTicks >= 0) {
            processingTicks--;
        }
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("ProcessingTicks", processingTicks);
        if (sendCloud && clientPacket) {
            compound.putBoolean("Cloud", true);
            sendCloud = false;
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (compound.contains("ProcessingTicks")) {
            processingTicks = compound.getInt("ProcessingTicks");
        }
        if (!clientPacket) {
            return;
        }
        if (compound.contains("Cloud")) {
            spawnCloud();
        }
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
            behaviour.awardPlayer(CCBAdvancements.GAS_INJECTION_CHAMBER);
        }
    }

    protected void spawnCloud() {
        if (isVirtual() || level == null) {
            return;
        }
        Vec3 vec = VecHelper.getCenterOf(worldPosition).subtract(0, 2 - 5 / 16f, 0);
        for (int i = 0; i < level.random.nextInt(3, 6); i++) {
            Vec3 m = VecHelper.offsetRandomly(Vec3.ZERO, level.random, 0.125f);
            m = new Vec3(m.x, Math.abs(m.y), m.z);
            level.addAlwaysVisibleParticle(ParticleTypes.CLOUD, vec.x, vec.y, vec.z, m.x, m.y, m.z);
        }
    }

    public IFluidHandler getFluidHandler() {
        return tankBehaviour.getCapability();
    }

    private FluidStack getFluidInTank() {
        return tankBehaviour.getPrimaryHandler().getFluid();
    }

    private void setFluidInTank(FluidStack fluidStack) {
        tankBehaviour.getPrimaryHandler().setFluid(fluidStack);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) {
            return false;
        }
        return gasTankTooltip(tooltip, tankBehaviour.getCapability());
    }

    private boolean gasTankTooltip(List<Component> tooltip, IFluidHandler handler) {
        if (handler == null) {
            return false;
        }

        LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
        CCBLang.translate("gui.goggles.gas_container").forGoggles(tooltip);

        FluidStack fluidStack = handler.getFluidInTank(0);
        if (!fluidStack.isEmpty()) {
            CCBLang.fluidName(fluidStack).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            CCBLang.builder().add(CCBLang.number(fluidStack.getAmount()).add(mb).style(ChatFormatting.GOLD)).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(handler.getTankCapacity(0)).add(mb).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        } else {
            CCBLang.translate("gui.goggles.gas_container.capacity").add(CCBLang.number(handler.getTankCapacity(0)).add(mb).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        }

        return true;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().expandTowards(0, -2, 0);
    }

    protected ProcessingResult onItemEntered(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (handler.blockEntity.isVirtual()) {
            return PASS;
        }
        if (GasInjectionRecipe.isItemInvalidForInjection(level, transported.stack)) {
            return PASS;
        }

        FluidStack tankFluid = getFluidInTank();
        if (tankFluid.isEmpty()) {
            return HOLD;
        }
        if (GasInjectionRecipe.getRequiredFluidAmountForItem(level, transported.stack, tankFluid) == -1) {
            return PASS;
        }
        return HOLD;
    }

    protected ProcessingResult onItemHeld(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (processingTicks != -1 && processingTicks != PROCESSING_TIME - NOZZLE_TIME - NOZZLE_PART_TIME - NOZZLE_IDLE_TIME) {
            return HOLD;
        }
        if (GasInjectionRecipe.isItemInvalidForInjection(level, transported.stack)) {
            return PASS;
        }

        FluidStack tankFluid = getFluidInTank();
        if (tankFluid.isEmpty()) {
            return HOLD;
        }

        int requiredAmount = GasInjectionRecipe.getRequiredFluidAmountForItem(level, transported.stack, tankFluid);
        if (requiredAmount == -1) {
            return PASS;
        }
        if (tankFluid.getAmount() < requiredAmount) {
            return HOLD;
        }

        if (processingTicks == -1) {
            processingTicks = PROCESSING_TIME + NOZZLE_IDLE_TIME;
            notifyUpdate();
            return HOLD;
        }

        ItemStack resultItem;
        if (CanisterUtil.isValidCanister(transported.stack, null)) {
            CompressedAirCanisterItem.change(transported.stack, requiredAmount);
            tankFluid.shrink(requiredAmount);
            setFluidInTank(tankFluid);
            notifyUpdate();
            sendCloud = true;
            CCBSoundEvents.INJECTING.playOnServer(level, worldPosition, 0.75f, 0.9f + 0.2f * level.random.nextFloat());
            return HOLD;
        } else {
            resultItem = GasInjectionRecipe.getResultItem(level, transported.stack, tankFluid);
            if (!resultItem.isEmpty()) {
                transported.stack.shrink(1);
            }
        }

        if (!resultItem.isEmpty()) {
            transported.clearFanProcessingData();
            TransportedItemStack held = null;
            TransportedItemStack result = transported.copy();
            result.stack = resultItem;
            if (!transported.stack.isEmpty()) {
                held = transported.copy();
            }

            List<TransportedItemStack> resultList = new ArrayList<>();
            resultList.add(result);
            handler.handleProcessingOnItem(transported, TransportedResult.convertToAndLeaveHeld(resultList, held));
            award();
        }

        tankFluid.shrink(requiredAmount);
        setFluidInTank(tankFluid);
        notifyUpdate();
        sendCloud = true;
        CCBSoundEvents.INJECTING.playOnServer(level, worldPosition, 0.75f, 0.9f + 0.2f * level.random.nextFloat());
        return HOLD;
    }
}
