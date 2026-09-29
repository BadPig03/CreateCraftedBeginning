package net.ty.createcraftedbeginning.gametests.client;

import com.mojang.logging.LogUtils;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DistanceUpdateLimiter;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualEmbedding;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentVisual;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirVentVisualClientTests {
    private AirVentVisualClientTests() {
    }

    public static void run() {
        verifyUpdates(BlockPos.ZERO, BlockPos.ZERO, true, false);
        verifyUpdates(new BlockPos(30000000, 64, 30000000), BlockPos.ZERO, true, true);
        BlockPos plot = new BlockPos(30000000, 64, 30000000);
        verifyUpdates(plot, plot, false, true);
        LogUtils.getLogger().info("AIR_VENT_VISUAL_TESTS_PASSED: local, offscreen and embedded louver updates");
    }

    private static void verifyUpdates(BlockPos pos, BlockPos origin, boolean allowDistanceUpdate, boolean expectCulled) {
        AirVentBlockEntity vent = new AirVentBlockEntity(CCBBlockEntities.AIR_VENT.get(), pos, CCBBlocks.AIR_VENT_BLOCK.getDefaultState());
        RecordingProvider instances = new RecordingProvider();
        TestVisual visual = new TestVisual(new TestContext(instances, origin), vent);
        Frame frame = new Frame(new Camera(), new FrustumIntersection(new Matrix4f()), 0, distance -> allowDistanceUpdate);
        boolean culled = !visual.isVisible(frame.frustum()) || visual.doDistanceLimitThisFrame(frame);
        if (culled != expectCulled) {
            throw new AssertionError("Unexpected visual culling fixture at " + pos + '.');
        }

        Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        CompoundTag packet = new CompoundTag();
        int face = 1 << Direction.NORTH.get3DDataValue();
        Model closed = Models.partial(CCBPartialModels.AIR_VENT_CLOSED);
        Model opened = Models.partial(CCBPartialModels.AIR_VENT_OPENED);
        try {
            packet.putInt("LouverMask", face);
            vent.readClient(packet, registries);
            visual.beginFrame(frame);
            instances.assertModel(closed, 2, "install louver at " + pos);
            packet.putInt("OpenedMask", face);
            vent.readClient(packet, registries);
            visual.beginFrame(frame);
            instances.assertModel(opened, 2, "open louver at " + pos);
            packet.putInt("OpenedMask", 0);
            vent.readClient(packet, registries);
            visual.beginFrame(frame);
            instances.assertModel(closed, 2, "close louver at " + pos);
            int changes = instances.changes;
            visual.beginFrame(frame);
            if (instances.handles.size() != 2 || instances.changes != changes) {
                throw new AssertionError("Unchanged louver must reuse its instances at " + pos + '.');
            }

            packet.putInt("LouverMask", 0);
            vent.readClient(packet, registries);
            visual.beginFrame(frame);
            instances.assertModel(closed, 0, "remove louver at " + pos);
        }
        finally {
            visual.delete();
        }
    }

    private static final class TestVisual extends AirVentVisual {
        private TestVisual(VisualizationContext context, AirVentBlockEntity vent) {
            super(context, vent, 0);
        }

        @Override
        protected void relight(@Nullable FlatLit... instances) {
        }
    }

    private record TestContext(InstancerProvider instancerProvider, Vec3i renderOrigin) implements VisualizationContext {
        @Override
        public VisualEmbedding createEmbedding(Vec3i origin) {
            throw new UnsupportedOperationException("Unexpected nested embedding in the louver visual test.");
        }
    }

    private record Frame(Camera camera, FrustumIntersection frustum, float partialTick, DistanceUpdateLimiter limiter) implements Context {}

    private static final class RecordingProvider implements InstancerProvider {
        private final List<RecordingHandle> handles = new ArrayList<>();
        private int changes;

        @Override
        public <I extends Instance> Instancer<I> instancer(InstanceType<I> type, Model model, int bias) {
            return new Instancer<>() {
                @Override
                public I createInstance() {
                    RecordingHandle handle = new RecordingHandle(model, RecordingProvider.this);
                    handles.add(handle);
                    return type.create(handle);
                }

                @Override
                public void stealInstance(@Nullable I instance) {
                    if (instance == null) {
                        throw new NullPointerException("Expected an existing louver instance when changing its model.");
                    }

                    RecordingHandle handle = (RecordingHandle) instance.handle();
                    handle.model = model;
                    changes++;
                }
            };
        }

        private void assertModel(Model expected, int count, String stage) {
            int active = 0;
            for (RecordingHandle handle : handles) {
                if (handle.deleted) {
                    continue;
                }

                if (handle.model != expected) {
                    throw new AssertionError("Unexpected louver model: " + stage + '.');
                }

                active++;
            }
            if (active != count) {
                throw new AssertionError("Expected " + count + " louver instances, found " + active + ": " + stage + '.');
            }
        }
    }

    private static final class RecordingHandle implements InstanceHandle {
        private final RecordingProvider owner;
        private Model model;
        private boolean deleted;
        private boolean visible = true;

        private RecordingHandle(Model model, RecordingProvider owner) {
            this.model = model;
            this.owner = owner;
        }

        @Override
        public void setChanged() {
            owner.changes++;
        }

        @Override
        public void setDeleted() {
            deleted = true;
            owner.changes++;
        }

        @Override
        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        @Override
        public boolean isVisible() {
            return visible;
        }
    }
}
