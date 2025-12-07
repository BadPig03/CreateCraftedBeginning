package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.MountedGasStorageType;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankMountedStorageType;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankMountedStorageType;
import net.ty.createcraftedbeginning.content.crates.andesitecrate.AndesiteCrateMountedStorageType;
import net.ty.createcraftedbeginning.content.crates.brasscrate.BrassCrateMountedStorageType;
import net.ty.createcraftedbeginning.content.crates.cardboardcrate.CardboardCrateMountedStorageType;
import net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateMountedStorageType;
import net.ty.createcraftedbeginning.data.CCBRegistrate;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class CCBMountedStorage {
    private static final CCBRegistrate CCB_REGISTRATE = CreateCraftedBeginning.registrate();

    public static final RegistryEntry<MountedItemStorageType<?>, AndesiteCrateMountedStorageType> ANDESITE_CRATE = simpleItem("andesite_crate", AndesiteCrateMountedStorageType::new);
    public static final RegistryEntry<MountedItemStorageType<?>, BrassCrateMountedStorageType> BRASS_CRATE = simpleItem("brass_crate", BrassCrateMountedStorageType::new);
    public static final RegistryEntry<MountedItemStorageType<?>, CardboardCrateMountedStorageType> CARDBOARD_CRATE = simpleItem("cardboard_crate", CardboardCrateMountedStorageType::new);
    public static final RegistryEntry<MountedItemStorageType<?>, SturdyCrateMountedStorageType> STURDY_CRATE = simpleItem("sturdy_crate", SturdyCrateMountedStorageType::new);

    public static final RegistryEntry<MountedGasStorageType<?>, AirtightTankMountedStorageType> AIRTIGHT_TANK = simpleGas("airtight_tank", AirtightTankMountedStorageType::new);
    public static final RegistryEntry<MountedGasStorageType<?>, CreativeAirtightTankMountedStorageType> CREATIVE_AIRTIGHT_TANK = simpleGas("creative_airtight_tank", CreativeAirtightTankMountedStorageType::new);

    private static <T extends MountedItemStorageType<?>> @NotNull RegistryEntry<MountedItemStorageType<?>, T> simpleItem(String name, Supplier<T> supplier) {
        return CCB_REGISTRATE.mountedItemStorage(name, supplier).register();
    }

    @SuppressWarnings("SameParameterValue")
    private static <T extends MountedGasStorageType<?>> @NotNull RegistryEntry<MountedGasStorageType<?>, T> simpleGas(String name, Supplier<T> supplier) {
        return CCB_REGISTRATE.mountedGasStorage(name, supplier).register();
    }

    public static void register() {
    }
}
