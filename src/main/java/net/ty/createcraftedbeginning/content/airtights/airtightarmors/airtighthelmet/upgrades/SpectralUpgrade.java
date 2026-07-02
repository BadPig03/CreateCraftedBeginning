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
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum SpectralUpgrade implements AirtightUpgrade {
    INSTANCE;

    private static final int RADIUS = 24;
    private static final int DURATION_THRESHOLD = 30;

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
        BlockPos pos = player.getOnPos();
        List<LivingEntity> nearbyEntities = player.level().getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(RADIUS)).stream().filter(e -> !(e instanceof Player) && e.isAlive() && !e.isRemoved() && pos.closerToCenterThan(e.position(), RADIUS)).toList();
        return !nearbyEntities.isEmpty();
    }

    @Override
    public boolean isRightIndicator() {
        return true;
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_SPECTRAL;
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
        return Couple.create(132, 55);
    }

    @Override
    public int getGasConsumptionPerSecond(Player player, ItemStack item) {
        return 0;
    }

    @Override
    public int getIndex() {
        return 4;
    }

    @Override
    public Item getUpgradeItem() {
        return Items.SPECTRAL_ARROW;
    }

    @Override
    public ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("spectral");
    }

    @Override
    public void applyEffect(Player player) {
        BlockPos pos = player.getOnPos();
        List<LivingEntity> nearbyEntities = player.level().getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(RADIUS)).stream().filter(e -> !(e instanceof Player) && e.isAlive() && !e.isRemoved() && pos.closerToCenterThan(e.position(), RADIUS)).toList();
        if (nearbyEntities.isEmpty()) {
            return;
        }

        nearbyEntities.forEach(entity -> {
            MobEffectInstance existingEffect = entity.getEffect(MobEffects.GLOWING);
            if (existingEffect != null && (existingEffect.getAmplifier() != 0 || !existingEffect.endsWithin(DURATION_THRESHOLD))) {
                return;
            }

            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, DURATION_THRESHOLD, 0));
        });
    }

    @Override
    public boolean isActive(Player player, ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_HELMET) && AirtightUpgrade.super.isActive(player, item);
    }
}
