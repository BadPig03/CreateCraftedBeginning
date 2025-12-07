package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import com.simibubi.create.AllSpecialTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.outliner.CCBOutliner;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.data.CCBSpecialTextures;

import java.util.Set;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class AirtightHandheldDrillBlockRender {
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
        if (!drill.is(CCBItems.AIRTIGHT_HANDHELD_DRILL) || !AirtightHandheldDrillUtils.isOutlineEnabled(drill)) {
            return;
        }

        BlockPos basePosition = AirtightHandheldDrillUtils.getHitResult(player);
        if (basePosition == null) {
            return;
        }

        renderOutline(player.level(), drill, basePosition);
    }

    private static void renderOutline(Level level, ItemStack drill, BlockPos base) {
        CCBOutliner outliner = CCBOutliner.INSTANCE;
        Set<BlockPos> totalPos = AirtightHandheldDrillUtils.getTotalPos(drill, base);
        boolean second = false;

        Set<BlockPos> protectedPos = AirtightHandheldDrillUtils.getProtectedPos(level, drill, totalPos);
        if (!protectedPos.isEmpty()) {
            outliner.showCluster("handheldDrillProtected", protectedPos).colored(COLOR_ORANGE).disableLineNormals().disableCull().lineWidth(0.03125f).withFaceTexture(AllSpecialTextures.HIGHLIGHT_CHECKERED);
            second = true;
        }

        Set<BlockPos> unprotectedPos = totalPos.stream().filter(pos -> !protectedPos.contains(pos)).collect(Collectors.toSet());
        Set<BlockPos> instantDestructionPos = AirtightHandheldDrillUtils.getInstantDestructionPos(level, unprotectedPos).stream().filter(pos -> !level.getBlockState(pos).isAir()).collect(Collectors.toSet());
        if (!instantDestructionPos.isEmpty()) {
            outliner.showCluster("handheldDrillInstant", instantDestructionPos).colored(COLOR_GREEN).disableLineNormals().disableCull().lineWidth(0.03125f).withFaceTexture(AllSpecialTextures.HIGHLIGHT_CHECKERED);
            second = true;
        }

        Set<BlockPos> unbreakablePos = AirtightHandheldDrillUtils.getUnbreakablePos(level, unprotectedPos);
        if (!unbreakablePos.isEmpty()) {
            outliner.showCluster("handheldDrillUnbreakable", unbreakablePos).colored(COLOR_RED).disableLineNormals().disableCull().lineWidth(0.03125f).withFaceTexture(AllSpecialTextures.HIGHLIGHT_CHECKERED);
            second = true;
        }

        Set<BlockPos> liquidPos = AirtightHandheldDrillUtils.getLiquidPos(level, unprotectedPos);
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
