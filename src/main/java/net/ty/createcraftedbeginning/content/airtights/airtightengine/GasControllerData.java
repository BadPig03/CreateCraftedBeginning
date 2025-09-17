package net.ty.createcraftedbeginning.content.airtights.airtightengine;

import com.simibubi.create.api.stress.BlockStressValues;
import joptsimple.internal.Strings;
import net.createmod.catnip.data.Iterate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.condensatedrain.CondensateDrainBlock;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineBlockEntity.BASE_ROTATION_SPEED;
import static net.ty.createcraftedbeginning.content.airtights.condensatedrain.CondensateDrainBlockEntity.PENALTY_COOLDOWN;

public class GasControllerData {
    public static final int MAX_LEVEL = 12;
    private static final float ALPHA = 0.5f;
    private static final float LOW_STABILITY_THRESHOLD = 0.5f;
    private static final float MEDIUM_STABILITY_THRESHOLD = 0.8f;
    private static final float STABILITY_SENSITIVITY = 2f;
    private static final float STABILITY_THRESHOLD = 0.8f;
    private static final int BASE_SAMPLE_RATE = 5;
    private static final int GAS_SUPPLY_PER_LEVEL = 15;
    private static final int SIZE_PER_LEVEL = 3;

    private float emaGasSupply = 0;
    private float gasSupply;
    private float stabilityFactor = 1f;
    private GasStack gasType = GasStack.EMPTY;
    private int attachedDrains;
    private int attachedEngines;
    private int condensatePenaltyCooldown = 0;
    private int condensateSuccessCount = 0;
    private int condensationLevel = 0;
    private int currentSampleRate = BASE_SAMPLE_RATE;
    private int maxLevelForCondensation = 0;
    private int maxLevelForGasSupply = 0;
    private int maxLevelForGasType = 0;
    private int maxLevelForSize = 0;
    private int maxValue = 0;
    private int minValue = 0;
    private long gatheredSupply;
    private long lastUpdateTick = 0;

    private @NotNull MutableComponent bars(int level, ChatFormatting format) {
        return Component.literal(Strings.repeat('|', level)).withStyle(format);
    }

    private MutableComponent componentHelper(String label, int level, boolean forGoggles, boolean useBlocksAsBars, ChatFormatting... styles) {
        MutableComponent blockComponent = Component.literal("█".repeat(minValue) + "▒".repeat(level - minValue) + "░".repeat(maxValue - level));
        MutableComponent barComponent = Component.empty().append(bars(Math.max(0, minValue - 1), ChatFormatting.DARK_GREEN)).append(bars(minValue > 0 ? 1 : 0, ChatFormatting.GREEN)).append(bars(Math.max(0, level - minValue), ChatFormatting.DARK_GREEN)).append(bars(Math.max(0, maxValue - level), ChatFormatting.DARK_RED)).append(bars(Math.max(0, Math.min(MAX_LEVEL - maxValue, ((maxValue / 3 + 1) * 3) - maxValue)), ChatFormatting.DARK_GRAY));
        MutableComponent base = useBlocksAsBars ? blockComponent : barComponent;

        if (!forGoggles) {
            return base;
        }

        ChatFormatting style1 = styles.length >= 1 ? styles[0] : ChatFormatting.GRAY;
        ChatFormatting style2 = styles.length >= 2 ? styles[1] : ChatFormatting.DARK_GRAY;

        return CCBLang.translateDirect("gui.goggles.gas_controller." + label).withStyle(style1).append(CCBLang.translateDirect("gui.goggles.gas_controller." + label + "_dots").withStyle(style2)).append(base);
    }

