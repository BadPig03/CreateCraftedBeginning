package net.ty.createcraftedbeginning.content.airtights.balloon;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.content.logistics.box.PackageStyles.PackageStyle;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BalloonItem extends PackageItem {
    private final boolean rare;

    public BalloonItem(Properties properties, PackageStyle style, boolean rare) {
        super(properties, style);
        this.rare = rare;
        PackageStyles.ALL_BOXES.remove(this);
        PackageStyles.STANDARD_BOXES.remove(this);
        PackageStyles.RARE_BOXES.remove(this);
    }

    public static boolean isBalloon(ItemStack stack) {
        return stack.getItem() instanceof BalloonItem;
    }

    @Override
    public String getDescriptionId() {
        return "item." + CreateCraftedBeginning.MOD_ID + (rare ? ".rare_balloon" : ".balloon");
    }
}
