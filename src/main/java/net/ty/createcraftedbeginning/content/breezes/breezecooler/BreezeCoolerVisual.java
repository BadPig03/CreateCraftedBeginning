package net.ty.createcraftedbeginning.content.breezes.breezecooler;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.transform.Translate;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class BreezeCoolerVisual extends AbstractBlockEntityVisual<BreezeCoolerBlockEntity> implements SimpleDynamicVisual, SimpleTickableVisual {
    private final TransformedInstance head;
    private final boolean isRiming;
    private FrostLevel frostLevel;
    @Nullable
    private TransformedInstance goggles;
    @Nullable
    private TransformedInstance hat;
    @Nullable
    private TransformedInstance wind;

    private boolean validBlockAbove;

    public BreezeCoolerVisual(VisualizationContext ctx, BreezeCoolerBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);
        frostLevel = FrostLevel.RIMING;
        validBlockAbove = blockEntity.isValidBlockAbove();
        isRiming = !blockEntity.getFrostLevel().isAtLeast(FrostLevel.CHILLED);
        head = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(BreezeCoolerRenderer.getBreezeModel(frostLevel, validBlockAbove))).createInstance();
        head.light(LightTexture.FULL_BRIGHT);
        animate(partialTick);
    }

    private void animate(float partialTicks) {
        float animation = blockEntity.getHeadAnimation().getValue(partialTicks) * 0.175f;
        boolean active = animation > 0.125f;
        FrostLevel currentFrostLevel = blockEntity.getFrostLevelForRender();
        if (active != validBlockAbove || currentFrostLevel != frostLevel) {
            validBlockAbove = active;
            instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(BreezeCoolerRenderer.getBreezeModel(currentFrostLevel, active))).stealInstance(head);
            frostLevel = currentFrostLevel;
        }

        boolean hasGoggles = blockEntity.hasGoggles();
        if (hasGoggles && goggles == null) {
            goggles = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(isRiming ? CCBPartialModels.BREEZE_COOLER_GOGGLES_SMALL : CCBPartialModels.BREEZE_COOLER_GOGGLES)).createInstance();
            goggles.light(LightTexture.FULL_BRIGHT);
        }
        else if (!hasGoggles && goggles != null) {
            goggles.delete();
            goggles = null;
        }

        boolean hatPresent = blockEntity.hasTrainHat();
        if (hatPresent && hat == null) {
            hat = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.BREEZE_TRAIN_HAT)).createInstance();
            hat.light(LightTexture.FULL_BRIGHT);
        }
        else if (!hatPresent && hat != null) {
            hat.delete();
            hat = null;
        }

        boolean hasWind = blockEntity.getFrostLevel().isAtLeast(FrostLevel.CHILLED);
        if (hasWind && wind == null) {
            wind = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.BREEZE_COOLER_WIND)).createInstance();
            wind.light(LightTexture.FULL_BRIGHT);
        }
        else if (!hasWind && wind != null) {
            wind.delete();
            wind = null;
        }

        float renderTime = AnimationTickHolder.getRenderTime(level);
        float headY = Mth.sin((renderTime + (blockEntity.hashCode() % 13) * 16.0f) / 16.0f % Mth.TWO_PI) / (currentFrostLevel.isAtLeast(FrostLevel.CHILLED) ? 64 : 16) - animation * 0.75f;
        float horizontalAngle = AngleHelper.rad(blockEntity.headAngle.getValue(partialTicks));
        head.setIdentityTransform().translate(getVisualPosition()).translateY(headY).translate(Translate.CENTER).rotateY(horizontalAngle).translateBack(Translate.CENTER).setChanged();
        if (goggles != null) {
            goggles.setIdentityTransform().translate(getVisualPosition()).translateY(headY + 0.5f).translate(Translate.CENTER).rotateY(horizontalAngle).translateBack(Translate.CENTER).setChanged();
        }
        if (hat != null) {
            hat.setIdentityTransform().translate(getVisualPosition()).translateY(headY).translateY(0.75f);
            hat.rotateCentered(horizontalAngle + Mth.PI, Direction.UP).translate(0.5f, 0, 0.5f).light(LightTexture.FULL_BRIGHT);
            hat.setChanged();
        }
        if (wind != null) {
            float totalRotation = horizontalAngle + AngleHelper.rad(renderTime * (hasWind ? 24.0f : 0) % 360);
            wind.setIdentityTransform().translate(getVisualPosition()).translateY(headY).translate(Translate.CENTER).rotateY(totalRotation).translateBack(Translate.CENTER).setChanged();
        }
    }

    @Override
    public void tick(TickableVisual.Context context) {
        blockEntity.tickAnimation();
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
        if (wind != null) {
            wind.delete();
        }
    }
}
