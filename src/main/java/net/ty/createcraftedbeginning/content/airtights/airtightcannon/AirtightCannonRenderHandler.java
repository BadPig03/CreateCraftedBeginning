package net.ty.createcraftedbeginning.content.airtights.airtightcannon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.zapper.ShootableGadgetRenderHandler;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge.AirtightCannonWindChargeProjectileEntity;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

public class AirtightCannonRenderHandler extends ShootableGadgetRenderHandler {
    private float nextPitch;

    @Override
    protected void playSound(InteractionHand hand, Vec3 position) {
        AirtightCannonWindChargeProjectileEntity.playLaunchSound(Minecraft.getInstance().level, position, nextPitch);
    }

    @Override
    protected boolean appliesTo(@NotNull ItemStack stack) {
        return stack.is(CCBItems.AIRTIGHT_CANNON);
    }

    @Override
    protected void transformTool(@NotNull PoseStack ms, float flip, float equipProgress, float recoil, float pt) {
        ms.translate(flip * -0.1f, 0, 0.14f);
        ms.scale(0.75f, 0.75f, 0.75f);
        TransformStack.of(ms).rotateXDegrees(recoil * 80);
    }

    @Override
    protected void transformHand(@NotNull PoseStack ms, float flip, float equipProgress, float recoil, float pt) {
        ms.translate(flip * -0.09, -0.275, -0.25);
        TransformStack.of(ms).rotateZDegrees(flip * -10);
    }

    public void beforeShoot(float pitch, Vec3 location, Vec3 motion, @NotNull ItemStack stack) {
        nextPitch = pitch;
        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (stack.isEmpty() || clientLevel == null) {
            return;
        }

        for (int i = 0; i < 2; i++) {
            Vec3 m = VecHelper.offsetRandomly(motion.scale(0.1f), clientLevel.random, 0.025f);
            clientLevel.addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack), location.x, location.y, location.z, m.x, m.y, m.z);
        }
    }
}
