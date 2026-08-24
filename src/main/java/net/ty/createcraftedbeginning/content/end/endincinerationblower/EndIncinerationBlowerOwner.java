package net.ty.createcraftedbeginning.content.end.endincinerationblower;

import com.mojang.authlib.GameProfile;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class EndIncinerationBlowerOwner {
    private static final String COMPOUND_KEY_OWNER = "IncinerationOwner";
    private static final String FAKE_PLAYER_NAME = "[CCB_EIB]";
    private static final String FAKE_PLAYER_UUID_PREFIX = "createcraftedbeginning:end_incineration_blower:";

    private GameProfile fakePlayerProfile;
    private UUID ownerId;

    boolean setOwner(UUID ownerId) {
        if (Objects.equals(this.ownerId, ownerId)) {
            return false;
        }

        this.ownerId = ownerId;
        fakePlayerProfile = null;
        return true;
    }

    void write(CompoundTag compoundTag) {
        if (ownerId == null) {
            return;
        }

        compoundTag.putUUID(COMPOUND_KEY_OWNER, ownerId);
    }

    void read(CompoundTag compoundTag) {
        if (compoundTag.contains(COMPOUND_KEY_OWNER)) {
            ownerId = compoundTag.getUUID(COMPOUND_KEY_OWNER);
        }
        else {
            ownerId = null;
        }
        fakePlayerProfile = null;
    }

    FakePlayer getFakePlayer(ServerLevel level, BlockPos pos) {
        if (fakePlayerProfile == null) {
            String ownerIdentity = ownerId == null ? "unowned" : ownerId.toString();
            UUID fakePlayerId = UUID.nameUUIDFromBytes((FAKE_PLAYER_UUID_PREFIX + ownerIdentity).getBytes(StandardCharsets.UTF_8));
            fakePlayerProfile = new GameProfile(fakePlayerId, FAKE_PLAYER_NAME);
        }

        FakePlayer fakePlayer = FakePlayerFactory.get(level, fakePlayerProfile);
        Vec3 center = VecHelper.getCenterOf(pos);
        fakePlayer.setPos(center.x, center.y, center.z);
        return fakePlayer;
    }
}
