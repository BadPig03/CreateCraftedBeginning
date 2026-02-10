package net.ty.createcraftedbeginning.content.cinder.cinderincinerationblower;

import com.mojang.authlib.GameProfile;
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
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;

public class CinderIncinerationBlowerFakePlayer extends FakePlayer {
    public static final UUID FALLBACK_UUID = UUID.fromString("053c6a34-9ba9-49fb-bb2d-88edcd146dde");

    private final UUID owner;

    public CinderIncinerationBlowerFakePlayer(ServerLevel level, UUID owner) {
        super(level, new CinderIncinerationBlowerGameProfile(FALLBACK_UUID, "CinderIncinerationBlower", owner));
        this.owner = owner;
    }

    @Override
	public @NotNull OptionalInt openMenu(MenuProvider menuProvider) {
		return OptionalInt.empty();
	}

    @Override
	@OnlyIn(Dist.CLIENT)
	public @NotNull EntityDimensions getDefaultDimensions(@NotNull Pose pose) {
		return super.getDefaultDimensions(pose).withEyeHeight(0);
	}

    @Override
	public @NotNull Vec3 position() {
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
	public @NotNull ItemStack eat(@NotNull Level level, @NotNull ItemStack food, @NotNull FoodProperties foodProperties) {
		food.shrink(1);
		return food;
	}

    @SuppressWarnings("deprecation")
    @Override
	public boolean canBeAffected(@NotNull MobEffectInstance effectInstance) {
		return false;
	}

    @Override
	public @NotNull UUID getUUID() {
		return owner == null ? super.getUUID() : owner;
	}

    @Override
	protected boolean doesEmitEquipEvent(@NotNull EquipmentSlot slot) {
		return false;
	}

    private static class CinderIncinerationBlowerGameProfile extends GameProfile {
        private final UUID owner;

        public CinderIncinerationBlowerGameProfile(UUID id, String name, UUID owner) {
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
            return this == other || other instanceof GameProfile otherProfile && Objects.equals(getId(), otherProfile.getId()) && Objects.equals(getName(), otherProfile.getName());
        }

        @Override
		public int hashCode() {
			UUID id = getId();
			String name = getName();
			int result = id == null ? 0 : id.hashCode();
            return 31 * result + (name == null ? 0 : name.hashCode());
		}
    }
}
