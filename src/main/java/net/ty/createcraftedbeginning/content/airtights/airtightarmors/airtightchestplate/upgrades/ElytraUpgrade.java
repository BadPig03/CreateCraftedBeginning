package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades;

import net.createmod.catnip.data.Couple;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.canisters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.AirtightChestplateElytraBoostPacket;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum ElytraUpgrade implements AirtightUpgrade {
    INSTANCE;

    public static float getBoostMultiplier(Player player) {
        if (!player.isFallFlying()) {
            return 0;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!INSTANCE.canApply(player) || player.getCooldowns().isOnCooldown(chestplate.getItem())) {
            return 0;
        }

        Gas gasType = CanisterContainerSuppliers.getFirstAvailableGasContent(player).getGasType();
        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasType);
        if (armorsHandler == null) {
            return 0;
        }

        return armorsHandler.getMultiplierForBoostingElytra();
    }

    public static void speedBoost(Player player, float multiplier) {
        Vec3 pos = player.position();
        Vec3 lookAngle = player.getLookAngle().scale(0.85f * multiplier);
        Vec3 movement = player.getDeltaMovement().scale(0.75f * multiplier);
        player.setDeltaMovement(movement.add(lookAngle));
        player.level().addParticle(ParticleTypes.GUST_EMITTER_SMALL, pos.x, pos.y, pos.z, 0, 0, 0);
        player.playSound(CCBSoundEvents.AIRTIGHT_JETPACK_LAUNCH.getMainEvent(), 1, 0.8f);
        CatnipServices.NETWORK.sendToServer(new AirtightChestplateElytraBoostPacket(multiplier));
    }

    @Override
    public @Unmodifiable List<Component> getComponents(Player player, ItemStack item) {
        int gasCost = getGasConsumptionPerSecond(player, item);
        if (gasCost == 0) {
            return List.of(CCBLang.translateDirect("gui.gas_consumption.supply_require_only"));
        }
        return List.of(CCBLang.translateDirect("gui.gas_consumption_per_second", gasCost));
    }

    @Override
    public boolean canApply(Player player) {
        return isActive(player, player.getItemBySlot(EquipmentSlot.CHEST));
    }

    @Override
    public boolean meetsConditions(Player player, ItemStack item) {
        return true;
    }

    @Override
    public boolean isRightIndicator() {
        return false;
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_ELYTRA;
    }

    @Override
    public Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_chestplate.elytra_upgrade.description");
    }

    @Override
    public Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_chestplate.elytra_upgrade");
    }

    @Override
    public Couple<Integer> getOffset() {
        return Couple.create(36, 31);
    }

    @Override
    public int getGasConsumptionPerSecond(Player player, ItemStack item) {
        return CCBConfig.server().equipments.elytraConsumption.get();
    }

    @Override
    public int getIndex() {
        return 0;
    }

    @Override
    public Item getUpgradeItem() {
        return Items.ELYTRA;
    }

    @Override
    public ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("elytra");
    }

    @Override
    public void applyEffect(Player player) {
    }

    @Override
    public boolean isActive(Player player, ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_CHESTPLATE) && AirtightUpgrade.super.isActive(player, item);
    }
}
