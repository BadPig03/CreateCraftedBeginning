package net.ty.createcraftedbeginning.registry;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.Direction;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasTransportBehaviour;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class CCBPartialModels {
    public static final PartialModel BREEZE_TRAIN_HAT = entity("train_hat");

    public static final PartialModel PNEUMATIC_ENGINE_COGS = block("pneumatic_engine/block_cogs");
    public static final PartialModel PHOTO_STRESS_BEARING_SHAFT = block("photo-stress_bearing/block_shaft");
    public static final PartialModel CINDER_INCINERATION_BLOWER_SHAFT = block("cinder_incineration_blower/block_shaft");
    public static final PartialModel AIRTIGHT_PUMP_COGS = block("airtight_pump/block_cogs");
    public static final PartialModel AIR_COMPRESSOR_SHAFT = block("air_compressor/block_shaft");
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

    public static final PartialModel ENCASED_UP = block("airtight_encased_pipe/up");
    public static final PartialModel ENCASED_DOWN = block("airtight_encased_pipe/down");
    public static final PartialModel ENCASED_NORTH = block("airtight_encased_pipe/north");
    public static final PartialModel ENCASED_EAST = block("airtight_encased_pipe/east");
    public static final PartialModel ENCASED_SOUTH = block("airtight_encased_pipe/south");
    public static final PartialModel ENCASED_WEST = block("airtight_encased_pipe/west");

    public static final Map<GasTransportBehaviour.AttachmentTypes.ComponentPartials, Map<Direction, PartialModel>> AIRTIGHT_PIPE_ATTACHMENTS = new EnumMap<>(GasTransportBehaviour.AttachmentTypes.ComponentPartials.class);

    static {
        for (GasTransportBehaviour.AttachmentTypes.ComponentPartials type : GasTransportBehaviour.AttachmentTypes.ComponentPartials.values()) {
            Map<Direction, PartialModel> map = new HashMap<>();
            for (Direction d : Iterate.directions) {
                String asId = Lang.asId(type.name());
                map.put(d, block("airtight_pipe/" + asId + "/" + Lang.asId(d.getSerializedName())));
            }
            AIRTIGHT_PIPE_ATTACHMENTS.put(type, map);
        }
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
