package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import com.simibubi.create.AllSpecialTextures;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.HandheldDrillOutlineDisplayButton;
import net.ty.createcraftedbeginning.foundation.client.outliner.CCBOutliner;
import net.ty.createcraftedbeginning.foundation.client.render.CCBSpecialTextures;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
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

    private static final String TOTAL_FIRST_KEY = "handheldDrillTotalFirst";
    private static final String TOTAL_SECOND_KEY = "handheldDrillTotalSecond";
    private static final String PROTECTED_KEY = "handheldDrillProtected";
    private static final String INSTANT_KEY = "handheldDrillInstant";
    private static final String UNBREAKABLE_KEY = "handheldDrillUnbreakable";
    private static final String LIQUID_KEY = "handheldDrillLiquid";
    private static final Map<String, Set<BlockPos>> CACHED_POSITIONS = new HashMap<>();

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

        BlockPos basePos = AirtightHandheldDrillUtils.getHitResult(player);
        if (basePos == null) {
            return;
        }

        renderOutline(player.level(), drill, basePos);
    }

    private static void renderOutline(Level level, ItemStack drill, BlockPos basePos) {
        CCBOutliner outliner = CCBOutliner.INSTANCE;
        AirtightHandheldDrillMiningContext miningContext = AirtightHandheldDrillMiningContext.of(drill, basePos, level);
        Set<BlockPos> targetPositions = miningContext.totalPos();
        boolean hasHighlightedOutline = showHighlightedCluster(outliner, level, PROTECTED_KEY, miningContext.protectedPos(), COLOR_ORANGE);
        hasHighlightedOutline |= showHighlightedCluster(outliner, level, INSTANT_KEY, miningContext.instantDestructionPos(), COLOR_GREEN);
        hasHighlightedOutline |= showHighlightedCluster(outliner, level, UNBREAKABLE_KEY, miningContext.unbreakablePos(), COLOR_RED);
        hasHighlightedOutline |= showHighlightedCluster(outliner, level, LIQUID_KEY, miningContext.liquidPos(), COLOR_BLUE);
        if (targetPositions.isEmpty()) {
            return;
        }

        if (!keepCachedCluster(outliner, TOTAL_FIRST_KEY, targetPositions)) {
            outliner.showCluster(TOTAL_FIRST_KEY, level, targetPositions).colored(COLOR_WHITE).disableLineNormals().disableCull().lineWidth(0.015625f).withFaceTexture(CCBSpecialTextures.LOW_TRANSLUCENT);
            CACHED_POSITIONS.put(TOTAL_FIRST_KEY, targetPositions);
        }
        if (hasHighlightedOutline) {
            return;
        }

        if (keepCachedCluster(outliner, TOTAL_SECOND_KEY, targetPositions)) {
            return;
        }

        outliner.showCluster(TOTAL_SECOND_KEY, level, targetPositions).colored(COLOR_WHITE).disableLineNormals().disableCull().lineWidth(0.015625f).withFaceTexture(CCBSpecialTextures.LOW_TRANSLUCENT_HIGHLIGHTED);
        CACHED_POSITIONS.put(TOTAL_SECOND_KEY, targetPositions);
    }

    private static boolean showHighlightedCluster(CCBOutliner outliner, Level level, String outlineKey, Set<BlockPos> positions, int color) {
        if (positions.isEmpty()) {
            return false;
        }

        if (!keepCachedCluster(outliner, outlineKey, positions)) {
            outliner.showCluster(outlineKey, level, positions).colored(color).disableLineNormals().disableCull().lineWidth(0.03125f).withFaceTexture(AllSpecialTextures.HIGHLIGHT_CHECKERED);
            CACHED_POSITIONS.put(outlineKey, positions);
        }
        return true;
    }

    private static boolean keepCachedCluster(CCBOutliner outliner, String outlineKey, Set<BlockPos> positions) {
        Set<BlockPos> cachedPositions = CACHED_POSITIONS.get(outlineKey);
        if (cachedPositions == null || !cachedPositions.equals(positions) || !outliner.getOutlines().containsKey(outlineKey)) {
            return false;
        }

        outliner.keep(outlineKey);
        return true;
    }
}
