package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberInteractionPoint;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberInteractionPoint.BreezeChamberType;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerInteractionPoint;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerInteractionPoint.BreezeCoolerType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class CCBArmInteractionPointTypes {
    private static final DeferredRegister<ArmInteractionPointType> ARM_INTERACTION_POINT_TYPES = DeferredRegister.create(CreateBuiltInRegistries.ARM_INTERACTION_POINT_TYPE, CreateCraftedBeginning.MOD_ID);

    public static DeferredHolder<ArmInteractionPointType, ? extends ArmInteractionPointType> BREEZE_COOLER = register("breeze_cooler", new BreezeCoolerType());
    public static DeferredHolder<ArmInteractionPointType, ? extends ArmInteractionPointType> BREEZE_CHAMBER = register("breeze_chamber", new BreezeChamberType());

    private static <T extends ArmInteractionPointType> @NotNull DeferredHolder<ArmInteractionPointType, T> register(String key, T type) {
        return ARM_INTERACTION_POINT_TYPES.register(key, () -> type);
    }

    public static void register(IEventBus modBus) {
        ARM_INTERACTION_POINT_TYPES.register(modBus);
    }
}
