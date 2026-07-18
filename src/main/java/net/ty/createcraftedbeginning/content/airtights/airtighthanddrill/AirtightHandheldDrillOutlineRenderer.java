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
import net.ty.createcraftedbeginning.api.outliner.CCBOutliner;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.HandheldDrillOutlineDisplayButton;
import net.ty.createcraftedbeginning.data.CCBSpecialTextures;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
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
        boolean hasSecondaryOutline = showHighlightedCluster(outliner, "handheldDrillProtected", context.protectedPos(), COLOR_ORANGE);
        hasSecondaryOutline |= showHighlightedCluster(outliner, "handheldDrillInstant", context.instantDestructionPos(), COLOR_GREEN);
        hasSecondaryOutline |= showHighlightedCluster(outliner, "handheldDrillUnbreakable", context.unbreakablePos(), COLOR_RED);
        hasSecondaryOutline |= showHighlightedCluster(outliner, "handheldDrillLiquid", context.liquidPos(), COLOR_BLUE);

        if (totalPos.isEmpty()) {
            return;
        }

        outliner.showCluster("handheldDrillTotalFirst", totalPos).colored(COLOR_WHITE).disableLineNormals().disableCull().lineWidth(0.015625f).withFaceTexture(CCBSpecialTextures.LOW_TRANSLUCENT);
        if (hasSecondaryOutline) {
            return;
        }

        outliner.showCluster("handheldDrillTotalSecond", totalPos).colored(COLOR_WHITE).disableLineNormals().disableCull().lineWidth(0.015625f).withFaceTexture(CCBSpecialTextures.LOW_TRANSLUCENT_HIGHLIGHTED);
    }

    private static boolean showHighlightedCluster(CCBOutliner outliner, String key, Set<BlockPos> positions, int color) {
        if (positions.isEmpty()) {
            return false;
        }

        outliner.showCluster(key, positions).colored(color).disableLineNormals().disableCull().lineWidth(0.03125f).withFaceTexture(AllSpecialTextures.HIGHLIGHT_CHECKERED);
        return true;
    }
}
