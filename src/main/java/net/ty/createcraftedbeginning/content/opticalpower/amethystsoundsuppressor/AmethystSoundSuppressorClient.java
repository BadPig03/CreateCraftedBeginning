package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.ponder.api.PonderPalette;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.particles.AmethystSoundSuppressorRippleParticleType.RippleOptions;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.Projection;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystSoundSuppressorClient {
    private static final int PARTICLE_INTERVAL_TICKS = 8;
    private static final int GROWTH_PARTICLE_COUNT = 6;
    private static final int RIPPLE_PARTICLE_COUNT = 10;
    private static final int RIPPLE_PARTICLE_DELAY_TICKS = 5;
    private static final double OUTLINE_VIEW_DISTANCE_SQR = 9216;

    private AmethystSoundSuppressorClient() {
    }

    public static void initialize() {
        AmethystSoundSuppressorBlockEntity.setClientTicker(AmethystSoundSuppressorClient::tick);
    }

    @Internal
    public static void showGrowthParticles(Level level, Vec3 crystalCenter, Vec3 suppressorTip) {
        if (!CCBConfig.client().particles.showAmethystSoundSuppressorParticles.get()) {
            return;
        }

        for (int index = 0; index < GROWTH_PARTICLE_COUNT; index++) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, crystalCenter.x + (level.random.nextDouble() - 0.5) * 0.7, crystalCenter.y + (level.random.nextDouble() - 0.5) * 0.7, crystalCenter.z + (level.random.nextDouble() - 0.5) * 0.7, 0, 0, 0);
        }
        for (int index = 0; index < RIPPLE_PARTICLE_COUNT; index++) {
            level.addParticle(new RippleOptions(index * RIPPLE_PARTICLE_DELAY_TICKS), suppressorTip.x, suppressorTip.y, suppressorTip.z, 0, 0, 0);
        }
    }

    static void tick(MovementContext context) {
        if (context.disabled || context.position == null || !AmethystSoundSuppressorSerialization.readShowOutline(context.blockEntityData, true)) {
            return;
        }

        LocalPlayer player = getOutlineViewer();
        if (player == null) {
            return;
        }

        showOutline(context, player, context.world, SubLevelBridge.resolve(context.world, context.position), AmethystSoundSuppressorBlockEntity.BASE_WORKING_RANGE);
    }

    private static void tick(AmethystSoundSuppressorBlockEntity suppressor) {
        Level suppressorLevel = suppressor.getLevel();
        if (suppressorLevel == null) {
            return;
        }

        BlockPos pos = suppressor.getBlockPos();
        boolean illuminated = suppressor.isIlluminated();
        if (illuminated && CCBConfig.client().particles.showAmethystSoundSuppressorParticles.get() && (suppressorLevel.getGameTime() + pos.asLong()) % PARTICLE_INTERVAL_TICKS == 0) {
            Vec3 tip = SubLevelBridge.resolve(suppressorLevel, Vec3.atLowerCornerOf(pos).add(0.5, 1.1, 0.5)).worldPosition();
            suppressorLevel.addParticle(ParticleTypes.END_ROD, tip.x, tip.y, tip.z, 0, 0.01, 0);
        }
        if (!suppressor.isShowingOutline()) {
            return;
        }

        LocalPlayer player = getOutlineViewer();
        if (player == null) {
            return;
        }

        AmethystSoundSuppressorIdentity identity = new AmethystSoundSuppressorIdentity(pos, null);
        short range = ClientAmethystSoundSuppressorCache.INSTANCE.getRange(identity, suppressorLevel.dimension().location());
        showOutline(suppressor, player, suppressorLevel, SubLevelBridge.resolve(suppressorLevel, pos), range);
    }

    private static AABB calculateArea(Level level, BlockPos pos, short range) {
        int radius = range - 1;
        int minX = (pos.getX() >> 4) - radius << 4;
        int minZ = (pos.getZ() >> 4) - radius << 4;
        int width = (radius * 2 + 1) * 16;
        return new AABB(minX, level.getMinBuildHeight(), minZ, minX + width, level.getMaxBuildHeight(), minZ + width);
    }

    private static @Nullable LocalPlayer getOutlineViewer() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !GogglesItem.isWearingGoggles(player) || !CCBConfig.client().outlines.showAmethystSoundSuppressorChunks.get()) {
            return null;
        }

        return player;
    }

    private static void showOutline(Object owner, LocalPlayer player, Level level, Projection projection, short range) {
        Vec3 projectedCenter = projection.worldPosition();
        if (player.distanceToSqr(projectedCenter.x, projectedCenter.y, projectedCenter.z) > OUTLINE_VIEW_DISTANCE_SQR) {
            return;
        }

        BlockPos pos = projection.blockPos();
        Outliner.getInstance().chaseAABB(owner, calculateArea(level, pos, range)).colored(PonderPalette.INPUT.getColor()).withFaceTexture(AllSpecialTextures.CHECKERED).lineWidth(0.0625F);
    }
}
