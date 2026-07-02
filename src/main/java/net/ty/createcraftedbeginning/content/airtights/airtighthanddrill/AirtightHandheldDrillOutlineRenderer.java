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

        BlockPos basePosition = AirtightHandheldDrillUtils.getHitResult(player);
        if (basePosition == null) {
            return;
        }

        renderOutline(player.level(), drill, basePosition);
    }

    private static void renderOutline(Level level, ItemStack drill, BlockPos basePos) {
        CCBOutliner outliner = CCBOutliner.INSTANCE;
        AirtightHandheldDrillMiningContext context = AirtightHandheldDrillMiningContext.of(drill, basePos, level);
        Set<BlockPos> totalPos = context.totalPos();
        boolean second = false;

        Set<BlockPos> protectedPos = context.protectedPos();
        if (!protectedPos.isEmpty()) {
            outliner.showCluster("handheldDrillProtected", protectedPos).colored(COLOR_ORANGE).disableLineNormals().disableCull().lineWidth(0.03125f).withFaceTexture(AllSpecialTextures.HIGHLIGHT_CHECKERED);
            second = true;
        }

        Set<BlockPos> instantDestructionPos = context.instantDestructionPos();
        if (!instantDestructionPos.isEmpty()) {
            outliner.showCluster("handheldDrillInstant", instantDestructionPos).colored(COLOR_GREEN).disableLineNormals().disableCull().lineWidth(0.03125f).withFaceTexture(AllSpecialTextures.HIGHLIGHT_CHECKERED);
            second = true;
        }

        Set<BlockPos> unbreakablePos = context.unbreakablePos();
        if (!unbreakablePos.isEmpty()) {
            outliner.showCluster("handheldDrillUnbreakable", unbreakablePos).colored(COLOR_RED).disableLineNormals().disableCull().lineWidth(0.03125f).withFaceTexture(AllSpecialTextures.HIGHLIGHT_CHECKERED);
            second = true;
        }

        Set<BlockPos> liquidPos = context.liquidPos();
        if (!liquidPos.isEmpty()) {
            outliner.showCluster("handheldDrillLiquid", liquidPos).colored(COLOR_BLUE).disableLineNormals().disableCull().lineWidth(0.03125f).withFaceTexture(AllSpecialTextures.HIGHLIGHT_CHECKERED);
            second = true;
        }

        if (totalPos.isEmpty()) {
            return;
        }

        outliner.showCluster("handheldDrillTotalFirst", totalPos).colored(COLOR_WHITE).disableLineNormals().disableCull().lineWidth(0.015625f).withFaceTexture(CCBSpecialTextures.LOW_TRANSLUCENT);
        if (second) {
            return;
        }

        outliner.showCluster("handheldDrillTotalSecond", totalPos).colored(COLOR_WHITE).disableLineNormals().disableCull().lineWidth(0.015625f).withFaceTexture(CCBSpecialTextures.LOW_TRANSLUCENT_HIGHLIGHTED);
    }
}