    public boolean addToGoggleTooltip(List<Component> tooltip, int size) {
        if (!isActive()) {
            return false;
        }

        calcMinMaxForSize(size);

        int level = Math.min(Math.min(maxLevelForGasSupply, maxLevelForSize), Math.min(maxLevelForCondensation, maxLevelForGasType));
        MutableComponent levelText = level == 0 ? CCBLang.translateDirect("gui.goggles.gas_controller.idle") : (level == MAX_LEVEL ? CCBLang.translateDirect("gui.goggles.gas_controller.max_level") : CCBLang.translateDirect("gui.goggles.gas_controller.level", String.valueOf(level)));
        CCBLang.translate("gui.goggles.gas_controller.status", levelText.withStyle(ChatFormatting.GREEN)).forGoggles(tooltip);

        MutableComponent sizeText = componentHelper("size", maxLevelForSize, true, false);
        CCBLang.builder().add(sizeText).forGoggles(tooltip, 1);

        MutableComponent gasSupplyText = componentHelper("gas_supply", maxLevelForGasSupply, true, false);
        CCBLang.builder().add(gasSupplyText).forGoggles(tooltip, 1);

        MutableComponent gasTypeText = componentHelper("gas_type", maxLevelForGasType, true, false);
        CCBLang.builder().add(gasTypeText).forGoggles(tooltip, 1);

        MutableComponent condensationText = componentHelper("condensation", maxLevelForCondensation, true, false);
        CCBLang.builder().add(condensationText).forGoggles(tooltip, 1);

        if (attachedEngines == 0) {
            return true;
        }

        double totalStress = BASE_ROTATION_SPEED * getCurrentLevel(size) * BlockStressValues.getCapacity(CCBBlocks.AIRTIGHT_ENGINE_BLOCK.get());

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("gui.goggles.gas_controller.gas_input_rate").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate("gui.goggles.gas_controller.milli_buckets_per_second", CCBLang.number((int) (gasSupply * 20))).style(ChatFormatting.BLUE).forGoggles(tooltip, 1);

        if (!gasType.isEmpty()) {
            tooltip.add(CommonComponents.EMPTY);
            CCBLang.translate("gui.goggles.gas_controller.gas_input_type").style(ChatFormatting.GRAY).forGoggles(tooltip);
            CCBLang.gasName(gasType).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        }

        tooltip.add(CommonComponents.EMPTY);
        if (attachedDrains == 0) {
            CCBLang.translate("gui.goggles.gas_controller.via_no_drain").style(ChatFormatting.GRAY).forGoggles(tooltip);
        } else if (attachedDrains == 1) {
            CCBLang.translate("gui.goggles.gas_controller.via_one_drain").style(ChatFormatting.GRAY).forGoggles(tooltip);
        } else {
            CCBLang.translate("gui.goggles.gas_controller.via_drains", attachedDrains).style(ChatFormatting.GRAY).forGoggles(tooltip);
        }

        tooltip.add(CommonComponents.EMPTY);
        CCBLang.translate("tooltip.capacity_provided").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.number(totalStress).translate("gui.goggles.unit.stress").style(ChatFormatting.AQUA).space().add((attachedEngines == 1 ? CCBLang.translate("gui.goggles.gas_controller.via_one_engine") : CCBLang.translate("gui.goggles.gas_controller.via_engines", attachedEngines)).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);

        return true;
    }

    public GasStack getGasType() {
        return gasType;
    }

    public boolean evaluate(@NotNull AirtightTankBlockEntity controller) {
        BlockPos controllerPos = controller.getBlockPos();
        Level level = controller.getLevel();

        if (level == null) {
            return false;
        }

        int previousEngines = attachedEngines;
        attachedEngines = 0;

        int previousDrains = attachedDrains;
        attachedDrains = 0;

        for (int yOffset = 0; yOffset < controller.getHeight(); yOffset++) {
            for (int xOffset = 0; xOffset < controller.getWidth(); xOffset++) {
                for (int zOffset = 0; zOffset < controller.getWidth(); zOffset++) {
                    BlockPos pos = controllerPos.offset(xOffset, yOffset, zOffset);
                    BlockState blockState = level.getBlockState(pos);
                    if (!AirtightTankBlock.isTank(blockState)) {
                        continue;
                    }

                    for (Direction direction : Iterate.directions) {
                        BlockPos attachedPos = pos.relative(direction);
                        BlockState attachedState = level.getBlockState(attachedPos);
                        if (CCBBlocks.AIRTIGHT_ENGINE_BLOCK.has(attachedState) && AirtightEngineBlock.getFacing(attachedState).getOpposite() == direction) {
                            attachedEngines++;
                        }
                        if (CCBBlocks.CONDENSATE_DRAIN_BLOCK.has(attachedState) && CondensateDrainBlock.getFacing(attachedState).getOpposite() == direction) {
                            attachedDrains++;
                        }
                    }
                }
            }
        }

        return previousEngines != attachedEngines || previousDrains != attachedDrains;
    }

