package net.ty.createcraftedbeginning.content.breezes.breezecooler;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.BaseCoolerState;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.ChilledCoolerState;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.CreativeCoolerState;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.InactiveCoolerState;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe.CoolingData;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.COOLER;
import static net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FROST_LEVEL;

public class BreezeCoolerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int MAX_COOLANT_CAPACITY = 72000;

    private static final String COMPOUND_KEY_STATE_TYPE = "StateType";
    private static final String COMPOUND_KEY_STATE_DATA = "StateData";
    private static final String COMPOUND_KEY_GOGGLES = "Goggles";
    private static final String COMPOUND_KEY_TRAIN_HAT = "TrainHat";
    private static final String COMPOUND_KEY_LAST_COOLER_STATE = "LastCoolerState";
    private static final String COMPOUND_KEY_IS_CREATIVE = "isCreative";
    private static final String COMPOUND_KEY_REMAINING_TIME = "RemainingTime";

    private final LerpedFloat headAnimation;

    protected LerpedFloat headAngle;

    private boolean goggles;
    private boolean trainHat;
    private CCBAdvancementBehaviour advancementBehaviour;
    private SmartFluidTankBehaviour tankBehaviour;
    private BaseCoolerState currentState;
    private boolean lastCoolerState;

    public BreezeCoolerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        currentState = new InactiveCoolerState();
        goggles = false;
        headAngle = LerpedFloat.angular();
        headAngle.startWithValue((AngleHelper.horizontalAngle(state.getOptionalValue(BreezeCoolerBlock.FACING).orElse(Direction.NORTH)) + 180) % 360);
        headAnimation = LerpedFloat.linear();
        lastCoolerState = false;
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(FluidHandler.BLOCK, CCBBlockEntities.BREEZE_COOLER.get(), (be, context) -> be.tankBehaviour.getCapability());
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartFluidTankBehaviour.single(this, AllConfigs.server().fluids.fluidTankCapacity.get() * 500);
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.A_MURDER, CCBAdvancements.FROZEN_NECTAR);
        behaviours.add(tankBehaviour);
        behaviours.add(advancementBehaviour);
    }

    @Override
	public void invalidate() {
		super.invalidate();
		invalidateCapabilities();
	}

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        currentState.tick(this);
        if (level.isClientSide) {
            spawnParticles();
            if (!shouldTickAnimation()) {
                return;
            }

            tickAnimation();
            return;
        }

        boolean active = isValidBlockAbove();
        if (lastCoolerState == active) {
            return;
        }

        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(COOLER, active));
        lastCoolerState = active;
        notifyUpdate();
    }

    @Override
    protected void write(@NotNull CompoundTag compoundTag, Provider registries, boolean clientPacket) {
        CompoundTag stateTag = new CompoundTag();
        currentState.save(stateTag);
        compoundTag.put(COMPOUND_KEY_STATE_DATA, stateTag);
        compoundTag.putInt(COMPOUND_KEY_STATE_TYPE, currentState.getCoolantType().ordinal());
        compoundTag.putBoolean(COMPOUND_KEY_GOGGLES, goggles);
        compoundTag.putBoolean(COMPOUND_KEY_TRAIN_HAT, trainHat);
        compoundTag.putBoolean(COMPOUND_KEY_LAST_COOLER_STATE, lastCoolerState);

        super.write(compoundTag, registries, clientPacket);
    }

    @Override
    protected void read(@NotNull CompoundTag compoundTag, Provider registries, boolean clientPacket) {
        if (compoundTag.contains(COMPOUND_KEY_STATE_TYPE) && compoundTag.contains(COMPOUND_KEY_STATE_DATA)) {
            CoolantType stateType = CoolantType.values()[compoundTag.getInt(COMPOUND_KEY_STATE_TYPE)];
            CompoundTag stateData = compoundTag.getCompound(COMPOUND_KEY_STATE_DATA);
            boolean isCreative = stateData.contains(COMPOUND_KEY_IS_CREATIVE) && stateData.getBoolean(COMPOUND_KEY_IS_CREATIVE);

            BaseCoolerState newState;
            if (isCreative) {
                newState = new CreativeCoolerState(stateType);
            }
            else {
                int remainingTime = stateData.contains(COMPOUND_KEY_REMAINING_TIME) ? stateData.getInt(COMPOUND_KEY_REMAINING_TIME) : 0;
                newState = switch (stateType) {
                    case NORMAL -> new ChilledCoolerState(remainingTime, false);
                    case NONE -> new InactiveCoolerState();
                };
            }

            newState.read(stateData);
            setCoolerState(newState);
        }
        if (compoundTag.contains(COMPOUND_KEY_GOGGLES)) {
            goggles = compoundTag.getBoolean(COMPOUND_KEY_GOGGLES);
        }
        if (compoundTag.contains(COMPOUND_KEY_TRAIN_HAT)) {
            trainHat = compoundTag.getBoolean(COMPOUND_KEY_TRAIN_HAT);
        }
        if (compoundTag.contains(COMPOUND_KEY_LAST_COOLER_STATE)) {
            lastCoolerState = compoundTag.getBoolean(COMPOUND_KEY_LAST_COOLER_STATE);
        }

        super.read(compoundTag, registries, clientPacket);
    }

    private boolean isLiquidInvalid() {
        if (tankBehaviour.getPrimaryHandler().isEmpty() || level == null) {
            return false;
        }

        FluidStack fluidStack = tankBehaviour.getPrimaryHandler().getFluid();
        CoolingData data = CoolingRecipe.getCoolingTime(level, null, fluidStack);
        return data.time() == 0 || data.amount() == 0;
    }

    @OnlyIn(Dist.CLIENT)
    private boolean shouldTickAnimation() {
        return !VisualizationManager.supportsVisualization(level);
    }

    private float getTarget() {
        float target = 0;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && !player.isInvisible()) {
            double x;
            double z;
            if (isVirtual()) {
                x = -4;
                z = -10;
            }
            else {
                x = player.getX();
                z = player.getZ();
            }
            double dx = x - (getBlockPos().getX() + 0.5);
            double dz = z - (getBlockPos().getZ() + 0.5);
            target = AngleHelper.deg(-Mth.atan2(dz, dx)) - 90;
        }
        target = headAngle.getValue() + AngleHelper.getShortestAngleDiff(headAngle.getValue(), target);
        return target;
    }

    private void spawnParticles() {
        if (level == null) {
            return;
        }

        RandomSource random = level.getRandom();
        if (random.nextInt(2) != 0) {
            return;
        }

        Vec3 center = VecHelper.getCenterOf(worldPosition);
        Vec3 added = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.125f).multiply(1, 0, 1));
        boolean empty = level.getBlockState(worldPosition.above()).getCollisionShape(level, worldPosition.above()).isEmpty();
        if (empty || random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SNOWFLAKE, added.x, added.y, added.z, 0, 0, 0);
        }
        Vec3 chilledAdded = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5f).multiply(1, 0.25f, 1).normalize().scale((empty ? 0.25f : 0.5) + random.nextDouble() * 0.125f)).add(0, 0.5, 0);
        if (!getFrostLevelFromBlock().isAtLeast(FrostLevel.CHILLED)) {
            return;
        }

        level.addParticle(ParticleTypes.SNOWFLAKE, chilledAdded.x, chilledAdded.y, chilledAdded.z, 0, empty ? 0.0625 : random.nextDouble() * 0.0125, 0);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) {
            return false;
        }

        FrostLevel frostLevel = getFrostLevel();
        CCBLang.translate("gui.goggles.breeze_cooler").forGoggles(tooltip);
        CCBLang.translate("gui.goggles.breeze_cooler.current_state").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate(frostLevel.getTranslatable()).style(frostLevel.getChatFormatting()).forGoggles(tooltip, 1);

        int time = getCoolRemainingTime();
        if (time > 0) {
            CCBLang.translate("gui.goggles.breeze_cooler.remaining_time").style(ChatFormatting.GRAY).forGoggles(tooltip);
            if (isCreative()) {
                CCBLang.translate("gui.goggles.fluid_container.infinity").style(ChatFormatting.GREEN).forGoggles(tooltip, 1);
            }
            else {
                CCBLang.seconds(time, level.tickRateManager().tickrate()).style(ChatFormatting.GREEN).forGoggles(tooltip, 1);
            }
        }

        IFluidHandler handler = tankBehaviour.getPrimaryHandler();
        FluidStack fluidStack = handler.getFluidInTank(0);
        tooltip.add(CommonComponents.EMPTY);
        LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
        if (fluidStack.isEmpty()) {
            CCBLang.translate("gui.goggles.fluid_container.capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CCBLang.number(handler.getTankCapacity(0)).add(mb).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        }
        else {
            CCBLang.translate("gui.goggles.fluid_container.capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CCBLang.fluidName(fluidStack).style(ChatFormatting.WHITE).forGoggles(tooltip, 1);
            CCBLang.number(fluidStack.getAmount()).add(mb).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(handler.getTankCapacity(0)).add(mb).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        }

        if (isLiquidInvalid()) {
            tooltip.add(CommonComponents.EMPTY);
            CCBLang.translate("gui.goggles.warning").style(ChatFormatting.GOLD).forGoggles(tooltip);
            CCBLang.addToGoggles(tooltip, "gui.goggles.breeze_cooler.invalid_fluid");
        }
        return true;
    }

    public boolean hasGoggles() {
        return goggles;
    }

    public boolean hasTrainHat() {
        return trainHat;
    }

    public boolean isCreative() {
        return currentState.isCreative();
    }

    public boolean isValidBlockAbove() {
        return level != null && level.getBlockEntity(worldPosition.above()) instanceof AirCompressorBlockEntity;
    }

    public boolean tryUpdateCoolantByItem(@NotNull ItemStack itemStack, boolean forceOverflow, boolean simulate) {
        if (itemStack.is(CCBItems.CREATIVE_ICE_CREAM)) {
            if (!simulate) {
                CoolantType coolantType = CreativeCoolerState.getNextCoolantType(currentState.getCoolantType());
                setCoolerState(coolantType == CoolantType.NONE ? new InactiveCoolerState() : new CreativeCoolerState(coolantType));
                spawnParticleBurst();
                playSound();
            }
            return true;
        }

        InteractionResult result = currentState.onItemInsert(this, itemStack, forceOverflow, simulate);
        if (result != InteractionResult.SUCCESS) {
            return false;
        }

        if (itemStack.is(CCBItemTags.ICE_CREAMS.tag)) {
            advancementBehaviour.awardPlayer(CCBAdvancements.FROZEN_NECTAR);
        }
        notifyUpdate();
        return true;
    }

    public FrostLevel getFrostLevel() {
        return currentState.getFrostLevel();
    }

    public FrostLevel getFrostLevelForRender() {
        return getFrostLevelFromBlock();
    }

    public FrostLevel getFrostLevelFromBlock() {
        return BreezeCoolerBlock.getFrostLevelOf(getBlockState());
    }

    public int getCoolRemainingTime() {
        return currentState.getRemainingTime();
    }

    public BaseCoolerState getCurrentState() {
        return currentState;
    }

    public LerpedFloat getHeadAnimation() {
        return headAnimation;
    }

    public CCBAdvancementBehaviour getAdvancementBehaviour() {
        return advancementBehaviour;
    }

    public SmartFluidTank getTankInventory() {
        return tankBehaviour.getPrimaryHandler();
    }

    public void playSound() {
        if (level == null) {
            return;
        }

        level.playSound(null, worldPosition, SoundEvents.BREEZE_SHOOT, SoundSource.BLOCKS, 0.125f + level.random.nextFloat() * 0.125f, 0.75f - level.random.nextFloat() * 0.25f);
    }

    public void setCoolerState(BaseCoolerState newState) {
        currentState = newState;
        if (level == null || level.isClientSide && !isVirtual()) {
            return;
        }

        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(FROST_LEVEL, currentState.getFrostLevel()));
        notifyUpdate();
    }

    public void setGoggles(boolean newGoggles) {
        goggles = newGoggles;
    }

    public void spawnParticleBurst() {
        if (level == null) {
            return;
        }

        Vec3 center = VecHelper.getCenterOf(worldPosition);
        RandomSource random = level.random;
        for (int i = 0; i < 20; i++) {
            Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5f).multiply(1, 0.25f, 1).normalize();
            Vec3 v = center.add(offset.scale(0.5 + random.nextDouble() * 0.125f)).add(0, 0.125, 0);
            Vec3 m = offset.scale(0.03125f);

            level.addParticle(ParticleTypes.SNOWFLAKE, v.x, v.y, v.z, m.x, m.y, m.z);
        }
    }

    public void SwitchToChilledState() {
        if (!(level instanceof PonderLevel)) {
            return;
        }

        setCoolerState(new CreativeCoolerState(CoolantType.NORMAL));
        spawnParticleBurst();
    }

    @OnlyIn(Dist.CLIENT)
    public void tickAnimation() {
        boolean active = isValidBlockAbove();
        if (active) {
            headAngle.chase((AngleHelper.horizontalAngle(getBlockState().getOptionalValue(BreezeCoolerBlock.FACING).orElse(Direction.SOUTH)) + 180) % 360, 0.125f, Chaser.EXP);
            headAngle.tickChaser();
        }
        else {
            headAngle.chase(getTarget(), 0.25f, Chaser.exp(5));
            headAngle.tickChaser();
        }
        headAnimation.chase(active ? 1 : 0, 0.25f, Chaser.exp(0.25f));
        headAnimation.tickChaser();
    }

    public enum CoolantType {
        NONE,
        NORMAL
    }
}
