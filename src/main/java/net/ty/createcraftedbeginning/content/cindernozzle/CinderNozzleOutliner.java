package net.ty.createcraftedbeginning.content.cindernozzle;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllSpecialTextures;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CinderNozzleOutliner {
    private final Object outline = new Object();
    private BlockPos activePos = null;

    public void toggleOutline(BlockPos pos) {
        if (activePos != null && activePos.equals(pos)) {
            activePos = null;
            return;
        }
        activePos = pos;
    }

    public void tick() {
        if (!isActive()) {
            return;
        }

        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        BlockEntity be = level.getBlockEntity(activePos);
        if (!(be instanceof CinderNozzleBlockEntity nozzle)) {
            activePos = null;
            return;
        }

        int currentRange = nozzle.getRange();
        AABB selectionBox = calculateSelectionBox(activePos, currentRange);
        Outliner outliner = Outliner.getInstance();
        outliner.chaseAABB(outline, selectionBox)
            .colored(0xddc166)
            .withFaceTextures(AllSpecialTextures.CHECKERED, AllSpecialTextures.HIGHLIGHT_CHECKERED)
            .lineWidth(1 / 16f);
    }

    private AABB calculateSelectionBox(BlockPos center, int edge) {
        double halfEdge = edge / 2F;
        Vec3 centerVec = Vec3.atCenterOf(center);
        return new AABB(centerVec.x - halfEdge, centerVec.y - halfEdge,centerVec.z - halfEdge, centerVec.x + halfEdge, centerVec.y + halfEdge, centerVec.z + halfEdge);
    }

    private boolean isActive() {
        if (activePos == null) {
            return false;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        return isPresent() && AllItems.GOGGLES.isIn(player.getItemBySlot(EquipmentSlot.HEAD));
    }

    private boolean isPresent() {
        return Minecraft.getInstance().level != null && Minecraft.getInstance().screen == null;
    }
}
