package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import com.simibubi.create.content.logistics.filter.FilterItem;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.api.gas.canisters.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.api.gas.canisters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.drillhandlers.AirtightHandheldDrillHandler;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates.AirtightHandheldDrillMiningTemplates;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.ExperienceConversionUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.LiquidReplacementUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.MagnetUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.SilkTouchUpgrade;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBDamageTypes;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightHandheldDrillUtils {
    private AirtightHandheldDrillUtils() {
    }

    private static ItemStack createDrillUsedTool(ItemStack drill, ServerLevel level) {
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        if (SilkTouchUpgrade.INSTANCE.canApply(drill)) {
            tool.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
        }
        return tool;
    }

    private static boolean hasClearAttackLine(Player player, LivingEntity entity, Level level) {
        Vec3 from = player.getEyePosition();
        Vec3 to = entity.getBoundingBox().getCenter();
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, Fluid.NONE, player));
        return hit.getType() == Type.MISS || hit.getLocation().distanceToSqr(from) >= to.distanceToSqr(from) - 0.25;
    }

    public static @Nullable BlockPos getHitResult(Player player) {
        Vec3 eyePosition = player.getEyePosition();
        Vec3 added = eyePosition.add(player.calculateViewVector(player.getXRot(), player.getYRot()).scale(player.blockInteractionRange()));
        Level level = player.level();
        BlockHitResult result = level.clip(new ClipContext(eyePosition, added, ClipContext.Block.OUTLINE, Fluid.NONE, player));
        return result.getType() == Type.MISS ? null : result.getBlockPos();
    }

    public static AirtightHandheldDrillMiningTemplates getMiningTemplate(ItemStack drill) {
        return drill.getOrDefault(CCBDataComponents.DRILL_MINING_TEMPLATE, AirtightHandheldDrillMiningTemplates.CUBOID);
    }

    public static Direction getMiningDirection(ItemStack drill) {
        return drill.getOrDefault(CCBDataComponents.DRILL_MINING_DIRECTION, Direction.NORTH);
    }

    public static boolean isInstantBreakable(BlockPos basePos, Level level) {
        return level.getBlockState(basePos).getDestroySpeed(level, basePos) == 0;
    }

    public static boolean isRelativePositionValid(AirtightHandheldDrillMiningTemplates template, int[] size, Direction dir, int[] relPos) {
        return template.getTemplate().getOffset(size, dir, relPos).contains(BlockPos.ZERO);
    }

    public static boolean isValidFilter(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof FilterItem || item instanceof BlockItem;
    }

    public static float calculateFinalBreakSpeed(float speed, Player player, ItemStack drill, BlockPos basePos) {
        Level level = player.level();
        AirtightHandheldDrillMiningContext context = AirtightHandheldDrillMiningContext.of(drill, basePos, level);
        if (!context.isValidBaseTarget()) {
            return -2;
        }

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            return -1;
        }

        int gasConsumption = calculateGasConsumption(drill, context, gasContent.getGasType());
        if (gasConsumption < 0 || gasContent.getAmount() < gasConsumption) {
            return -1;
        }

        if (isInstantBreakable(basePos, level)) {
            return 1;
        }

        speed *= calculateMiningSizeMultiplier(context);
        speed *= calculateMiningHardnessMultiplier(context);
        if (player.getOffhandItem().is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            speed *= 2;
        }
        return speed;
    }

    public static float calculateGasConsumptionForBlock(Level level, BlockPos pos, boolean silkTouch, boolean magnet, boolean conversion, boolean liquidReplacement) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (isInstantBreakable(pos, level)) {
            return 0;
        }

        float gasConsumption = 0;
        int blockBaseGasConsumption = CCBConfig.server().equipments.perBlockConsumption.get();
        float liquidBaseGasConsumption = CCBConfig.server().equipments.liquidReplacementMultiplier.getF() * blockBaseGasConsumption;
        if (block instanceof LiquidBlock) {
            if (liquidReplacement) {
                gasConsumption += liquidBaseGasConsumption;
            }
            return gasConsumption;
        }

        if (liquidReplacement && !state.getFluidState().is(Fluids.EMPTY)) {
            gasConsumption += liquidBaseGasConsumption;
        }

        gasConsumption += blockBaseGasConsumption;
        if (silkTouch) {
            float silkTouchMultiplier = CCBConfig.server().equipments.silkTouchMultiplier.getF();
            gasConsumption *= silkTouchMultiplier;
        }
        if (magnet) {
            float magnetMultiplier = CCBConfig.server().equipments.magnetMultiplier.getF();
            gasConsumption *= magnetMultiplier;
        }
        if (conversion) {
            float conversionMultiplier = CCBConfig.server().equipments.experienceConversionMultiplier.getF();
            gasConsumption *= conversionMultiplier;
        }

        return gasConsumption;
    }

    public static float calculateMiningHardnessMultiplier(AirtightHandheldDrillMiningContext context) {
        Set<BlockPos> breakSpeedPos = context.breakSpeedPos();
        if (breakSpeedPos.isEmpty()) {
            return 1;
        }

        Level level = context.level();
        BlockPos basePos = context.basePos();
        float baseHardness = level.getBlockState(basePos).getDestroySpeed(level, basePos);
        if (baseHardness <= 0) {
            return 1;
        }

        float totalHardness = (float) breakSpeedPos.stream().mapToDouble(pos -> Math.max(0, level.getBlockState(pos).getDestroySpeed(level, pos))).sum();
        if (totalHardness <= 0) {
            return 1;
        }

        return baseHardness / totalHardness * breakSpeedPos.size();
    }

    public static float calculateMiningSizeMultiplier(AirtightHandheldDrillMiningContext context) {
        int size = context.breakSpeedPos().size();
        if (size == 0) {
            return 1;
        }

        return Mth.clamp(1 / (float) Math.pow(Math.log10(size + 9), 3), 0.01f, 1);
    }

    public static int @NotNull [] getMiningSizeParams(ItemStack drill) {
        BlockPos pos = drill.getOrDefault(CCBDataComponents.DRILL_MINING_SIZE, new BlockPos(1, 1, 1));
        return new int[]{pos.getX(), pos.getY(), pos.getZ()};
    }

    public static int @NotNull [] getRelativePositionParams(ItemStack drill) {
        BlockPos pos = drill.getOrDefault(CCBDataComponents.DRILL_MINING_RELATIVE_POSITION, new BlockPos(0, 0, 0));
        return new int[]{pos.getX(), pos.getY(), pos.getZ()};
    }

    public static int calculateGasConsumption(ItemStack drill, AirtightHandheldDrillMiningContext context, Gas gasType) {
        Set<BlockPos> destructionPos = context.destructionPos();
        if (destructionPos.isEmpty()) {
            return -1;
        }

        AirtightHandheldDrillHandler drillHandler = AirtightHandheldDrillHandler.REGISTRY.get(gasType);
        if (drillHandler == null) {
            return -1;
        }

        boolean silkTouch = SilkTouchUpgrade.INSTANCE.canApply(drill);
        boolean magnet = MagnetUpgrade.INSTANCE.canApply(drill);
        boolean experienceConversion = ExperienceConversionUpgrade.INSTANCE.canApply(drill);
        boolean liquidReplacement = LiquidReplacementUpgrade.INSTANCE.canApply(drill);
        double totalConsumption = destructionPos.stream().mapToDouble(pos -> calculateGasConsumptionForBlock(context.level(), pos, silkTouch, magnet, experienceConversion, liquidReplacement)).sum();
        return Mth.ceil(1.5 * drillHandler.getConsumptionMultiplier() * Math.pow(totalConsumption, Math.log(2.25)));
    }

    public static void destroyBlockAs(ServerLevel level, BlockPos basePos, BlockPos pos, Player player, ItemStack usedTool, boolean magnet, boolean conversion, boolean liquidReplacement) {
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
            CCBAdvancements.EVEN_HARDER_THAN_OBSIDIAN.awardTo(player);
        }

        if (!pos.equals(basePos)) {
            Vec3 center = VecHelper.getCenterOf(pos);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), center.x, center.y, center.z, 16, 0, 0, 0, 0);
        }

        BlockDropsEvent dropsEvent = null;
        boolean shouldDrop = level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && !level.restoringBlockSnapshots;
        if (shouldDrop && !conversion) {
            List<ItemEntity> dropEntities = new ArrayList<>();
            for (ItemStack stack : Block.getDrops(state, level, pos, blockEntity, player, usedTool)) {
                if (stack.isEmpty()) {
                    continue;
                }

                dropEntities.add(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
            }

            dropsEvent = new BlockDropsEvent(level, pos, state, blockEntity, dropEntities, player, usedTool);
            NeoForge.EVENT_BUS.post(dropsEvent);
        }

        if (liquidReplacement && !state.getFluidState().is(Fluids.EMPTY)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        else {
            level.removeBlock(pos, false);
        }

        if (!shouldDrop) {
            return;
        }

        if (conversion && new ItemStack(block.asItem()).canFitInsideContainerItems()) {
            int experienceAmount = Mth.ceil(state.getDestroySpeed(level, pos) / 10);
            if (experienceAmount > 0) {
                if (magnet) {
                    player.giveExperiencePoints(experienceAmount);
                }
                else {
                    block.popExperience(level, pos, experienceAmount);
                }
            }
            state.spawnAfterBreak(level, pos, usedTool, true);
        }
        else if (dropsEvent != null && !dropsEvent.isCanceled()) {
            for (ItemEntity dropEntity : dropsEvent.getDrops()) {
                ItemStack drop = dropEntity.getItem();
                if (drop.isEmpty()) {
                    continue;
                }

                if (magnet) {
                    ItemHandlerHelper.giveItemToPlayer(player, drop);
                }
                else {
                    level.addFreshEntity(dropEntity);
                }
            }

            int droppedExperience = dropsEvent.getDroppedExperience();
            if (droppedExperience > 0) {
                if (magnet) {
                    player.giveExperiencePoints(droppedExperience);
                }
                else {
                    block.popExperience(level, pos, droppedExperience);
                }
            }

            state.spawnAfterBreak(level, pos, usedTool, true);
        }
    }

    public static void doDrillAttack(Player player, Level level) {
        double range = player.blockInteractionRange();
        Vec3 eyePosition = player.getEyePosition();
        Vec3 viewVector = player.calculateViewVector(player.getXRot(), player.getYRot());
        Gas gasType = CanisterContainerSuppliers.getFirstAvailableGasContent(player).getGasType();
        if (CanisterContainerSuppliers.getFirstAvailableGasContent(player).isEmpty()) {
            return;
        }

        AirtightHandheldDrillHandler drillHandler = AirtightHandheldDrillHandler.REGISTRY.get(gasType);
        if (drillHandler == null) {
            return;
        }

        int perEntityHit = CCBConfig.server().equipments.perEntityHitConsumption.get();
        DamageSource damageSource = CCBDamageTypes.source(DamageTypes.THORNS, level, player);
        List<LivingEntity> vulnerableEntities = new ArrayList<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range, range, range))) {
            if (entity.is(player)) {
                continue;
            }

            if (entity.isInvulnerableTo(damageSource) || entity.isInvulnerable()) {
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

            if (!hasClearAttackLine(player, entity, level)) {
                continue;
            }

            vulnerableEntities.add(entity);
        }

        if (vulnerableEntities.isEmpty()) {
            return;
        }

        vulnerableEntities.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        int maxGasConsumption = Mth.ceil(perEntityHit * drillHandler.getConsumptionMultiplier() * vulnerableEntities.size());
        if (!CanisterContainerConsumers.interactContainer(player, gasType, maxGasConsumption, () -> true, true)) {
            GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.insufficient_gas", CanisterContainerSuppliers.getFirstAvailableGasContent(player).getHoverName());
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

        long gasConsumption = Mth.ceil(perEntityHit * drillHandler.getConsumptionMultiplier() * damagedCount);
        CanisterContainerConsumers.interactContainer(player, gasType, gasConsumption, () -> true, false);
    }

    public static void mineAreaBlocks(ItemStack drill, ServerLevel level, BlockPos basePos, Player player) {
        AirtightHandheldDrillMiningContext context = AirtightHandheldDrillMiningContext.of(drill, basePos, level);
        if (context.isEmpty() || !context.isValidBaseTarget()) {
            return;
        }

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            return;
        }

        Gas gasType = gasContent.getGasType();
        int gasConsumption = calculateGasConsumption(drill, context, gasType);
        if (gasConsumption < 0) {
            return;
        }

        if (isInstantBreakable(basePos, level) && context.destructionPos().stream().anyMatch(pos -> !isInstantBreakable(pos, level))) {
            return;
        }

        ItemStack tool = createDrillUsedTool(drill, level);
        boolean consumedGas = CanisterContainerConsumers.interactContainer(player, gasType, gasConsumption, () -> true, false);
        if (!consumedGas) {
            GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.insufficient_gas", gasContent.getHoverName());
            return;
        }

        if (context.destructionPos().size() >= 64) {
            CCBAdvancements.MINI_TUNNEL_BORER.awardTo(player);
        }

        boolean magnet = MagnetUpgrade.INSTANCE.canApply(drill);
        boolean experienceConversion = ExperienceConversionUpgrade.INSTANCE.canApply(drill);
        boolean liquidReplacement = LiquidReplacementUpgrade.INSTANCE.canApply(drill);
        for (BlockPos targetPos : context.destructionPos()) {
            destroyBlockAs(level, basePos, targetPos, player, tool, magnet, experienceConversion, liquidReplacement);
        }
    }
}
