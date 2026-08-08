package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import com.simibubi.create.AllSpecialTextures;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.client.outliner.CCBOutliner;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.HandheldDrillOutlineDisplayButton;
import net.ty.createcraftedbeginning.data.CCBSpecialTextures;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class AirtightHandheldDrillOutlineRenderer {
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
        AirtightHandheldDrillMiningContext context = AirtightHandheldDrillMiningContext.of(drill, basePos, level);
        Set<BlockPos> totalPos = context.totalPos();
        boolean hasSecondaryOutline = showHighlightedCluster(outliner, PROTECTED_KEY, context.protectedPos(), COLOR_ORANGE);
        hasSecondaryOutline |= showHighlightedCluster(outliner, INSTANT_KEY, context.instantDestructionPos(), COLOR_GREEN);
        hasSecondaryOutline |= showHighlightedCluster(outliner, UNBREAKABLE_KEY, context.unbreakablePos(), COLOR_RED);
        hasSecondaryOutline |= showHighlightedCluster(outliner, LIQUID_KEY, context.liquidPos(), COLOR_BLUE);

        if (totalPos.isEmpty()) {
            return;
        }

        if (!keepCachedCluster(outliner, TOTAL_FIRST_KEY, totalPos)) {
            outliner.showCluster(TOTAL_FIRST_KEY, totalPos).colored(COLOR_WHITE).disableLineNormals().disableCull().lineWidth(0.015625f).withFaceTexture(CCBSpecialTextures.LOW_TRANSLUCENT);
            CACHED_POSITIONS.put(TOTAL_FIRST_KEY, totalPos);
        }
        if (hasSecondaryOutline) {
            return;
        }

        if (keepCachedCluster(outliner, TOTAL_SECOND_KEY, totalPos)) {
            return;
        }

        outliner.showCluster(TOTAL_SECOND_KEY, totalPos).colored(COLOR_WHITE).disableLineNormals().disableCull().lineWidth(0.015625f).withFaceTexture(CCBSpecialTextures.LOW_TRANSLUCENT_HIGHLIGHTED);
        CACHED_POSITIONS.put(TOTAL_SECOND_KEY, totalPos);
    }

    private static boolean showHighlightedCluster(CCBOutliner outliner, String key, Set<BlockPos> positions, int color) {
        if (positions.isEmpty()) {
            return false;
        }

        if (!keepCachedCluster(outliner, key, positions)) {
            outliner.showCluster(key, positions).colored(color).disableLineNormals().disableCull().lineWidth(0.03125f).withFaceTexture(AllSpecialTextures.HIGHLIGHT_CHECKERED);
            CACHED_POSITIONS.put(key, positions);
        }
        return true;
    }

    private static boolean keepCachedCluster(CCBOutliner outliner, String key, Set<BlockPos> positions) {
        Set<BlockPos> cachedPositions = CACHED_POSITIONS.get(key);
        if (cachedPositions == null || !cachedPositions.equals(positions) || !outliner.getOutlines().containsKey(key)) {
            return false;
        }

        outliner.keep(key);
        return true;
    }
}
