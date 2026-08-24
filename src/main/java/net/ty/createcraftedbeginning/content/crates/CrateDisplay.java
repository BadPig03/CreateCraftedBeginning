package net.ty.createcraftedbeginning.content.crates;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class CrateDisplay {
    private final CrateBlockEntityStorage storage;

    CrateDisplay(CrateBlockEntityStorage storage) {
        this.storage = storage;
    }

    int maxValue() {
        return storage.handler().getConfiguredCapacity();
    }

    int currentValue() {
        return Math.min(storage.storedCount(), maxValue());
    }

    MutableComponent format(int value) {
        return CCBLang.text(value + " ").add(CCBLang.translate("gui.threshold.items")).component();
    }

    void addToGoggleTooltip(List<Component> tooltip) {
        CCBLang.translate("gui.crates.header").forGoggles(tooltip);
        ItemStack storedItem = storage.storedItem();
        int storedCount = storage.storedCount();
        int capacity = maxValue();
        if (storedItem.isEmpty() || storedCount == 0) {
            CCBLang.translate("gui.crates.capacity").style(ChatFormatting.GRAY).add(CCBLang.number(capacity).style(ChatFormatting.GOLD)).forGoggles(tooltip, 1);
            return;
        }

        CCBLang.itemName(storedItem).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        CCBLang.number(storedCount).style(ChatFormatting.GOLD).add(CCBLang.text(" / ").style(ChatFormatting.GRAY)).add(CCBLang.number(capacity).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
    }
}
