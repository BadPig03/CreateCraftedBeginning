package net.ty.createcraftedbeginning.api.gas.gases;

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
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasFilteringBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class GasFilteringRenderer {
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        HitResult target = mc.hitResult;
        if (!(target instanceof BlockHitResult result)) {
            return;
        }

        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        BlockPos pos = result.getBlockPos();
        if (!(level.getBlockEntity(pos) instanceof SmartBlockEntity smartBlockEntity)) {
            return;
        }

        LocalPlayer player = mc.player;
        if (player == null || player.isShiftKeyDown()) {
            return;
        }

        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        for (BlockEntityBehaviour blockEntityBehaviour : smartBlockEntity.getAllBehaviours()) {
            if (!(blockEntityBehaviour instanceof GasFilteringBehaviour behaviour) || !behaviour.isActive()) {
                continue;
            }

            ValueBoxTransform slotPositioning = behaviour.getSlotPositioning();
            BlockState state = level.getBlockState(pos);
            if (!slotPositioning.shouldRender(level, pos, state) || !behaviour.mayInteract(player)) {
                continue;
            }

            Component label = behaviour.getLabel();
            boolean hit = slotPositioning.testHit(level, pos, state, target.getLocation().subtract(Vec3.atLowerCornerOf(pos)));
            ValueBox box = new ItemValueBox(label, new AABB(Vec3.ZERO, Vec3.ZERO).inflate(0.25f), pos, behaviour.getFilter(), Component.empty());
            box.passive(!hit || behaviour.bypassesInput(mainHandItem));
            Outliner.getInstance().showOutline(Pair.of("filter" + behaviour.netId(), pos), box.transform(slotPositioning)).lineWidth(0.015625f).withFaceTexture(hit ? AllSpecialTextures.THIN_CHECKERED : null).highlightFace(result.getDirection());
            if (!hit) {
                continue;
            }

            List<MutableComponent> tip = new ArrayList<>();
            tip.add(label.copy());
            tip.add(behaviour.getTip());
            CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
        }
    }
}
