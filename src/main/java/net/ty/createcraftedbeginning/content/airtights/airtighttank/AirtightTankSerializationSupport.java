package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightTankSerializationSupport {
    public static final String TANK_CONTENT = "TankContent";
    private static final String UPDATE_CONNECTIVITY = "UpdateConnectivity";
    private static final String LAST_KNOWN_POS = "LastKnownPos";
    private static final String CONTROLLER_POS = "Controller";
    private static final String WIDTH = "Width";
    private static final String HEIGHT = "Height";

    private AirtightTankSerializationSupport() {
    }

    public static void writeMultiblock(AbstractAirtightTankBlockEntity tank, CompoundTag compoundTag, boolean clientPacket) {
        AirtightTankMultiblockController multiblock = tank.multiblockController();
        if (tank.isController()) {
            CCBNbtUtils.putInt(compoundTag, WIDTH, multiblock.getWidth());
            CCBNbtUtils.putInt(compoundTag, HEIGHT, multiblock.getHeight());
        }
        else {
            BlockPos controllerPos = Objects.requireNonNull(multiblock.getControllerPos());
            CCBNbtUtils.putTag(compoundTag, CONTROLLER_POS, NbtUtils.writeBlockPos(controllerPos));
        }

        if (clientPacket) {
            return;
        }

        CCBNbtUtils.putBoolean(compoundTag, UPDATE_CONNECTIVITY, multiblock.isUpdateConnectivity());
        BlockPos lastKnownPos = multiblock.getLastKnownPos();
        if (lastKnownPos == null) {
            return;
        }

        CCBNbtUtils.putTag(compoundTag, LAST_KNOWN_POS, NbtUtils.writeBlockPos(lastKnownPos));
    }

    public static void writeSafeMultiblock(AbstractAirtightTankBlockEntity tank, CompoundTag compoundTag) {
        if (!tank.isController()) {
            return;
        }

        CCBNbtUtils.putInt(compoundTag, WIDTH, tank.getWidth());
        CCBNbtUtils.putInt(compoundTag, HEIGHT, tank.getHeight());
    }

    public static boolean readMultiblock(AbstractAirtightTankBlockEntity tank, CompoundTag compoundTag, boolean clientPacket) {
        AirtightTankMultiblockController multiblock = tank.multiblockController();
        BlockPos previousControllerPos = multiblock.getControllerPos();
        int previousWidth = multiblock.getWidth();
        int previousHeight = multiblock.getHeight();
        if (!clientPacket) {
            multiblock.setUpdateConnectivity(CCBNbtUtils.getBoolean(compoundTag, UPDATE_CONNECTIVITY));
            multiblock.setLastKnownPos(readOptionalBlockPos(compoundTag, LAST_KNOWN_POS));
        }
        multiblock.setControllerPos(readOptionalBlockPos(compoundTag, CONTROLLER_POS));
        if (tank.isController()) {
            multiblock.setWidth(readDimension(compoundTag, WIDTH, AbstractAirtightTankBlockEntity.configuredMaxWidth()));
            multiblock.setHeight(readDimension(compoundTag, HEIGHT, AbstractAirtightTankBlockEntity.configuredMaxLength()));
        }
        multiblock.requestCapabilityRefresh();
        return clientPacket && (!Objects.equals(previousControllerPos, multiblock.getControllerPos()) || previousWidth != multiblock.getWidth() || previousHeight != multiblock.getHeight());
    }

    private static @Nullable BlockPos readOptionalBlockPos(CompoundTag compoundTag, String key) {
        if (!CCBNbtUtils.contains(compoundTag, key)) {
            return null;
        }
        return NBTHelper.readBlockPos(compoundTag, key);
    }

    private static int readDimension(CompoundTag compoundTag, String key, int maxDimension) {
        return Mth.clamp(CCBNbtUtils.getIntOrDefault(compoundTag, key, 1), 1, maxDimension);
    }
}
