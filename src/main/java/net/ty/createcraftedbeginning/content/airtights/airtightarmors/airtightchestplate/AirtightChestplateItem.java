package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
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
import net.ty.createcraftedbeginning.api.gas.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerClients;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorsUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.ElytraUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.HasteUpgrade;
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
        return entity instanceof Player player && ElytraUpgrade.INSTANCE.canApply(player);
    }

    @Override
    public boolean elytraFlightTick(ItemStack chestplate, LivingEntity entity, int flightTicks) {
        return entity instanceof Player player && ElytraUpgrade.INSTANCE.canApply(player);
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

        player.openMenu(this, buf -> ItemStack.STREAM_CODEC.encode(buf, chestplate));
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
    public boolean isEnchantable(ItemStack chestplate) {
        return true;
    }

    @Override
    public Component getDisplayName() {
        return getDescription();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AirtightChestplateMenu(CCBMenuTypes.AIRTIGHT_CHESTPLATE_MENU.get(), containerId, playerInventory, player.getMainHandItem());
    }
}
