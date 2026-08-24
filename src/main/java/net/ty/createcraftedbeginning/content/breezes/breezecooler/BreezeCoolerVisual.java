package net.ty.createcraftedbeginning.content.breezes.breezecooler;

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
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.client.BreezeCoolerClientAnimation;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BreezeCoolerVisual extends AbstractBlockEntityVisual<BreezeCoolerBlockEntity> implements SimpleDynamicVisual, SimpleTickableVisual {
    protected final TransformedInstance head;
    protected FrostLevel frostLevel;
    @Nullable
    protected TransformedInstance goggles;
    @Nullable
    protected PartialModel gogglesModel;
    @Nullable
    protected TransformedInstance hat;
    @Nullable
    protected PartialModel hatModel;
    @Nullable
    protected TransformedInstance wind;

    protected boolean validBlockAbove;

    public BreezeCoolerVisual(VisualizationContext ctx, BreezeCoolerBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);
        frostLevel = FrostLevel.RIMING;
        validBlockAbove = blockEntity.getBlockState().getValue(BreezeCoolerBlock.ATTACHED);
        head = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(BreezeCoolerRenderer.getBreezeModel(frostLevel, validBlockAbove))).createInstance();
        animate(partialTick);
    }

    protected void animate(float partialTicks) {
        float animation = blockEntity.getHeadAnimation().getValue(partialTicks) * 0.175f;
        boolean isActive = animation > 0.125f;
        FrostLevel currentFrostLevel = blockEntity.getFrostLevelForRender();
        if (isActive != validBlockAbove || currentFrostLevel != frostLevel) {
            validBlockAbove = isActive;
            instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(BreezeCoolerRenderer.getBreezeModel(currentFrostLevel, isActive))).stealInstance(head);
            frostLevel = currentFrostLevel;
        }

        PartialModel desiredGogglesModel = currentFrostLevel.isAtLeast(FrostLevel.CHILLED) ? CCBPartialModels.BREEZE_COOLER_GOGGLES : CCBPartialModels.BREEZE_COOLER_GOGGLES_SMALL;
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

        PartialModel desiredHatModel = blockEntity.hasTrainHat() ? CCBPartialModels.BREEZE_TRAIN_HAT : blockEntity.isStockKeeper() ? CCBPartialModels.BREEZE_LOGISTICS_HAT : null;
        if (desiredHatModel != null && hat == null) {
            hat = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(desiredHatModel)).createInstance();
            hatModel = desiredHatModel;
        }
        else if (desiredHatModel != null && hatModel != desiredHatModel) {
            instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(desiredHatModel)).stealInstance(hat);
            hatModel = desiredHatModel;
        }
        else if (desiredHatModel == null && hat != null) {
            hat.delete();
            hat = null;
            hatModel = null;
        }

        boolean hasWind = blockEntity.getFrostLevelFromBlock().isAtLeast(FrostLevel.CHILLED);
        if (hasWind && wind == null) {
            wind = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(CCBPartialModels.BREEZE_COOLER_WIND)).createInstance();
        }
        else if (!hasWind && wind != null) {
            wind.delete();
            wind = null;
        }

        float renderTime = AnimationTickHolder.getRenderTime(level);
        float headY = Mth.sin((renderTime + blockEntity.hashCode() % 13 * 16) / 16 % Mth.TWO_PI) / (currentFrostLevel.isAtLeast(FrostLevel.CHILLED) ? 64 : 16) - animation * 0.75f;
        float horizontalAngle = AngleHelper.rad(blockEntity.headAngle.getValue(partialTicks));
        head.setIdentityTransform().translate(getVisualPosition()).translateY(headY).translate(Translate.CENTER).rotateY(horizontalAngle).translateBack(Translate.CENTER).setChanged();
        if (goggles != null) {
            goggles.setIdentityTransform().translate(getVisualPosition()).translateY(headY + 0.5f).translate(Translate.CENTER).rotateY(horizontalAngle).translateBack(Translate.CENTER).setChanged();
        }
        if (hat != null) {
            hat.setIdentityTransform().translate(getVisualPosition()).translateY(headY);
            if (currentFrostLevel.isAtLeast(FrostLevel.CHILLED)) {
                hat.translateY(0.75f);
            }
            else {
                hat.translateY(0.5f).translate(Translate.CENTER).scale(0.75f).translateBack(Translate.CENTER);
            }
            hat.rotateCentered(horizontalAngle + Mth.PI, Direction.UP).translate(0.5, 0, 0.5);
            hat.setChanged();
        }
        if (wind == null) {
            return;
        }

        wind.setIdentityTransform().translate(getVisualPosition()).translateY(headY).translate(Translate.CENTER).rotateY(horizontalAngle + AngleHelper.rad(renderTime * (hasWind ? 24 : 0) % 360)).translateBack(Translate.CENTER).setChanged();
    }

    @Override
    public void tick(TickableVisual.Context context) {
        BreezeCoolerClientAnimation.tickAnimation(blockEntity);
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
}
