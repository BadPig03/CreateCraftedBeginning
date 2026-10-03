package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.ExperienceConversionUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.HarvestOptimizationUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.LiquidReplacementUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.MagnetUpgrade;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers.AffordableFuel;
import net.ty.createcraftedbeginning.gas.interaction.GasInteractionFeedback;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightHandheldDrillMining {
    private static final int MAX_ADDITIONAL_BREAK_PARTICLE_BLOCKS = 64;
    private static final ThreadLocal<Integer> ADDITIONAL_BLOCK_BREAK_DEPTH = ThreadLocal.withInitial(() -> 0);

    private AirtightHandheldDrillMining() {
    }

    static boolean isAdditionalBlockBreak() {
        return ADDITIONAL_BLOCK_BREAK_DEPTH.get() > 0;
    }

    static ItemStack createDrillUsedTool(ItemStack drill, ServerLevel level) {
        ItemStack usedTool = new ItemStack(Items.NETHERITE_PICKAXE);
        usedTool.set(DataComponents.ENCHANTMENTS, drill.getTagEnchantments());
        usedTool.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
        if (!HarvestOptimizationUpgrade.INSTANCE.isInstalled(drill)) {
            return usedTool;
        }

        boolean silkTouch = HarvestOptimizationUpgrade.INSTANCE.canApply(drill);
        Holder<Enchantment> miningEnchantment = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(silkTouch ? Enchantments.SILK_TOUCH : Enchantments.FORTUNE);
        int enchantmentLevel = silkTouch ? 1 : HarvestOptimizationUpgrade.FORTUNE_LEVEL;
        EnchantmentHelper.updateEnchantments(usedTool, enchantments -> {
            enchantments.removeIf(enchantment -> enchantment.is(Enchantments.SILK_TOUCH) || enchantment.is(Enchantments.FORTUNE));
            enchantments.set(miningEnchantment, enchantmentLevel);
        });
        return usedTool;
    }

    static float calculateFinalBreakSpeed(float breakSpeed, Player player, ItemStack drill, BlockPos basePos) {
        Level level = player.level();
        AirtightHandheldDrillMiningContext context = AirtightHandheldDrillMiningContext.of(drill, basePos, level, player);
        if (!context.isValidBaseTarget()) {
            return -2;
        }

        if (AirtightHandheldDrillFuel.findAffordableDrillFuel(player, drill, context).isEmpty()) {
            return -1;
        }

        if (isInstantBreakable(basePos, level)) {
            return 1;
        }

        breakSpeed *= calculateMiningSizeMultiplier(context);
        breakSpeed *= calculateMiningHardnessMultiplier(context);
        if (!player.getOffhandItem().is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return breakSpeed;
        }

        return breakSpeed * 2;
    }

    static void mineAreaBlocks(ItemStack drill, ServerLevel level, BlockState baseState, BlockPos basePos, Player player) {
        if (isAdditionalBlockBreak() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (baseState.is(Blocks.REINFORCED_DEEPSLATE) && !level.getBlockState(basePos).is(Blocks.REINFORCED_DEEPSLATE)) {
            CCBAdvancements.EVEN_HARDER_THAN_OBSIDIAN.awardTo(player);
        }

        AirtightHandheldDrillMiningContext context = AirtightHandheldDrillMiningContext.of(drill, basePos, level, baseState, player);
        if (context.isEmpty() || !context.isValidBaseTarget()) {
            return;
        }

        Optional<AffordableFuel> affordableFuel = AirtightHandheldDrillFuel.findAffordableDrillFuel(player, drill, context);
        if (affordableFuel.isEmpty()) {
            AirtightHandheldDrillFuel.displayInsufficientGasWarning(player);
            return;
        }

        AffordableFuel selectedFuel = affordableFuel.get();
        if (baseState.getDestroySpeed(level, basePos) == 0 && context.destructionPos().stream().anyMatch(pos -> !isInstantBreakable(pos, level))) {
            return;
        }

        boolean silkTouch = HarvestOptimizationUpgrade.INSTANCE.canApply(drill);
        boolean magnet = MagnetUpgrade.INSTANCE.canApply(drill);
        boolean experienceConversion = ExperienceConversionUpgrade.INSTANCE.canApply(drill);
        boolean liquidReplacement = LiquidReplacementUpgrade.INSTANCE.canApply(drill);
        double successfulBlockConsumption = AirtightHandheldDrillFuel.calculateGasConsumptionForBlock(level, basePos, silkTouch, magnet, experienceConversion, liquidReplacement);
        int successfulBreakCount = 1;
        int additionalTargetCount = Math.max(0, context.destructionPos().size() - 1);
        int additionalTargetIndex = 0;
        for (BlockPos targetPos : context.destructionPos()) {
            if (targetPos.equals(basePos)) {
                continue;
            }

            boolean showBreakParticles = shouldSpawnAdditionalBreakParticles(additionalTargetIndex, additionalTargetCount);
            additionalTargetIndex++;
            float targetBlockConsumption = AirtightHandheldDrillFuel.calculateGasConsumptionForBlock(level, targetPos, silkTouch, magnet, experienceConversion, liquidReplacement);
            if (!destroyAdditionalBlock(level, targetPos, serverPlayer, liquidReplacement, showBreakParticles)) {
                continue;
            }

            successfulBlockConsumption += targetBlockConsumption;
            successfulBreakCount++;
        }

        HandheldDrillAerogelProtection.apply(serverPlayer, drill, context.totalPos());

        long gasConsumption = GasConsumptionMath.roundUp(AirtightHandheldDrillFuel.calculateRawGasConsumption(successfulBlockConsumption, selectedFuel.gasType()));
        if (!CanisterContainerConsumers.interactContainer(player, new AffordableFuel(selectedFuel.gasContent(), selectedFuel.sourcePressurePa(), gasConsumption), () -> true, false)) {
            GasInteractionFeedback.sendWarningFeedback(player, "gui.warnings.insufficient_gas", selectedFuel.gasContent().getHoverName());
        }
        if (successfulBreakCount < 64) {
            return;
        }

        CCBAdvancements.MINI_TUNNEL_BORER.awardTo(player);
    }

    static void clearRemainingLiquid(ServerLevel level, BlockPos pos, BlockState originalState) {
        if (originalState.getFluidState().isEmpty()) {
            return;
        }

        if (!(level.getBlockState(pos).getBlock() instanceof LiquidBlock)) {
            return;
        }

        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    private static boolean isInstantBreakable(BlockPos basePos, Level level) {
        return level.getBlockState(basePos).getDestroySpeed(level, basePos) == 0;
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
        int blockCount = context.breakSpeedPos().size();
        if (blockCount == 0) {
            return 1;
        }

        float logarithmicSize = (float) Math.log10(blockCount + 9);
        return Mth.clamp(1 / logarithmicSize, 0.01F, 1);
    }

    private static boolean destroyAdditionalBlockAsPlayer(ServerPlayer player, BlockPos pos) {
        int previousBreakDepth = ADDITIONAL_BLOCK_BREAK_DEPTH.get();
        ADDITIONAL_BLOCK_BREAK_DEPTH.set(previousBreakDepth + 1);
        try {
            return player.gameMode.destroyBlock(pos);
        }
        finally {
            if (previousBreakDepth == 0) {
                ADDITIONAL_BLOCK_BREAK_DEPTH.remove();
            }
            else {
                ADDITIONAL_BLOCK_BREAK_DEPTH.set(previousBreakDepth);
            }
        }
    }

    private static boolean destroyAdditionalBlock(ServerLevel level, BlockPos pos, ServerPlayer player, boolean liquidReplacement, boolean showBreakParticles) {
        BlockState originalState = level.getBlockState(pos);
        if (originalState.getBlock() instanceof LiquidBlock) {
            if (!liquidReplacement) {
                return false;
            }

            boolean wasRemoved = destroyAdditionalBlockAsPlayer(player, pos);
            if (wasRemoved && showBreakParticles) {
                spawnBreakParticles(level, pos, originalState);
            }
            return wasRemoved;
        }

        if (!destroyAdditionalBlockAsPlayer(player, pos)) {
            return false;
        }

        if (liquidReplacement) {
            clearRemainingLiquid(level, pos, originalState);
        }

        if (originalState.is(Blocks.REINFORCED_DEEPSLATE)) {
            CCBAdvancements.EVEN_HARDER_THAN_OBSIDIAN.awardTo(player);
        }
        if (showBreakParticles) {
            spawnBreakParticles(level, pos, originalState);
        }
        return true;
    }

    private static void spawnBreakParticles(ServerLevel level, BlockPos pos, BlockState state) {
        Vec3 blockCenter = VecHelper.getCenterOf(pos);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), blockCenter.x, blockCenter.y, blockCenter.z, 16, 0, 0, 0, 0);
    }

    private static boolean shouldSpawnAdditionalBreakParticles(int targetIndex, int targetCount) {
        if (targetCount <= MAX_ADDITIONAL_BREAK_PARTICLE_BLOCKS) {
            return true;
        }

        long previousParticleBucket = (long) targetIndex * MAX_ADDITIONAL_BREAK_PARTICLE_BLOCKS / targetCount;
        long currentParticleBucket = (long) (targetIndex + 1) * MAX_ADDITIONAL_BREAK_PARTICLE_BLOCKS / targetCount;
        return currentParticleBucket > previousParticleBucket;
    }
}
