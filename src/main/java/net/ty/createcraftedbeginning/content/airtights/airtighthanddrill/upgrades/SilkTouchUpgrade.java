package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades;

import com.simibubi.create.AllItems;
import net.createmod.catnip.data.Couple;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum SilkTouchUpgrade implements AirtightUpgrade {
    INSTANCE;

    @Override
    public int getIndex() {
        return 0;
    }

    @Override
    public @NotNull ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("silk_touch");
    }

    @Override
    public @NotNull Item getUpgradeItem() {
        return AllItems.PRECISION_MECHANISM.asItem();
    }

    @Override
    public @NotNull Couple<Integer> getOffset() {
        return Couple.create(123, 78);
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_SILK_TOUCH;
    }

    @Override
    public boolean isRightIndicator() {
        return false;
    }

    @Override
    public @NotNull Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_handheld_drill.silk_touch_upgrade");
    }

    @Override
    public @NotNull Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_handheld_drill.silk_touch_upgrade.description");
    }

    @Override
    public @Nullable Component getGasCostComponent(Player player) {
        return null;
    }

    @Override
    public int getGasCost(Player player) {
        return 0;
    }

    @Override
    public boolean canApply(Player player) {
        return false;
    }

    @Override
    public void applyEffect(Player player) {
    }
}
