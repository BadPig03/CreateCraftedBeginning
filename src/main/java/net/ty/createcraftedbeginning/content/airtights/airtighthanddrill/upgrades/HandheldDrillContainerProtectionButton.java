package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades;

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

public enum HandheldDrillContainerProtectionButton implements AirtightUpgrade {
    INSTANCE;

    @Override
    public int getIndex() {
        return 5;
    }

    @Override
    public @NotNull ResourceLocation getID() {
        return CreateCraftedBeginning.asResource("handheld_drill_container_protection");
    }

    @Override
    public @Nullable Item getUpgradeItem() {
        return null;
    }

    @Override
    public @NotNull Couple<Integer> getOffset() {
        return Couple.create(58, 114);
    }

    @Override
    public CCBIcons getIcon() {
        return CCBIcons.I_CONTAINER_PROTECTION;
    }

    @Override
    public boolean isRightIndicator() {
        return true;
    }

    @Override
    public @NotNull Component getTitle() {
        return CCBLang.translateDirect("gui.airtight_handheld_drill.container_protection");
    }

    @Override
    public @NotNull Component getDescription() {
        return CCBLang.translateDirect("gui.airtight_handheld_drill.container_protection.description");
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

    @Override
    public boolean startsInstalled() {
        return true;
    }
}
