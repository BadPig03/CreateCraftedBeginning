package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.andesitecrate.AndesiteCrateMountedStorageType;
import net.ty.createcraftedbeginning.content.brasscrate.BrassCrateMountedStorageType;
import net.ty.createcraftedbeginning.content.cardboardcrate.CardboardCrateMountedStorageType;
import net.ty.createcraftedbeginning.content.airtighttank.AirtightTankMountedStorageType;
import net.ty.createcraftedbeginning.content.sturdycrate.SturdyCrateMountedStorageType;
import net.ty.createcraftedbeginning.data.CCBRegistrate;

import java.util.function.Supplier;

public class CCBMountedStorage {
    private static final CCBRegistrate CCB_REGISTRATE = CreateCraftedBeginning.registrate();

    public static final RegistryEntry<MountedItemStorageType<?>, AndesiteCrateMountedStorageType> ANDESITE_CRATE = simpleItem("andesite_crate", AndesiteCrateMountedStorageType::new);

    public static final RegistryEntry<MountedItemStorageType<?>, BrassCrateMountedStorageType> BRASS_CRATE = simpleItem("brass_crate", BrassCrateMountedStorageType::new);

    public static final RegistryEntry<MountedItemStorageType<?>, CardboardCrateMountedStorageType> CARDBOARD_CRATE = simpleItem("cardboard_crate", CardboardCrateMountedStorageType::new);

    public static final RegistryEntry<MountedItemStorageType<?>, SturdyCrateMountedStorageType> STURDY_CRATE = simpleItem("sturdy_crate", SturdyCrateMountedStorageType::new);

    public static final RegistryEntry<MountedFluidStorageType<?>, AirtightTankMountedStorageType> AIRTIGHT_TANK = simpleFluid("airtight_tank", AirtightTankMountedStorageType::new);

    private static <T extends MountedItemStorageType<?>> RegistryEntry<MountedItemStorageType<?>, T> simpleItem(String name, Supplier<T> supplier) {
        return CCB_REGISTRATE.mountedItemStorage(name, supplier).register();
    }

    private static <T extends MountedFluidStorageType<?>> RegistryEntry<MountedFluidStorageType<?>, T> simpleFluid(String name, Supplier<T> supplier) {
        return CCB_REGISTRATE.mountedFluidStorage(name, supplier).register();
    }

    public static void register() {
    }
}
