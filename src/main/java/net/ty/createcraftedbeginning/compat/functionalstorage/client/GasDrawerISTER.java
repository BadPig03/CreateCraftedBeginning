package net.ty.createcraftedbeginning.compat.functionalstorage.client;

import com.buuz135.functionalstorage.FunctionalStorage.DrawerType;
import com.buuz135.functionalstorage.block.tile.ControllableDrawerTile.DrawerOptions;
import com.buuz135.functionalstorage.client.item.FunctionalStorageISTER;
import com.buuz135.functionalstorage.item.FSAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.compat.functionalstorage.GasDrawerBlock;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasDrawerISTER extends FunctionalStorageISTER {
    private static final String COMPOUND_KEY_DRAWER_OPTIONS = "drawerOptions";
    private static final String COMPOUND_KEY_CREATIVE = "isCreative";

    public static final GasDrawerISTER SLOT_1 = new GasDrawerISTER(DrawerType.X_1);
    public static final GasDrawerISTER SLOT_2 = new GasDrawerISTER(DrawerType.X_2);
    public static final GasDrawerISTER SLOT_4 = new GasDrawerISTER(DrawerType.X_4);

    private final DrawerType drawerType;

    private GasDrawerISTER(DrawerType drawerType) {
        this.drawerType = drawerType;
    }

    private static void renderSlot(Provider registryAccess, CompoundTag tileTag, int slot, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, DrawerOptions options, boolean creative, double offsetX, double offsetY, double width, double height, boolean compact) {
        GasStack storedGas = GasDrawerBlock.readStoredGas(tileTag, slot, registryAccess);
        if (storedGas.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(offsetX, offsetY, 0);
        AABB gasBounds = new AABB(0.0625, 0.078125, 0.0625, 0.0625 + width, 0.078125 + height, 0.9375);
        GasDrawerRenderer.renderItemGas(poseStack, buffers, light, overlay, storedGas, options, gasBounds, compact, creative);
        poseStack.popPose();
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
    }

    @Override
    public void renderByItem(Provider access, ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        renderBlockItem(stack, displayContext, poseStack, buffers, light, overlay, getData(stack), itemPoseStack -> {
            itemPoseStack.mulPose(Axis.YP.rotationDegrees(180));
            itemPoseStack.mulPose(Axis.XN.rotationDegrees(90));
            itemPoseStack.mulPose(Axis.YP.rotationDegrees(90));
        });
        if (!stack.has(FSAttachments.TILE)) {
            return;
        }

        CompoundTag tileTag = stack.get(FSAttachments.TILE);
        if (tileTag == null) {
            return;
        }

        DrawerOptions drawerOptions = new DrawerOptions();
        drawerOptions.deserializeNBT(access, CCBNbtUtils.getCompound(tileTag, COMPOUND_KEY_DRAWER_OPTIONS));
        boolean isCreative = CCBNbtUtils.getBoolean(tileTag, COMPOUND_KEY_CREATIVE);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.translate(0, -1, -1);
        switch (drawerType) {
            case X_1 -> renderSlot(access, tileTag, 0, poseStack, buffers, light, overlay, drawerOptions, isCreative, 0, 0, 0.875, 0.78125, false);
            case X_2 -> {
                renderSlot(access, tileTag, 0, poseStack, buffers, light, overlay, drawerOptions, isCreative, 0, 0, 0.875, 0.34375, false);
                renderSlot(access, tileTag, 1, poseStack, buffers, light, overlay, drawerOptions, isCreative, 0, 0.5, 0.875, 0.34375, false);
            }
            case X_4 -> {
                renderSlot(access, tileTag, 0, poseStack, buffers, light, overlay, drawerOptions, isCreative, 0.5, 0, 0.4375, 0.34375, true);
                renderSlot(access, tileTag, 1, poseStack, buffers, light, overlay, drawerOptions, isCreative, 0, 0, 0.4375, 0.34375, true);
                renderSlot(access, tileTag, 2, poseStack, buffers, light, overlay, drawerOptions, isCreative, 0.5, 0.5, 0.4375, 0.34375, true);
                renderSlot(access, tileTag, 3, poseStack, buffers, light, overlay, drawerOptions, isCreative, 0, 0.5, 0.4375, 0.34375, true);
            }
        }
    }
}
