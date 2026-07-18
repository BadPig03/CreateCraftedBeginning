package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades;

import com.simibubi.create.AllItems;
import net.createmod.catnip.data.Couple;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum GogglesUpgrade implements AirtightUpgrade {
    INSTANCE;

    private static final ResourceLocation ID = CreateCraftedBeginning.asResource("goggles");
    private static final Couple<Integer> OFFSET = Couple.create(36, 79);

    @Override
    public List<Component> getComponents(Player player, ItemStack item) {
        return List.of();
    }

    @Override
    public boolean canApply(Player player) {
        return isActive(player, player.getItemBySlot(EquipmentSlot.HEAD));
    }

    @Override
    public boolean meetsConditions(Player player, ItemStack item) {
        return true;
    }

    @Override
    public boolean isRightIndicator() {
        return false;
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_GOGGLES;
    }

    @Override
    public Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_helmet.goggles_upgrade.description");
    }

    @Override
    public Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_helmet.goggles_upgrade");
    }

    @Override
    public Couple<Integer> getOffset() {
        return OFFSET;
    }

    @Override
    public Item getUpgradeItem() {
        return AllItems.GOGGLES.asItem();
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public void applyEffect(Player player) {
    }

    @Override
    public boolean isActive(Player player, ItemStack item) {
        return item.is(CCBItems.AIRTIGHT_HELMET) && AirtightUpgrade.super.isActive(player, item);
    }
}
