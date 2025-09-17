package net.ty.createcraftedbeginning.api.airtightcannon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.ty.createcraftedbeginning.content.airtightcannon.CCBAirtightCannonProjectileRenderModes;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
public record AirtightCannonProjectileType(HolderSet<Item> items, int reloadTicks, int damage, int split, float knockback, float drag, float velocityMultiplier, float gravityMultiplier, float soundPitch, boolean sticky, ItemStack dropStack, AirtightCannonProjectileRenderMode renderMode, Optional<AirtightCannonProjectileEntityHitAction> preEntityHit,
                                           Optional<AirtightCannonProjectileEntityHitAction> onEntityHit, Optional<AirtightCannonProjectileBlockHitAction> onBlockHit) {
    public static final Codec<AirtightCannonProjectileType> CODEC = RecordCodecBuilder.create(i -> i.group(RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(AirtightCannonProjectileType::items), Codec.INT.optionalFieldOf("reload_ticks", 10).forGetter(AirtightCannonProjectileType::reloadTicks), Codec.INT.optionalFieldOf("damage", 1).forGetter(AirtightCannonProjectileType::damage), Codec.INT.optionalFieldOf("split", 1).forGetter(AirtightCannonProjectileType::split), Codec.FLOAT.optionalFieldOf("knockback", 1f).forGetter(AirtightCannonProjectileType::knockback), Codec.FLOAT.optionalFieldOf("drag", .99f).forGetter(AirtightCannonProjectileType::drag), Codec.FLOAT.optionalFieldOf("velocity_multiplier", 1f).forGetter(AirtightCannonProjectileType::velocityMultiplier), Codec.FLOAT.optionalFieldOf("gravity_multiplier", 1f).forGetter(AirtightCannonProjectileType::gravityMultiplier), Codec.FLOAT.optionalFieldOf("sound_pitch", 1f).forGetter(AirtightCannonProjectileType::soundPitch), Codec.BOOL.optionalFieldOf("sticky", false).forGetter(AirtightCannonProjectileType::sticky), ItemStack.CODEC.optionalFieldOf("drop_stack", ItemStack.EMPTY).forGetter(AirtightCannonProjectileType::dropStack), AirtightCannonProjectileRenderMode.CODEC.optionalFieldOf("render_mode", CCBAirtightCannonProjectileRenderModes.Billboard.INSTANCE).forGetter(AirtightCannonProjectileType::renderMode), AirtightCannonProjectileEntityHitAction.CODEC.optionalFieldOf("pre_entity_hit").forGetter(p -> p.preEntityHit), AirtightCannonProjectileEntityHitAction.CODEC.optionalFieldOf("on_entity_hit").forGetter(p -> p.onEntityHit), AirtightCannonProjectileBlockHitAction.CODEC.optionalFieldOf("on_block_hit").forGetter(p -> p.onBlockHit)).apply(i, AirtightCannonProjectileType::new));

    @SuppressWarnings("deprecation")
    public static @NotNull Optional<Holder.Reference<AirtightCannonProjectileType>> getTypeForItem(@NotNull RegistryAccess registryAccess, Item item) {
        return registryAccess.lookupOrThrow(CCBRegistries.AIRTIGHT_CANNON_PROJECTILE_TYPE).listElements().filter(ref -> ref.value().items.contains(item.builtInRegistryHolder())).findFirst();
    }

    public boolean preEntityHit(ItemStack stack, EntityHitResult ray) {
        return preEntityHit.map(i -> i.execute(stack, ray, AirtightCannonProjectileEntityHitAction.Type.PRE_HIT)).orElse(false);
    }

    public boolean onEntityHit(ItemStack stack, EntityHitResult ray) {
        return onEntityHit.map(i -> i.execute(stack, ray, AirtightCannonProjectileEntityHitAction.Type.ON_HIT)).orElse(false);
    }

    public boolean onBlockHit(LevelAccessor level, ItemStack stack, BlockHitResult ray) {
        return onBlockHit.map(i -> i.execute(level, stack, ray)).orElse(false);
    }

    @Override
    public @NotNull ItemStack dropStack() {
        return dropStack.copy();
    }

    public static class Builder {
        private final List<Holder<Item>> items = new ArrayList<>();
        private int reloadTicks = 10;
        private int damage = 1;
        private int split = 1;
        private float knockback = 1f;
        private float drag = 0.99f;
        private float velocityMultiplier = 1f;
        private float gravityMultiplier = 1f;
        private float soundPitch = 1f;
        private boolean sticky = false;
        private ItemStack dropStack = ItemStack.EMPTY;
        private AirtightCannonProjectileRenderMode renderMode = CCBAirtightCannonProjectileRenderModes.Billboard.INSTANCE;
        private AirtightCannonProjectileEntityHitAction preEntityHit = null;
        private AirtightCannonProjectileEntityHitAction onEntityHit = null;
        private AirtightCannonProjectileBlockHitAction onBlockHit = null;

        public Builder reloadTicks(int reload) {
            this.reloadTicks = reload;
            return this;
        }

        public Builder damage(int damage) {
            this.damage = damage;
            return this;
        }

        public Builder splitInto(int split) {
            this.split = split;
            return this;
        }

        public Builder knockback(float knockback) {
            this.knockback = knockback;
            return this;
        }

        public Builder drag(float drag) {
            this.drag = drag;
            return this;
        }

        public Builder velocity(float velocity) {
            this.velocityMultiplier = velocity;
            return this;
        }

        public Builder gravity(float modifier) {
            this.gravityMultiplier = modifier;
            return this;
        }

        public Builder soundPitch(float pitch) {
            this.soundPitch = pitch;
            return this;
        }

        public Builder sticky() {
            this.sticky = true;
            return this;
        }

        public Builder dropStack(ItemStack stack) {
            this.dropStack = stack;
            return this;
        }

        public void renderMode(AirtightCannonProjectileRenderMode renderMode) {
            this.renderMode = renderMode;
        }

        public Builder renderBillboard() {
            renderMode(CCBAirtightCannonProjectileRenderModes.Billboard.INSTANCE);
            return this;
        }

        public Builder renderTumbling() {
            renderMode(CCBAirtightCannonProjectileRenderModes.Tumble.INSTANCE);
            return this;
        }

        public Builder renderTowardMotion(int spriteAngle, float spin) {
            renderMode(new CCBAirtightCannonProjectileRenderModes.TowardMotion(spriteAngle, spin));
            return this;
        }

        public Builder preEntityHit(AirtightCannonProjectileEntityHitAction entityHitAction) {
            this.preEntityHit = entityHitAction;
            return this;
        }

        public Builder onEntityHit(AirtightCannonProjectileEntityHitAction entityHitAction) {
            this.onEntityHit = entityHitAction;
            return this;
        }

        public Builder onBlockHit(AirtightCannonProjectileBlockHitAction blockHitAction) {
            this.onBlockHit = blockHitAction;
            return this;
        }

        @SuppressWarnings("deprecation")
        public Builder addItems(ItemLike @NotNull ... items) {
            for (ItemLike provider : items) {
                this.items.add(provider.asItem().builtInRegistryHolder());
            }
            return this;
        }

        public AirtightCannonProjectileType build() {
            return new AirtightCannonProjectileType(HolderSet.direct(items), reloadTicks, damage, split, knockback, drag, velocityMultiplier, gravityMultiplier, soundPitch, sticky, dropStack, renderMode, Optional.ofNullable(preEntityHit), Optional.ofNullable(onEntityHit), Optional.ofNullable(onBlockHit));
        }
    }
}
