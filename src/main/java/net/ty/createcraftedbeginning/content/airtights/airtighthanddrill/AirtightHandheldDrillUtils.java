package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import com.simibubi.create.content.logistics.filter.FilterItem;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates.AirtightHandheldDrillMiningTemplates;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.ExperienceConversionUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.LiquidReplacementUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.MagnetUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.SilkTouchUpgrade;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers.AffordableFuel;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerSuppliers;
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
import java.util.Optional;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightHandheldDrillUtils {
    private static final int MAX_ADDITIONAL_BREAK_PARTICLE_BLOCKS = 64;
    private static final ThreadLocal<Integer> ADDITIONAL_BLOCK_BREAK_DEPTH = ThreadLocal.withInitial(() -> 0);

    private AirtightHandheldDrillUtils() {
    }

    public static int @NotNull [] getMiningSizeParams(ItemStack drill) {
        BlockPos pos = drill.getOrDefault(CCBDataComponents.DRILL_MINING_SIZE, new BlockPos(1, 1, 1));
        return new int[]{pos.getX(), pos.getY(), pos.getZ()};
    }

    public static int @NotNull [] getRelativePositionParams(ItemStack drill) {
        BlockPos pos = drill.getOrDefault(CCBDataComponents.DRILL_MINING_RELATIVE_POSITION, new BlockPos(0, 0, 0));
        return new int[]{pos.getX(), pos.getY(), pos.getZ()};
    }

    public static Direction getMiningDirection(ItemStack drill) {
        return drill.getOrDefault(CCBDataComponents.DRILL_MINING_DIRECTION, Direction.NORTH);
    }

    static ItemStack createDrillUsedTool(ItemStack drill, ServerLevel level) {
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        tool.set(DataComponents.ENCHANTMENTS, drill.getTagEnchantments());
        if (SilkTouchUpgrade.INSTANCE.canApply(drill)) {
            tool.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
        }
        return tool;
    }

    static @Nullable BlockPos getHitResult(Player player) {
        Vec3 eyePosition = player.getEyePosition();
        Vec3 end = eyePosition.add(player.calculateViewVector(player.getXRot(), player.getYRot()).scale(player.blockInteractionRange()));
        Level level = player.level();
        BlockHitResult hit = level.clip(new ClipContext(eyePosition, end, ClipContext.Block.OUTLINE, Fluid.NONE, player));
        if (hit.getType() == Type.MISS) {
            return null;
        }
        return hit.getBlockPos();
    }

    static AirtightHandheldDrillMiningTemplates getMiningTemplate(ItemStack drill) {
        return drill.getOrDefault(CCBDataComponents.DRILL_MINING_TEMPLATE, AirtightHandheldDrillMiningTemplates.CUBOID);
    }

    static boolean isRelativePositionValid(AirtightHandheldDrillMiningTemplates template, int[] size, Direction dir, int[] relPos) {
        return !template.getTemplate().usesSpatialParameters() || template.getTemplate().getOffset(size, dir, relPos).contains(BlockPos.ZERO);
    }

    static boolean isValidFilter(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof FilterItem || item instanceof BlockItem;
    }

    static float calculateFinalBreakSpeed(float speed, Player player, ItemStack drill, BlockPos basePos) {
        Level level = player.level();
        AirtightHandheldDrillMiningContext context = AirtightHandheldDrillMiningContext.of(drill, basePos, level);
        if (!context.isValidBaseTarget()) {
            return -2;
        }

        Optional<AffordableFuel> fuel = findAffordableDrillFuel(player, drill, context);
        if (fuel.isEmpty()) {
            return -1;
        }

        if (isInstantBreakable(basePos, level)) {
            return 1;
        }

        speed *= calculateMiningSizeMultiplier(context);
        speed *= calculateMiningHardnessMultiplier(context);
        if (!player.getOffhandItem().is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return speed;
        }
        return speed * 2;
    }

    static void doDrillAttack(Player player, Level level) {
        double range = player.blockInteractionRange();
        Vec3 eyePosition = player.getEyePosition();
        Vec3 viewVector = player.calculateViewVector(player.getXRot(), player.getYRot());
        int perEntityHit = CCBConfig.server().equipments.perEntityHitConsumption.get();
        DamageSource damageSource = CCBDamageTypes.source(DamageTypes.THORNS, level, player);
        List<LivingEntity> vulnerableEntities = getVulnerableEntities(player, level, damageSource, range, eyePosition, viewVector);
        if (vulnerableEntities.isEmpty()) {
            return;
        }

        vulnerableEntities.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        Optional<AffordableFuel> fuel = CanisterContainerConsumers.findAffordableFuel(player, gasType -> {
            AirtightDrillHandler drillHandler = AirtightDrillHandlerUtils.of(gasType);
            return (double) perEntityHit * drillHandler.getConsumptionMultiplier() * vulnerableEntities.size();
        });
        if (fuel.isEmpty()) {
            displayInsufficientGasWarning(player);
            return;
        }

        AffordableFuel selectedFuel = fuel.get();
        AirtightDrillHandler drillHandler = AirtightDrillHandlerUtils.of(selectedFuel.gasType());
        int successfulHits = 0;
        for (LivingEntity entity : vulnerableEntities) {
            int damageAmount = AirtightDrillHandler.BASE_DAMAGE_AMOUNT + drillHandler.getDamageAddition();
            if (!entity.hurt(damageSource, damageAmount)) {
                continue;
            }

            successfulHits++;
            if (!(level instanceof ServerLevel serverLevel)) {
                continue;
            }

            drillHandler.extraBehaviour(entity, player, serverLevel);
        }

        if (successfulHits == 0) {
            return;
        }

        long actualAmount = GasConsumptions.roundUp((double) perEntityHit * drillHandler.getConsumptionMultiplier() * successfulHits);
        if (CanisterContainerConsumers.interactContainer(player, selectedFuel.gasType(), actualAmount, () -> true, false)) {
            return;
        }

        GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.insufficient_gas", selectedFuel.gasContent().getHoverName());
    }

    static void mineAreaBlocks(ItemStack drill, ServerLevel level, BlockState baseState, BlockPos basePos, Player player) {
        if (ADDITIONAL_BLOCK_BREAK_DEPTH.get() > 0 || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        AirtightHandheldDrillMiningContext context = AirtightHandheldDrillMiningContext.of(drill, basePos, level, baseState);
        if (context.isEmpty() || !context.isValidBaseTarget()) {
            return;
        }

        Optional<AffordableFuel> fuel = findAffordableDrillFuel(player, drill, context);
        if (fuel.isEmpty()) {
            displayInsufficientGasWarning(player);
            return;
        }

        AffordableFuel selectedFuel = fuel.get();
        if (isInstantBreakable(baseState, basePos, level) && context.destructionPos().stream().anyMatch(pos -> !isInstantBreakable(pos, level))) {
            return;
        }

        boolean silkTouch = SilkTouchUpgrade.INSTANCE.canApply(drill);
        boolean magnet = MagnetUpgrade.INSTANCE.canApply(drill);
        boolean experienceConversion = ExperienceConversionUpgrade.INSTANCE.canApply(drill);
        boolean liquidReplacement = LiquidReplacementUpgrade.INSTANCE.canApply(drill);
        double successfulConsumption = calculateGasConsumptionForBlock(level, basePos, silkTouch, magnet, experienceConversion, liquidReplacement);
        int successfulBreakCount = 1;
        int additionalTargetCount = Math.max(0, context.destructionPos().size() - 1);
        int additionalTargetIndex = 0;
        for (BlockPos targetPos : context.destructionPos()) {
            if (targetPos.equals(basePos)) {
                continue;
            }

            boolean showBreakParticles = shouldSpawnAdditionalBreakParticles(additionalTargetIndex, additionalTargetCount);
            additionalTargetIndex++;
            float targetConsumption = calculateGasConsumptionForBlock(level, targetPos, silkTouch, magnet, experienceConversion, liquidReplacement);
            if (!destroyAdditionalBlock(level, targetPos, serverPlayer, liquidReplacement, showBreakParticles)) {
                continue;
            }

            successfulConsumption += targetConsumption;
            successfulBreakCount++;
        }

        double baseGasConsumption = calculateBaseGasConsumption(successfulConsumption);
        long actualAmount = GasConsumptions.roundUp(calculateRawGasConsumption(baseGasConsumption, selectedFuel.gasType()));
        if (!CanisterContainerConsumers.interactContainer(player, selectedFuel.gasType(), actualAmount, () -> true, false)) {
            GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.insufficient_gas", selectedFuel.gasContent().getHoverName());
        }
        if (successfulBreakCount < 64) {
            return;
        }

        CCBAdvancements.MINI_TUNNEL_BORER.awardTo(player);
    }

    private static boolean isInstantBreakable(BlockPos basePos, Level level) {
        return level.getBlockState(basePos).getDestroySpeed(level, basePos) == 0;
    }

    private static float calculateGasConsumptionForBlock(Level level, BlockPos pos, boolean silkTouch, boolean magnet, boolean conversion, boolean liquidReplacement) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (isInstantBreakable(state, pos, level)) {
            return 0;
        }

        float consumption = 0;
        int blockCost = CCBConfig.server().equipments.perBlockConsumption.get();
        float liquidCost = CCBConfig.server().equipments.liquidReplacementMultiplier.getF() * blockCost;
        if (block instanceof LiquidBlock) {
            return liquidReplacement ? liquidCost : consumption;
        }

        if (liquidReplacement && !state.getFluidState().is(Fluids.EMPTY)) {
            consumption += liquidCost;
        }

        consumption += blockCost;
        if (silkTouch && !conversion) {
            consumption *= CCBConfig.server().equipments.silkTouchMultiplier.getF();
        }
        if (magnet) {
            consumption *= CCBConfig.server().equipments.magnetMultiplier.getF();
        }
        if (!conversion) {
            return consumption;
        }

        consumption *= CCBConfig.server().equipments.experienceConversionMultiplier.getF();
        return consumption;
    }

    private static float calculateMiningHardnessMultiplier(AirtightHandheldDrillMiningContext context) {
        Set<BlockPos> breakSpeedPos = context.breakSpeedPos();
        if (breakSpeedPos.isEmpty()) {
            return 1;
        }

        float baseHardness = context.baseHardness();
        float totalHardness = context.totalBreakHardness();
        if (baseHardness <= 0 || totalHardness <= 0) {
            return 1;
        }
        return baseHardness / totalHardness * breakSpeedPos.size();
    }

    private static float calculateMiningSizeMultiplier(AirtightHandheldDrillMiningContext context) {
        int size = context.breakSpeedPos().size();
        if (size == 0) {
            return 1;
        }

        double logSize = Math.log10(size + 9);
        return Mth.clamp(1 / (float) (Mth.square(logSize) * logSize), 0.01f, 1);
    }

    private static boolean isInstantBreakable(BlockState state, BlockPos pos, Level level) {
        return state.getDestroySpeed(level, pos) == 0;
    }

    private static Optional<AffordableFuel> findAffordableDrillFuel(Player player, ItemStack drill, AirtightHandheldDrillMiningContext context) {
        double[] baseGasConsumption = {Double.NaN};
        return CanisterContainerConsumers.findAffordableFuel(player, gasType -> {
            if (Double.isNaN(baseGasConsumption[0])) {
                baseGasConsumption[0] = calculateBaseGasConsumption(drill, context);
            }
            return calculateRawGasConsumption(baseGasConsumption[0], gasType);
        });
    }

    private static double calculateBaseGasConsumption(ItemStack drill, AirtightHandheldDrillMiningContext context) {
        Set<BlockPos> destructionPos = context.destructionPos();
        if (destructionPos.isEmpty()) {
            return -1;
        }

        boolean silkTouch = SilkTouchUpgrade.INSTANCE.canApply(drill);
        boolean magnet = MagnetUpgrade.INSTANCE.canApply(drill);
        boolean experienceConversion = ExperienceConversionUpgrade.INSTANCE.canApply(drill);
        boolean liquidReplacement = LiquidReplacementUpgrade.INSTANCE.canApply(drill);
        double totalConsumption = destructionPos.stream().mapToDouble(pos -> calculateGasConsumptionForBlock(context.level(), pos, silkTouch, magnet, experienceConversion, liquidReplacement)).sum();
        return calculateBaseGasConsumption(totalConsumption);
    }

    private static double calculateBaseGasConsumption(double totalConsumption) {
        if (!GasConsumptions.isNonNegativeFinite(totalConsumption) || totalConsumption < 0) {
            return -1;
        }

        double baseGasConsumption = 1.5 * Math.pow(totalConsumption, Math.log(2.25));
        return GasConsumptions.isNonNegativeFinite(baseGasConsumption) ? baseGasConsumption : -1;
    }

    private static double calculateRawGasConsumption(double baseGasConsumption, Gas gasType) {
        if (!GasConsumptions.isNonNegativeFinite(baseGasConsumption)) {
            return -1;
        }

        AirtightDrillHandler drillHandler = AirtightDrillHandlerUtils.of(gasType);
        double rawConsumption = baseGasConsumption * drillHandler.getConsumptionMultiplier();
        return GasConsumptions.isNonNegativeFinite(rawConsumption) ? rawConsumption : -1;
    }

    private static boolean destroyAdditionalBlockAsPlayer(ServerPlayer player, BlockPos pos) {
        int previousDepth = ADDITIONAL_BLOCK_BREAK_DEPTH.get();
        ADDITIONAL_BLOCK_BREAK_DEPTH.set(previousDepth + 1);
        try {
            return player.gameMode.destroyBlock(pos);
        } finally {
            if (previousDepth == 0) {
                ADDITIONAL_BLOCK_BREAK_DEPTH.remove();
            }
            else {
                ADDITIONAL_BLOCK_BREAK_DEPTH.set(previousDepth);
            }
        }
    }

    private static boolean destroyAdditionalBlock(ServerLevel level, BlockPos pos, ServerPlayer player, boolean liquidReplacement, boolean showBreakParticles) {
        BlockState originalState = level.getBlockState(pos);
        if (originalState.getBlock() instanceof LiquidBlock) {
            if (!liquidReplacement) {
                return false;
            }

            boolean removed = level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            if (removed && showBreakParticles) {
                spawnBreakParticles(level, pos, originalState);
            }
            return removed;
        }

        if (!destroyAdditionalBlockAsPlayer(player, pos)) {
            return false;
        }

        if (liquidReplacement && !originalState.getFluidState().isEmpty()) {
            BlockState remainingState = level.getBlockState(pos);
            if (remainingState.getBlock() instanceof LiquidBlock) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }

        if (originalState.is(Blocks.REINFORCED_DEEPSLATE)) {
            CCBAdvancements.EVEN_HARDER_THAN_OBSIDIAN.awardTo(player);
        }
        if (showBreakParticles) {
            spawnBreakParticles(level, pos, originalState);
        }
        return true;
    }

    private static List<LivingEntity> getVulnerableEntities(Player player, Level level, DamageSource damageSource, double range, Vec3 eyePosition, Vec3 viewVector) {
        List<LivingEntity> entities = new ArrayList<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range, range, range))) {
            if (entity.is(player)) {
                continue;
            }

            if (entity.isInvulnerableTo(damageSource) || entity.isInvulnerable()) {
                continue;
            }

            Vec3 toEntity = entity.position().subtract(eyePosition);
            if (toEntity.length() > range) {
                continue;
            }

            if (viewVector.dot(toEntity.normalize()) < 0.5) {
                continue;
            }

            if (!hasClearAttackLine(player, entity, level)) {
                continue;
            }

            entities.add(entity);
        }
        return entities;
    }

    private static boolean hasClearAttackLine(Player player, LivingEntity entity, Level level) {
        Vec3 from = player.getEyePosition();
        Vec3 to = entity.getBoundingBox().getCenter();
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, Fluid.NONE, player));
        return hit.getType() == Type.MISS || hit.getLocation().distanceToSqr(from) >= to.distanceToSqr(from) - 0.25;
    }

    private static void displayInsufficientGasWarning(Player player) {
        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.insufficient_gas");
            return;
        }

        GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.insufficient_gas", gasContent.getHoverName());
    }

    private static void spawnBreakParticles(ServerLevel level, BlockPos pos, BlockState state) {
        Vec3 center = VecHelper.getCenterOf(pos);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), center.x, center.y, center.z, 16, 0, 0, 0, 0);
    }

    private static boolean shouldSpawnAdditionalBreakParticles(int index, int total) {
        if (total <= MAX_ADDITIONAL_BREAK_PARTICLE_BLOCKS) {
            return true;
        }

        long previousBucket = (long) index * MAX_ADDITIONAL_BREAK_PARTICLE_BLOCKS / total;
        long currentBucket = (long) (index + 1) * MAX_ADDITIONAL_BREAK_PARTICLE_BLOCKS / total;
        return currentBucket > previousBucket;
    }
}
