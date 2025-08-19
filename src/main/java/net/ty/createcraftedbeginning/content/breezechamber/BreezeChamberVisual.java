package net.ty.createcraftedbeginning.content.breezechamber;

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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlock.FrostLevel;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class BreezeChamberVisual extends AbstractBlockEntityVisual<BreezeChamberBlockEntity> implements SimpleDynamicVisual, SimpleTickableVisual {
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

    public BreezeChamberVisual(VisualizationContext ctx, BreezeChamberBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);

        frostLevel = FrostLevel.RIMING;
        validBlockAbove = blockEntity.isValidBlockAbove();

        PartialModel breezeModel = BreezeChamberRenderer.getBreezeModel(frostLevel, validBlockAbove);
        isRiming = !blockEntity.getFrostLevel().isAtLeast(FrostLevel.WANING);

        head = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(breezeModel)).createInstance();
        head.light(LightTexture.FULL_BRIGHT);

        animate(partialTick);
    }

    @Override
    public void tick(TickableVisual.Context context) {
        blockEntity.tickAnimation();
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        if (!isVisible(ctx.frustum()) || doDistanceLimitThisFrame(ctx)) {
            return;
        }

        animate(ctx.partialTick());
    }

    private void animate(float partialTicks) {
        float animation = blockEntity.headAnimation.getValue(partialTicks) * .175f;

        boolean validBlockAbove = animation > 0.125f;
        FrostLevel frostLevel = blockEntity.getFrostLevelForRender();

        if (validBlockAbove != this.validBlockAbove || frostLevel != this.frostLevel) {
            this.validBlockAbove = validBlockAbove;

            PartialModel breezeModel = BreezeChamberRenderer.getBreezeModel(frostLevel, validBlockAbove);
            instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(breezeModel)).stealInstance(head);

            this.frostLevel = frostLevel;
        }

        boolean hasGoggles = blockEntity.goggles;
        if (hasGoggles && goggles == null) {
            goggles = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(isRiming ? CCBPartialModels.BREEZE_GOGGLES_SMALL : CCBPartialModels.BREEZE_GOGGLES)).createInstance();
            goggles.light(LightTexture.FULL_BRIGHT);
        } else if (!hasGoggles && goggles != null) {
            goggles.delete();
            goggles = null;
        }

        boolean hatPresent = blockEntity.hat;
        if (hatPresent && hat == null) {
            hat = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.BREEZE_TRAIN_HAT)).createInstance();
            hat.light(LightTexture.FULL_BRIGHT);
        } else if (!hatPresent && hat != null) {
            hat.delete();
            hat = null;
        }

        boolean hasWind = blockEntity.wind;
        if (hasWind && wind == null) {
            wind = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.BREEZE_WIND)).createInstance();
            wind.light(LightTexture.FULL_BRIGHT);
        } else if (!hasWind && wind != null) {
            wind.delete();
            wind = null;
        }

        var hashCode = blockEntity.hashCode();
        float time = AnimationTickHolder.getRenderTime(level);
        float renderTick = time + (hashCode % 13) * 16f;
        float offsetMultiplier = frostLevel.isAtLeast(FrostLevel.WANING) ? 64 : 16;
        float offset = Mth.sin((float) ((renderTick / 16f) % (2 * Math.PI))) / offsetMultiplier;
        float headY = offset - (animation * .75f);
        float horizontalAngle = AngleHelper.rad(blockEntity.headAngle.getValue(partialTicks));

        head.setIdentityTransform().translate(getVisualPosition()).translateY(headY).translate(Translate.CENTER).rotateY(horizontalAngle).translateBack(Translate.CENTER).setChanged();

        if (goggles != null) {
            goggles.setIdentityTransform().translate(getVisualPosition()).translateY(headY + 8 / 16f).translate(Translate.CENTER).rotateY(horizontalAngle).translateBack(Translate.CENTER).setChanged();
        }

        if (hat != null) {
            hat.setIdentityTransform().translate(getVisualPosition()).translateY(headY).translateY(0.75f);
            hat.rotateCentered(horizontalAngle + Mth.PI, Direction.UP).translate(0.5f, 0, 0.5f).light(LightTexture.FULL_BRIGHT);
            hat.setChanged();
        }

        if (wind != null) {
            float rotationSpeed = blockEntity.windRotationSpeed;
            float windRotation = (AnimationTickHolder.getRenderTime(level) * rotationSpeed) % 360;
            float totalRotation = horizontalAngle + AngleHelper.rad(windRotation);

            wind.setIdentityTransform().translate(getVisualPosition()).translateY(headY).translate(Translate.CENTER).rotateY(totalRotation).translateBack(Translate.CENTER).setChanged();
        }
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
