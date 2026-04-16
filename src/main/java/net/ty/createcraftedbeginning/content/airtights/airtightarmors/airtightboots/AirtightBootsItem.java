package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerClients;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorsUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightBaseArmorItem;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.MovementEfficiencyUpgrade;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AirtightBootsItem extends AirtightBaseArmorItem implements MenuProvider {
    public static final ResourceLocation MOVEMENT_EFFICIENCY_ID = CreateCraftedBeginning.asResource("movement_efficiency");
    public static final AttributeModifier MOVEMENT_EFFICIENCY_MODIFIER = new AttributeModifier(MOVEMENT_EFFICIENCY_ID, 1, Operation.ADD_VALUE);
    public static final ResourceLocation JUMP_STRENGTH_ID = CreateCraftedBeginning.asResource("jump_strength");
    public static final AttributeModifier JUMP_STRENGTH_MODIFIER = new AttributeModifier(JUMP_STRENGTH_ID, 0.58, Operation.ADD_VALUE);
    public static final ResourceLocation SAFE_FALL_DISTANCE_ID = CreateCraftedBeginning.asResource("safe_fall_distance");
    public static final AttributeModifier SAFE_FALL_DISTANCE_MODIFIER = new AttributeModifier(SAFE_FALL_DISTANCE_ID, 5, Operation.ADD_VALUE);
    public static final ResourceLocation STEP_HEIGHT_ID = CreateCraftedBeginning.asResource("step_height");
    public static final AttributeModifier STEP_HEIGHT_MODIFIER = new AttributeModifier(STEP_HEIGHT_ID, 2.4, Operation.ADD_VALUE);


    public AirtightBootsItem(@NotNull Properties properties) {
        super(Type.BOOTS, properties);
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack boots, @NotNull Holder<Enchantment> enchantment) {
        return enchantment.is(EnchantmentTags.ARMOR_EXCLUSIVE) || enchantment.is(EnchantmentTags.CURSE);
    }

    @Override
    public int getEnchantmentLevel(@NotNull ItemStack boots, @NotNull Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.DEPTH_STRIDER) && MovementEfficiencyUpgrade.INSTANCE.isEnabled(boots)) {
            return 3;
        }
        return super.getEnchantmentLevel(boots, enchantment);
    }

    @Override
    public @NotNull ItemEnchantments getAllEnchantments(@NotNull ItemStack boots, @NotNull RegistryLookup<Enchantment> lookup) {
        Mutable enchants = new Mutable(super.getAllEnchantments(boots, lookup));
        if (MovementEfficiencyUpgrade.INSTANCE.isEnabled(boots)) {
            enchants.set(lookup.getOrThrow(Enchantments.DEPTH_STRIDER), 3);
        }
        return enchants.toImmutable();
    }

    @Override
    public boolean canWalkOnPowderedSnow(@NotNull ItemStack boots, @NotNull LivingEntity wearer) {
        return wearer instanceof Player && boots.is(CCBItems.AIRTIGHT_BOOTS);
    }

    @Override
    public boolean isDamageable(@NotNull ItemStack boots) {
        return false;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }

        ItemStack boots = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(boots, true);
        }

        player.openMenu(this, buf -> ItemStack.STREAM_CODEC.encode(buf, boots));
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(boots, false);
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack boots) {
        return CanisterContainerClients.isBarVisible();
    }

    @Override
    public int getBarWidth(@NotNull ItemStack boots) {
        return CanisterContainerClients.getBarWidth();
    }

    @Override
    public int getBarColor(@NotNull ItemStack boots) {
        return CanisterContainerClients.getBarColor();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack boots, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
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
        armorsHandler.appendBootsHoverText(boots, context, tooltip, tooltipFlag);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDescription();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new AirtightBootsMenu(CCBMenuTypes.AIRTIGHT_BOOTS_MENU.get(), containerId, playerInventory, player.getMainHandItem());
    }
}
