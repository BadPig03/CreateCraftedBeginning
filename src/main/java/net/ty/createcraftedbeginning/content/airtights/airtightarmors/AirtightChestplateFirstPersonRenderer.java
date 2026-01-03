package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(Dist.CLIENT)
public class AirtightChestplateFirstPersonRenderer {
    private static final ResourceLocation CHESTPLATE_ARM_LOCATION = CreateCraftedBeginning.asResource("textures/models/armor/airtight_chestplate_arm.png");

    private static boolean rendererActive;

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        rendererActive = mc.player != null && mc.player.getItemBySlot(EquipmentSlot.CHEST).is(CCBItems.AIRTIGHT_CHESTPLATE);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onChestplateFirstPersonRender(@NotNull RenderArmEvent event) {
        if (!CCBConfig.client().enableChestplateFirstPersonArm.get()) {
            return;
        }
        if (!rendererActive) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }

        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        model.attackTime = 0;
        model.crouching = false;
        model.swimAmount = 0;
        model.setupAnim(player, 0, 0, 0, 0, 0);

        ModelPart armPart = event.getArm() == HumanoidArm.LEFT ? model.leftSleeve : model.rightSleeve;
        armPart.xRot = 0;
        armPart.render(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.entitySolid(CHESTPLATE_ARM_LOCATION)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        event.setCanceled(true);
    }
}
