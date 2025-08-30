package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.Direction;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

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

    public static final PartialModel BREEZE_RIMING = block("breeze_chamber/breeze/riming");
    public static final PartialModel BREEZE_CHILLED = block("breeze_chamber/breeze/chilled");
    public static final PartialModel BREEZE_CHILLED_ACTIVE = block("breeze_chamber/breeze/chilled_active");
    public static final PartialModel BREEZE_GALLING = block("breeze_chamber/breeze/galling");
    public static final PartialModel BREEZE_GALLING_ACTIVE = block("breeze_chamber/breeze/galling_active");
    public static final PartialModel BREEZE_WIND = block("breeze_chamber/breeze/wind");
    public static final PartialModel BREEZE_GOGGLES = block("breeze_chamber/breeze/goggles");
    public static final PartialModel BREEZE_GOGGLES_SMALL = block("breeze_chamber/breeze/goggles_small");

    public static final PartialModel NOZZLE = block("gas_injection_chamber/nozzle");
    public static final PartialModel NOZZLE_TOP = block("gas_injection_chamber/nozzle_top");
    public static final PartialModel NOZZLE_BOTTOM = block("gas_injection_chamber/nozzle_bottom");

    public static final PartialModel ENCASED_UP = block("airtight_encased_pipe/up");
    public static final PartialModel ENCASED_DOWN = block("airtight_encased_pipe/down");
    public static final PartialModel ENCASED_NORTH = block("airtight_encased_pipe/north");
    public static final PartialModel ENCASED_EAST = block("airtight_encased_pipe/east");
    public static final PartialModel ENCASED_SOUTH = block("airtight_encased_pipe/south");
    public static final PartialModel ENCASED_WEST = block("airtight_encased_pipe/west");

    public static final Map<FluidTransportBehaviour.AttachmentTypes.ComponentPartials, Map<Direction, PartialModel>> AIRTIGHT_PIPE_ATTACHMENTS = new EnumMap<>(FluidTransportBehaviour.AttachmentTypes.ComponentPartials.class);

    static {
        for (FluidTransportBehaviour.AttachmentTypes.ComponentPartials type : FluidTransportBehaviour.AttachmentTypes.ComponentPartials.values()) {
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

    private static PartialModel entity(String path) {
        return PartialModel.of(CreateCraftedBeginning.asResource("entity/" + path));
    }

    public static void register() {
    }
}
