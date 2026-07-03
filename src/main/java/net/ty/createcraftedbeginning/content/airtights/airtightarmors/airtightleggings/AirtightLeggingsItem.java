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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
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
import net.ty.createcraftedbeginning.api.gas.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerClients;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorsUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightBaseArmorItem;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.BlastResistanceUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.SwiftSneakUpgrade;
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
        if (SwiftSneakUpgrade.INSTANCE.canApply(leggings)) {
            modifiers = modifiers.withModifierAdded(Attributes.SNEAKING_SPEED, new AttributeModifier(ID, 0.45, Operation.ADD_VALUE), EquipmentSlotGroup.LEGS);
        }
        return modifiers;
    }

    @Override
    public int getEnchantmentLevel(ItemStack leggings, Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.BLAST_PROTECTION) && BlastResistanceUpgrade.INSTANCE.canApply(leggings)) {
            return 4;
        }
        return super.getEnchantmentLevel(leggings, enchantment);
    }

    @Override
    public ItemEnchantments getAllEnchantments(ItemStack leggings, RegistryLookup<Enchantment> lookup) {
        ItemEnchantments enchantments = super.getAllEnchantments(leggings, lookup);
        Mutable enchants = new Mutable(enchantments);
        if (BlastResistanceUpgrade.INSTANCE.canApply(leggings)) {
            enchants.set(lookup.getOrThrow(Enchantments.BLAST_PROTECTION), 4);
        }
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

        player.openMenu(this, buf -> ItemStack.STREAM_CODEC.encode(buf, leggings));
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
        if (player == null || !CanisterContainerSuppliers.isAnyContainerAvailable(player)) {
            return;
        }

        if (AirtightArmorsUtils.isEntireArmoredUp(player) && tooltipFlag.hasShiftDown()) {
            tooltip.add(CCBLang.translate("gui.tooltips.airtight_armors.fire_immune_condition").style(ChatFormatting.GRAY).component());
            tooltip.addAll(TooltipHelper.cutTextComponent(CCBLang.translateDirect("gui.tooltips.airtight_armors.fire_immune_behaviour"), Palette.STANDARD_CREATE));
        }

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            return;
        }

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasContent.getGasType());
        if (armorsHandler == null) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(CCBLang.gasName(gasContent).add(CCBLang.translate("gui.tooltips.gas_tools.content")).style(ChatFormatting.GRAY).component());
        armorsHandler.appendLeggingsHoverText(leggings, context, tooltip, tooltipFlag);
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
        return new AirtightLeggingsMenu(CCBMenuTypes.AIRTIGHT_LEGGINGS_MENU.get(), containerId, playerInventory, player.getMainHandItem());
    }
}
