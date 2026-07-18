package net.ty.createcraftedbeginning.content.airtights.airtightcannon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.zapper.ShootableGadgetRenderHandler;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge.AirtightCannonWindChargeProjectileEntity;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightCannonRenderHandler extends ShootableGadgetRenderHandler {
    private float nextPitch;

    @Override
    protected void playSound(InteractionHand hand, Vec3 position) {
        AirtightCannonWindChargeProjectileEntity.playLaunchSound(Minecraft.getInstance().level, position, nextPitch);
    }

    @Override
    protected boolean appliesTo(ItemStack stack) {
        return stack.is(CCBItems.AIRTIGHT_CANNON);
    }

    @Override
    protected void transformTool(PoseStack poseStack, float flip, float equipProgress, float recoil, float partialTick) {
        poseStack.translate(flip * -0.1f, 0, 0.14f);
        poseStack.scale(0.75f, 0.75f, 0.75f);
        TransformStack.of(poseStack).rotateXDegrees(recoil * 80);
    }

    @Override
    protected void transformHand(PoseStack poseStack, float flip, float equipProgress, float recoil, float partialTick) {
        poseStack.translate(flip * -0.09, -0.275, -0.25);
        TransformStack.of(poseStack).rotateZDegrees(flip * -10);
    }

    public void beforeShoot(float pitch, Vec3 location, Vec3 motion, ItemStack stack) {
        nextPitch = pitch;
        ClientLevel level = Minecraft.getInstance().level;
        if (stack.isEmpty() || level == null) {
            return;
        }

        for (int i = 0; i < 2; i++) {
            Vec3 particleMotion = VecHelper.offsetRandomly(motion.scale(0.1f), level.random, 0.025f);
            level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack), location.x, location.y, location.z, particleMotion.x, particleMotion.y, particleMotion.z);
        }
    }
}
