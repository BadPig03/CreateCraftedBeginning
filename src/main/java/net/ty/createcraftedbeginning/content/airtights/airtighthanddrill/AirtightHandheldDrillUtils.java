package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.foundation.item.ItemHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.ty.createcraftedbeginning.CreateCraftedBeginningClient;
import net.ty.createcraftedbeginning.api.gas.cansiters.GasCanisterExecuteUtils;
import net.ty.createcraftedbeginning.api.gas.cansiters.GasCanisterSupplierUtils;
import net.ty.createcraftedbeginning.api.gas.drillhandlers.AirtightHandheldDrillHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillMenu.DrillItemHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates.AirtightHandheldDrillMiningTemplates;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBDamageTypes;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class AirtightHandheldDrillUtils {
    public static final Item SILK_TOUCH_UPGRADE_ITEM = AllItems.PRECISION_MECHANISM.asItem();
    public static final Item MAGNET_UPGRADE_ITEM = AllItems.BRASS_HAND.asItem();
    public static final Item CONVERSION_UPGRADE_ITEM = Items.ECHO_SHARD.asItem();
    public static final Item LIQUID_REPLACEMENT_UPGRADE_ITEM = Items.SPONGE.asItem();

    private static final int SLOT_LENGTH = 18;

    private AirtightHandheldDrillUtils() {
    }

    public static @NotNull Direction getMiningDirection(@NotNull ItemStack drill) {
        return drill.getOrDefault(CCBDataComponents.DRILL_MINING_DIRECTION, Direction.NORTH);
    }

    public static @NotNull DrillItemHandler getInventoryHandler(@NotNull ItemStack drill) {
        DrillItemHandler handler = new DrillItemHandler();
        if (!drill.has(CCBDataComponents.DRILL_INVENTORY)) {
            return handler;
        }

        ItemHelper.fillItemStackHandler(Objects.requireNonNull(drill.get(CCBDataComponents.DRILL_INVENTORY)), handler);
        return handler;
    }

    public static @NotNull Set<BlockPos> getInstantDestructionPos(@NotNull Level level, @NotNull Set<BlockPos> totalPos) {
        return totalPos.stream().filter(pos -> level.getBlockState(pos).getDestroySpeed(level, pos) == 0).collect(Collectors.toSet());
    }

    public static @NotNull Set<BlockPos> getLiquidPos(@NotNull Level level, @NotNull Set<BlockPos> totalPos) {
        return totalPos.stream().filter(pos -> level.getBlockState(pos).getBlock() instanceof LiquidBlock).collect(Collectors.toSet());
    }

    public static @NotNull Set<BlockPos> getTotalPos(ItemStack drill, @NotNull BlockPos base) {
        return getMiningTemplate(drill).getTemplate().getFinalOffset(drill).stream().map(base::offset).collect(Collectors.toSet());
    }

    public static @NotNull Set<BlockPos> getUnbreakablePos(@NotNull Level level, @NotNull Set<BlockPos> totalPos) {
        return totalPos.stream().filter(pos -> level.getBlockState(pos).getDestroySpeed(level, pos) == -1).collect(Collectors.toSet());
    }

    public static @Nullable BlockPos getHitResult(@NotNull Player player) {
        Vec3 eyePosition = player.getEyePosition();
        Vec3 added = eyePosition.add(player.calculateViewVector(player.getXRot(), player.getYRot()).scale(player.blockInteractionRange()));
        Level level = player.level();
        BlockHitResult result = level.clip(new ClipContext(eyePosition, added, ClipContext.Block.OUTLINE, Fluid.NONE, player));
        return result.getType() == Type.MISS ? null : result.getBlockPos();
    }

    public static boolean canPassContainerTest(ItemStack drill, BlockPos pos, @NotNull Level level) {
        return !isContainerDisabled(drill) && level.getCapability(ItemHandler.BLOCK, pos, null) != null;
    }

    public static boolean canPassFilterTest(ItemStack drill, ItemStack testItemStack, Level level) {
        if (isFilterDisabled(drill)) {
            return false;
        }

        ItemStack filterStack = getInventoryHandler(drill).getStackInSlot(AirtightHandheldDrillMenu.FILTER_SLOT_INDEX);
        return !filterStack.isEmpty() && FilterItemStack.of(filterStack).test(level, testItemStack);
    }

    public static boolean canPassLiquidTest(ItemStack drill, BlockState state) {
        return isLiquidReplacementEnabled(drill) && state.getBlock() instanceof LiquidBlock;

    }

    public static boolean isContainerDisabled(@NotNull ItemStack drill) {
        return (drill.getOrDefault(CCBDataComponents.DRILL_OPTION_FLAGS, 0) & AirtightHandheldDrillMenu.CONTAINER_DISABLED_FLAG) != 0;
    }

    public static boolean isConversionEnabled(@NotNull ItemStack drill) {
        return (drill.getOrDefault(CCBDataComponents.DRILL_OPTION_FLAGS, 0) & AirtightHandheldDrillMenu.CONVERSION_ENABLED_FLAG) != 0;
    }

    public static boolean isDrillAttackEnabled(@NotNull ItemStack drill) {
        return (drill.getOrDefault(CCBDataComponents.DRILL_OPTION_FLAGS, 0) & AirtightHandheldDrillMenu.DRILL_ATTACK_DISABLED_FLAG) == 0;
    }

    public static boolean isFilterDisabled(@NotNull ItemStack drill) {
        return (drill.getOrDefault(CCBDataComponents.DRILL_OPTION_FLAGS, 0) & AirtightHandheldDrillMenu.FILTER_DISABLED_FLAG) != 0;
    }

    public static boolean isInstantBreakable(BlockPos basePos, @NotNull Level level) {
        return level.getBlockState(basePos).getDestroySpeed(level, basePos) == 0;
    }

    public static boolean isLiquidReplacementEnabled(@NotNull ItemStack drill) {
        return (drill.getOrDefault(CCBDataComponents.DRILL_OPTION_FLAGS, 0) & AirtightHandheldDrillMenu.LIQUID_REPLACEMENT_ENABLED_FLAG) != 0;
    }

    public static boolean isMagnetEnabled(@NotNull ItemStack drill) {
        return (drill.getOrDefault(CCBDataComponents.DRILL_OPTION_FLAGS, 0) & AirtightHandheldDrillMenu.MAGNET_ENABLED_FLAG) != 0;
    }

    public static boolean isMouseOverSlot(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseY >= y && mouseX < x + SLOT_LENGTH && mouseY < y + SLOT_LENGTH;
    }

    public static boolean isOutlineEnabled(@NotNull ItemStack drill) {
        return (drill.getOrDefault(CCBDataComponents.DRILL_OPTION_FLAGS, 0) & AirtightHandheldDrillMenu.OUTLINE_DISABLED_FLAG) == 0;
    }

    public static boolean isSilkTouchEnabled(@NotNull ItemStack drill) {
        return (drill.getOrDefault(CCBDataComponents.DRILL_OPTION_FLAGS, 0) & AirtightHandheldDrillMenu.SILK_TOUCH_ENABLED_FLAG) != 0;
    }

    public static boolean isValidFilter(@NotNull ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof FilterItem || item instanceof BlockItem;
    }

    public static boolean isValidMiningTarget(ItemStack drill, BlockPos basePos, @NotNull Level level) {
        BlockState state = level.getBlockState(basePos);
        Block block = state.getBlock();
        return !(state.getDestroySpeed(level, basePos) < 0) && !canPassLiquidTest(drill, state) && !canPassContainerTest(drill, basePos, level) && !canPassFilterTest(drill, new ItemStack(block.asItem()), level);
    }

    public static boolean shouldCauseBlockBreakReset(@NotNull ItemStack oldStack, @NotNull ItemStack newStack) {
        if (!newStack.is(oldStack.getItem())) {
            return true;
        }

        if (!newStack.isDamageableItem() || !oldStack.isDamageableItem()) {
            return !ItemStack.isSameItemSameComponents(newStack, oldStack);
        }

        DataComponentMap newComponents = newStack.getComponents();
        DataComponentMap oldComponents = oldStack.getComponents();
        if (newComponents.isEmpty() || oldComponents.isEmpty()) {
            return !(newComponents.isEmpty() && oldComponents.isEmpty());
        }

        Set<DataComponentType<?>> newKeys = new HashSet<>(newComponents.keySet());
        Set<DataComponentType<?>> oldKeys = new HashSet<>(oldComponents.keySet());
        newKeys.remove(DataComponents.DAMAGE);
        oldKeys.remove(DataComponents.DAMAGE);
        return !newKeys.equals(oldKeys) || !newKeys.stream().allMatch(key -> Objects.equals(newComponents.get(key), oldComponents.get(key)));
    }

    public static float calculateConsumptionForBlock(@NotNull Level level, BlockPos pos, boolean silkTouch, boolean magnet, boolean conversion, boolean liquidReplacement) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (isInstantBreakable(pos, level)) {
            return 0;
        }

        float gasConsumption = 0;
        int liquidBaseGasConsumption = CCBConfig.server().equipments.drillGasCostPerLiquidBlock.get();
        if (block instanceof LiquidBlock) {
            if (liquidReplacement) {
                gasConsumption += liquidBaseGasConsumption;
            }
            return gasConsumption;
        }

        if (liquidReplacement && !state.getFluidState().is(Fluids.EMPTY)) {
            gasConsumption += liquidBaseGasConsumption;
        }
        int blockBaseGasConsumption = CCBConfig.server().equipments.drillGasCostPerBlock.get();
        gasConsumption += blockBaseGasConsumption;

        if (silkTouch) {
            float silkTouchMultiplier = CCBConfig.server().equipments.drillGasMultiplierForSilkTouch.getF();
            gasConsumption *= silkTouchMultiplier;
        }
        if (magnet) {
            float magnetMultiplier = CCBConfig.server().equipments.drillGasMultiplierForMagnet.getF();
            gasConsumption *= magnetMultiplier;
        }
        if (conversion) {
            float conversionMultiplier = CCBConfig.server().equipments.drillGasMultiplierForConversion.getF();
            gasConsumption *= conversionMultiplier;
        }

        return gasConsumption;
    }

    public static float calculateFinalBreakSpeed(float speed, Player player, ItemStack drill, @NotNull BlockPos basePos) {
        if (GasCanisterSupplierUtils.noUsableGasAvailable(player)) {
            return -1;
        }

        Level level = player.level();
        if (!isValidMiningTarget(drill, basePos, level)) {
            return -2;
        }
        if (isInstantBreakable(basePos, level)) {
            return 1;
        }

        GasStack availableGas = GasCanisterSupplierUtils.getFirstNonEmptyGasContent(player);
        int requiredGas = calculateRequiredGasForMining(drill, level, basePos);
        if (availableGas.getAmount() < requiredGas) {
            return -1;
        }

        speed *= calculateMiningSizeMultiplier(drill, basePos, level);
        speed *= calculateMiningHardnessMultiplier(drill, basePos, level);
        if (player.getOffhandItem().is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            speed *= 2;
        }
        return speed;
    }

    public static float calculateMiningHardnessMultiplier(ItemStack drill, BlockPos basePos, @NotNull Level level) {
        Set<BlockPos> destructionPos = getDestructionPos(drill, basePos, level, false);
        if (destructionPos.isEmpty()) {
            return 1;
        }

        float baseHardness = level.getBlockState(basePos).getDestroySpeed(level, basePos);
        float totalHardness = (float) destructionPos.stream().mapToDouble(pos -> level.getBlockState(pos).getDestroySpeed(level, pos)).sum();
        return baseHardness <= 0 || totalHardness <= 0 ? 1 : baseHardness / totalHardness * destructionPos.size();
    }

    public static float calculateMiningSizeMultiplier(ItemStack drill, BlockPos basePos, Level level) {
        return Mth.clamp(1 / (float) Math.pow(Math.log10(getDestructionPos(drill, basePos, level, false).size() + 9), 3), 0.01f, 1);
    }

    public static int calculateRequiredGasForMining(ItemStack drill, Level level, BlockPos basePos) {
        Set<BlockPos> destructionPos = getDestructionPos(drill, basePos, level, true);
        if (destructionPos.isEmpty()) {
            return -1;
        }

        boolean isSilkTouchEnabled = isSilkTouchEnabled(drill);
        boolean isMagnetEnabled = isMagnetEnabled(drill);
        boolean isConversionEnabled = isConversionEnabled(drill);
        boolean isLiquidReplacementEnabled = isLiquidReplacementEnabled(drill);

        double totalConsumption = destructionPos.stream().mapToDouble(pos -> calculateConsumptionForBlock(level, pos, isSilkTouchEnabled, isMagnetEnabled, isConversionEnabled, isLiquidReplacementEnabled)).sum();
        return Mth.ceil(1.5 * Math.pow(totalConsumption, Math.log(2.25)));
    }

    public static int @NotNull [] getMiningSizeParams(@NotNull ItemStack drill) {
        BlockPos pos = drill.getOrDefault(CCBDataComponents.DRILL_MINING_SIZE, new BlockPos(1, 1, 1));
        return new int[]{pos.getX(), pos.getY(), pos.getZ()};
    }

    public static int @NotNull [] getRelativePositionParams(@NotNull ItemStack drill) {
        BlockPos pos = drill.getOrDefault(CCBDataComponents.DRILL_MINING_RELATIVE_POSITION, new BlockPos(0, 0, 0));
        return new int[]{pos.getX(), pos.getY(), pos.getZ()};
    }

    public static Set<BlockPos> getDestructionPos(ItemStack drill, BlockPos basePos, Level level, boolean destroyExtra) {
        Set<BlockPos> totalPos = getTotalPos(drill, basePos);
        Set<BlockPos> excludedPos = new HashSet<>();
        excludedPos.addAll(getUnbreakablePos(level, totalPos));
        excludedPos.addAll(getProtectedPos(level, drill, totalPos));
        if (!destroyExtra) {
            excludedPos.addAll(getLiquidPos(level, totalPos));
            excludedPos.addAll(getInstantDestructionPos(level, totalPos));
        }
        return totalPos.stream().filter(pos -> !excludedPos.contains(pos)).collect(Collectors.toSet());
    }

    public static Set<BlockPos> getProtectedPos(@NotNull Level level, ItemStack drill, @NotNull Set<BlockPos> totalPos) {
        return totalPos.stream().filter(pos -> canPassFilterTest(drill, new ItemStack(level.getBlockState(pos).getBlock().asItem()), level) || canPassContainerTest(drill, pos, level)).collect(Collectors.toSet());
    }

    @OnlyIn(Dist.CLIENT)
    public static void appendHoverText(ItemStack drill, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || GasCanisterSupplierUtils.noUsableGasAvailable(player)) {
            return;
        }

        GasStack gasStack = GasCanisterSupplierUtils.getFirstNonEmptyGasContent(player);
        if (gasStack.isEmpty()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(CCBLang.gasName(gasStack).add(CCBLang.translate("gui.tooltips.gas_tools.content")).style(ChatFormatting.GRAY).component());
        AirtightHandheldDrillHandler drillHandler = AirtightHandheldDrillHandler.REGISTRY.get(gasStack.getGas());
        if (drillHandler == null) {
            return;
        }

        drillHandler.appendHoverText(drill, context, tooltip, tooltipFlag);
    }

    public static void configureDrill(@NotNull ItemStack drill, int optionFlags, AirtightHandheldDrillMiningTemplates template, BlockPos sizeParams, Direction direction, BlockPos relativeParams) {
        drill.set(CCBDataComponents.DRILL_OPTION_FLAGS, optionFlags);
        drill.set(CCBDataComponents.DRILL_MINING_TEMPLATE, template);
        drill.set(CCBDataComponents.DRILL_MINING_SIZE, sizeParams);
        drill.set(CCBDataComponents.DRILL_MINING_DIRECTION, direction);
        drill.set(CCBDataComponents.DRILL_MINING_RELATIVE_POSITION, relativeParams);
    }

    public static void destroyBlockAs(@NotNull ServerLevel level, BlockPos basePos, BlockPos pos, @NotNull Player player, ItemStack usedTool, boolean magnet, boolean conversion, boolean liquidReplacement) {
        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        Block block = state.getBlock();
        if (block instanceof LiquidBlock) {
            if (liquidReplacement) {
                if (!pos.equals(basePos)) {
                    Vec3 v = VecHelper.getCenterOf(pos);
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), v.x, v.y, v.z, 16, 0, 0, 0, 0);
                }
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            return;
        }

        BreakEvent breakEvent = new BreakEvent(level, pos, state, player);
        NeoForge.EVENT_BUS.post(breakEvent);
        if (breakEvent.isCanceled()) {
            return;
        }

        usedTool.mineBlock(level, state, pos, player);
        player.awardStat(Stats.ITEM_USED.get(usedTool.getItem()), -1);
        player.awardStat(Stats.BLOCK_MINED.get(block));
        if (state.is(Blocks.REINFORCED_DEEPSLATE)) {
            CCBAdvancements.EVEN_IF_HARDER_THAN_OBSIDIAN.awardTo(player);
        }

        if (!pos.equals(basePos)) {
            Vec3 v = VecHelper.getCenterOf(pos);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), v.x, v.y, v.z, 16, 0, 0, 0, 0);
        }

        if (!level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) || level.restoringBlockSnapshots) {
            return;
        }

        if (conversion && block.asItem().canFitInsideContainerItems()) {
            int experienceAmount = Mth.ceil(state.getDestroySpeed(level, pos) / 10);
            if (experienceAmount > 0) {
                if (magnet) {
                    player.giveExperiencePoints(experienceAmount);
                }
                else {
                    block.popExperience(level, pos, experienceAmount);
                }
            }
        }
        else {
            BlockDropsEvent dropsEvent = new BlockDropsEvent(level, pos, state, blockEntity, List.of(), player, usedTool);
            NeoForge.EVENT_BUS.post(dropsEvent);
            int droppedExperience = dropsEvent.isCanceled() ? 0 : dropsEvent.getDroppedExperience();
            List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, player, usedTool);
            for (ItemStack drop : drops) {
                if (drop.isEmpty()) {
                    continue;
                }

                if (magnet) {
                    player.getInventory().placeItemBackInInventory(drop);
                    if (droppedExperience > 0) {
                        player.giveExperiencePoints(droppedExperience);
                    }
                }
                else {
                    Block.popResource(level, pos, drop);
                    if (droppedExperience > 0) {
                        block.popExperience(level, pos, droppedExperience);
                    }
                }
            }
        }

        if (block instanceof IceBlock iceBlock && usedTool.getEnchantmentLevel(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH)) == 0) {
            iceBlock.playerDestroy(level, player, pos, state, blockEntity, usedTool);
        }
        else {
            if (liquidReplacement && !state.getFluidState().is(Fluids.EMPTY)) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            else {
                level.removeBlock(pos, false);
            }
        }
        state.spawnAfterBreak(level, pos, ItemStack.EMPTY, true);
    }

    public static void doDrillAttack(@NotNull Player player, @NotNull Level level) {
        double range = player.blockInteractionRange();
        Vec3 eyePosition = player.getEyePosition();
        Vec3 viewVector = player.calculateViewVector(player.getXRot(), player.getYRot());
        GasStack gasStack = GasCanisterSupplierUtils.getTotalGasStack(player);
        if (gasStack.isEmpty()) {
            return;
        }

        AirtightHandheldDrillHandler drillHandler = AirtightHandheldDrillHandler.REGISTRY.get(gasStack.getGas());
        if (drillHandler == null) {
            return;
        }

        int entityHitGasConsumption = CCBConfig.server().equipments.drillGasCostPerEntityHit.get();
        DamageSource damageSource = CCBDamageTypes.source(DamageTypes.THORNS, level, player);
        List<LivingEntity> vulnerableEntities = new ArrayList<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range, range, range))) {
            if (entity.is(player)) {
                continue;
            }

            Vec3 toEntity = entity.position().subtract(eyePosition);
            double distance = toEntity.length();
            if (distance > range) {
                continue;
            }

            double dotProduct = viewVector.dot(toEntity.normalize());
            if (dotProduct < 0.5) {
                continue;
            }

            if (entity.isInvulnerableTo(damageSource) || entity.isInvulnerable()) {
                continue;
            }

            vulnerableEntities.add(entity);
        }

        if (vulnerableEntities.isEmpty()) {
            return;
        }

        vulnerableEntities.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        int totalGasConsumption = Mth.ceil(entityHitGasConsumption * drillHandler.getConsumptionMultiplier() * vulnerableEntities.size());
        if (gasStack.getAmount() < totalGasConsumption) {
            GasCanisterExecuteUtils.displayCustomWarningHint(player, "gui.warnings.insufficient_gas", gasStack.getHoverName());
            return;
        }

        int damagedCount = 0;
        for (LivingEntity entity : vulnerableEntities) {
            int damageAmount = AirtightHandheldDrillHandler.BASE_DAMAGE_AMOUNT + drillHandler.getDamageAddition();
            if (!entity.hurt(damageSource, damageAmount)) {
                continue;
            }

            damagedCount++;
            if (!(level instanceof ServerLevel serverLevel)) {
                continue;
            }

            drillHandler.extraBehaviour(entity, player, serverLevel);
        }
        GasCanisterExecuteUtils.tryGasConsumption(player, gasStack.getGas(), Mth.ceil(entityHitGasConsumption * drillHandler.getConsumptionMultiplier() * damagedCount));
    }

    public static void mineAreaBlocks(ItemStack drill, @NotNull ServerLevel level, @NotNull BlockPos basePos, @NotNull Player player) {
        GasStack gasStack = GasCanisterSupplierUtils.getFirstNonEmptyGasContent(player);
        if (gasStack.isEmpty()) {
            return;
        }

        int requiredGas = calculateRequiredGasForMining(drill, level, basePos);
        if (requiredGas < 0) {
            return;
        }

        Set<BlockPos> destructionPos = getDestructionPos(drill, basePos, level, true);
        if (destructionPos.isEmpty()) {
            return;
        }
        if (isInstantBreakable(basePos, level) && destructionPos.stream().anyMatch(pos -> !isInstantBreakable(pos, level))) {
            return;
        }

        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        boolean isSilkTouchEnabled = isSilkTouchEnabled(drill);
        if (isSilkTouchEnabled) {
            tool.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
        }

        boolean isMagnetEnabled = isMagnetEnabled(drill);
        boolean isConversionEnabled = isConversionEnabled(drill);
        boolean isLiquidReplacementEnabled = isLiquidReplacementEnabled(drill);
        if (!GasCanisterExecuteUtils.tryGasConsumption(player, gasStack.getGas(), requiredGas)) {
            GasCanisterExecuteUtils.displayCustomWarningHint(player, "gui.warnings.insufficient_gas", gasStack.getHoverName());
            return;
        }

        if (destructionPos.size() >= 64) {
            CCBAdvancements.MINI_TBM.awardTo(player);
        }
        for (BlockPos targetPos : destructionPos) {
            destroyBlockAs(level, basePos, targetPos, player, tool, isMagnetEnabled, isConversionEnabled, isLiquidReplacementEnabled);
        }
    }

    @SuppressWarnings("ConstantExpression")
    public static void tryUpdateAnimation(@NotNull Player player) {
        ItemStack drill = player.getMainHandItem();
        Level level = player.level();
        if (!drill.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            if (level.isClientSide) {
                AirtightHandheldDrillRenderHandler renderHandler = CreateCraftedBeginningClient.AIRTIGHT_HAND_DRILL_RENDER_HANDLER;
                if (renderHandler.hasHandAnimation(0)) {
                    renderHandler.stop();
                }
            }
            else {
                CompoundTag tag = player.getPersistentData();
                if (tag.getFloat(AirtightHandheldDrillAnimationPacket.COMPOUND_KEY_ANIMATION) != 0) {
                    tag.putFloat(AirtightHandheldDrillAnimationPacket.COMPOUND_KEY_ANIMATION, 0);
                }
            }
            return;
        }

        if (level.isClientSide) {
            AirtightHandheldDrillRenderHandler renderHandler = CreateCraftedBeginningClient.AIRTIGHT_HAND_DRILL_RENDER_HANDLER;
            boolean hasAnimation = renderHandler.hasHandAnimation(0);
            if (GasCanisterSupplierUtils.noUsableGasAvailable(player)) {
                if (hasAnimation) {
                    renderHandler.stop();
                    CatnipServices.NETWORK.sendToServer(new AirtightHandheldDrillAnimationPacket(0));
                }
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            boolean shouldRotate = minecraft.options.keyAttack.isDown() || minecraft.options.keyUse.isDown();
            if (shouldRotate && !hasAnimation) {
                renderHandler.start();
                CatnipServices.NETWORK.sendToServer(new AirtightHandheldDrillAnimationPacket(0.1f));
            }
            else if (!shouldRotate && hasAnimation) {
                renderHandler.stop();
                CatnipServices.NETWORK.sendToServer(new AirtightHandheldDrillAnimationPacket(0));
            }
            else if (shouldRotate && level.getGameTime() % 5 == 0) {
                CatnipServices.NETWORK.sendToServer(new AirtightHandheldDrillAnimationPacket(renderHandler.handAnimation));
            }
            return;
        }

        float animation = player.getPersistentData().getFloat(AirtightHandheldDrillAnimationPacket.COMPOUND_KEY_ANIMATION);
        if (GasCanisterSupplierUtils.noUsableGasAvailable(player) || !isDrillAttackEnabled(drill) || animation < 2 / 3.0f) {
            return;
        }

        doDrillAttack(player, level);
    }

    public static @NotNull AirtightHandheldDrillMiningTemplates getMiningTemplate(@NotNull ItemStack drill) {
        return drill.getOrDefault(CCBDataComponents.DRILL_MINING_TEMPLATE, AirtightHandheldDrillMiningTemplates.CUBOID);
    }
}
