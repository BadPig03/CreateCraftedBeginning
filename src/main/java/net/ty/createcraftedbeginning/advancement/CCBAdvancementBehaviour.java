package net.ty.createcraftedbeginning.advancement;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
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

    public static void setPlacedBy(Level level, BlockPos pos, @Nullable LivingEntity entity) {
        CCBAdvancementBehaviour behaviour = get(level, pos, TYPE);
        if (behaviour == null || entity instanceof FakePlayer || !(entity instanceof ServerPlayer player)) {
            return;
        }

        behaviour.setPlayer(player.getUUID());
    }

    public void add(CCBAdvancement... advancements) {
        Collections.addAll(this.advancements, advancements);
    }

    public void awardPlayer(CCBAdvancement advancement) {
        Player player = getPlayer();
        if (player == null || advancement.isAlreadyAwardedTo(player)) {
            return;
        }

        if (advancements.contains(advancement)) {
            advancement.awardTo(player);
        }
        removeAwarded();
    }

    public @Nullable Player getPlayer() {
        if (playerId == null) {
            return null;
        }

        return getWorld().getPlayerByUUID(playerId);
    }

    public void setPlayer(UUID id) {
        if (getWorld().getPlayerByUUID(id) == null) {
            return;
        }

        playerId = id;
        removeAwarded();
        blockEntity.setChanged();
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
    public void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        super.read(tag, provider, clientPacket);
        if (!tag.contains(COMPOUND_KEY_OWNER)) {
            return;
        }

        playerId = tag.getUUID(COMPOUND_KEY_OWNER);
    }

    @Override
    public void write(CompoundTag tag, Provider provider, boolean clientPacket) {
        super.write(tag, provider, clientPacket);
        if (playerId == null) {
            return;
        }

        tag.putUUID(COMPOUND_KEY_OWNER, playerId);
    }
}
