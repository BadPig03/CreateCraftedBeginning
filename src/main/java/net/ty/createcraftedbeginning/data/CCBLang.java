package net.ty.createcraftedbeginning.data;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper;
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
import net.ty.createcraftedbeginning.api.gas.GasStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("unused")
public class CCBLang extends Lang {
    public static @NotNull MutableComponent translateDirect(String key, Object... args) {
        Object[] args1 = LangBuilder.resolveBuilders(args);
        return Component.translatable(CreateCraftedBeginning.MOD_ID + "." + key, args1);
    }

    @Contract(value = " -> new", pure = true)
    public static @NotNull LangBuilder builder() {
        return new LangBuilder(CreateCraftedBeginning.MOD_ID);
    }

    public static @NotNull LangBuilder blockName(@NotNull BlockState state) {
        return builder().add(state.getBlock().getName());
    }

    public static @NotNull LangBuilder itemName(@NotNull ItemStack stack) {
        return builder().add(stack.getHoverName().copy());
    }

    public static @NotNull LangBuilder fluidName(@NotNull FluidStack stack) {
        return builder().add(stack.getHoverName().copy());
    }

    public static @NotNull LangBuilder gasName(@NotNull GasStack stack) {
        return builder().add(stack.getHoverName().copy());
    }

    public static @NotNull LangBuilder number(double d) {
        return builder().text(LangNumberFormat.format(d));
    }

    public static @NotNull LangBuilder translate(String langKey, Object... args) {
        return builder().translate(langKey, args);
    }

    public static @NotNull LangBuilder text(String text) {
        return builder().text(text);
    }

    public static @NotNull LangBuilder seconds(int ticks, float tickRate) {
        ticks = Mth.abs(ticks);
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

    public static @NotNull LangBuilder secondsWithGameTicks(int ticks, float tickRate) {
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

    public static void addToGoggles(List<Component> tooltip, String text, Object... args) {
        MutableComponent hint = CCBLang.translateDirect(text, args);
        List<Component> cutString = TooltipHelper.cutTextComponent(hint, FontHelper.Palette.GRAY_AND_WHITE);
        for (Component component : cutString) {
            CCBLang.builder().add(component.copy()).forGoggles(tooltip);
        }
    }
}
