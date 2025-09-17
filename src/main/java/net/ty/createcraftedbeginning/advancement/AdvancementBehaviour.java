package net.ty.createcraftedbeginning.advancement;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("unused")
public class AdvancementBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<AdvancementBehaviour> TYPE = new BehaviourType<>();
    private final Set<CCBAdvancement> advancements;
    private UUID playerId;

    public AdvancementBehaviour(SmartBlockEntity be, CCBAdvancement... advancements) {
        super(be);
        this.advancements = new HashSet<>();
        add(advancements);
    }

    public static void tryAward(BlockGetter reader, BlockPos pos, CCBAdvancement advancement) {
        AdvancementBehaviour behaviour = BlockEntityBehaviour.get(reader, pos, AdvancementBehaviour.TYPE);
        if (behaviour != null) {
            behaviour.awardPlayer(advancement);
        }
    }

    public static void tryAwardToNearbyPlayers(@NotNull Level level, BlockPos pos, CCBAdvancement advancement, int radius) {
        List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(radius));
        for (Player player : players) {
            if (!(player instanceof FakePlayer) && player instanceof ServerPlayer) {
                advancement.awardTo(player);
            }
        }
    }

    public static void tryAwardToNearbyPlayersWithLooking(@NotNull Level level, BlockPos pos, CCBAdvancement advancement, int radius, float maxAngleDegrees) {
        List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(radius));

        Vec3 targetPos = Vec3.atCenterOf(pos);

        for (Player player : players) {
            if (!(player instanceof ServerPlayer) || player instanceof FakePlayer) {
                continue;
            }

            if (!((player.getMainHandItem().is(Items.SPYGLASS) || player.getOffhandItem().is(Items.SPYGLASS)) && player.isUsingItem())) {
                continue;
            }

            Vec3 playerPos = player.getEyePosition(1f);
            Vec3 lookDirection = player.getViewVector(1f);
            Vec3 toTarget = targetPos.subtract(playerPos).normalize();

            double dotProduct = lookDirection.dot(toTarget);
            double angleRadians = Math.acos(dotProduct);
            double angleDegrees = Math.toDegrees(angleRadians);

            if (angleDegrees <= maxAngleDegrees) {
                advancement.awardTo(player);
            }
        }
    }

    public static void setPlacedBy(Level worldIn, BlockPos pos, LivingEntity placer) {
        AdvancementBehaviour behaviour = BlockEntityBehaviour.get(worldIn, pos, TYPE);
        if (behaviour == null) {
            return;
        }
        if (placer instanceof FakePlayer) {
            return;
        }
        if (placer instanceof ServerPlayer) {
            behaviour.setPlayer(placer.getUUID());
        }
    }

    public void add(CCBAdvancement... advancements) {
        Collections.addAll(this.advancements, advancements);
    }

    public boolean isOwnerPresent() {
        return playerId != null;
    }

    private void removeAwarded() {
        Player player = getPlayer();
        if (player == null) {
            return;
        }
        advancements.removeIf(c -> c.isAlreadyAwardedTo(player));
        if (advancements.isEmpty()) {
            playerId = null;
            blockEntity.setChanged();
        }
    }

    public void awardPlayerIfNear(CCBAdvancement advancement, int maxDistance) {
        Player player = getPlayer();
        if (player == null) {
            return;
        }
        if (player.distanceToSqr(Vec3.atCenterOf(getPos())) > maxDistance * maxDistance) {
            return;
        }
        award(advancement, player);
    }

    public void awardPlayer(CCBAdvancement advancement) {
        Player player = getPlayer();
        if (player == null) {
            return;
        }

        award(advancement, player);
    }

    private void award(CCBAdvancement advancement, Player player) {
        if (advancements.contains(advancement)) {
            advancement.awardTo(player);
        }
        removeAwarded();
    }

    private @Nullable Player getPlayer() {
        if (playerId == null) {
            return null;
        }
        return getWorld().getPlayerByUUID(playerId);
    }

    public void setPlayer(UUID id) {
        Player player = getWorld().getPlayerByUUID(id);
        if (player == null) {
            return;
        }
        playerId = id;
        removeAwarded();
        blockEntity.setChanged();
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public void initialize() {
        super.initialize();
        removeAwarded();
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(nbt, registries, clientPacket);
        if (nbt.contains("Owner")) {
            playerId = nbt.getUUID("Owner");
        }
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(nbt, registries, clientPacket);
        if (playerId != null) {
            nbt.putUUID("Owner", playerId);
        }
    }
}
