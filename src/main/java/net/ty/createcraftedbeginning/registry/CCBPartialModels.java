package net.ty.createcraftedbeginning.registry;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.Direction;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasTransportBehaviour.AttachmentTypes.ComponentPartials;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class CCBPartialModels {
    public static final PartialModel BREEZE_TRAIN_HAT = entity("train_hat");

    public static final PartialModel PNEUMATIC_ENGINE_COGS = block("pneumatic_engine/block_cogs");
    public static final PartialModel PHOTO_STRESS_BEARING_SHAFT = block("photo-stress_bearing/block_shaft");
    public static final PartialModel CINDER_INCINERATION_BLOWER_SHAFT = block("cinder_incineration_blower/block_shaft");
    public static final PartialModel AIRTIGHT_PUMP_COGS = block("airtight_pump/block_cogs");
    public static final PartialModel AIRTIGHT_ENGINE_COGS = block("airtight_engine/block_cogs");
    public static final PartialModel AIRTIGHT_ENGINE_PISTON = block("airtight_engine/block_piston");

    public static final PartialModel BREEZE_ILL = block("breeze_chamber/breeze/ill");
    public static final PartialModel BREEZE_CALM = block("breeze_chamber/breeze/calm");
    public static final PartialModel BREEZE_GALE = block("breeze_chamber/breeze/gale");
    public static final PartialModel BREEZE_GALE_ACTIVE = block("breeze_chamber/breeze/gale_active");
    public static final PartialModel BREEZE_CHAMBER_WIND = block("breeze_chamber/breeze/wind");
    public static final PartialModel BREEZE_CHAMBER_GOGGLES = block("breeze_chamber/breeze/goggles");
    public static final PartialModel BREEZE_CHAMBER_GOGGLES_SMALL = block("breeze_chamber/breeze/goggles_small");

    public static final PartialModel BREEZE_RIMING = block("breeze_cooler/breeze/riming");
    public static final PartialModel BREEZE_CHILLED = block("breeze_cooler/breeze/chilled");
    public static final PartialModel BREEZE_CHILLED_ACTIVE = block("breeze_cooler/breeze/chilled_active");
    public static final PartialModel BREEZE_COOLER_WIND = block("breeze_cooler/breeze/wind");
    public static final PartialModel BREEZE_COOLER_GOGGLES = block("breeze_cooler/breeze/goggles");
    public static final PartialModel BREEZE_COOLER_GOGGLES_SMALL = block("breeze_cooler/breeze/goggles_small");

    public static final PartialModel NOZZLE = block("gas_injection_chamber/nozzle");
    public static final PartialModel NOZZLE_TOP = block("gas_injection_chamber/nozzle_top");
    public static final PartialModel NOZZLE_BOTTOM = block("gas_injection_chamber/nozzle_bottom");

    public static final PartialModel PORTABLE_GAS_INTERFACE_MIDDLE = block("portable_gas_interface/block_middle");
    public static final PartialModel PORTABLE_GAS_INTERFACE_MIDDLE_POWERED = block("portable_gas_interface/block_middle_powered");
    public static final PartialModel PORTABLE_GAS_INTERFACE_TOP = block("portable_gas_interface/block_top");

    public static final PartialModel TESLA_TURBINE_ROTOR = block("tesla_turbine/rotor");

    public static final PartialModel ENCASED_UP = block("airtight_encased_pipe/up");
    public static final PartialModel ENCASED_DOWN = block("airtight_encased_pipe/down");
    public static final PartialModel ENCASED_NORTH = block("airtight_encased_pipe/north");
    public static final PartialModel ENCASED_EAST = block("airtight_encased_pipe/east");
    public static final PartialModel ENCASED_SOUTH = block("airtight_encased_pipe/south");
    public static final PartialModel ENCASED_WEST = block("airtight_encased_pipe/west");

    public static final PartialModel AIRTIGHT_CANNON_BARREL = item("airtight_cannon/barrel");
    public static final PartialModel AIRTIGHT_CANNON_PISTON_LEFT = item("airtight_cannon/piston_left");
    public static final PartialModel AIRTIGHT_CANNON_PISTON_RIGHT = item("airtight_cannon/piston_right");
    public static final PartialModel AIRTIGHT_HANDHELD_DRILL_FRONT = item("airtight_handheld_drill/front");
    public static final PartialModel AIRTIGHT_JETPACK = item("airtight_jetpack");
    public static final PartialModel AIRTIGHT_ELYTRA = item("airtight_elytra");
    public static final PartialModel AIRTIGHT_SHIELD = item("airtight_shield");

    public static final PartialModel AIR_VENT_CLOSED = block("air_vent/closed");
    public static final PartialModel AIR_VENT_OPENED = block("air_vent/opened");

    public static final PartialModel SHAFT_HALF_UP = block("shaft_half_up");

    public static final Map<ComponentPartials, Map<Direction, PartialModel>> AIRTIGHT_PIPE_ATTACHMENTS = new EnumMap<>(ComponentPartials.class);

    static {
        for (ComponentPartials type : ComponentPartials.values()) {
            Map<Direction, PartialModel> map = new HashMap<>();
            for (Direction direction : Iterate.directions) {
                map.put(direction, block("airtight_pipe/" + Lang.asId(type.name()) + '/' + Lang.asId(direction.getSerializedName())));
            }
            AIRTIGHT_PIPE_ATTACHMENTS.put(type, map);
        }
    }

    private static PartialModel item(String path) {
        return PartialModel.of(CreateCraftedBeginning.asResource("item/" + path));
    }

    private static PartialModel block(String path) {
        return PartialModel.of(CreateCraftedBeginning.asResource("block/" + path));
    }

    @SuppressWarnings("SameParameterValue")
    private static PartialModel entity(String path) {
        return PartialModel.of(CreateCraftedBeginning.asResource("entity/" + path));
    }

    public static void register() {
    }
}
