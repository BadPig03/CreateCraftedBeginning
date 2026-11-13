package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapRepaint.Plan;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapRepaint.Region;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapSnapshot.Column;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
public final class ChunkStabilizerMapTexture implements AutoCloseable {
    private static final int WIDTH = ChunkStabilizerMapSnapshot.TEXTURE_WIDTH;
    private static final int HEIGHT = ChunkStabilizerMapSnapshot.TEXTURE_HEIGHT;
    private static final long RETRY_DELAY_NS = 1000000000L;
    private static final ResourceLocation UNKNOWN_MAP = CCBAPI.asResource("textures/gui/chunk_stabilizer_map_unknown.png");
    private static int paletteRevision;
    private int renderedPaletteRevision;
    @Nullable
    private List<Column> failedColumns;
    private int failedPaletteRevision;
    private long retryAtNs;
    private boolean terrainUnavailable;
    @Nullable
    private ChunkStabilizerMapSnapshot rendered;
    @Nullable
    private DynamicTexture texture;
    @Nullable
    private ResourceLocation location;

    @SubscribeEvent
    private static void onReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> paletteRevision++);
    }

    @Override
    public void close() {
        releaseTexture(Minecraft.getInstance().getTextureManager());
        failedColumns = null;
        terrainUnavailable = false;
    }

    public void render(GuiGraphics graphics, ChunkStabilizerMapSnapshot snapshot, Vec3 point, Vec3 camera, int x, int y, boolean running) {
        GlobalPos position = snapshot.position();
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (position == null || level == null || !ensureTerrain(minecraft.getTextureManager(), level, snapshot)) {
            renderUnknown(graphics, x, y);
            return;
        }

        ResourceLocation terrainLocation = location;
        if (terrainLocation == null) {
            renderUnknown(graphics, x, y);
            return;
        }

        BlockPos center = position.pos();
        BlockPos origin = ChunkStabilizerMapSnapshot.textureOrigin(center);
        int startX = origin.getX();
        int startZ = origin.getZ();
        PoseStack pose = graphics.pose();
        graphics.enableScissor(x, y, x + ChunkStabilizerMapSnapshot.WIDTH, y + ChunkStabilizerMapSnapshot.HEIGHT);
        try {
            pose.pushPose();
            try {
                pose.translate((float) (x + startX - camera.x), (float) (y + startZ - camera.z), 0);
                graphics.blit(terrainLocation, 0, 0, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
                for (int column = Math.floorMod(-startX, 16); column < WIDTH; column += 16) {
                    graphics.fill(column, 0, column + 1, HEIGHT, 0x50000000);
                }
                for (int row = Math.floorMod(-startZ, 16); row < HEIGHT; row += 16) {
                    graphics.fill(0, row, WIDTH, row + 1, 0x50000000);
                }
                graphics.fill(64, 64, 80, 80, running ? 0x557F4DAD : 0x33696969);
                graphics.renderOutline(64, 64, 16, 16, running ? 0xFFD4ABFF : 0xFFAAAAAA);
                pose.translate((float) (point.x - startX - 0.5), (float) (point.z - startZ - 0.5), 0);
                graphics.fill(-1, -1, 2, 2, 0xFFFFFFFF);
            }
            finally {
                pose.popPose();
            }
        }
        finally {
            graphics.disableScissor();
        }
    }

    public void renderUnknown(GuiGraphics graphics, int x, int y) {
        rendered = null;
        int width = ChunkStabilizerMapSnapshot.WIDTH;
        int height = ChunkStabilizerMapSnapshot.HEIGHT;
        graphics.blit(UNKNOWN_MAP, x, y, 0, 0, width, height, width, height);
    }

    private boolean ensureTerrain(TextureManager manager, ClientLevel level, ChunkStabilizerMapSnapshot snapshot) {
        Registry<Biome> biomes = level.registryAccess().registry(Registries.BIOME).orElse(null);
        if (biomes == null) {
            if (!terrainUnavailable) {
                CCBAPI.LOGGER.warn("Cannot draw the chunk stabilizer map in dimension '{}': the biome registry is unavailable.", level.dimension().location());
                terrainUnavailable = true;
            }
            failedColumns = null;
            releaseTexture(manager);
            return false;
        }

        if (rendered != null && rendered.columns() == snapshot.columns() && renderedPaletteRevision == paletteRevision) {
            return true;
        }

        if (failedColumns == snapshot.columns() && failedPaletteRevision == paletteRevision && System.nanoTime() - retryAtNs < 0) {
            return false;
        }

        try {
            repaint(manager, snapshot, biomes);
        }
        catch (RuntimeException failure) {
            if (!terrainUnavailable) {
                CCBAPI.LOGGER.warn("Failed to draw the chunk stabilizer map for loader '{}' in dimension '{}'; showing unknown terrain.", snapshot.identity(), level.dimension().location(), failure);
                terrainUnavailable = true;
            }
            failedColumns = snapshot.columns();
            failedPaletteRevision = paletteRevision;
            retryAtNs = System.nanoTime() + RETRY_DELAY_NS;
            releaseTexture(manager);
            return false;
        }

        rendered = snapshot;
        renderedPaletteRevision = paletteRevision;
        failedColumns = null;
        terrainUnavailable = false;
        return true;
    }

    private void repaint(TextureManager manager, ChunkStabilizerMapSnapshot snapshot, Registry<Biome> biomes) {
        Plan plan = ChunkStabilizerMapRepaint.plan(rendered, snapshot, renderedPaletteRevision != paletteRevision);
        DynamicTexture current = texture;
        NativeImage previous = current == null ? null : current.getPixels();
        if (!plan.reusable() || current == null || previous == null) {
            updateTexture(manager, ChunkStabilizerMapRaster.createImage(snapshot, biomes));
            return;
        }

        int dx = plan.dx();
        int dz = plan.dz();
        List<Region> regions = plan.regions();
        if (dx != 0 || dz != 0) {
            NativeImage image = new NativeImage(WIDTH, HEIGHT, false);
            try {
                for (int row = 0; row < HEIGHT; row++) {
                    for (int column = 0; column < WIDTH; column++) {
                        int oldX = column + dx;
                        int oldZ = row + dz;
                        if (oldX < 0 || oldX >= WIDTH || oldZ < 0 || oldZ >= HEIGHT) {
                            continue;
                        }

                        image.setPixelRGBA(column, row, previous.getPixelRGBA(oldX, oldZ));
                    }
                }
                ChunkStabilizerMapRaster.paint(image, snapshot, biomes, regions);
            }
            catch (RuntimeException | Error failure) {
                image.close();
                throw failure;
            }

            updateTexture(manager, image);
            return;
        }

        ChunkStabilizerMapRaster.paint(previous, snapshot, biomes, regions);
        current.bind();
        for (Region region : regions) {
            int regionX = region.x();
            int regionZ = region.z();
            previous.upload(0, regionX, regionZ, regionX, regionZ, region.width(), region.height(), false, false);
        }
    }

    private void updateTexture(TextureManager manager, NativeImage image) {
        DynamicTexture current = texture;
        if (current != null) {
            current.setPixels(image);
            current.upload();
            return;
        }

        DynamicTexture pending = null;
        try {
            pending = new DynamicTexture(image);
            location = manager.register("chunk_stabilizer_map", pending);
            texture = pending;
        }
        catch (RuntimeException | Error failure) {
            if (pending == null) {
                image.close();
            }
            else {
                pending.close();
            }
            throw failure;
        }
    }

    private void releaseTexture(TextureManager manager) {
        ResourceLocation released = location;
        location = null;
        texture = null;
        rendered = null;
        if (released == null) {
            return;
        }

        manager.release(released);
    }
}
