package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankMountedStorageType;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankMountedStorageType;
import net.ty.createcraftedbeginning.content.airtights.gas.mounted.MountedGasStorageType;
import net.ty.createcraftedbeginning.content.crates.CrateMountedStorageType;
import net.ty.createcraftedbeginning.content.crates.andesitecrate.AndesiteCrateBlockEntity;
import net.ty.createcraftedbeginning.content.crates.andesitecrate.AndesiteCrateMountedStorage;
import net.ty.createcraftedbeginning.content.crates.brasscrate.BrassCrateBlockEntity;
import net.ty.createcraftedbeginning.content.crates.brasscrate.BrassCrateMountedStorage;
import net.ty.createcraftedbeginning.content.crates.cardboardcrate.CardboardCrateBlockEntity;
import net.ty.createcraftedbeginning.content.crates.cardboardcrate.CardboardCrateMountedStorage;
import net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateBlockEntity;
import net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateMountedStorage;
import net.ty.createcraftedbeginning.registry.registrate.CCBRegistrate;
import net.ty.createcraftedbeginning.registry.registrate.CCBRegistrateProvider;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBMountedStorage {
    private static final CCBRegistrate CCB_REGISTRATE = CCBRegistrateProvider.get();

    public static final RegistryEntry<MountedItemStorageType<?>, CrateMountedStorageType<AndesiteCrateBlockEntity, AndesiteCrateMountedStorage>> ANDESITE_CRATE = simpleItem("andesite_crate", () -> new CrateMountedStorageType<>(AndesiteCrateMountedStorage.CODEC, AndesiteCrateBlockEntity.class, AndesiteCrateMountedStorage::fromBlockEntity));
    public static final RegistryEntry<MountedItemStorageType<?>, CrateMountedStorageType<BrassCrateBlockEntity, BrassCrateMountedStorage>> BRASS_CRATE = simpleItem("brass_crate", () -> new CrateMountedStorageType<>(BrassCrateMountedStorage.CODEC, BrassCrateBlockEntity.class, BrassCrateMountedStorage::fromBlockEntity));
    public static final RegistryEntry<MountedItemStorageType<?>, CrateMountedStorageType<CardboardCrateBlockEntity, CardboardCrateMountedStorage>> CARDBOARD_CRATE = simpleItem("cardboard_crate", () -> new CrateMountedStorageType<>(CardboardCrateMountedStorage.CODEC, CardboardCrateBlockEntity.class, CardboardCrateMountedStorage::fromBlockEntity));
    public static final RegistryEntry<MountedItemStorageType<?>, CrateMountedStorageType<SturdyCrateBlockEntity, SturdyCrateMountedStorage>> STURDY_CRATE = simpleItem("sturdy_crate", () -> new CrateMountedStorageType<>(SturdyCrateMountedStorage.CODEC, SturdyCrateBlockEntity.class, SturdyCrateMountedStorage::fromBlockEntity));

    public static final RegistryEntry<MountedGasStorageType<?>, AirtightTankMountedStorageType> AIRTIGHT_TANK = simpleGas("airtight_tank", AirtightTankMountedStorageType::new);
    public static final RegistryEntry<MountedGasStorageType<?>, AirtightTankMountedStorageType> HORIZONTAL_AIRTIGHT_TANK = simpleGas("horizontal_airtight_tank", AirtightTankMountedStorageType::new);
    public static final RegistryEntry<MountedGasStorageType<?>, CreativeAirtightTankMountedStorageType> CREATIVE_AIRTIGHT_TANK = simpleGas("creative_airtight_tank", CreativeAirtightTankMountedStorageType::new);

    private static <T extends MountedItemStorageType<?>> @NotNull RegistryEntry<MountedItemStorageType<?>, T> simpleItem(String name, Supplier<T> supplier) {
        return CCB_REGISTRATE.mountedItemStorage(name, supplier).register();
    }

    private static <T extends MountedGasStorageType<?>> @NotNull RegistryEntry<MountedGasStorageType<?>, T> simpleGas(String name, Supplier<T> supplier) {
        return CCB_REGISTRATE.mountedGasStorage(name, supplier).register();
    }

    public static void register() {
    }
}
