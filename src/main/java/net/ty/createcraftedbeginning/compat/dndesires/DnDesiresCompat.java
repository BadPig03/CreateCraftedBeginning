package net.ty.createcraftedbeginning.compat.dndesires;

import com.simibubi.create.Create;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.ty.createcraftedbeginning.compat.CCBCompatMods;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class DnDesiresCompat {
    private DnDesiresCompat() {
    }

    public static void register() {
        GasInjectionChamberUtils.registerFanProcessingColor(CCBCompatMods.DNDESIRES.asResource("seething"), ARGB32.average(0xFF64C9FD, 0xFF3F74E8));
    }

    public static void registerJeiFanProcessingCategories(BiConsumer<ResourceLocation, ResourceLocation> registrar) {
        registrar.accept(CCBCompatMods.DNDESIRES.asResource("seething"), Create.asResource("fan_seething"));
    }
}
