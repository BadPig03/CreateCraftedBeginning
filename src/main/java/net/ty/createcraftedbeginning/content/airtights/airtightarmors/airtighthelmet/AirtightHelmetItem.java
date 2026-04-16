package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments.Mutable;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerClients;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorsUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightBaseArmorItem;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.GogglesUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.WaterBreathingUpgrade;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AirtightHelmetItem extends AirtightBaseArmorItem implements MenuProvider {
    static {
        GogglesItem.addIsWearingPredicate(GogglesUpgrade.INSTANCE::canApply);
    }

    public AirtightHelmetItem(@NotNull Properties properties) {
        super(Type.HELMET, properties);
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack helmet, @NotNull Holder<Enchantment> enchantment) {
        return enchantment.is(EnchantmentTags.ARMOR_EXCLUSIVE) || enchantment.is(EnchantmentTags.CURSE);
    }

    @Override
    public int getEnchantmentLevel(@NotNull ItemStack helmet, @NotNull Holder<Enchantment> enchantment) {
        if (!enchantment.is(Enchantments.AQUA_AFFINITY) || !WaterBreathingUpgrade.INSTANCE.isEnabled(helmet)) {
            return super.getEnchantmentLevel(helmet, enchantment);
        }

        return 1;
    }

    @Override
    public @NotNull ItemEnchantments getAllEnchantments(@NotNull ItemStack helmet, @NotNull RegistryLookup<Enchantment> lookup) {
        ItemEnchantments enchantments = super.getAllEnchantments(helmet, lookup);
        if (!WaterBreathingUpgrade.INSTANCE.isEnabled(helmet)) {
            return enchantments;
        }

        Mutable enchants = new Mutable(enchantments);
        enchants.set(lookup.getOrThrow(Enchantments.AQUA_AFFINITY), 1);
        return enchants.toImmutable();
    }

    @Override
    public boolean isEnderMask(@NotNull ItemStack helmet, @NotNull Player player, @NotNull EnderMan endermanEntity) {
        return helmet.is(CCBItems.AIRTIGHT_HELMET);
    }

    @Override
    public boolean isDamageable(@NotNull ItemStack helmet) {
        return false;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }

        ItemStack helmet = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(helmet, true);
        }

        player.openMenu(this, buf -> ItemStack.STREAM_CODEC.encode(buf, helmet));
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(helmet, false);
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack helmet) {
        return CanisterContainerClients.isBarVisible();
    }

    @Override
    public int getBarWidth(@NotNull ItemStack helmet) {
        return CanisterContainerClients.getBarWidth();
    }

    @Override
    public int getBarColor(@NotNull ItemStack helmet) {
        return CanisterContainerClients.getBarColor();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack helmet, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !CanisterContainerSuppliers.isAnyContainerAvailable(player)) {
            return;
        }

        if (AirtightArmorsUtils.isEntireArmoredUp(player) && tooltipFlag.hasShiftDown()) {
            tooltip.add(CCBLang.translate("gui.tooltips.airtight_armors.fire_immune_condition").style(ChatFormatting.GRAY).component());
            tooltip.addAll(TooltipHelper.cutTextComponent(CCBLang.translateDirect("gui.tooltips.airtight_armors.fire_immune_behaviour"), FontHelper.Palette.STANDARD_CREATE));
        }
        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(CCBLang.gasName(gasContent).add(CCBLang.translate("gui.tooltips.gas_tools.content")).style(ChatFormatting.GRAY).component());
        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasContent.getGasType());
        if (armorsHandler == null) {
            return;
        }

        armorsHandler.appendHelmetHoverText(helmet, context, tooltip, tooltipFlag);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDescription();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new AirtightHelmetMenu(CCBMenuTypes.AIRTIGHT_HELMET_MENU.get(), containerId, playerInventory, player.getMainHandItem());
    }
}
