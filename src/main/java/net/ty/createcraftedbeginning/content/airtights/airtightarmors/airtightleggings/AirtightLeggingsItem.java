package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments.Mutable;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerClients;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptionUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorsUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightBaseArmorItem;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.BlastResistanceUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.SwiftSneakUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableMenu;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightLeggingsItem extends AirtightBaseArmorItem implements MenuProvider {
    private static final ResourceLocation ID = CreateCraftedBeginning.asResource("airtight_leggings");

    public AirtightLeggingsItem(Properties properties) {
        super(Type.LEGGINGS, properties);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack leggings) {
        ItemAttributeModifiers modifiers = super.getDefaultAttributeModifiers(leggings);
        if (!SwiftSneakUpgrade.INSTANCE.canApply(leggings)) {
            return modifiers;
        }

        modifiers = modifiers.withModifierAdded(Attributes.SNEAKING_SPEED, new AttributeModifier(ID, 0.45, Operation.ADD_VALUE), EquipmentSlotGroup.LEGS);
        return modifiers;
    }

    @Override
    public int getEnchantmentLevel(ItemStack leggings, Holder<Enchantment> enchantment) {
        int existingLevel = super.getEnchantmentLevel(leggings, enchantment);
        if (enchantment.is(Enchantments.BLAST_PROTECTION) && BlastResistanceUpgrade.INSTANCE.canApply(leggings)) {
            return Math.max(4, existingLevel);
        }
        return existingLevel;
    }

    @Override
    public ItemEnchantments getAllEnchantments(ItemStack leggings, RegistryLookup<Enchantment> lookup) {
        ItemEnchantments enchantments = super.getAllEnchantments(leggings, lookup);
        Mutable enchants = new Mutable(enchantments);
        if (!BlastResistanceUpgrade.INSTANCE.canApply(leggings)) {
            return enchants.toImmutable();
        }

        Holder<Enchantment> blastProtection = lookup.getOrThrow(Enchantments.BLAST_PROTECTION);
        enchants.set(blastProtection, Math.max(4, enchantments.getLevel(blastProtection)));
        return enchants.toImmutable();
    }

    @Override
    public boolean isDamageable(ItemStack leggings) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }

        ItemStack leggings = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(leggings, true);
        }

        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return AirtightLeggingsItem.this.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player menuPlayer) {
                return new AirtightLeggingsMenu(CCBMenuTypes.AIRTIGHT_LEGGINGS_MENU.get(), containerId, playerInventory, leggings, hand);
            }
        };
        player.openMenu(provider, buffer -> AirtightUpgradableMenu.writeOpeningData(buffer, leggings, hand));
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(leggings, false);
    }

    @Override
    public boolean isBarVisible(ItemStack leggings) {
        return CanisterContainerClients.isBarVisible();
    }

    @Override
    public int getBarWidth(ItemStack leggings) {
        return CanisterContainerClients.getBarWidth();
    }

    @Override
    public int getBarColor(ItemStack leggings) {
        return CanisterContainerClients.getBarColor();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack leggings, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (AirtightArmorsUtils.isEntireArmoredUp(player) && tooltipFlag.hasShiftDown()) {
            tooltip.add(CCBLang.translate("gui.airtight_armors.fire_immune_condition").style(ChatFormatting.GRAY).component());
            tooltip.addAll(TooltipHelper.cutTextComponent(CCBLang.translateDirect("gui.airtight_armors.fire_immune_behaviour"), Palette.STANDARD_CREATE));
        }

        GasStack gas = CanisterContainerClients.getDisplayedGasContent();
        if (gas.isEmpty()) {
            return;
        }

        AirtightArmorsHandler handler = AirtightArmorsHandlerUtils.of(gas.getGasType());
        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(CCBLang.gasName(gas).add(CCBLang.translate("gui.gas_tools.content")).style(ChatFormatting.GRAY).component());

        float multiplier = handler.getConsumptionMultiplier(EquipmentSlot.LEGS);
        MutableComponent advancedMultiplier = tooltipFlag.isAdvanced() ? CCBLang.text(" [x" + GasConsumptionUtils.format(multiplier) + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.gas_tools.gas_consumption", GasConsumptionUtils.formatPercent(multiplier)).add(advancedMultiplier.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());
    }

    @Override
    public boolean isEnchantable(ItemStack leggings) {
        return true;
    }

    @Override
    public Component getDisplayName() {
        return getDescription();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        InteractionHand hand = player.getMainHandItem().is(this) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack leggings = player.getItemInHand(hand);
        return new AirtightLeggingsMenu(CCBMenuTypes.AIRTIGHT_LEGGINGS_MENU.get(), containerId, playerInventory, leggings, hand);
    }
}
