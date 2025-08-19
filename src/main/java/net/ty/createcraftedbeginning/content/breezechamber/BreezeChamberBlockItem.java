package net.ty.createcraftedbeginning.content.breezechamber;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.data.CCBTags;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

public class BreezeChamberBlockItem extends BlockItem {
    public BreezeChamberBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack heldItem, @NotNull Player player, @NotNull LivingEntity entity, @NotNull InteractionHand hand) {
        if (!CCBTags.CCBEntityFlags.BREEZE_CHAMBER_CAPTURABLE.matches(entity)) {
            return InteractionResult.PASS;
        }

        Level world = player.level();
        spawnCaptureEffects(world, entity.position());
        if (world.isClientSide) {
            return InteractionResult.FAIL;
        }

        giveChamberItemTo(player, heldItem, hand);
        entity.discard();
        return InteractionResult.FAIL;
    }

    protected void giveChamberItemTo(Player player, ItemStack heldItem, InteractionHand hand) {
        ItemStack filled = CCBBlocks.BREEZE_CHAMBER_BLOCK.asStack();
        if (!player.isCreative()) {
            heldItem.shrink(1);
        }
        if (heldItem.isEmpty()) {
            player.setItemInHand(hand, filled);
            return;
        }
        player.getInventory().placeItemBackInInventory(filled);
    }

    private void spawnCaptureEffects(Level level, Vec3 vec) {
        if (level.isClientSide) {
            for (int i = 0; i < 40; i++) {
                Vec3 motion = VecHelper.offsetRandomly(Vec3.ZERO, level.random, .125f);
                level.addParticle(ParticleTypes.GUST, vec.x, vec.y + 1, vec.z, motion.x, motion.y, motion.z);
            }
            return;
        }

        BlockPos soundPos = BlockPos.containing(vec);
        level.playSound(null, soundPos, SoundEvents.BREEZE_HURT, SoundSource.HOSTILE, .25f, .75f);
        level.playSound(null, soundPos, SoundEvents.BREEZE_LAND, SoundSource.HOSTILE, .5f, .75f);
    }
}
