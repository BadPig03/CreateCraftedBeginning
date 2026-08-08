package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades;

import net.createmod.catnip.data.Couple;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeIcon;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradePowerMode;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.TickingAirtightUpgrade;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum SpectralUpgrade implements TickingAirtightUpgrade {
    INSTANCE;

    private static final ResourceLocation ID = CreateCraftedBeginning.asResource("spectral");
    private static final Couple<Integer> OFFSET = Couple.create(132, 55);

    private static final int RADIUS = 24;
    private static final int SCAN_INTERVAL = 20;
    private static final int EFFECT_DURATION = 60;
    private static final int REFRESH_THRESHOLD = 20;

    private static List<LivingEntity> getNearbyEntities(Player player) {
        BlockPos origin = player.getOnPos();
        AABB bounds = new AABB(origin).inflate(RADIUS);
        return player.level().getEntitiesOfClass(LivingEntity.class, bounds, entity -> isValidTarget(origin, entity));
    }

    private static boolean isValidTarget(BlockPos origin, LivingEntity entity) {
        return !(entity instanceof Player) && entity.isAlive() && !entity.isRemoved() && origin.closerToCenterThan(entity.position(), RADIUS);
    }

    @Override
    public @Unmodifiable List<Component> getComponents(Player player, ItemStack item) {
        return List.of(CCBLang.translateDirect("gui.gas_consumption.supply_require_only"));
    }

    @Override
    public boolean canApply(Player player) {
        return isActive(player, player.getItemBySlot(EquipmentSlot.HEAD));
    }

    @Override
    public boolean meetsConditions(Player player, ItemStack item) {
        return true;
    }

    @Override
    public boolean isRightIndicator() {
        return true;
    }

    @Override
    public AirtightUpgradeIcon getIcon() {
        return AirtightUpgradeIcon.SPECTRAL;
    }

    @Override
    public Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_helmet.spectral_upgrade.description", RADIUS);
    }

    @Override
    public Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_helmet.spectral_upgrade");
    }

    @Override
    public Couple<Integer> getOffset() {
        return OFFSET;
    }

    @Override
    public AirtightUpgradePowerMode getPowerMode() {
        return AirtightUpgradePowerMode.SUPPLY_REQUIRED;
    }

    @Override
    public Item getUpgradeItem() {
        return Items.SPECTRAL_ARROW;
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public void applyEffect(Player player) {
        for (LivingEntity entity : getNearbyEntities(player)) {
            MobEffectInstance effect = entity.getEffect(MobEffects.GLOWING);
            if (effect != null && (effect.getAmplifier() != 0 || !effect.endsWithin(REFRESH_THRESHOLD))) {
                continue;
            }

            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, EFFECT_DURATION, 0));
        }
    }

    @Override
    public boolean isActive(Player player, ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_HELMET) && TickingAirtightUpgrade.super.isActive(player, item);
    }

    @Override
    public boolean shouldApplyEffect(Player player, ItemStack item) {
        return player.tickCount % SCAN_INTERVAL == 0;
    }

    @Override
    public void tick(Player player, ItemStack item) {
        if (!shouldApplyEffect(player, item) || !isActive(player, item)) {
            return;
        }

        applyEffect(player);
    }
}
