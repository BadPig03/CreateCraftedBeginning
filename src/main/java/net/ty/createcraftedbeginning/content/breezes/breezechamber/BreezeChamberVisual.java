package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.Translate;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.client.BreezeChamberClientAnimation;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BreezeChamberVisual extends AbstractBlockEntityVisual<BreezeChamberBlockEntity> implements SimpleDynamicVisual, SimpleTickableVisual {
    private final TransformedInstance head;
    private WindLevel windLevel;
    @Nullable
    private TransformedInstance goggles;
    @Nullable
    private PartialModel gogglesModel;
    @Nullable
    private TransformedInstance hat;
    @Nullable
    private TransformedInstance wind;

    private boolean controllerActive;

    public BreezeChamberVisual(VisualizationContext ctx, BreezeChamberBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);
        windLevel = WindLevel.CALM;
        controllerActive = blockEntity.isControllerActive();
        head = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(BreezeChamberRenderer.getBreezeModel(windLevel, controllerActive))).createInstance();
        animate(partialTick);
    }

    @Override
    public void tick(TickableVisual.Context context) {
        BreezeChamberClientAnimation.tickAnimation(blockEntity);
    }

    @Override
    public void beginFrame(DynamicVisual.@NotNull Context context) {
        if (!isVisible(context.frustum()) || doDistanceLimitThisFrame(context)) {
            return;
        }

        animate(context.partialTick());
    }

    @Override
    public void updateLight(float partialTick) {
        relight(head, goggles, hat, wind);
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
    }

    @Override
    protected void _delete() {
        head.delete();
        if (goggles != null) {
            goggles.delete();
        }
        if (hat != null) {
            hat.delete();
        }
        if (wind == null) {
            return;
        }

        wind.delete();
    }

    private void animate(float partialTicks) {
        float headAnimation = blockEntity.getHeadAnimation().getValue(partialTicks) * 0.175f;
        boolean shouldUseActiveModel = headAnimation > 0.125f;
        WindLevel currentWindLevel = blockEntity.getWindLevelForRender();
        if (shouldUseActiveModel != controllerActive || currentWindLevel != windLevel) {
            controllerActive = shouldUseActiveModel;
            instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(BreezeChamberRenderer.getBreezeModel(currentWindLevel, shouldUseActiveModel))).stealInstance(head);
            windLevel = currentWindLevel;
        }

        PartialModel desiredGogglesModel = currentWindLevel.isActive() ? CCBPartialModels.BREEZE_CHAMBER_GOGGLES : CCBPartialModels.BREEZE_CHAMBER_GOGGLES_SMALL;
        boolean hasGoggles = blockEntity.hasGoggles();
        if (hasGoggles && goggles == null) {
            goggles = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(desiredGogglesModel)).createInstance();
            gogglesModel = desiredGogglesModel;
        }
        else if (hasGoggles && gogglesModel != desiredGogglesModel) {
            instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(desiredGogglesModel)).stealInstance(goggles);
            gogglesModel = desiredGogglesModel;
        }
        else if (!hasGoggles && goggles != null) {
            goggles.delete();
            goggles = null;
            gogglesModel = null;
        }

        boolean hasHat = blockEntity.hasTrainHat();
        if (hasHat && hat == null) {
            hat = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.BREEZE_TRAIN_HAT)).createInstance();
        }
        else if (!hasHat && hat != null) {
            hat.delete();
            hat = null;
        }

        boolean hasWind = blockEntity.getWindLevel().isActive();
        if (hasWind && wind == null) {
            wind = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.BREEZE_CHAMBER_WIND)).createInstance();
        }
        else if (!hasWind && wind != null) {
            wind.delete();
            wind = null;
        }

        float renderTime = AnimationTickHolder.getRenderTime(level);
        float headY = Mth.sin((renderTime + blockEntity.hashCode() % 13 * 16) / 16 % (2 * Mth.PI)) / (currentWindLevel.isActive() ? 64 : 16) - headAnimation * 0.75f;
        float horizontalAngle = AngleHelper.rad(blockEntity.getHeadAngle().getValue(partialTicks));
        head.setIdentityTransform().translate(getVisualPosition()).translateY(headY).translate(Translate.CENTER).rotateY(horizontalAngle).translateBack(Translate.CENTER).setChanged();
        if (goggles != null) {
            goggles.setIdentityTransform().translate(getVisualPosition()).translateY(headY + 0.5f).translate(Translate.CENTER).rotateY(horizontalAngle).translateBack(Translate.CENTER).setChanged();
        }
        if (hat != null) {
            hat.setIdentityTransform().translate(getVisualPosition()).translateY(headY).translateY(0.75f).rotateCentered(horizontalAngle + Mth.PI, Direction.UP).translate(0.5f, 0, 0.5f).setChanged();
        }
        if (wind == null) {
            return;
        }

        wind.setIdentityTransform().translate(getVisualPosition()).translateY(headY).translate(Translate.CENTER).rotateY(horizontalAngle + AngleHelper.rad(renderTime * (hasWind ? 24 : 0) % 360)).translateBack(Translate.CENTER).setChanged();
    }
}
