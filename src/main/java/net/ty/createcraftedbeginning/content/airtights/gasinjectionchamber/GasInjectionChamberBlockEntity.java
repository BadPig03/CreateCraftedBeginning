package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

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
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.HOLD;
import static com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult.PASS;

public class GasInjectionChamberBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int PROCESSING_TIME = 60;
    public static final int NOZZLE_TIME = 15;
    public static final int NOZZLE_PART_TIME = 15;
    public static final int NOZZLE_IDLE_TIME = 5;

    private static final String COMPOUND_KEY_PROCESSING_TICKS = "ProcessingTicks";
    private static final String COMPOUND_KEY_CLOUD = "Cloud";

    public int processingTicks = -1;
    public boolean sendCloud;

    protected SmartGasTankBehaviour tankBehaviour;
    protected BeltProcessingBehaviour beltProcessing;

    public GasInjectionChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.GAS_INJECTION_CHAMBER.get(), (be, context) -> context == Direction.UP ? be.tankBehaviour.getCapability() : null);
    }

    public static long getMaxCapacity() {
        return CCBConfig.server().airtights.maxCanisterCapacity.get() * 500L;
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartGasTankBehaviour.single(this, getMaxCapacity());
        beltProcessing = new BeltProcessingBehaviour(this).whenItemEnters(this::onItemEntered).whileItemHeld(this::onItemHeld);
        behaviours.add(tankBehaviour);
        behaviours.add(beltProcessing);
    }

    @Override
	public void invalidate() {
		super.invalidate();
		invalidateCapabilities();
	}

    @Override
    public void tick() {
        super.tick();
        if (processingTicks < 0) {
            return;
        }

        processingTicks--;
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.putInt(COMPOUND_KEY_PROCESSING_TICKS, processingTicks);
        if (!sendCloud || !clientPacket) {
            return;
        }

        compoundTag.putBoolean(COMPOUND_KEY_CLOUD, true);
        sendCloud = false;
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (compoundTag.contains(COMPOUND_KEY_PROCESSING_TICKS)) {
            processingTicks = compoundTag.getInt(COMPOUND_KEY_PROCESSING_TICKS);
        }
        if (!clientPacket || !compoundTag.contains(COMPOUND_KEY_CLOUD)) {
            return;
        }

        spawnCloud();
    }

    private void spawnCloud() {
        if (level == null || !level.isClientSide || isVirtual()) {
            return;
        }
		
        Vec3 subtracted = VecHelper.getCenterOf(worldPosition).subtract(0, 1.6875f, 0);
        for (int i = 0; i < level.random.nextInt(3, 6); i++) {
            Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, level.random, 0.125f);
            offset = new Vec3(offset.x, Math.abs(offset.y), offset.z);
            level.addAlwaysVisibleParticle(CCBParticleTypes.BREEZE_CLOUD.getParticleOptions(), subtracted.x, subtracted.y, subtracted.z, offset.x, offset.y, offset.z);
        }
    }

    private ProcessingResult onItemEntered(TransportedItemStack transported, @NotNull TransportedItemStackHandlerBehaviour handler) {
        if (handler.blockEntity.isVirtual()) {
            return PASS;
        }

        GasStack tankGas = getGasInTank();
        if (tankGas.isEmpty()) {
            return HOLD;
        }

        if (GasInjectionRecipe.isItemInvalidForInjection(level, transported.stack, tankGas)) {
            return PASS;
        }

        return GasInjectionRecipe.getRequiredGasAmountForItem(level, transported.stack, tankGas) == -1 ? PASS : HOLD;
    }

    private @NotNull GasStack getGasInTank() {
        return tankBehaviour.getPrimaryHandler().getGasStack();
    }

    private void setGasInTank(GasStack gasStack) {
        tankBehaviour.getPrimaryHandler().setGasStack(gasStack);
    }

    private ProcessingResult onItemHeld(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (processingTicks != -1 && processingTicks != PROCESSING_TIME - NOZZLE_TIME - NOZZLE_PART_TIME - NOZZLE_IDLE_TIME) {
            return HOLD;
        }

        GasStack tankGas = getGasInTank();
        if (tankGas.isEmpty()) {
            return HOLD;
        }

        if (GasInjectionRecipe.isItemInvalidForInjection(level, transported.stack, tankGas)) {
            return PASS;
        }

        long requiredAmount = GasInjectionRecipe.getRequiredGasAmountForItem(level, transported.stack, tankGas);
        if (requiredAmount == -1) {
            return PASS;
        }

        if (tankGas.getAmount() < requiredAmount) {
            return HOLD;
        }

        if (processingTicks == -1) {
            processingTicks = PROCESSING_TIME + NOZZLE_IDLE_TIME;
            notifyUpdate();
            return HOLD;
        }

        ItemStack resultItem;
        if (transported.stack.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents) {
            long filled = canisterContents.fill(0, tankGas.copyWithAmount(requiredAmount), GasAction.EXECUTE);
            tankBehaviour.getPrimaryHandler().drain(filled, GasAction.EXECUTE);
            CCBSoundEvents.INJECTING.playOnServer(level, worldPosition, 0.75f, 0.9f + 0.2f * level.random.nextFloat());
            sendCloud = true;
            notifyUpdate();
            return HOLD;
        }
        else {
            resultItem = GasInjectionRecipe.getResultItem(level, transported.stack, tankGas);
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
        }
        tankGas.shrink(requiredAmount);
        setGasInTank(tankGas);
        notifyUpdate();
        sendCloud = true;
        CCBSoundEvents.INJECTING.playOnServer(level, worldPosition, 0.75f, 0.9f + 0.2f * level.random.nextFloat());
        return HOLD;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) {
            return false;
        }

        IGasHandler gasHandler = tankBehaviour.getPrimaryHandler();
        LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
        CCBLang.translate("gui.goggles.gas_container").forGoggles(tooltip);
        GasStack gasStack = gasHandler.getGasInTank(0);
        if (gasStack.isEmpty()) {
            CCBLang.translate("gui.goggles.gas_container.capacity").add(CCBLang.number(gasHandler.getTankCapacity(0)).add(mb).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        }
        else {
            CCBLang.gasName(gasStack).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            CCBLang.number(gasStack.getAmount()).add(mb).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(gasHandler.getTankCapacity(0)).add(mb).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        }
        return true;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().expandTowards(0, -2, 0);
    }
}
