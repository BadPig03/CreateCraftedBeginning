package net.ty.createcraftedbeginning.api.gas;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class GasFilteringRenderer {
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        HitResult target = mc.hitResult;
        if (!(target instanceof BlockHitResult result)) {
            return;
        }

        ClientLevel world = mc.level;
        if (world == null) {
            return;
        }

        BlockPos pos = result.getBlockPos();
        BlockState state = world.getBlockState(pos);
        LocalPlayer player = mc.player;

        if (player == null || player.isShiftKeyDown()) {
            return;
        }
        if (!(world.getBlockEntity(pos) instanceof SmartBlockEntity smartBlockEntity)) {
            return;
        }

        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        for (BlockEntityBehaviour blockEntityBehaviour : smartBlockEntity.getAllBehaviours()) {
            if (!(blockEntityBehaviour instanceof GasFilteringBehaviour behaviour)) {
                continue;
            }
            if (!behaviour.isActive()) {
                continue;
            }
            if (!behaviour.slotPositioning.shouldRender(world, pos, state)) {
                continue;
            }
            if (!behaviour.mayInteract(player)) {
                continue;
            }

            ItemStack filter = behaviour.getFilter();
            Component label = behaviour.getLabel();
            boolean hit = behaviour.slotPositioning.testHit(world, pos, state, target.getLocation().subtract(Vec3.atLowerCornerOf(pos)));
            AABB bb = new AABB(Vec3.ZERO, Vec3.ZERO).inflate(.25f);

            ValueBox box = new ValueBox.ItemValueBox(label, bb, pos, filter, Component.empty());
            box.passive(!hit || behaviour.bypassesInput(mainHandItem));

            Outliner.getInstance().showOutline(Pair.of("filter" + behaviour.netId(), pos), box.transform(behaviour.slotPositioning)).lineWidth(1 / 64f).withFaceTexture(hit ? AllSpecialTextures.THIN_CHECKERED : null).highlightFace(result.getDirection());

            if (!hit) {
                continue;
            }

            List<MutableComponent> tip = new ArrayList<>();
            tip.add(label.copy());
            tip.add(behaviour.getTip());

            CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
        }
    }

    public static void renderOnBlockEntity(SmartBlockEntity be, float ignored, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (be == null || be.isRemoved()) {
            return;
        }

        Level level = be.getLevel();
        BlockPos blockPos = be.getBlockPos();

        for (BlockEntityBehaviour b : be.getAllBehaviours()) {
            if (!(b instanceof GasFilteringBehaviour behaviour)) {
                continue;
            }

            if (!be.isVirtual()) {
                Entity cameraEntity = Minecraft.getInstance().cameraEntity;
                if (cameraEntity != null && level == cameraEntity.level()) {
                    float max = behaviour.getRenderDistance();
                    if (cameraEntity.position().distanceToSqr(VecHelper.getCenterOf(blockPos)) > (max * max)) {
                        continue;
                    }
                }
            }

            if (!behaviour.isActive()) {
                continue;
            }
            if (behaviour.getFilter().isEmpty()) {
                continue;
            }

            ValueBoxTransform slotPositioning = behaviour.slotPositioning;
            BlockState blockState = be.getBlockState();

            if (slotPositioning.shouldRender(level, blockPos, blockState)) {
                ms.pushPose();
                slotPositioning.transform(level, blockPos, blockState, ms);
                ValueBoxRenderer.renderItemIntoValueBox(behaviour.getFilter(), ms, buffer, light, overlay);
                ms.popPose();
            }
        }
    }
}
