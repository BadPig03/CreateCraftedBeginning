package net.ty.createcraftedbeginning.content.crates.sturdycrate;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class SturdyCrateTooltip {
    private SturdyCrateTooltip() {
    }

    static void append(ItemStack crateStack, List<Component> tooltips) {
        SturdyCrateContents storedContents = crateStack.getOrDefault(CCBDataComponents.STURDY_CRATE_CONTENTS, SturdyCrateContents.empty());
        ItemStack storedItem = storedContents.content();
        int storedCount = storedContents.count();
        if (!storedItem.isEmpty()) {
            tooltips.add(CCBLang.translate("gui.sturdy_crate.item").add(CCBLang.itemName(storedItem)).style(ChatFormatting.GRAY).component());
        }

        tooltips.add(CCBLang.translate("gui.sturdy_crate.capacity").style(ChatFormatting.GRAY).add(CCBLang.number(storedCount).style(ChatFormatting.GOLD)).add(CCBLang.text(" / ").style(ChatFormatting.GRAY)).add(CCBLang.number(CCBConfig.server().storage.sturdyCrate.itemCapacity.get()).style(ChatFormatting.DARK_GRAY)).component());
    }
}
