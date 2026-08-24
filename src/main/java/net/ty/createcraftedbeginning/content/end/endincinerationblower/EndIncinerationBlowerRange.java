package net.ty.createcraftedbeginning.content.end.endincinerationblower;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.config.CCBConfig;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class EndIncinerationBlowerRange {
    private EndIncinerationBlowerRange() {
    }

    public static float getMaxRange() {
        return Mth.clamp(CCBConfig.server().endDevices.maxRange.getF(), 0, 32);
    }

    public static float calculateRange(float speed) {
        float absSpeed = Mth.abs(speed);
        float maxRange = getMaxRange();
        if (absSpeed <= 0 || maxRange <= 0) {
            return 0;
        }

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();
        if (mediumSpeed <= 0) {
            return maxRange;
        }

        if (absSpeed < mediumSpeed) {
            return 0;
        }
        return Mth.clamp(absSpeed / mediumSpeed - 0.5f, 0, maxRange);
    }

    public static int calculateBlockRadius(float speed) {
        return Mth.ceil(calculateRange(speed));
    }

    public static AABB calculateArea(BlockPos pos, float speed) {
        Vec3 center = Vec3.atCenterOf(pos);
        float range = calculateRange(speed);
        return new AABB(center.x - range, center.y - range, center.z - range, center.x + range, center.y + range, center.z + range);
    }
}