    public boolean isActive() {
        return attachedEngines > 0;
    }

    public CompoundTag write(HolderLookup.Provider lookupProvider) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putFloat("GasSupply", gasSupply);
        compoundTag.putInt("CondensatePenalty", condensatePenaltyCooldown);
        compoundTag.putInt("CondensateSuccess", condensateSuccessCount);
        compoundTag.putInt("CondensationLevel", condensationLevel);
        compoundTag.putInt("Drains", attachedDrains);
        compoundTag.putInt("Engines", attachedEngines);
        compoundTag.putFloat("EmaGasSupply", emaGasSupply);
        compoundTag.putInt("SampleRate", currentSampleRate);
        compoundTag.putLong("LastUpdateTick", lastUpdateTick);
        compoundTag.putFloat("Stability", stabilityFactor);

        if (!gasType.isEmpty()) {
            compoundTag.put("Gas", gasType.save(lookupProvider));
        } else {
            compoundTag.putString("Gas", "Empty");
        }
        return compoundTag;
    }

    public GasControllerHandler createHandler() {
        return new GasControllerHandler();
    }

    public int getAttachedDrains() {
        return attachedDrains;
    }

    public int getAttachedEngines() {
        return attachedEngines;
    }

    public int getCondensationLevel() {
        return condensationLevel;
    }

    public void setCondensationLevel(int level) {
        condensationLevel = level;
    }

    public int getCondensatePenaltyCooldown() {
        return condensatePenaltyCooldown;
    }

    public void setCondensatePenaltyCooldown(int cooldown) {
        condensatePenaltyCooldown = cooldown;
    }

    public int getCondensateSuccessCount() {
        return condensateSuccessCount;
    }

    public void setCondensateSuccessCount(int count) {
        condensateSuccessCount = count;
    }

    public int getCurrentLevel(int size) {
        int maxLevelForSize = getMaxLevelForSize(size);
        int maxLevelForGasSupply = getMaxLevelForGasSupply();
        int maxLevelForCondensation = getMaxLevelForCondensation();
        int maxLevelForGasType = getMaxLevelForGasType();

        return Math.min(Math.min(maxLevelForSize, maxLevelForGasSupply), Math.min(maxLevelForCondensation, maxLevelForGasType));
    }

    public int getMaxLevelForCondensation() {
        return Math.min(MAX_LEVEL, condensationLevel);
    }

    public int getMaxLevelForGasSupply() {
        return Math.min(MAX_LEVEL, (int) (gasSupply / GAS_SUPPLY_PER_LEVEL));
    }

    public int getMaxLevelForSize(int size) {
        return Math.min(MAX_LEVEL, size / SIZE_PER_LEVEL);
    }

    public int getMaxLevelForGasType() {
        return (int) gasType.getGas().getEnergy();
    }

    public void calcMinMaxForSize(int size) {
        maxLevelForSize = getMaxLevelForSize(size);
        maxLevelForGasSupply = getMaxLevelForGasSupply();
        maxLevelForCondensation = getMaxLevelForCondensation();
        maxLevelForGasType = getMaxLevelForGasType();

        minValue = Math.min(Math.min(maxLevelForGasSupply, maxLevelForSize), Math.min(maxLevelForCondensation, maxLevelForGasType));
        maxValue = Math.max(Math.max(maxLevelForGasSupply, maxLevelForSize), Math.max(maxLevelForCondensation, maxLevelForGasType));
    }

    public void clear() {
        gasSupply = 0;
        attachedEngines = 0;
        attachedDrains = 0;
        condensatePenaltyCooldown = 0;
        condensateSuccessCount = 0;
        condensationLevel = 0;
        gasType = GasStack.EMPTY;
        emaGasSupply = 0;
        currentSampleRate = BASE_SAMPLE_RATE;
        lastUpdateTick = 0;
        stabilityFactor = 1f;
        gatheredSupply = 0;
    }

    public void read(@NotNull CompoundTag compoundTag, HolderLookup.Provider lookupProvider) {
        attachedDrains = compoundTag.getInt("Drains");
        attachedEngines = compoundTag.getInt("Engines");
        condensatePenaltyCooldown = compoundTag.getInt("CondensatePenalty");
        condensateSuccessCount = compoundTag.getInt("CondensateSuccess");
        condensationLevel = compoundTag.getInt("CondensationLevel");
        gasSupply = compoundTag.getFloat("GasSupply");
        emaGasSupply = compoundTag.getFloat("EmaGasSupply");
        currentSampleRate = compoundTag.getInt("SampleRate");
        lastUpdateTick = compoundTag.getLong("LastUpdateTick");
        stabilityFactor = compoundTag.getFloat("Stability");

        gasType = GasStack.parseOptional(lookupProvider, compoundTag.getCompound("Gas"));
        gatheredSupply = 0;
    }

    public void tick(AirtightTankBlockEntity controller) {
        Level level = controller.getLevel();
        if (!isActive() || level == null || level.isClientSide) {
            return;
        }

        if (condensatePenaltyCooldown > 0) {
            condensatePenaltyCooldown--;
        } else if (attachedDrains == 0) {
            condensationLevel = Mth.clamp(condensationLevel - 1, 0, MAX_LEVEL);
            condensatePenaltyCooldown = PENALTY_COOLDOWN;
        }

        long currentTick = level.getGameTime();
        long ticksPassed = currentTick - lastUpdateTick;

        if (ticksPassed >= currentSampleRate || stabilityFactor < STABILITY_THRESHOLD) {
            long capacity = controller.getTankInventory().getCapacity();
            if (capacity == 0) {
                gasType = GasStack.EMPTY;
                return;
            }

            float currentRate = 0;
            if (ticksPassed > 0) {
                currentRate = (float) gatheredSupply / ticksPassed;
            }

            if (emaGasSupply == 0) {
                emaGasSupply = currentRate;
            } else {
                emaGasSupply = ALPHA * currentRate + (1 - ALPHA) * emaGasSupply;
            }

            gasSupply = emaGasSupply;
            updateStability(currentRate);
            adjustSamplingRate();
            gatheredSupply = 0;
            lastUpdateTick = currentTick;
            if (gasSupply < 1e-2 && !gasType.isEmpty()) {
                gasSupply = 0;
                gasType = GasStack.EMPTY;
            }

            controller.notifyUpdate();
        }
    }

    private void updateStability(float currentRate) {
        float change = 0;
        if (emaGasSupply > 0) {
            change = Math.abs(currentRate - emaGasSupply) / emaGasSupply;
        }

        stabilityFactor = 1f - Math.min(1f, change * STABILITY_SENSITIVITY);
    }

    private void adjustSamplingRate() {
        if (stabilityFactor < LOW_STABILITY_THRESHOLD) {
            currentSampleRate = Math.max(2, BASE_SAMPLE_RATE - 3);
        } else if (stabilityFactor < MEDIUM_STABILITY_THRESHOLD) {
            currentSampleRate = Math.max(3, BASE_SAMPLE_RATE - 2);
        } else {
            currentSampleRate = BASE_SAMPLE_RATE;
        }
    }

    public class GasControllerHandler implements IGasHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull GasStack getGasInTank(int tank) {
            return GasStack.EMPTY;
        }

        @Override
        public long getTankCapacity(int tank) {
            return 8000;
        }

        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return true;
        }

        @Override
        public long fill(@NotNull GasStack resource, @NotNull GasAction action) {
            if (!isGasValid(0, resource)) {
                return 0;
            }
            long amount = resource.getAmount();
            if (action.execute()) {
                gatheredSupply += amount;
                if (!gasType.is(resource.getGas())) {
                    gasType = resource;
                }
            }
            return amount;
        }

        @Override
        public @NotNull GasStack drain(@NotNull GasStack resource, @NotNull GasAction action) {
            return GasStack.EMPTY;
        }

        @Override
        public @NotNull GasStack drain(long maxDrain, @NotNull GasAction action) {
            return GasStack.EMPTY;
        }
    }
}