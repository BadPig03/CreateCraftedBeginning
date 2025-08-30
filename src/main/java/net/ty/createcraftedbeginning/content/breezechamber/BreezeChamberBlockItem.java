package net.ty.createcraftedbeginning.content.breezechamber;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerData;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.config.CCBConfig;
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

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);

        if (!CCBConfig.server().compressedAir.canChamberGetFromSpawners.get()) {
            return super.useOn(context);
        }

        if (be instanceof TrialSpawnerBlockEntity spawnerBlockEntity) {
            InteractionResult result = getResultFromTrialSpawner(spawnerBlockEntity, context);
            return result == InteractionResult.FAIL ? super.useOn(context) : result;
        }

        if (be instanceof SpawnerBlockEntity spawnerBlockEntity) {
            InteractionResult result = getResultFromSpawner(spawnerBlockEntity, context);
            return result == InteractionResult.FAIL ? super.useOn(context) : result;
        }

        return super.useOn(context);
    }

    private InteractionResult getResultFromTrialSpawner(TrialSpawnerBlockEntity spawnerBlockEntity, @NotNull UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        InteractionHand hand = context.getHand();
        ItemStack heldItem = context.getItemInHand();

        if (player == null) {
            return InteractionResult.FAIL;
        }

        TrialSpawnerState state = spawnerBlockEntity.getState();
        if (!state.isCapableOfSpawning() || !state.hasSpinningMob()) {
            return InteractionResult.FAIL;
        }

        TrialSpawner spawner = spawnerBlockEntity.getTrialSpawner();
        TrialSpawnerData data = spawner.getData();
        if (data.getOrCreateDisplayEntity(spawner, level, state) instanceof Breeze) {
            giveChamberItemTo(player, heldItem, hand);
            spawnCaptureEffects(level, VecHelper.getCenterOf(spawnerBlockEntity.getBlockPos().below()));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    private InteractionResult getResultFromSpawner(SpawnerBlockEntity spawnerBlockEntity, @NotNull UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        InteractionHand hand = context.getHand();
        ItemStack heldItem = context.getItemInHand();

        if (player == null) {
            return InteractionResult.FAIL;
        }

        BaseSpawner spawner = spawnerBlockEntity.getSpawner();
        if (spawner.getOrCreateDisplayEntity(level, pos) instanceof Breeze) {
            giveChamberItemTo(player, heldItem, hand);
            spawnCaptureEffects(level, VecHelper.getCenterOf(spawnerBlockEntity.getBlockPos().below()));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    private void giveChamberItemTo(Player player, ItemStack heldItem, InteractionHand hand) {
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
