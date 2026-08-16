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
public final class CrateDisplay {
    private final CrateBlockEntityStorage storage;

    public CrateDisplay(CrateBlockEntityStorage storage) {
        this.storage = storage;
    }

    public int maxValue() {
        return storage.handler().getSlotLimit(0);
    }

    public int currentValue() {
        return storage.storedCount();
    }

    public MutableComponent format(int value) {
        return CCBLang.text(value + " ").add(CCBLang.translate("gui.threshold.items")).component();
    }

    public void addToGoggleTooltip(List<Component> tooltip) {
        CCBLang.translate("gui.crates.header").forGoggles(tooltip);
        ItemStack content = storage.storedItem();
        int count = storage.storedCount();
        int maxCount = maxValue();
        if (content.isEmpty() || count == 0) {
            CCBLang.translate("gui.crates.capacity").style(ChatFormatting.GRAY).add(CCBLang.number(maxCount).style(ChatFormatting.GOLD)).forGoggles(tooltip, 1);
            return;
        }

        CCBLang.itemName(content).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        CCBLang.number(count).style(ChatFormatting.GOLD).add(CCBLang.text(" / ").style(ChatFormatting.GRAY)).add(CCBLang.number(maxCount).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
    }
}
