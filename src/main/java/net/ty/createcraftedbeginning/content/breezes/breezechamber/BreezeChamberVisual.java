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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class BreezeChamberVisual extends AbstractBlockEntityVisual<BreezeChamberBlockEntity> implements SimpleDynamicVisual, SimpleTickableVisual {
    private final TransformedInstance head;
    private final boolean isCalm;
    private WindLevel windLevel;
    @Nullable
    private TransformedInstance goggles;
    @Nullable
    private TransformedInstance hat;
    @Nullable
    private TransformedInstance wind;

    private boolean validBlockBelow;

    public BreezeChamberVisual(VisualizationContext ctx, BreezeChamberBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);

        windLevel = WindLevel.CALM;
        validBlockBelow = blockEntity.isValidBlockBelow(false);

        PartialModel breezeModel = BreezeChamberRenderer.getBreezeModel(windLevel, validBlockBelow);
        isCalm = !blockEntity.getWindLevel().isAtLeast(WindLevel.BREEZE);

        head = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(breezeModel)).createInstance();
        head.light(LightTexture.FULL_BRIGHT);

        animate(partialTick);
    }

    private void animate(float partialTicks) {
        float animation = blockEntity.headAnimation.getValue(partialTicks) * 0.175f;

        boolean validBlockBelow = animation > 0.125f;
        WindLevel windLevel = blockEntity.getWindLevelForRender();

        if (validBlockBelow != this.validBlockBelow || windLevel != this.windLevel) {
            this.validBlockBelow = validBlockBelow;

            PartialModel breezeModel = BreezeChamberRenderer.getBreezeModel(windLevel, validBlockBelow);
            instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(breezeModel)).stealInstance(head);

            this.windLevel = windLevel;
        }

        boolean hasGoggles = blockEntity.goggles;
        if (hasGoggles && goggles == null) {
            goggles = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(isCalm ? CCBPartialModels.BREEZE_COOLER_GOGGLES_SMALL : CCBPartialModels.BREEZE_COOLER_GOGGLES)).createInstance();
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
            wind = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.BREEZE_COOLER_WIND)).createInstance();
            wind.light(LightTexture.FULL_BRIGHT);
        } else if (!hasWind && wind != null) {
            wind.delete();
            wind = null;
        }

        var hashCode = blockEntity.hashCode();
        float time = AnimationTickHolder.getRenderTime(level);
        float renderTick = time + (hashCode % 13) * 16f;
        float offsetMultiplier = windLevel.isAtLeast(WindLevel.BREEZE) ? 64 : 16;
        float offset = Mth.sin((float) ((renderTick / 16f) % (2 * Math.PI))) / offsetMultiplier;
        float headY = offset - (animation * 0.75f);
        float horizontalAngle = AngleHelper.rad(blockEntity.headAngle.getValue(partialTicks));

        head.setIdentityTransform().translate(getVisualPosition()).translateY(headY).translate(Translate.CENTER).rotateY(horizontalAngle).translateBack(Translate.CENTER).setChanged();

        if (goggles != null) {
            goggles.setIdentityTransform().translate(getVisualPosition()).translateY(headY + 1 / 2f).translate(Translate.CENTER).rotateY(horizontalAngle).translateBack(Translate.CENTER).setChanged();
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
    public void tick(TickableVisual.Context context) {
        blockEntity.tickAnimation();
    }

    @Override
    public void beginFrame(DynamicVisual.@NotNull Context ctx) {
        if (!isVisible(ctx.frustum()) || doDistanceLimitThisFrame(ctx)) {
            return;
        }

        animate(ctx.partialTick());
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
