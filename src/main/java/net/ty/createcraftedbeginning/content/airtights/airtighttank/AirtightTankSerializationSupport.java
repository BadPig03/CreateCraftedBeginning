package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightTankSerializationSupport {
    @Internal
    public static final String COMPOUND_KEY_TANK_CONTENT = "TankContent";
    private static final String COMPOUND_KEY_UPDATE_CONNECTIVITY = "UpdateConnectivity";
    @Internal
    public static final String COMPOUND_KEY_LAST_KNOWN_POS = "LastKnownPos";
    @Internal
    public static final String COMPOUND_KEY_CONTROLLER_POS = "Controller";
    @Internal
    public static final String COMPOUND_KEY_WIDTH = "Width";
    @Internal
    public static final String COMPOUND_KEY_HEIGHT = "Height";

    private AirtightTankSerializationSupport() {
    }

    public static void writeMultiblock(AbstractAirtightTankBlockEntity tank, CompoundTag compoundTag, boolean clientPacket) {
        AirtightTankMultiblockController multiblock = tank.multiblockController();
        if (tank.isController()) {
            compoundTag.putInt(COMPOUND_KEY_WIDTH, multiblock.getWidth());
            compoundTag.putInt(COMPOUND_KEY_HEIGHT, multiblock.getHeight());
        }
        else {
            BlockPos controllerPos = Objects.requireNonNull(multiblock.getControllerPos(), "Airtight tank controller position must not be null.");
            compoundTag.put(COMPOUND_KEY_CONTROLLER_POS, NbtUtils.writeBlockPos(controllerPos));
        }
        if (clientPacket) {
            return;
        }

        compoundTag.putBoolean(COMPOUND_KEY_UPDATE_CONNECTIVITY, multiblock.isUpdateConnectivity());
        BlockPos lastKnownPos = multiblock.getLastKnownPos();
        if (lastKnownPos == null) {
            return;
        }

        compoundTag.put(COMPOUND_KEY_LAST_KNOWN_POS, NbtUtils.writeBlockPos(lastKnownPos));
    }

    public static void writeSafeMultiblock(AbstractAirtightTankBlockEntity tank, CompoundTag compoundTag) {
        if (!tank.isController()) {
            return;
        }

        compoundTag.putInt(COMPOUND_KEY_WIDTH, tank.getWidth());
        compoundTag.putInt(COMPOUND_KEY_HEIGHT, tank.getHeight());
    }

    public static boolean readMultiblock(AbstractAirtightTankBlockEntity tank, CompoundTag compoundTag, boolean clientPacket) {
        AirtightTankMultiblockController multiblock = tank.multiblockController();
        BlockPos previousControllerPos = multiblock.getControllerPos();
        int previousWidth = multiblock.getWidth();
        int previousHeight = multiblock.getHeight();
        if (!clientPacket) {
            multiblock.setUpdateConnectivity(compoundTag.getBoolean(COMPOUND_KEY_UPDATE_CONNECTIVITY));
            multiblock.setLastKnownPos(readOptionalBlockPos(compoundTag, COMPOUND_KEY_LAST_KNOWN_POS));
        }
        multiblock.setControllerPos(readOptionalBlockPos(compoundTag, COMPOUND_KEY_CONTROLLER_POS));

        if (tank.isController()) {
            multiblock.setWidth(readDimension(compoundTag, COMPOUND_KEY_WIDTH, AbstractAirtightTankBlockEntity.configuredMaxWidth()));
            multiblock.setHeight(readDimension(compoundTag, COMPOUND_KEY_HEIGHT, AbstractAirtightTankBlockEntity.configuredMaxLength()));
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
        return Mth.clamp(NbtValues.getIntOrDefault(compoundTag, key, 1), 1, maxDimension);
    }
}
