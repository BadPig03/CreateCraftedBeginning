package net.ty.createcraftedbeginning.content.airtights.balloon;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageStyles.PackageStyle;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public final class BalloonStyleUtils {
    public static final List<PackageItem> REGULAR_BALLOONS = new ArrayList<>();
    public static final List<PackageItem> RARE_BALLOONS = new ArrayList<>();

    public static final PackageStyle BALLOON_10_8 = new PackageStyle("balloon", 10, 8, 20, true);
    public static final PackageStyle BALLOON_10_12 = new PackageStyle("balloon", 10, 12, 20, true);
    public static final PackageStyle BALLOON_12_10 = new PackageStyle("balloon", 12, 10, 20, true);
    public static final PackageStyle BALLOON_12_12 = new PackageStyle("balloon", 12, 12, 20, true);
    public static final PackageStyle BALLOON_RARE_REVERTED = new PackageStyle("rare_reverted", 12, 10, 20, true);
    public static final PackageStyle BALLOON_RARE_SMILE = new PackageStyle("rare_smile", 12, 10, 20, true);
    public static final PackageStyle BALLOON_RARE_CRY = new PackageStyle("rare_cry", 12, 10, 20, true);
    public static final PackageStyle BALLOON_RARE_EYE = new PackageStyle("rare_eye", 12, 10, 20, true);
    public static final PackageStyle BALLOON_RARE_ISAAC = new PackageStyle("rare_isaac", 12, 10, 20, true);
    public static final PackageStyle BALLOON_RARE_GHAST = new PackageStyle("rare_ghast", 12, 10, 20, true);
    public static final PackageStyle BALLOON_RARE_TROLLFACE = new PackageStyle("rare_trollface", 12, 10, 20, true);
    public static final PackageStyle BALLOON_RARE_TENNA = new PackageStyle("rare_tenna", 12, 10, 20, true);
    public static final PackageStyle BALLOON_RARE_PVZ = new PackageStyle("rare_pvz", 12, 10, 20, true);
    public static final PackageStyle BALLOON_RARE_QUESTION_MARKS = new PackageStyle("rare_question_marks", 12, 10, 20, true);
    public static final PackageStyle BALLOON_RARE_POWERFUL = new PackageStyle("rare_powerful", 12, 10, 20, true);
    public static final PackageStyle BALLOON_RARE_CHEESE = new PackageStyle("rare_cheese", 12, 10, 20, true);

    private static final Random RANDOM = new Random();
    private static final int RARE_CHANCE = 2003;

    private BalloonStyleUtils() {
    }

    public static ItemStack getRandomBalloon() {
        List<PackageItem> styles = RANDOM.nextInt(RARE_CHANCE) == 0 ? RARE_BALLOONS : REGULAR_BALLOONS;
        return new ItemStack(styles.get(RANDOM.nextInt(styles.size())));
    }

    public static boolean isRareBalloon(ItemStack stack) {
        return stack.getItem() instanceof BalloonItem balloon && balloon.isRare();
    }

    public static boolean isRegularBalloon(ItemStack stack) {
        return stack.getItem() instanceof BalloonItem balloon && !balloon.isRare();
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

        return 0.6875f + (balloon.style.width() - 12) / 32.0f;
    }

    public static float getFrogportHookDistance(Vec3 diff, float distance, ItemStack box) {
        return getHookDistance(box) * getFrogportChainBlend(diff, distance);
    }

    public static float getFrogportBoxDistance(Vec3 diff, float distance, ItemStack box) {
        return getBoxDistance(box) * getFrogportChainBlend(diff, distance);
    }

    public static float getFrogportBaseY(Vec3 diff, float distance, boolean depositing) {
        if (!depositing) {
            return 0.1875f;
        }
        return Mth.lerp(getFrogportChainBlend(diff, distance), 0.1875f, 0.625f);
    }

    public static Vec3 getPackageOffset(Vec3 diff, float distance, boolean depositing) {
        Vec3 direction = diff.lengthSqr() < 1.0E-6 ? Vec3.ZERO : diff.normalize();
        Vec3 offset = direction.scale(distance);
        if (depositing) {
            offset = offset.subtract(0, 0.75, 0);
        }
        return offset;
    }

    private static float getFrogportChainBlend(Vec3 diff, float itemDistance) {
        return Mth.clamp(itemDistance / Math.max((float) diff.length(), 1), 0, 1);
    }
}
