package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots;

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
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerClients;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorsUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightBaseArmorItem;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.JumpStrengthUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.MovementEfficiencyUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.StepHeightUpgrade;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightBootsItem extends AirtightBaseArmorItem implements MenuProvider {
    private static final ResourceLocation ID = CreateCraftedBeginning.asResource("airtight_boots");

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
        if (StepHeightUpgrade.INSTANCE.canApply(boots)) {
            modifiers = modifiers.withModifierAdded(Attributes.STEP_HEIGHT, new AttributeModifier(ID, 1.4, Operation.ADD_VALUE), EquipmentSlotGroup.FEET);
        }
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
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack boots, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
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

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandlerUtils.of(gasContent.getGasType());
        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(CCBLang.gasName(gasContent).add(CCBLang.translate("gui.tooltips.gas_tools.content")).style(ChatFormatting.GRAY).component());
        float consumptionMultiplier = armorsHandler.getConsumptionMultiplier()[3];
        MutableComponent advancedConsumptionMultiplier = tooltipFlag.isAdvanced() ? CCBLang.text(" [x" + armorsHandler.getRenderStr(consumptionMultiplier) + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.tooltips.gas_tools.gas_consumption", armorsHandler.getRenderStr(consumptionMultiplier * 100)).add(advancedConsumptionMultiplier.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());
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

        player.openMenu(this, buf -> ItemStack.STREAM_CODEC.encode(buf, boots));
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(boots, false);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AirtightBootsMenu(CCBMenuTypes.AIRTIGHT_BOOTS_MENU.get(), containerId, playerInventory, player.getMainHandItem());
    }
}
