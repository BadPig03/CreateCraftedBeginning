package net.ty.createcraftedbeginning.registry.registrate;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder.ConditionGroup;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightPipeBlockStateGenerator {
    private final BlockModelProvider models;
    private final MultiPartBlockStateBuilder multipart;

    AirtightPipeBlockStateGenerator(BlockModelProvider models, MultiPartBlockStateBuilder multipart) {
        this.models = models;
        this.multipart = multipart;
    }

    void generate() {
        for (Axis axis : Iterate.axes) {
            PartBuilder part = multipart.part().modelFile(componentModel("core_" + axis.getName())).addModel();
            ConditionGroup aligned = part.nestedGroup().condition(AirtightPipeBlock.CASED, false);
            for (Direction direction : Iterate.directions) {
                if (direction.getAxis() == axis) {
                    continue;
                }

                aligned.condition(AirtightPipeBlock.PROPERTY_BY_DIRECTION.get(direction), false);
            }
            if (axis == Axis.Y) {
                continue;
            }

            part.nestedGroup().useOr().condition(AirtightPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE)), true).condition(AirtightPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.fromAxisAndDirection(axis, AxisDirection.NEGATIVE)), true);
        }

        for (Direction direction : Iterate.directions) {
            Axis axis = direction.getAxis();
            String name = direction.getSerializedName();
            PartBuilder capPart = multipart.part().modelFile(componentModel("cap_" + name)).addModel().condition(AirtightPipeBlock.CASED, false).condition(AirtightPipeBlock.PROPERTY_BY_DIRECTION.get(direction), false);
            PartBuilder armPart = multipart.part().modelFile(componentModel("arm_" + name)).addModel().condition(AirtightPipeBlock.CASED, false).condition(AirtightPipeBlock.PROPERTY_BY_DIRECTION.get(direction), true);
            for (Direction other : Iterate.directions) {
                if (other.getAxis() == axis) {
                    continue;
                }

                capPart.condition(AirtightPipeBlock.PROPERTY_BY_DIRECTION.get(other), false);
                armPart.condition(AirtightPipeBlock.PROPERTY_BY_DIRECTION.get(other), false);
            }
            if (axis != Axis.Y) {
                capPart.condition(AirtightPipeBlock.PROPERTY_BY_DIRECTION.get(direction.getOpposite()), true);
            }

            PartBuilder junctionArmPart = multipart.part().modelFile(componentModel("junction_arm_" + name)).addModel();
            junctionArmPart.nestedGroup().condition(AirtightPipeBlock.CASED, false).condition(AirtightPipeBlock.PROPERTY_BY_DIRECTION.get(direction), true);
            ConditionGroup perpendicular = junctionArmPart.nestedGroup().useOr();
            for (Direction other : Iterate.directions) {
                if (other.getAxis() == axis) {
                    continue;
                }

                perpendicular.condition(AirtightPipeBlock.PROPERTY_BY_DIRECTION.get(other), true);
            }

            for (boolean open : new boolean[]{false, true}) {
                String suffix = (open ? "open_" : "closed_") + name;
                PartBuilder junctionPart = multipart.part().modelFile(componentModel("junction_" + suffix)).addModel();
                junctionPart.nestedGroup().condition(AirtightPipeBlock.CASED, false).condition(AirtightPipeBlock.PROPERTY_BY_DIRECTION.get(direction), open);
                ConditionGroup corners = junctionPart.nestedGroup().useOr();
                for (Direction first : Iterate.directions) {
                    for (Direction second : Iterate.directions) {
                        if (first.get3DDataValue() >= second.get3DDataValue() || first.getAxis() == second.getAxis()) {
                            continue;
                        }

                        corners.nestedGroup().condition(AirtightPipeBlock.PROPERTY_BY_DIRECTION.get(first), true).condition(AirtightPipeBlock.PROPERTY_BY_DIRECTION.get(second), true);
                    }
                }

                multipart.part().modelFile(componentModel("casing_" + suffix)).addModel().condition(AirtightPipeBlock.CASED, true).condition(AirtightPipeBlock.PROPERTY_BY_DIRECTION.get(direction), open);
            }
        }
    }

    private ModelFile componentModel(String name) {
        return models.getExistingFile(models.modLoc("block/airtight_pipe/" + name));
    }
}
