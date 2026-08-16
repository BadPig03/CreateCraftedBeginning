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
import net.minecraft.world.phys.HitResult;
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
        HitResult target = minecraft.hitResult;
        if (!(target instanceof BlockHitResult hitResult)) {
            return;
        }

        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        BlockPos pos = hitResult.getBlockPos();
        if (!(level.getBlockEntity(pos) instanceof SmartBlockEntity blockEntity)) {
            return;
        }

        LocalPlayer player = minecraft.player;
        if (player == null || player.isShiftKeyDown()) {
            return;
        }

        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        for (BlockEntityBehaviour candidate : blockEntity.getAllBehaviours()) {
            if (!(candidate instanceof GasFilteringBehaviour behaviour) || !behaviour.isActive()) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            Vec3 localHit = target.getLocation().subtract(Vec3.atLowerCornerOf(pos));
            ValueBoxTransform transform = behaviour.getSlotPositioning();
            if (!transform.shouldRender(level, pos, state) || !behaviour.mayInteract(player)) {
                continue;
            }

            Component label = behaviour.getLabel();
            boolean hit = transform.testHit(level, pos, state, localHit);
            ValueBox box = new ItemValueBox(label, new AABB(Vec3.ZERO, Vec3.ZERO).inflate(0.25), pos, behaviour.getFilter(), Component.empty());
            box.passive(!hit || behaviour.bypassesInput(heldItem));
            Outliner.getInstance().showOutline(Pair.of("filter" + behaviour.netId(), pos), box.transform(transform)).lineWidth(0.015625f).withFaceTexture(hit ? AllSpecialTextures.THIN_CHECKERED : null).highlightFace(hitResult.getDirection());
            if (!hit) {
                continue;
            }

            List<MutableComponent> tooltip = new ArrayList<>();
            tooltip.add(label.copy());
            tooltip.add(behaviour.getTip());
            CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tooltip);
        }
    }
}
