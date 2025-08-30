package net.ty.createcraftedbeginning.content.breezechamber;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.item.TooltipHelper;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ty.createcraftedbeginning.advancement.AdvancementBehaviour;
import net.ty.createcraftedbeginning.advancement.CCBAdvancement;
import net.ty.createcraftedbeginning.advancement.CCBAdvancements;
import net.ty.createcraftedbeginning.content.aircompressor.AirCompressorBlockEntity;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlock.FrostLevel;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class BreezeChamberBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int INSERTION_THRESHOLD = 500;
    public static final int MAX_COOLANT_CAPACITY = 50000;

    public boolean goggles;
    public boolean hat;
    public boolean wind;
    public float windRotationSpeed = 0;
    public LerpedFloat headAnimation;

    protected CoolantType activeCoolant;
    protected int coolRemainingTime;
    protected LerpedFloat headAngle;

    private SmartFluidTankBehaviour tankBehaviour;
    private boolean lastCoolerState = false;
    private boolean isCreative = false;

    public BreezeChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        activeCoolant = CoolantType.NONE;
        coolRemainingTime = 0;
        headAnimation = LerpedFloat.linear();
        headAngle = LerpedFloat.angular();
        goggles = false;
        wind = false;

        headAngle.startWithValue((AngleHelper.horizontalAngle(state.getOptionalValue(BreezeChamberBlock.FACING).orElse(Direction.SOUTH)) + 180) % 360);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CCBBlockEntities.BREEZE_CHAMBER.get(), (be, context) -> be.tankBehaviour.getCapability());
    }

    public int getCoolRemainingTime() {
        return coolRemainingTime;
    }

    private void fillWaterFromWaterloggedState() {
        if (level == null) {
            return;
        }

        BlockState state = getBlockState();
        if (!(state.getValue(BreezeChamberBlock.WATERLOGGED))) {
            return;
        }

        FluidStack waterStack = new FluidStack(Fluids.WATER, 1000);
        IFluidHandler handler = tankBehaviour.getPrimaryHandler();
        int space = handler.fill(waterStack, IFluidHandler.FluidAction.SIMULATE);
        handler.fill(waterStack.copyWithAmount(space), IFluidHandler.FluidAction.EXECUTE);

        BlockState newState = state.setValue(BreezeChamberBlock.WATERLOGGED, false);
        level.setBlock(worldPosition, newState, 3);
    }

    @OnlyIn(Dist.CLIENT)
    private boolean shouldTickAnimation() {
        return !VisualizationManager.supportsVisualization(level);
    }

    @OnlyIn(Dist.CLIENT)
    void tickAnimation() {
        boolean active = getFrostLevelFromBlock().isAtLeast(FrostLevel.RIMING) && isValidBlockAbove();

        if (!active) {
            float target = getTarget();
            headAngle.chase(target, .25f, Chaser.exp(5));
            headAngle.tickChaser();
        } else {
            headAngle.chase((AngleHelper.horizontalAngle(getBlockState().getOptionalValue(BreezeChamberBlock.FACING).orElse(Direction.SOUTH)) + 180) % 360, .125f, Chaser.EXP);
            headAngle.tickChaser();
        }

        headAnimation.chase(active ? 1 : 0, .25f, Chaser.exp(.25f));
        headAnimation.tickChaser();
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
            } else {
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

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartFluidTankBehaviour.single(this, 4000);
        registerAwardables(behaviours, CCBAdvancements.BREEZE_CHAMBER_GALLING);
        registerAwardables(behaviours, CCBAdvancements.BREEZE_CHAMBER_LAVA);
        behaviours.add(tankBehaviour);
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null) {
            return;
        }

        if (level.getGameTime() % 10 == 0) {
            tryUpdateCoolantByFluid();
            if (getFrostLevel().isAtLeast(FrostLevel.WANING) && !wind) {
                wind = true;
                windRotationSpeed = getFrostLevel().isAtLeast(FrostLevel.GALLING) ? 36.f : 24.f;
            } else if (!getFrostLevel().isAtLeast(FrostLevel.WANING) && wind) {
                wind = false;
                windRotationSpeed = 0;
            }
            notifyUpdate();
        }

        if (level.isClientSide) {
            if (shouldTickAnimation()) {
                tickAnimation();
            }
            if (!isVirtual()) {
                spawnParticles(getFrostLevelFromBlock());
            }
            return;
        } else {
            if (coolRemainingTime > 0 && level.getGameTime() % 10 == 0) {
                notifyUpdate();
            }
            applyFrozenEffects(level);
            updateCoolerState();
        }

        if (getFrostLevel().isAtLeast(FrostLevel.RIMING) && tankBehaviour.getPrimaryHandler().getSpace() > 0) {
            fillWaterFromWaterloggedState();
        }

        if (!isCreative && coolRemainingTime > 0) {
            coolRemainingTime--;
        }

        if (activeCoolant == CoolantType.NORMAL) {
            updateBlockState();
        }
        if (coolRemainingTime > 0) {
            return;
        }

        if (activeCoolant == CoolantType.POWERFUL) {
            activeCoolant = CoolantType.NORMAL;
            coolRemainingTime = MAX_COOLANT_CAPACITY / 2;
        } else {
            activeCoolant = CoolantType.NONE;
        }

        updateBlockState();
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putInt("coolantLevel", activeCoolant.ordinal());
        compound.putInt("coolTimeRemaining", coolRemainingTime);
        compound.putBoolean("Goggles", goggles);
        compound.putBoolean("TrainHat", hat);
        compound.putBoolean("Wind", wind);
        compound.putFloat("WindSpeed", windRotationSpeed);
        compound.putBoolean("isCreative", isCreative);
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        if (compound.contains("coolTimeRemaining")) {
            activeCoolant = CoolantType.values()[compound.getInt("coolantLevel")];
        }
        if (compound.contains("coolTimeRemaining")) {
            coolRemainingTime = compound.getInt("coolTimeRemaining");
        }
        if (compound.contains("Goggles")) {
            goggles = compound.getBoolean("Goggles");
        }
        if (compound.contains("TrainHat")) {
            hat = compound.getBoolean("TrainHat");
        }
        if (compound.contains("Wind")) {
            wind = compound.getBoolean("Wind");
            windRotationSpeed = compound.getFloat("WindSpeed");
        }
        if (compound.contains("isCreative")) {
            isCreative = compound.getBoolean("isCreative");
        }
        super.read(compound, registries, clientPacket);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) {
            return false;
        }
        return breezeChamberTooltip(tooltip);
    }

    private void applyFrozenEffects(@NotNull Level level) {
        if (level.isClientSide) {
            return;
        }

        Vec3 center = Vec3.atCenterOf(worldPosition);
        double halfEdge = 0.5f * (1 + getFrostLevel().ordinal());
        AABB area = new AABB(center.x - halfEdge, center.y - halfEdge, center.z - halfEdge, center.x + halfEdge, center.y + halfEdge, center.z + halfEdge);
        for (Entity entity : level.getEntities(null, area)) {
            if (!(entity instanceof LivingEntity) || !entity.isAlive()) {
                continue;
            }
            if (!(entity instanceof Mob livingEntity) || !entity.canFreeze()) {
                continue;
            }

            livingEntity.setTicksFrozen(Math.min(livingEntity.getTicksRequiredToFreeze(), livingEntity.getTicksFrozen()) + entity.getTicksRequiredToFreeze() / 20);
        }
    }

    private boolean isLiquidValid() {
        if (tankBehaviour.getPrimaryHandler().isEmpty() || level == null) {
            return true;
        }

        FluidStack fluidStack = tankBehaviour.getPrimaryHandler().getFluid();
        CoolingRecipe.CoolingData data = CoolingRecipe.getResultingCoolingTime(level, null, fluidStack);
        return !Objects.equals(data.type(), "none") && data.time() != 0 && data.amount() != 0;
    }

    private void updateCoolerState() {
        if (level == null) {
            return;
        }

        boolean currentCooler = isValidBlockAbove();
        if (currentCooler == lastCoolerState) {
            return;
        }

        BlockState newState = getBlockState().setValue(BreezeChamberBlock.COOLER, currentCooler);
        level.setBlockAndUpdate(worldPosition, newState);
        lastCoolerState = currentCooler;
        notifyUpdate();
    }

    private boolean breezeChamberTooltip(List<Component> tooltip) {
        if (level == null) {
            return false;
        }

        FrostLevel frostLevel = getFrostLevel();

        CCBLang.translate("gui.goggles.breeze_chamber").forGoggles(tooltip);
        CCBLang.builder().add(CCBLang.translate("gui.goggles.breeze_chamber.current_state").style(ChatFormatting.GRAY)).forGoggles(tooltip);
        CCBLang.builder().add(CCBLang.translate(frostLevel.getTranslatable()).style(frostLevel.getChatFormatting())).forGoggles(tooltip, 1);

        boolean hasRemainingTime = coolRemainingTime > 0;
        boolean isEmpty = tankBehaviour.getPrimaryHandler().isEmpty();
        boolean isLiquidInvalid = !isLiquidValid();

        if (hasRemainingTime) {
            CCBLang.builder().add(CCBLang.translate("gui.goggles.breeze_chamber.remaining_time")).style(ChatFormatting.GRAY).forGoggles(tooltip);
            if (isCreative) {
                CCBLang.builder().add(CCBLang.translateDirect("gui.goggles.breeze_chamber.infinite")).style(ChatFormatting.GREEN).forGoggles(tooltip, 1);
            } else {
                CCBLang.builder().add(CCBLang.seconds(coolRemainingTime, 20)).style(ChatFormatting.GREEN).forGoggles(tooltip, 1);
            }
        }

        LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
        IFluidHandler handler = tankBehaviour.getPrimaryHandler();
        FluidStack fluidStack = handler.getFluidInTank(0);
        if (!fluidStack.isEmpty()) {
            CCBLang.translate("gui.goggles.fluid_container.capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CCBLang.fluidName(fluidStack).style(ChatFormatting.WHITE).forGoggles(tooltip, 1);
            CCBLang.builder().add(CCBLang.number(fluidStack.getAmount()).add(mb).style(ChatFormatting.GOLD)).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(handler.getTankCapacity(0)).add(mb).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        } else {
            CCBLang.translate("gui.goggles.fluid_container.capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CCBLang.builder().add(CCBLang.number(handler.getTankCapacity(0)).add(mb)).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        }

        if ((!hasRemainingTime && isEmpty) || isLiquidInvalid) {
            tooltip.add(CommonComponents.EMPTY);
            CCBLang.translate("gui.goggles.warning").style(ChatFormatting.GOLD).forGoggles(tooltip);
        }

        if (!hasRemainingTime && isEmpty) {
            MutableComponent hint = CCBLang.translateDirect("gui.goggles.breeze_chamber.no_coolant");
            List<Component> cutString = TooltipHelper.cutTextComponent(hint, FontHelper.Palette.GRAY_AND_WHITE);
            for (Component component : cutString) {
                CCBLang.builder().add(component.copy()).forGoggles(tooltip);
            }
            return true;
        }

        if (!isEmpty && isLiquidInvalid) {
            MutableComponent hint = CCBLang.translateDirect("gui.goggles.breeze_chamber.invalid_fluid");
            List<Component> cutString = TooltipHelper.cutTextComponent(hint, FontHelper.Palette.GRAY_AND_WHITE);
            for (Component component : cutString) {
                CCBLang.builder().add(component.copy()).forGoggles(tooltip);
            }
        }

        return true;
    }

    public boolean isCreative() {
        return isCreative;
    }

    public IFluidHandler getFluidHandler() {
        return tankBehaviour.getCapability();
    }

    public FrostLevel getFrostLevelForRender() {
        return getFrostLevelFromBlock();
    }

    public FrostLevel getFrostLevelFromBlock() {
        return BreezeChamberBlock.getFrostLevelOf(getBlockState());
    }

    protected boolean tryUpdateCoolantByItem(ItemStack itemStack, boolean forceOverflow, boolean simulate) {
        if (level == null) {
            return false;
        }

        if (itemStack.getItem() == CCBItems.CREATIVE_ICE_CREAM.asItem()) {
            return handleCreativeIceCream(simulate);
        }

        CoolingRecipe.CoolingData data = CoolingRecipe.getResultingCoolingTime(level, itemStack, null);
        if (Objects.equals(data.type(), "none") || data.time() == 0 || data.amount() == 0) {
            return false;
        }

        return handleCoolantType(CoolantType.getCoolantType(data.type()), data.time(), forceOverflow, simulate, 2);
    }

    private boolean handleCreativeIceCream(boolean simulate) {
        CoolantType newCoolant = switch (activeCoolant) {
            case POWERFUL -> CoolantType.NONE;
            case NORMAL -> CoolantType.POWERFUL;
            case NONE -> CoolantType.NORMAL;
        };

        if (!simulate) {
            activeCoolant = newCoolant;
            coolRemainingTime = activeCoolant == CoolantType.NONE ? 0 : MAX_COOLANT_CAPACITY;
            isCreative = activeCoolant != CoolantType.NONE;
            updateBlockState();

            if (level != null) {
                if (!level.isClientSide) {
                    playSound();
                } else if (newCoolant == CoolantType.POWERFUL) {
                    spawnParticleBurst();
                }
            }

            if (activeCoolant == CoolantType.POWERFUL) {
                awardGalling();
            }
        }
        return true;
    }

    private boolean handleCoolantType(CoolantType newCoolant, int newCoolTime, boolean forceOverflow, boolean simulate, float thresholdMultiplier) {
        if (newCoolant == CoolantType.NONE) {
            return false;
        }
        if (newCoolant.ordinal() < activeCoolant.ordinal()) {
            return false;
        }

        if (newCoolant == CoolantType.POWERFUL) {
            thresholdMultiplier *= 5;
        }

        if (newCoolant == activeCoolant) {
            if (coolRemainingTime <= INSERTION_THRESHOLD * thresholdMultiplier) {
                newCoolTime += coolRemainingTime;
            } else if (forceOverflow && newCoolant == CoolantType.NORMAL) {
                if (coolRemainingTime < MAX_COOLANT_CAPACITY) {
                    newCoolTime = Math.min(coolRemainingTime + newCoolTime, MAX_COOLANT_CAPACITY);
                } else {
                    newCoolTime = coolRemainingTime;
                }
            } else {
                return false;
            }
        }

        if (!simulate) {
            activeCoolant = newCoolant;
            coolRemainingTime = newCoolTime;
            updateBlockState();

            if (level != null) {
                if (!level.isClientSide) {
                    playSound();
                } else if (newCoolant == CoolantType.POWERFUL) {
                    spawnParticleBurst();
                }
            }

            if (activeCoolant == CoolantType.POWERFUL) {
                awardGalling();
            }
        }
        return true;
    }

    public void updateBlockState() {
        setBlockCoolant(getFrostLevel());
    }

    private void playSound() {
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.BREEZE_SHOOT, SoundSource.BLOCKS, .125f + level.random.nextFloat() * .125f, .75f - level.random.nextFloat() * .25f);
        }
    }

    private void setBlockCoolant(FrostLevel frost) {
        FrostLevel inBlockState = getFrostLevelFromBlock();
        if (inBlockState == frost || level == null) {
            return;
        }
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(BreezeChamberBlock.FROST_LEVEL, frost));
        notifyUpdate();
    }

    public FrostLevel getFrostLevel() {
        FrostLevel level = FrostLevel.RIMING;
        switch (activeCoolant) {
            case POWERFUL:
                level = FrostLevel.GALLING;
                break;
            case NORMAL:
                boolean lowPercent = (double) coolRemainingTime / MAX_COOLANT_CAPACITY < 0.0125;
                level = lowPercent ? FrostLevel.WANING : FrostLevel.CHILLED;
                break;
            case NONE:
            default:
                break;
        }
        return level;
    }

    private void tryUpdateCoolantByFluid() {
        SmartFluidTank fluidTank = tankBehaviour.getPrimaryHandler();
        Fluid fluid = fluidTank.getFluid().getFluid();
        if (fluid == Fluids.EMPTY || level == null) {
            return;
        }

        if (fluid.isSame(Fluids.LAVA) || fluid.isSame(Fluids.FLOWING_LAVA)) {
            if (!level.isClientSide) {
                ItemStack newItem = new ItemStack(CCBBlocks.EMPTY_BREEZE_CHAMBER_BLOCK.get());
                ItemEntity itemEntity = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, newItem);
                itemEntity.setDeltaMovement(0, level.random.nextDouble() * 0.1 + 0.05, 0);
                level.addFreshEntity(itemEntity);
            }
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .75f, 1);
            level.playSound(null, worldPosition, SoundEvents.BREEZE_DEATH, SoundSource.BLOCKS, .75f, 1);
            level.destroyBlock(worldPosition, false);
            awardLava();
            return;
        }

        CoolingRecipe.CoolingData data = CoolingRecipe.getResultingCoolingTime(level, null, fluidTank.getFluid());
        if (Objects.equals(data.type(), "none") || data.time() == 0 || data.amount() == 0) {
            return;
        }

        CoolantType coolantType = CoolantType.getCoolantType(data.type());
        int newCoolTime = data.time();
        int newAmount = data.amount();

        int left = fluidTank.getFluidAmount() - newAmount;
        if (left < 0) {
            return;
        }

        boolean result = handleCoolantType(coolantType, newCoolTime, false, true, 5);
        if (!result) {
            return;
        }

        fluidTank.drain(newAmount, IFluidHandler.FluidAction.EXECUTE);
        handleCoolantType(coolantType, newCoolTime, false, false, 5);

        notifyUpdate();
    }

    public void spawnParticleBurst() {
        if (level == null) {
            return;
        }

        Vec3 c = VecHelper.getCenterOf(worldPosition);
        RandomSource r = level.random;
        for (int i = 0; i < 20; i++) {
            Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, r, .5f).multiply(1, .25f, 1).normalize();
            Vec3 v = c.add(offset.scale(.5 + r.nextDouble() * .125f)).add(0, .125, 0);
            Vec3 m = offset.scale(1 / 32f);

            level.addParticle(ParticleTypes.SNOWFLAKE, v.x, v.y, v.z, m.x, m.y, m.z);
        }
    }

    public boolean isValidBlockAbove() {
        boolean isVirtual = isVirtual();
        if (level == null || isVirtual) {
            return false;
        }

        BlockEntity be = level.getBlockEntity(worldPosition.above());
        return be instanceof AirCompressorBlockEntity;
    }

    private void spawnParticles(FrostLevel frostLevel) {
        if (level == null) {
            return;
        }

        RandomSource r = level.getRandom();
        if (r.nextInt(4) != 0) {
            return;
        }

        Vec3 c = VecHelper.getCenterOf(worldPosition);
        Vec3 v = c.add(VecHelper.offsetRandomly(Vec3.ZERO, r, .125f).multiply(1, 0, 1));
        boolean empty = level.getBlockState(worldPosition.above()).getCollisionShape(level, worldPosition.above()).isEmpty();
        if (empty || r.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.SNOWFLAKE, v.x, v.y, v.z, 0, 0, 0);
        }

        double yMotion = empty ? .0625f : r.nextDouble() * .0125f;
        Vec3 v2 = c.add(VecHelper.offsetRandomly(Vec3.ZERO, r, .5f).multiply(1, .25f, 1).normalize().scale((empty ? .25f : .5) + r.nextDouble() * .125f)).add(0, .5, 0);
        if (frostLevel.isAtLeast(FrostLevel.WANING)) {
            level.addParticle(ParticleTypes.SNOWFLAKE, v2.x, v2.y, v2.z, 0, yMotion, 0);
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

    private void awardGalling() {
        AdvancementBehaviour behaviour = getBehaviour(AdvancementBehaviour.TYPE);
        if (behaviour != null) {
            behaviour.awardPlayer(CCBAdvancements.BREEZE_CHAMBER_GALLING);
        }
    }

    private void awardLava() {
        AdvancementBehaviour behaviour = getBehaviour(AdvancementBehaviour.TYPE);
        if (behaviour != null) {
            behaviour.awardPlayer(CCBAdvancements.BREEZE_CHAMBER_LAVA);
        }
    }

    public enum CoolantType {
        NONE,
        NORMAL,
        POWERFUL;

        public static CoolantType getCoolantType(String coolant) {
            return "powerful".equalsIgnoreCase(coolant) ? CoolantType.POWERFUL : CoolantType.NORMAL;
        }
    }
}
