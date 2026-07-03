package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerClients;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorsUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightBaseArmorItem;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.GogglesUpgrade;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightHelmetItem extends AirtightBaseArmorItem implements MenuProvider {
    static {
        GogglesItem.addIsWearingPredicate(GogglesUpgrade.INSTANCE::canApply);
    }

    public AirtightHelmetItem(Properties properties) {
        super(Type.HELMET, properties);
    }

    @Override
    public boolean isEnderMask(ItemStack helmet, Player player, EnderMan endermanEntity) {
        return helmet.is(CCBItems.AIRTIGHT_HELMET);
    }

    @Override
    public boolean isDamageable(ItemStack helmet) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
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
    public boolean isBarVisible(ItemStack helmet) {
        return CanisterContainerClients.isBarVisible();
    }

    @Override
    public int getBarWidth(ItemStack helmet) {
        return CanisterContainerClients.getBarWidth();
    }

    @Override
    public int getBarColor(ItemStack helmet) {
        return CanisterContainerClients.getBarColor();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack helmet, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
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

        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(CCBLang.gasName(gasContent).add(CCBLang.translate("gui.tooltips.gas_tools.content")).style(ChatFormatting.GRAY).component());
        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasContent.getGasType());
        if (armorsHandler == null) {
            return;
        }

        armorsHandler.appendHelmetHoverText(helmet, context, tooltip, tooltipFlag);
    }

    @Override
    public boolean isEnchantable(ItemStack helmet) {
        return true;
    }

    @Override
    public Component getDisplayName() {
        return getDescription();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AirtightHelmetMenu(CCBMenuTypes.AIRTIGHT_HELMET_MENU.get(), containerId, playerInventory, player.getMainHandItem());
    }
}
