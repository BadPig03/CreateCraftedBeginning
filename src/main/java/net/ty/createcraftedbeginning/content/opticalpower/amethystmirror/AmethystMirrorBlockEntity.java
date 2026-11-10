package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.compat.sable.AmethystMirrorCollisionWorld;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystMirrorBlockEntity extends SmartBlockEntity implements TransformableBlockEntity, IHaveGoggleInformation {
    private Quaternionf rotation;
    private @Nullable Quaternionf previewRotation;
    private final Quaternionf geometryRotation;
    private AmethystMirrorOptics optics;
    private final Quaternionf collisionRotation;
    private AmethystMirrorCollisionShape collisionShape;

    public AmethystMirrorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        rotation = state.getValue(AmethystMirrorBlock.FACING).getRotation();
        geometryRotation = new Quaternionf(rotation);
        optics = new AmethystMirrorOptics(AmethystMirrorGeometry.create(rotation));
        collisionRotation = new Quaternionf(rotation);
        collisionShape = new AmethystMirrorCollisionShape(optics.geometry());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        AmethystMirrorCollisionWorld.update(this);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        AmethystMirrorCollisionWorld.update(this);
    }

    @Override
    public void notifyUpdate() {
        super.notifyUpdate();
        AmethystMirrorCollisionWorld.update(this);
    }

    @Override
    public void transform(BlockEntity be, StructureTransform transform) {
        if (level == null) {
            return;
        }

        rotation = AmethystMirrorSerialization.transformRotation(getBlockState().getValue(AmethystMirrorBlock.FACING), rotation, transform);
        notifyUpdate();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(0.25);
    }

    @Override
    public void writeSafe(CompoundTag compoundTag, Provider provider) {
        super.writeSafe(compoundTag, provider);
        AmethystMirrorSerialization.writeRotation(getBlockState().getValue(AmethystMirrorBlock.FACING), rotation, compoundTag);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        AmethystMirrorDisplay.appendGoggleTooltip(tooltip, getRotation());
        return true;
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        AmethystMirrorSerialization.writeRotation(getBlockState().getValue(AmethystMirrorBlock.FACING), rotation, compoundTag);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        rotation = AmethystMirrorSerialization.readStoredRotation(getBlockState().getValue(AmethystMirrorBlock.FACING), compoundTag);
        AmethystMirrorCollisionWorld.update(this);
    }

    @Internal
    public AmethystMirrorCollisionShape getPhysicalShape() {
        if (collisionRotation.equals(rotation)) {
            return collisionShape;
        }

        collisionRotation.set(rotation);
        collisionShape = new AmethystMirrorCollisionShape(AmethystMirrorGeometry.create(rotation));
        return collisionShape;
    }

    Quaternionf getRotation() {
        if (previewRotation != null) {
            return new Quaternionf(previewRotation);
        }

        return new Quaternionf(rotation);
    }

    void rotateLocal(boolean horizontalAxis, double degrees) {
        if (horizontalAxis) {
            rotation.rotateX((float) Math.toRadians(degrees));
        }
        else {
            rotation.rotateY((float) Math.toRadians(degrees));
        }
        rotation.normalize();
        notifyUpdate();
    }

    void applyAim(Quaternionf candidate) {
        rotation = new Quaternionf(candidate).normalize();
        notifyUpdate();
    }

    AmethystMirrorOptics getMirrorOptics() {
        Quaternionf current = rotation;
        if (previewRotation != null) {
            current = previewRotation;
        }
        if (geometryRotation.equals(current)) {
            return optics;
        }

        geometryRotation.set(current);
        optics = new AmethystMirrorOptics(AmethystMirrorGeometry.create(current));
        return optics;
    }

    Quaternionf getStoredRotation() {
        return new Quaternionf(rotation);
    }

    void setPreviewRotation(@Nullable Quaternionf preview) {
        previewRotation = preview;
    }
}
