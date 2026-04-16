package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades;

import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public enum SpectralUpgrade implements AirtightUpgrade {
    INSTANCE;

    private static final int RADIUS = 24;
    private static final int DURATION_LIMIT = 230;
    private static final int DURATION_THRESHOLD = 10;

    private static @NotNull @Unmodifiable List<LivingEntity> getNearbyEntities(@NotNull Player player) {
        BlockPos pos = player.getOnPos();
        return player.level().getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(RADIUS)).stream().filter(entity -> !(entity instanceof Player) && entity.isAlive() && !entity.isRemoved() && pos.closerToCenterThan(entity.position(), RADIUS)).toList();
    }

    private static boolean canApply(@NotNull Player player, @NotNull LivingEntity entity) {
        MobEffectInstance existingEffect = entity.getEffect(MobEffects.GLOWING);
        if (existingEffect != null && (existingEffect.getAmplifier() != 0 || !existingEffect.endsWithin(DURATION_THRESHOLD))) {
            return false;
        }

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.is(CCBItems.AIRTIGHT_HELMET) || !INSTANCE.isEnabled(helmet) || !CanisterContainerSuppliers.isAnyContainerAvailable(player)) {
            return false;
        }

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty() || getNearbyEntities(player).isEmpty()) {
            return false;
        }

        int gasCost = INSTANCE.getGasCost(player);
        return gasCost >= 0;
    }

    @Override
    public int getIndex() {
        return 4;
    }

    @Override
    public @NotNull ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("spectral");
    }

    @Override
    public Item getUpgradeItem() {
        return Items.SPECTRAL_ARROW;
    }

    @Override
    public @NotNull Couple<Integer> getOffset() {
        return Couple.create(132, 55);
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_SPECTRAL;
    }

    @Override
    public boolean isRightIndicator() {
        return true;
    }

    @Override
    public @NotNull Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_helmet.spectral_upgrade");
    }

    @Override
    public @NotNull Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_helmet.spectral_upgrade.description", RADIUS);
    }

    @Override
    public @Nullable Component getGasCostComponent(Player player) {
        int gasCost = getGasCost(player);
        if (gasCost < 0) {
            return null;
        }

        return CCBLang.translateDirect("gui.gas_cost_per_second", gasCost);
    }

    @Override
    public int getGasCost(Player player) {
        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            return -1;
        }

        return 0;
    }

    @Override
    public boolean canApply(@NotNull Player player) {
        Level level = player.level();
        if (level.getGameTime() % 10 != 0) {
            return false;
        }

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.is(CCBItems.AIRTIGHT_HELMET) || !isEnabled(helmet) || !CanisterContainerSuppliers.isAnyContainerAvailable(player)) {
            return false;
        }

        List<LivingEntity> nearbyEntities = getNearbyEntities(player);
        if (nearbyEntities.isEmpty()) {
            return false;
        }

        nearbyEntities.forEach(entity -> {
            if (!canApply(player, entity)) {
                return;
            }

            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, DURATION_LIMIT, 0));
        });
        return false;
    }

    @Override
    public void applyEffect(@NotNull Player player) {
    }
}
