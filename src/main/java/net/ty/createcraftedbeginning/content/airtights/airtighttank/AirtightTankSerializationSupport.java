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

    public static void writeMultiblock(AbstractAirtightTankBlockEntity tank, CompoundTag tag, boolean clientPacket) {
        AirtightTankMultiblockController multiblock = tank.multiblockController();
        if (tank.isController()) {
            tag.putInt(WIDTH, multiblock.getWidth());
            tag.putInt(HEIGHT, multiblock.getHeight());
        }
        else {
            BlockPos controllerPos = Objects.requireNonNull(multiblock.getControllerPos());
            tag.put(CONTROLLER_POS, NbtUtils.writeBlockPos(controllerPos));
        }

        if (clientPacket) {
            return;
        }

        tag.putBoolean(UPDATE_CONNECTIVITY, multiblock.isUpdateConnectivity());
        BlockPos lastKnownPos = multiblock.getLastKnownPos();
        if (lastKnownPos == null) {
            return;
        }

        tag.put(LAST_KNOWN_POS, NbtUtils.writeBlockPos(lastKnownPos));
    }

    public static void writeSafeMultiblock(AbstractAirtightTankBlockEntity tank, CompoundTag tag) {
        if (!tank.isController()) {
            return;
        }
        tag.putInt(WIDTH, tank.getWidth());
        tag.putInt(HEIGHT, tank.getHeight());
    }

    public static boolean readMultiblock(AbstractAirtightTankBlockEntity tank, CompoundTag tag, boolean clientPacket) {
        AirtightTankMultiblockController multiblock = tank.multiblockController();
        BlockPos previousController = multiblock.getControllerPos();
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

        return clientPacket && (!Objects.equals(previousController, multiblock.getControllerPos()) || previousWidth != multiblock.getWidth() || previousHeight != multiblock.getHeight());
    }

    private static @Nullable BlockPos readOptionalBlockPos(CompoundTag tag, String key) {
        return tag.contains(key) ? NBTHelper.readBlockPos(tag, key) : null;
    }

    private static int readDimension(CompoundTag tag, String key, int maxValue) {
        return tag.contains(key) ? Mth.clamp(tag.getInt(key), 1, maxValue) : 1;
    }
}
