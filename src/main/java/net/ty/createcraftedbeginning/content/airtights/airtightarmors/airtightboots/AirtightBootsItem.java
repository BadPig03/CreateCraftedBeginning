package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptionUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorsUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightBaseArmorItem;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.JumpStrengthUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.MovementEfficiencyUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.StepHeightUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableMenu;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClients;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.CCBClientBridge;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightBootsItem extends AirtightBaseArmorItem implements MenuProvider {
    private static final ResourceLocation ID = CCBAPI.asResource("airtight_boots");

    public AirtightBootsItem(Properties properties) {
        super(Type.BOOTS, properties);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack boots) {
        ItemAttributeModifiers modifiers = super.getDefaultAttributeModifiers(boots);
        if (MovementEfficiencyUpgrade.INSTANCE.canApply(boots)) {
            modifiers = modifiers.withModifierAdded(Attributes.MOVEMENT_EFFICIENCY, new AttributeModifier(ID, 1, Operation.ADD_VALUE), EquipmentSlotGroup.FEET).withModifierAdded(Attributes.WATER_MOVEMENT_EFFICIENCY, new AttributeModifier(ID, 1, Operation.ADD_VALUE), EquipmentSlotGroup.FEET);
        }
        if (JumpStrengthUpgrade.INSTANCE.canApply(boots)) {
            modifiers = modifiers.withModifierAdded(Attributes.JUMP_STRENGTH, new AttributeModifier(ID, 1, Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.FEET).withModifierAdded(Attributes.SAFE_FALL_DISTANCE, new AttributeModifier(ID, 1, Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.FEET);
        }
        if (!StepHeightUpgrade.INSTANCE.canApply(boots)) {
            return modifiers;
        }

        modifiers = modifiers.withModifierAdded(Attributes.STEP_HEIGHT, new AttributeModifier(ID, 1.4, Operation.ADD_VALUE), EquipmentSlotGroup.FEET);
        return modifiers;
    }

    @Override
    public boolean canWalkOnPowderedSnow(ItemStack boots, LivingEntity entity) {
        return entity instanceof Player && boots.is(CCBItems.AIRTIGHT_BOOTS);
    }

    @Override
    public boolean isDamageable(ItemStack boots) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack boots) {
        return CanisterContainerClients.isBarVisible();
    }

    @Override
    public int getBarWidth(ItemStack boots) {
        return CanisterContainerClients.getBarWidth();
    }

    @Override
    public int getBarColor(ItemStack boots) {
        return CanisterContainerClients.getBarColor();
    }

    @Override
    public void appendHoverText(ItemStack boots, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        Player player = CCBClientBridge.getClientPlayer();
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

        float multiplier = handler.getConsumptionMultiplier(EquipmentSlot.FEET);
        MutableComponent advancedMultiplier = tooltipFlag.isAdvanced() ? CCBLang.text(" [x" + GasConsumptionUtils.format(multiplier) + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.gas_tools.gas_consumption", GasConsumptionUtils.formatPercent(multiplier)).add(advancedMultiplier.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());
    }

    @Override
    public boolean isEnchantable(ItemStack boots) {
        return true;
    }

    @Override
    public Component getDisplayName() {
        return getDescription();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }

        ItemStack boots = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(boots, true);
        }

        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return AirtightBootsItem.this.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player menuPlayer) {
                return new AirtightBootsMenu(CCBMenuTypes.AIRTIGHT_BOOTS_MENU.get(), containerId, playerInventory, boots, hand);
            }
        };
        player.openMenu(provider, buffer -> AirtightUpgradableMenu.writeOpeningData(buffer, boots, hand));
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(boots, false);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        InteractionHand hand = player.getMainHandItem().is(this) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack boots = player.getItemInHand(hand);
        return new AirtightBootsMenu(CCBMenuTypes.AIRTIGHT_BOOTS_MENU.get(), containerId, playerInventory, boots, hand);
    }
}
