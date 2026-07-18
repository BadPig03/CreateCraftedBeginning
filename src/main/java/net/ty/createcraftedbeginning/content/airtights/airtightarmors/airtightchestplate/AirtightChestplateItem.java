package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
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
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.ElytraUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.HasteUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableMenu;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightChestplateItem extends AirtightChestplateArmorItem implements MenuProvider {
    private static final ResourceLocation ID = CreateCraftedBeginning.asResource("airtight_chestplate");

    public AirtightChestplateItem(Properties properties) {
        super(Type.CHESTPLATE, properties);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack chestplate) {
        ItemAttributeModifiers modifiers = super.getDefaultAttributeModifiers(chestplate);
        if (HasteUpgrade.INSTANCE.canApply(chestplate)) {
            modifiers = modifiers.withModifierAdded(Attributes.ATTACK_SPEED, new AttributeModifier(ID, 0.2, Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.CHEST).withModifierAdded(Attributes.BLOCK_BREAK_SPEED, new AttributeModifier(ID, 0.4, Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.CHEST).withModifierAdded(Attributes.SUBMERGED_MINING_SPEED, new AttributeModifier(ID, 4, Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.CHEST);
        }
        return modifiers;
    }

    @Override
    public boolean canElytraFly(ItemStack chestplate, LivingEntity entity) {
        return entity instanceof Player player && ElytraUpgrade.INSTANCE.canFly(player, chestplate);
    }

    @Override
    public boolean elytraFlightTick(ItemStack chestplate, LivingEntity entity, int flightTicks) {
        return entity instanceof Player player && ElytraUpgrade.INSTANCE.canFly(player, chestplate);
    }

    @Override
    public boolean isDamageable(ItemStack chestplate) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }

        ItemStack chestplate = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(chestplate, true);
        }

        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return AirtightChestplateItem.this.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player menuPlayer) {
                return new AirtightChestplateMenu(CCBMenuTypes.AIRTIGHT_CHESTPLATE_MENU.get(), containerId, playerInventory, chestplate, hand);
            }
        };
        player.openMenu(provider, buffer -> AirtightUpgradableMenu.writeOpeningData(buffer, chestplate, hand));
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(chestplate, false);
    }

    @Override
    public boolean isBarVisible(ItemStack chestplate) {
        return CanisterContainerClients.isBarVisible();
    }

    @Override
    public int getBarWidth(ItemStack chestplate) {
        return CanisterContainerClients.getBarWidth();
    }

    @Override
    public int getBarColor(ItemStack chestplate) {
        return CanisterContainerClients.getBarColor();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack chestplate, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
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

        float consumption = handler.getConsumptionMultiplier(EquipmentSlot.CHEST);
        MutableComponent advancedConsumption = tooltipFlag.isAdvanced() ? CCBLang.text(" [x" + GasConsumptionUtils.format(consumption) + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.gas_tools.gas_consumption", GasConsumptionUtils.formatPercent(consumption)).add(advancedConsumption.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());

        float boostMultiplier = handler.getMultiplierForBoostingElytra();
        MutableComponent advancedBoost = tooltipFlag.isAdvanced() ? CCBLang.text(" [x" + GasConsumptionUtils.format(boostMultiplier) + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.airtight_armors.elytra_boost", GasConsumptionUtils.formatPercent(boostMultiplier)).add(advancedBoost.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());
    }

    @Override
    public boolean isEnchantable(ItemStack chestplate) {
        return true;
    }

    @Override
    public Component getDisplayName() {
        return getDescription();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        InteractionHand hand = player.getMainHandItem().is(this) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack chestplate = player.getItemInHand(hand);
        return new AirtightChestplateMenu(CCBMenuTypes.AIRTIGHT_CHESTPLATE_MENU.get(), containerId, playerInventory, chestplate, hand);
    }
}
