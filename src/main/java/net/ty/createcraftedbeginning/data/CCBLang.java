package net.ty.createcraftedbeginning.data;

import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

import java.util.ArrayList;
import java.util.List;

public class CCBLang extends Lang {
    public static MutableComponent translateDirect(String key, Object... args) {
        Object[] args1 = LangBuilder.resolveBuilders(args);
        return Component.translatable(CreateCraftedBeginning.MOD_ID + "." + key, args1);
    }

    public static List<Component> translatedOptions(String prefix, String... keys) {
        List<Component> result = new ArrayList<>(keys.length);
        for (String key : keys) {
            result.add(translate((prefix != null ? prefix + "." : "") + key).component());
        }
        return result;
    }

    public static LangBuilder builder() {
        return new LangBuilder(CreateCraftedBeginning.MOD_ID);
    }

    public static LangBuilder blockName(BlockState state) {
        return builder().add(state.getBlock().getName());
    }

    public static LangBuilder itemName(ItemStack stack) {
        return builder().add(stack.getHoverName().copy());
    }

    public static LangBuilder fluidName(FluidStack stack) {
        return builder().add(stack.getHoverName().copy());
    }

    public static LangBuilder number(double d) {
        return builder().text(LangNumberFormat.format(d));
    }

    public static LangBuilder translate(String langKey, Object... args) {
        return builder().translate(langKey, args);
    }

    public static LangBuilder text(String text) {
        return builder().text(text);
    }

    public static LangBuilder seconds(int ticks, float tickRate) {
        int totalSeconds = Mth.floor(ticks / tickRate);

        if (totalSeconds >= 60) {
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;

            if (seconds == 0) {
                return builder().translate("gui.minutes", minutes);
            } else {
                return builder().translate("gui.minutes_seconds", minutes, seconds);
            }
        }
        return builder().translate("gui.seconds", totalSeconds);
    }

    public static LangBuilder secondsWithGameTicks(int ticks, float tickRate) {
        float totalSeconds = ticks / tickRate;

        if (totalSeconds < 1.f) {
            return builder().translate("gui.ticks", ticks);
        }

        int wholeSeconds = Mth.floor(totalSeconds);

        if (wholeSeconds >= 60) {
            int minutes = wholeSeconds / 60;
            int seconds = wholeSeconds % 60;

            if (seconds == 0) {
                return builder().translate("gui.minutes", minutes);
            } else {
                return builder().translate("gui.minutes_seconds", minutes, seconds);
            }
        }
        return builder().translate("gui.seconds", wholeSeconds);
    }
}
