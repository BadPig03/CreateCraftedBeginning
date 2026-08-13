package net.ty.createcraftedbeginning.compat.functionalstorage.registry;

import com.buuz135.functionalstorage.FunctionalStorage.DrawerType;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.ty.createcraftedbeginning.compat.functionalstorage.GasDrawerBlockEntity;
import net.ty.createcraftedbeginning.registry.registrate.CCBRegistrate;
import net.ty.createcraftedbeginning.registry.registrate.CCBRegistrateProvider;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBFunctionalStorageBlockEntities {
    private static final CCBRegistrate CCB_REGISTRATE = CCBRegistrateProvider.get();

    public static final BlockEntityEntry<GasDrawerBlockEntity> GAS_DRAWER_1 = CCB_REGISTRATE.<GasDrawerBlockEntity>blockEntity("gas_drawer_1", (type, pos, state) -> new GasDrawerBlockEntity(CCBFunctionalStorageBlocks.GAS_DRAWER_1_BLOCK.get(), type, pos, state, DrawerType.X_1)).validBlock(CCBFunctionalStorageBlocks.GAS_DRAWER_1_BLOCK).register();
    public static final BlockEntityEntry<GasDrawerBlockEntity> GAS_DRAWER_2 = CCB_REGISTRATE.<GasDrawerBlockEntity>blockEntity("gas_drawer_2", (type, pos, state) -> new GasDrawerBlockEntity(CCBFunctionalStorageBlocks.GAS_DRAWER_2_BLOCK.get(), type, pos, state, DrawerType.X_2)).validBlock(CCBFunctionalStorageBlocks.GAS_DRAWER_2_BLOCK).register();
    public static final BlockEntityEntry<GasDrawerBlockEntity> GAS_DRAWER_4 = CCB_REGISTRATE.<GasDrawerBlockEntity>blockEntity("gas_drawer_4", (type, pos, state) -> new GasDrawerBlockEntity(CCBFunctionalStorageBlocks.GAS_DRAWER_4_BLOCK.get(), type, pos, state, DrawerType.X_4)).validBlock(CCBFunctionalStorageBlocks.GAS_DRAWER_4_BLOCK).register();

    public static BlockEntityType<GasDrawerBlockEntity> get(DrawerType drawerType) {
        return switch (drawerType) {
            case X_1 -> GAS_DRAWER_1.get();
            case X_2 -> GAS_DRAWER_2.get();
            case X_4 -> GAS_DRAWER_4.get();
        };
    }

    public static void register() {
    }
}
