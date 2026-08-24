package net.ty.createcraftedbeginning.client.contents;

import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox.ItemValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasFilteringBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class GasFilteringRenderer {
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.hitResult instanceof BlockHitResult hitResult)) {
            return;
        }

        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        BlockPos hitPos = hitResult.getBlockPos();
        if (!(level.getBlockEntity(hitPos) instanceof SmartBlockEntity blockEntity)) {
            return;
        }

        LocalPlayer player = minecraft.player;
        if (player == null || player.isShiftKeyDown()) {
            return;
        }

        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        for (BlockEntityBehaviour candidateBehaviour : blockEntity.getAllBehaviours()) {
            if (!(candidateBehaviour instanceof GasFilteringBehaviour behaviour) || !behaviour.isActive()) {
                continue;
            }

            BlockState blockState = level.getBlockState(hitPos);
            Vec3 localHitPosition = hitResult.getLocation().subtract(Vec3.atLowerCornerOf(hitPos));
            ValueBoxTransform slotTransform = behaviour.getSlotPositioning();
            if (!slotTransform.shouldRender(level, hitPos, blockState) || !behaviour.mayInteract(player)) {
                continue;
            }

            Component filterLabel = behaviour.getLabel();
            boolean isSlotHit = slotTransform.testHit(level, hitPos, blockState, localHitPosition);
            ValueBox filterBox = new ItemValueBox(filterLabel, new AABB(Vec3.ZERO, Vec3.ZERO).inflate(0.25), hitPos, behaviour.getFilter(), Component.empty());
            filterBox.passive(!isSlotHit || behaviour.bypassesInput(heldItem));
            Outliner.getInstance().showOutline(Pair.of("filter" + behaviour.netId(), hitPos), filterBox.transform(slotTransform)).lineWidth(0.015625f).withFaceTexture(isSlotHit ? AllSpecialTextures.THIN_CHECKERED : null).highlightFace(hitResult.getDirection());
            if (!isSlotHit) {
                continue;
            }

            List<MutableComponent> tooltip = new ArrayList<>();
            tooltip.add(filterLabel.copy());
            tooltip.add(behaviour.getTip());
            CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tooltip);
        }
    }
}
