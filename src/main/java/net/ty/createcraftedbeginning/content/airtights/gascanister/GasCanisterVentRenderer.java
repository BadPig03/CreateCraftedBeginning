package net.ty.createcraftedbeginning.content.airtights.gascanister;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags.Items;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasCanisterVentRenderer {
    private static final Object OUTLINE_KEY = new Object();
    private static final Sided TRANSFORM = new ValveTransform();

    private GasCanisterVentRenderer() {
    }

    static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        Outliner outliner = Outliner.getInstance();
        outliner.remove(OUTLINE_KEY);
        if (level == null || player == null || minecraft.screen != null || !GasCanisterVentInteraction.canUse(player)) {
            return;
        }

        if (!player.getMainHandItem().is(Items.TOOLS_WRENCH) && !player.getOffhandItem().is(Items.TOOLS_WRENCH)) {
            return;
        }

        if (!(minecraft.hitResult instanceof BlockHitResult hitResult) || hitResult.getType() != Type.BLOCK) {
            return;
        }

        BlockPos pos = hitResult.getBlockPos();
        if (!(level.getBlockEntity(pos) instanceof GasCanisterBlockEntity canister)) {
            return;
        }

        MutableComponent label = CCBLang.translateDirect("gui.gas_canister.release_all");
        ValueBox box = new ValueBox(label, new AABB(Vec3.ZERO, Vec3.ZERO).inflate(0.25), pos).transform(TRANSFORM);
        outliner.showOutline(OUTLINE_KEY, box).highlightFace(Direction.UP);
        if (!GasCanisterVentInteraction.isValveHit(hitResult)) {
            return;
        }

        boolean empty = canister.getCanister().getOrDefault(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, GasStack.EMPTY).isEmpty();
        MutableComponent hint = CCBLang.translateDirect(empty ? "gui.gas_canister.empty" : "gui.gas_canister.release_all_hint");
        CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(List.of(label, hint));
    }

    private static final class ValveTransform extends Sided {
        @Override
        public float getScale() {
            return (float) GasCanisterVentInteraction.VALVE_WIDTH;
        }

        @Override
        protected Vec3 getSouthLocation() {
            return new Vec3(0.5, 0.5, GasCanisterVentInteraction.VALVE_HEIGHT + 1.0 / 1024);
        }
    }
}
