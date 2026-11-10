package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import com.simibubi.create.AllSpecialTextures;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.outliner.Outliner.OutlineEntry;
import net.createmod.catnip.render.BindableTexture;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.client.render.CCBSpecialTextures;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.HandheldDrillOutlineDisplayButton;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.LiquidReplacementUpgrade;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class AirtightHandheldDrillOutlineRenderer {
    private static final int COLOR_WHITE = 0xBFBFBF;
    private static final int COLOR_ORANGE = 0xDBA149;
    private static final int COLOR_BLUE = 0x0091B9;
    private static final int COLOR_RED = 0xFF5D6C;
    private static final int COLOR_GREEN = 0x4EB483;

    private static final ResourceLocation TOTAL_FIRST_KEY = CCBAPI.asResource("outliner/handheld_drill/total_first");
    private static final ResourceLocation TOTAL_SECOND_KEY = CCBAPI.asResource("outliner/handheld_drill/total_second");
    private static final ResourceLocation PROTECTED_KEY = CCBAPI.asResource("outliner/handheld_drill/protected");
    private static final ResourceLocation INSTANT_KEY = CCBAPI.asResource("outliner/handheld_drill/instant");
    private static final ResourceLocation UNBREAKABLE_KEY = CCBAPI.asResource("outliner/handheld_drill/unbreakable");
    private static final ResourceLocation LIQUID_KEY = CCBAPI.asResource("outliner/handheld_drill/liquid");
    private static final List<ResourceLocation> OUTLINE_KEYS = List.of(TOTAL_FIRST_KEY, TOTAL_SECOND_KEY, PROTECTED_KEY, INSTANT_KEY, UNBREAKABLE_KEY, LIQUID_KEY);

    private AirtightHandheldDrillOutlineRenderer() {
    }

    public static void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) {
            return;
        }

        ItemStack drill = player.getMainHandItem();
        if (!drill.is(CCBItems.AIRTIGHT_HANDHELD_DRILL) || !HandheldDrillOutlineDisplayButton.INSTANCE.isActive(player, drill)) {
            return;
        }

        Vec3 eyePosition = player.getEyePosition();
        Vec3 rayEnd = eyePosition.add(player.calculateViewVector(player.getXRot(), player.getYRot()).scale(player.blockInteractionRange()));
        Level level = player.level();
        Fluid fluidMode = LiquidReplacementUpgrade.INSTANCE.canApply(drill) ? Fluid.ANY : Fluid.NONE;
        BlockHitResult blockHit = level.clip(new ClipContext(eyePosition, rayEnd, Block.OUTLINE, fluidMode, player));
        if (blockHit.getType() == Type.MISS) {
            return;
        }

        renderOutline(level, drill, blockHit.getBlockPos(), player);
    }

    static void clear() {
        Outliner outliner = Outliner.getInstance();
        for (ResourceLocation outlineKey : OUTLINE_KEYS) {
            outliner.remove(outlineKey);
        }
    }

    private static void renderOutline(Level level, ItemStack drill, BlockPos basePos, LocalPlayer player) {
        Outliner outliner = Outliner.getInstance();
        AirtightHandheldDrillMiningContext miningContext = AirtightHandheldDrillMiningContext.of(drill, basePos, level, player);
        Set<BlockPos> targetPositions = miningContext.totalPos();
        boolean hasHighlightedOutline = showHighlightedCluster(outliner, level, PROTECTED_KEY, miningContext.protectedPos(), COLOR_ORANGE);
        hasHighlightedOutline |= showHighlightedCluster(outliner, level, INSTANT_KEY, miningContext.instantDestructionPos(), COLOR_GREEN);
        hasHighlightedOutline |= showHighlightedCluster(outliner, level, UNBREAKABLE_KEY, miningContext.unbreakablePos(), COLOR_RED);
        hasHighlightedOutline |= showHighlightedCluster(outliner, level, LIQUID_KEY, miningContext.liquidPos(), COLOR_BLUE);
        if (targetPositions.isEmpty()) {
            return;
        }

        if (!keepCachedCluster(outliner, level, TOTAL_FIRST_KEY, targetPositions)) {
            showCluster(outliner, level, TOTAL_FIRST_KEY, targetPositions, COLOR_WHITE, 0.015625F, CCBSpecialTextures.LOW_TRANSLUCENT);
        }
        if (hasHighlightedOutline) {
            return;
        }

        if (keepCachedCluster(outliner, level, TOTAL_SECOND_KEY, targetPositions)) {
            return;
        }

        showCluster(outliner, level, TOTAL_SECOND_KEY, targetPositions, COLOR_WHITE, 0.015625F, CCBSpecialTextures.LOW_TRANSLUCENT_HIGHLIGHTED);
    }

    private static boolean showHighlightedCluster(Outliner outliner, Level level, ResourceLocation outlineKey, Set<BlockPos> positions, int color) {
        if (positions.isEmpty()) {
            return false;
        }

        if (!keepCachedCluster(outliner, level, outlineKey, positions)) {
            showCluster(outliner, level, outlineKey, positions, color, 0.03125F, AllSpecialTextures.HIGHLIGHT_CHECKERED);
        }
        return true;
    }

    private static void showCluster(Outliner outliner, Level level, ResourceLocation outlineKey, Set<BlockPos> positions, int color, float lineWidth, BindableTexture faceTexture) {
        AirtightHandheldDrillClusterOutline outline = new AirtightHandheldDrillClusterOutline(level, positions).withFaceTexture(faceTexture).disableLineNormals();
        outliner.showOutline(outlineKey, outline).colored(color).lineWidth(lineWidth);
    }

    private static boolean keepCachedCluster(Outliner outliner, Level level, ResourceLocation outlineKey, Set<BlockPos> positions) {
        OutlineEntry entry = outliner.getOutlines().get(outlineKey);
        if (entry == null || !(entry.getOutline() instanceof AirtightHandheldDrillClusterOutline outline) || !outline.matches(level, positions)) {
            return false;
        }

        outliner.keep(outlineKey);
        return true;
    }
}
