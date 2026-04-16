package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
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
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerClients;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorsUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.ElytraUpgrade;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AirtightChestplateItem extends AirtightChestplateArmorItem implements MenuProvider {
    public static final ResourceLocation CREATIVE_FLIGHT_ID = CreateCraftedBeginning.asResource("creative_flight");
    public static final AttributeModifier CREATIVE_FLIGHT_MODIFIER = new AttributeModifier(CREATIVE_FLIGHT_ID, 1, Operation.ADD_VALUE);
    public static final ResourceLocation ATTACK_SPEED_ID = CreateCraftedBeginning.asResource("attack_speed");
    public static final AttributeModifier ATTACK_SPEED_MODIFIER = new AttributeModifier(ATTACK_SPEED_ID, 0.5, Operation.ADD_MULTIPLIED_TOTAL);
    public static final ResourceLocation BLOCK_BREAK_SPEED_ID = CreateCraftedBeginning.asResource("block_break_speed");
    public static final AttributeModifier BLOCK_BREAK_SPEED_MODIFIER = new AttributeModifier(BLOCK_BREAK_SPEED_ID, 1, Operation.ADD_MULTIPLIED_TOTAL);

    public AirtightChestplateItem(Properties properties) {
        super(Type.CHESTPLATE, properties);
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack chestplate, @NotNull Holder<Enchantment> enchantment) {
        return enchantment.is(EnchantmentTags.ARMOR_EXCLUSIVE) || enchantment.is(EnchantmentTags.CURSE);
    }

    @Override
    public boolean canElytraFly(@NotNull ItemStack chestplate, @NotNull LivingEntity entity) {
        return entity instanceof Player player && ElytraUpgrade.canApply(player, 10);
    }

    @Override
    public boolean elytraFlightTick(@NotNull ItemStack chestplate, @NotNull LivingEntity entity, int flightTicks) {
        return entity instanceof Player player && ElytraUpgrade.canApply(player, flightTicks);
    }

    @Override
    public boolean isDamageable(@NotNull ItemStack chestplate) {
        return false;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }

        ItemStack chestplate = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(chestplate, true);
        }

        player.openMenu(this, buf -> ItemStack.STREAM_CODEC.encode(buf, chestplate));
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(chestplate, false);
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack chestplate) {
        return CanisterContainerClients.isBarVisible();
    }

    @Override
    public int getBarWidth(@NotNull ItemStack chestplate) {
        return CanisterContainerClients.getBarWidth();
    }

    @Override
    public int getBarColor(@NotNull ItemStack chestplate) {
        return CanisterContainerClients.getBarColor();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack chestplate, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
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
        armorsHandler.appendChestplateHoverText(chestplate, context, tooltip, tooltipFlag);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDescription();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new AirtightChestplateMenu(CCBMenuTypes.AIRTIGHT_CHESTPLATE_MENU.get(), containerId, playerInventory, player.getMainHandItem());
    }
}
