package net.ty.createcraftedbeginning.content.obsolete.phohostressbearing;

import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity.RotationDirection;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PhotoStressBearingBlockEntity extends GeneratingKineticBlockEntity {
    private final DustColorTransitionOptions particleColor = new DustColorTransitionOptions(Vec3.fromRGB24(16761855).toVector3f(), Vec3.fromRGB24(10185983).toVector3f(), 1.0f);
    protected ScrollOptionBehaviour<RotationDirection> movementDirection;
    private int skyLight;
    private int lightTimer;
    private boolean isClockwise;

    public PhotoStressBearingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void initialize() {
        super.initialize();

        updateGeneratedRotation();
    }

    @Override
    protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        if (!clientPacket) {
            compound.putInt("SkyLight", skyLight);
            compound.putBoolean("Clockwise", isClockwise);
        }
    }

    @Override
    protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (clientPacket) {
            return;
        }
        if (compound.contains("SkyLight")) {
            skyLight = compound.getInt("SkyLight");
        }
        if (compound.contains("Clockwise")) {
            isClockwise = compound.getBoolean("Clockwise");
        }
    }

    @Override
    public float getGeneratedSpeed() {
        if (!isInOverworld() || level == null) {
            return 0;
        }
        int speedFactor = level.isRaining() || level.isThundering() ? 1 : 2;
        int speedDirection = isClockwise ? 1 : -1;
        return (skyLight + 1) * speedFactor * speedDirection;
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        movementDirection = new ScrollOptionBehaviour<>(RotationDirection.class, CreateLang.translateDirect("contraptions.windmill.rotation_direction"), this, new PhotoStressBearingValueBox());
        movementDirection.withCallback(this::onDirectionChanged);
        behaviours.add(movementDirection);
    }

    private void onDirectionChanged(int direction) {
        isClockwise = direction != 1;
        updateGeneratedRotation();
        notifyUpdate();
    }

    private boolean isInOverworld() {
        return level != null && level.dimension() == Level.OVERWORLD;
    }

    @Override
    public void tick() {
        super.tick();

        if (!isOverStressed()) {
            updateGeneratedRotation();
        }

        if (level != null && level instanceof PonderLevel) {
            skyLight = getPonderSkyLight(level);
        }

        if (lightTimer > 15 - skyLight) {
            spawnParticle();
            lightTimer = 0;
        }

        updateSkyLight();
        lightTimer++;
    }

    private int getPonderSkyLight(@NotNull Level level) {
        CompoundTag compound = new CompoundTag();
        saveAdditional(compound, level.registryAccess());
        return compound.contains("SkyLight") ? compound.getInt("SkyLight") : 15;
    }

    private void updateSkyLight() {
        int light;
        if (level == null || level instanceof PonderLevel) {
            return;
        }
        light = level.getBrightness(LightLayer.SKY, worldPosition.above());
        skyLight = light;
    }

    private void spawnParticle() {
        if (level == null || !level.isClientSide || speed == 0) {
            return;
        }
        Vec3 centerOf = VecHelper.getCenterOf(worldPosition);
        Vec3 v = VecHelper.offsetRandomly(centerOf, level.random, 0.95f);
        Vec3 m = centerOf.subtract(v);
        level.addParticle(particleColor, v.x, v.y, v.z, m.x, m.y, m.z);
    }
}
