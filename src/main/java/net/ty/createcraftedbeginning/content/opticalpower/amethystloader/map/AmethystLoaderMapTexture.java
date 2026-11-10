package net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
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
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapRepaint.Plan;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapRepaint.Region;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
public final class AmethystLoaderMapTexture implements AutoCloseable {
    private static final int WIDTH = AmethystLoaderMapSnapshot.TEXTURE_WIDTH;
    private static final int HEIGHT = AmethystLoaderMapSnapshot.TEXTURE_HEIGHT;
    private static int paletteRevision;
    private int renderedPaletteRevision;
    @Nullable
    private AmethystLoaderMapSnapshot rendered;
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
        if (location != null) {
            Minecraft.getInstance().getTextureManager().release(location);
        }
        location = null;
        texture = null;
        rendered = null;
    }

    public void render(GuiGraphics graphics, AmethystLoaderMapSnapshot snapshot, Vec3 point, Vec3 camera, int x, int y, boolean running) {
        GlobalPos position = snapshot.position();
        Minecraft minecraft = Minecraft.getInstance();
        if (position == null || minecraft.level == null) {
            renderUnknown(graphics, x, y);
            return;
        }

        if (rendered == null || rendered.columns() != snapshot.columns() || renderedPaletteRevision != paletteRevision) {
            Registry<Biome> biomes = minecraft.level.registryAccess().registryOrThrow(Registries.BIOME);
            Plan plan = AmethystLoaderMapRepaint.plan(rendered, snapshot, renderedPaletteRevision != paletteRevision);
            int dx = plan.dx();
            int dz = plan.dz();
            List<Region> regions = plan.regions();
            NativeImage previous = texture == null ? null : texture.getPixels();
            if (!plan.reusable() || previous == null) {
                NativeImage image = AmethystLoaderMapRaster.createImage(snapshot, biomes);
                updateTexture(image);
            }
            else if (dx != 0 || dz != 0) {
                NativeImage image = new NativeImage(WIDTH, HEIGHT, false);
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
                AmethystLoaderMapRaster.paint(image, snapshot, biomes, regions);
                updateTexture(image);
            }
            else {
                AmethystLoaderMapRaster.paint(previous, snapshot, biomes, regions);
                texture.bind();
                for (Region region : regions) {
                    int regionX = region.x();
                    int regionZ = region.z();
                    previous.upload(0, regionX, regionZ, regionX, regionZ, region.width(), region.height(), false, false);
                }
            }
            rendered = snapshot;
            renderedPaletteRevision = paletteRevision;
        }
        if (location == null) {
            return;
        }

        BlockPos center = position.pos();
        BlockPos origin = AmethystLoaderMapSnapshot.textureOrigin(center);
        int startX = origin.getX();
        int startZ = origin.getZ();
        PoseStack pose = graphics.pose();
        graphics.enableScissor(x, y, x + AmethystLoaderMapSnapshot.WIDTH, y + AmethystLoaderMapSnapshot.HEIGHT);
        try {
            pose.pushPose();
            try {
                pose.translate((float) (x + startX - camera.x), (float) (y + startZ - camera.z), 0);
                graphics.blit(location, 0, 0, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
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
        if (texture == null || rendered != null) {
            NativeImage image = new NativeImage(WIDTH, HEIGHT, false);
            for (int row = 0; row < HEIGHT; row++) {
                for (int column = 0; column < WIDTH; column++) {
                    image.setPixelRGBA(column, row, AmethystLoaderMapRaster.unknownColor(column, row));
                }
            }
            updateTexture(image);
            rendered = null;
        }
        if (location == null) {
            return;
        }

        graphics.blit(location, x, y, 0, 0, AmethystLoaderMapSnapshot.WIDTH, AmethystLoaderMapSnapshot.HEIGHT, WIDTH, HEIGHT);
    }

    private void updateTexture(NativeImage image) {
        if (texture == null) {
            texture = new DynamicTexture(image);
            location = Minecraft.getInstance().getTextureManager().register("amethyst_loader_map", texture);
            return;
        }

        texture.setPixels(image);
        texture.upload();
    }
}
