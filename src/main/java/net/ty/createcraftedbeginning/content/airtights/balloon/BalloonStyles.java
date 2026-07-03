package net.ty.createcraftedbeginning.content.airtights.balloon;

import com.simibubi.create.content.logistics.box.PackageStyles.PackageStyle;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public final class BalloonStyles {
    public static final PackageStyle BALLON_10_8 = new PackageStyle("ballon", 10, 8, 20, true);
    public static final PackageStyle BALLON_10_12 = new PackageStyle("ballon", 10, 12, 20, true);
    public static final PackageStyle BALLON_12_10 = new PackageStyle("ballon", 12, 10, 20, true);
    public static final PackageStyle BALLON_12_12 = new PackageStyle("ballon", 12, 12, 20, true);
    public static final PackageStyle BALLON_RARE = new PackageStyle("rare_", 12, 10, 20, true);
    private static final Map<ResourceLocation, PartialModel> BALLOONS = new HashMap<>();

    private BalloonStyles() {
    }

    public static PackageStyle getDefaultStyle() {
        return BALLON_12_10;
    }

    public static String getPath(PackageStyle style) {
        return style.width() + "x" + style.height();
    }

    public static void addPackage(ResourceLocation location, PartialModel model) {
        BALLOONS.put(location, model);
    }

    public static PartialModel getPackageModel(ResourceLocation location) {
        return BALLOONS.get(location);
    }

    public static float getHookDistance(ItemStack box) {
        if (!(box.getItem() instanceof BalloonItem balloon)) {
            return 1;
        }

        return balloon.style.riggingOffset() / 16.0f;
    }

    public static float getBoxDistance(ItemStack box) {
        if (!(box.getItem() instanceof BalloonItem balloon)) {
            return 1;
        }

        return 0.6875f + (balloon.style.width() - 10) / 32.0f;
    }
}
