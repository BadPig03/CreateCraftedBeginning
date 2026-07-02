package net.ty.createcraftedbeginning.data;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBLang extends Lang {
    public static LangBuilder blockName(BlockState state) {
        return builder().add(state.getBlock().getName());
    }

    @Contract(value = " -> new", pure = true)
    public static LangBuilder builder() {
        return new LangBuilder(CreateCraftedBeginning.MOD_ID);
    }

    public static LangBuilder itemName(ItemStack stack) {
        return builder().add(stack.getHoverName().copy());
    }

    public static LangBuilder fluidName(FluidStack stack) {
        return builder().add(stack.getHoverName().copy());
    }

    public static LangBuilder gasName(GasStack stack) {
        return builder().add(stack.getHoverName().copy());
    }

    public static LangBuilder gasName(Gas gasType) {
        return builder().add(Component.translatable(gasType.getTranslationKey()).copy());
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
        ticks = Mth.abs(ticks);
        int totalSeconds = Mth.floor(ticks / tickRate);

        if (totalSeconds >= 60) {
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;

            return seconds == 0 ? builder().translate("gui.minutes", minutes) : builder().translate("gui.minutes_seconds", minutes, seconds);
        }
        return builder().translate("gui.seconds", totalSeconds);
    }

    public static LangBuilder secondsWithGameTicks(int ticks, float tickRate) {
        float totalSeconds = ticks / tickRate;

        if (totalSeconds < 1.0f) {
            return builder().translate("gui.ticks", ticks);
        }

        int wholeSeconds = Mth.floor(totalSeconds);

        if (wholeSeconds >= 60) {
            int minutes = wholeSeconds / 60;
            int seconds = wholeSeconds % 60;

            return seconds == 0 ? builder().translate("gui.minutes", minutes) : builder().translate("gui.minutes_seconds", minutes, seconds);
        }
        return builder().translate("gui.seconds", wholeSeconds);
    }

    public static MutableComponent translateDirect(String key, Object... args) {
        return Component.translatable(CreateCraftedBeginning.MOD_ID + '.' + key, LangBuilder.resolveBuilders(args));
    }

    public static List<Component> translatedOptions(String prefix, String @NotNull ... keys) {
        List<Component> result = new ArrayList<>(keys.length);
        for (String key : keys) {
            result.add(translate(prefix + '.' + key).component());
        }
        return result;
    }

    public static void addToGoggles(List<Component> tooltip, String text, Object... args) {
        MutableComponent hint = translateDirect(text, args);
        List<Component> cutString = TooltipHelper.cutTextComponent(hint, Palette.GRAY_AND_WHITE);
        for (Component component : cutString) {
            builder().add(component.copy()).forGoggles(tooltip);
        }
    }
}
