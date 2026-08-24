package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Mth;
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
            compoundTag.putInt(WIDTH, multiblock.getWidth());
            compoundTag.putInt(HEIGHT, multiblock.getHeight());
        }
        else {
            BlockPos controllerPos = Objects.requireNonNull(multiblock.getControllerPos());
            compoundTag.put(CONTROLLER_POS, NbtUtils.writeBlockPos(controllerPos));
        }

        if (clientPacket) {
            return;
        }

        compoundTag.putBoolean(UPDATE_CONNECTIVITY, multiblock.isUpdateConnectivity());
        BlockPos lastKnownPos = multiblock.getLastKnownPos();
        if (lastKnownPos == null) {
            return;
        }

        compoundTag.put(LAST_KNOWN_POS, NbtUtils.writeBlockPos(lastKnownPos));
    }

    public static void writeSafeMultiblock(AbstractAirtightTankBlockEntity tank, CompoundTag compoundTag) {
        if (!tank.isController()) {
            return;
        }

        compoundTag.putInt(WIDTH, tank.getWidth());
        compoundTag.putInt(HEIGHT, tank.getHeight());
    }

    public static boolean readMultiblock(AbstractAirtightTankBlockEntity tank, CompoundTag tag, boolean clientPacket) {
        AirtightTankMultiblockController multiblock = tank.multiblockController();
        BlockPos previousControllerPos = multiblock.getControllerPos();
        int previousWidth = multiblock.getWidth();
        int previousHeight = multiblock.getHeight();
        if (!clientPacket) {
            multiblock.setUpdateConnectivity(tag.getBoolean(UPDATE_CONNECTIVITY));
            multiblock.setLastKnownPos(readOptionalBlockPos(tag, LAST_KNOWN_POS));
        }
        multiblock.setControllerPos(readOptionalBlockPos(tag, CONTROLLER_POS));
        if (tank.isController()) {
            multiblock.setWidth(readDimension(tag, WIDTH, AbstractAirtightTankBlockEntity.configuredMaxWidth()));
            multiblock.setHeight(readDimension(tag, HEIGHT, AbstractAirtightTankBlockEntity.configuredMaxLength()));
        }
        multiblock.requestCapabilityRefresh();
        return clientPacket && (!Objects.equals(previousControllerPos, multiblock.getControllerPos()) || previousWidth != multiblock.getWidth() || previousHeight != multiblock.getHeight());
    }

    private static @Nullable BlockPos readOptionalBlockPos(CompoundTag compoundTag, String key) {
        if (!compoundTag.contains(key)) {
            return null;
        }
        return NBTHelper.readBlockPos(compoundTag, key);
    }

    private static int readDimension(CompoundTag compoundTag, String key, int maxDimension) {
        if (!compoundTag.contains(key)) {
            return 1;
        }
        return Mth.clamp(compoundTag.getInt(key), 1, maxDimension);
    }
}
