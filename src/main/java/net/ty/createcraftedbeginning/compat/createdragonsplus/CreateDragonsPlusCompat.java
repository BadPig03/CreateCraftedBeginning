package net.ty.createcraftedbeginning.compat.createdragonsplus;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.item.DyeColor;
import net.ty.createcraftedbeginning.compat.CCBCompatMods;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CreateDragonsPlusCompat {
    private static final ResourceLocation COLORING_CATEGORY = CCBCompatMods.CREATE_DRAGONS_PLUS.asResource("coloring");

    private CreateDragonsPlusCompat() {
    }

    public static void register() {
        for (DyeColor dyeColor : DyeColor.values()) {
            GasInjectionChamberUtils.registerFanProcessingColor(CCBCompatMods.CREATE_DRAGONS_PLUS.asResource("coloring_" + dyeColor.getName()), 0xFF000000 | dyeColor.getFireworkColor());
        }
        GasInjectionChamberUtils.registerFanProcessingColor(CCBCompatMods.CREATE_DRAGONS_PLUS.asResource("ending"), ARGB32.average(0xFFB700D2, 0xFFDF00F9));
        GasInjectionChamberUtils.registerFanProcessingColor(CCBCompatMods.CREATE_DRAGONS_PLUS.asResource("sanding"), 0xFFDBD3A0);
        GasInjectionChamberUtils.registerFanProcessingColor(CCBCompatMods.CREATE_DRAGONS_PLUS.asResource("freezing"), ARGB32.average(0xFFFFFFFF, 0xFF8ADCE8));
    }

    public static void registerJeiFanProcessingCategories(BiConsumer<ResourceLocation, ResourceLocation> registrar) {
        for (DyeColor dyeColor : DyeColor.values()) {
            registrar.accept(CCBCompatMods.CREATE_DRAGONS_PLUS.asResource("coloring_" + dyeColor.getName()), COLORING_CATEGORY);
        }
        registrar.accept(CCBCompatMods.CREATE_DRAGONS_PLUS.asResource("ending"), CCBCompatMods.CREATE_DRAGONS_PLUS.asResource("ending"));
        registrar.accept(CCBCompatMods.CREATE_DRAGONS_PLUS.asResource("sanding"), CCBCompatMods.CREATE_DRAGONS_PLUS.asResource("sanding"));
        registrar.accept(CCBCompatMods.CREATE_DRAGONS_PLUS.asResource("freezing"), CCBCompatMods.CREATE_DRAGONS_PLUS.asResource("freezing"));
    }
}
