package net.ty.createcraftedbeginning.advancement;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("unused")
public class CCBAdvancementBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<CCBAdvancementBehaviour> TYPE = new BehaviourType<>();

    private static final String COMPOUND_KEY_OWNER = "Owner";

    private final Set<CCBAdvancement> advancements;
    private UUID playerId;

    public CCBAdvancementBehaviour(SmartBlockEntity be, CCBAdvancement... advancements) {
        super(be);
        this.advancements = new HashSet<>();
        add(advancements);
    }

    public static void tryAward(BlockGetter level, BlockPos pos, CCBAdvancement advancement) {
        CCBAdvancementBehaviour behaviour = get(level, pos, TYPE);
        if (behaviour == null) {
            return;
        }

        behaviour.awardPlayer(advancement);
    }

    public static void setPlacedBy(Level level, BlockPos pos, LivingEntity entity) {
        CCBAdvancementBehaviour behaviour = get(level, pos, TYPE);
        if (behaviour == null || entity instanceof FakePlayer || !(entity instanceof ServerPlayer)) {
            return;
        }

        behaviour.setPlayer(entity.getUUID());
    }

    public void add(CCBAdvancement... advancements) {
        Collections.addAll(this.advancements, advancements);
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

    private void removeAwarded() {
        Player player = getPlayer();
        if (player == null) {
            return;
        }

        advancements.removeIf(advancement -> advancement.isAlreadyAwardedTo(player));
        if (!advancements.isEmpty()) {
            return;
        }

        playerId = null;
        blockEntity.setChanged();
    }

    private @Nullable Player getPlayer() {
        return playerId == null ? null : getWorld().getPlayerByUUID(playerId);

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

    public boolean isOwnerPresent() {
        return playerId != null;
    }

    public void awardPlayerIfNear(CCBAdvancement advancement, int maxDistance) {
        Player player = getPlayer();
        if (player == null || player.distanceToSqr(Vec3.atCenterOf(getPos())) > maxDistance * maxDistance) {
            return;
        }

        award(advancement, player);
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
    public void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (!compoundTag.contains(COMPOUND_KEY_OWNER)) {
            return;
        }

        playerId = compoundTag.getUUID(COMPOUND_KEY_OWNER);
    }

    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        if (playerId == null) {
            return;
        }

        compoundTag.putUUID(COMPOUND_KEY_OWNER, playerId);
    }
}
