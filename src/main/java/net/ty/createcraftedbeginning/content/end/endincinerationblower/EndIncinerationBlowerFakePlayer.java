package net.ty.createcraftedbeginning.content.end.endincinerationblower;

import com.mojang.authlib.GameProfile;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.UsernameCache;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.OptionalInt;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EndIncinerationBlowerFakePlayer extends FakePlayer {
    public static final UUID FALLBACK_UUID = UUID.fromString("053c6a34-9ba9-49fb-bb2d-88edcd146dde");

    private final UUID owner;

    public EndIncinerationBlowerFakePlayer(ServerLevel level, UUID owner) {
        super(level, new EndIncinerationBlowerGameProfile(FALLBACK_UUID, "EndIncinerationBlower", owner));
        this.owner = owner;
    }

    @Override
	public OptionalInt openMenu(@Nullable MenuProvider menuProvider) {
		return OptionalInt.empty();
	}

    @Override
	@OnlyIn(Dist.CLIENT)
	public EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).withEyeHeight(0);
	}

    @Override
	public Vec3 position() {
		return new Vec3(getX(), getY(), getZ());
	}

    @Override
	public float getCurrentItemAttackStrengthDelay() {
		return 0;
	}

    @Override
	public boolean canEat(boolean ignoreHunger) {
		return false;
	}

    @Override
	public ItemStack eat(Level level, ItemStack food, FoodProperties foodProperties) {
		food.shrink(1);
		return food;
	}

    @SuppressWarnings("deprecation")
    @Override
	public boolean canBeAffected(MobEffectInstance effectInstance) {
		return false;
	}

    @Override
	public UUID getUUID() {
		return owner == null ? super.getUUID() : owner;
	}

    @Override
	protected boolean doesEmitEquipEvent(EquipmentSlot slot) {
		return false;
	}

    private static class EndIncinerationBlowerGameProfile extends GameProfile {
        private final UUID owner;

        public EndIncinerationBlowerGameProfile(UUID id, String name, UUID owner) {
            super(id, name);
            this.owner = owner;
        }

        @Override
        public UUID getId() {
            return owner == null ? super.getId() : owner;
        }

        @Override
        public String getName() {
            if (owner == null) {
                return super.getName();
            }

            String lastKnownUsername = UsernameCache.getLastKnownUsername(owner);
            return lastKnownUsername == null ? super.getName() : lastKnownUsername;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof GameProfile otherProfile && getId().equals(otherProfile.getId()) && getName().equals(otherProfile.getName());
        }

        @Override
		public int hashCode() {
            return 31 * getId().hashCode() + getName().hashCode();
		}
    }
}
