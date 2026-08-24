package net.ty.createcraftedbeginning.compat.functionalstorage;

import com.buuz135.functionalstorage.FunctionalStorage.DrawerType;
import com.buuz135.functionalstorage.block.Drawer;
import com.buuz135.functionalstorage.block.DrawerBlock;
import com.buuz135.functionalstorage.item.FSAttachments;
import com.buuz135.functionalstorage.util.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.compat.functionalstorage.registry.CCBFunctionalStorageBlockEntities;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasDrawerBlock extends Drawer<GasDrawerBlockEntity> {
    private static final String COMPOUND_KEY_CREATIVE = "isCreative";
    private static final String COMPOUND_KEY_VOID = "isVoid";
    private static final Component CONTENTS_HEADER = Component.translatable("drawer.block.contents").withStyle(ChatFormatting.GRAY);
    private static final Component UPGRADES_HEADER = Component.translatable("drawer.block.upgrades").withStyle(ChatFormatting.GRAY);
    private static final Component EMPTY_ENTRY = Component.literal("- ").withStyle(ChatFormatting.GRAY).append(Component.translatable("gui.createcraftedbeginning.empty").withStyle(ChatFormatting.DARK_GRAY));
    private static final Component NO_UPGRADES_ENTRY = Component.literal("- ").withStyle(ChatFormatting.GRAY).append(Component.translatable("drawer.block.upgrades.none").withStyle(ChatFormatting.GRAY));

    private final DrawerType drawerType;

    public GasDrawerBlock(DrawerType drawerType, Properties properties) {
        super("gas_drawer_" + drawerType.getSlots(), properties, GasDrawerBlockEntity.class);
        this.drawerType = drawerType;
        registerDefaultState(defaultBlockState().setValue(FACING_HORIZONTAL_CUSTOM, Direction.NORTH).setValue(FACING_ALL, Direction.DOWN).setValue(DrawerBlock.LOCKED, false));
    }

    private static void appendUpgradesTooltip(CompoundTag tileTag, List<Component> tooltip) {
        tooltip.add(UPGRADES_HEADER);
        boolean hasUpgrades = false;
        if (tileTag.getBoolean(COMPOUND_KEY_CREATIVE)) {
            tooltip.add(Component.literal("- ").withStyle(ChatFormatting.GRAY).append(Component.translatable("drawer.block.upgrades.is_creative").withStyle(ChatFormatting.LIGHT_PURPLE)));
            hasUpgrades = true;
        }
        if (tileTag.getBoolean(COMPOUND_KEY_VOID)) {
            tooltip.add(Component.literal("- ").withStyle(ChatFormatting.GRAY).append(Component.translatable("drawer.block.upgrades.is_void").withStyle(ChatFormatting.BLUE)));
            hasUpgrades = true;
        }
        if (hasUpgrades) {
            return;
        }

        tooltip.add(NO_UPGRADES_ENTRY);
    }

    public static GasStack readStoredGas(CompoundTag tileTag, int slot, Provider provider) {
        return GasDrawerStorage.readStoredGas(tileTag.getCompound(GasDrawerStorage.COMPOUND_KEY_STORAGE), slot, provider);
    }

    @Override
    public BlockEntitySupplier<GasDrawerBlockEntity> getTileEntityFactory() {
        return (pos, state) -> new GasDrawerBlockEntity(this, CCBFunctionalStorageBlockEntities.get(drawerType), pos, state, drawerType);
    }

    @Override
    public List<VoxelShape> getBoundingBoxes(BlockState state, BlockGetter source, BlockPos pos) {
        List<VoxelShape> boundingBoxes = new ArrayList<>(DrawerBlock.getDefaultHitShapes(drawerType, state));
        boundingBoxes.add(Shapes.block());
        return boundingBoxes;
    }

    @Override
    public void registerRecipe(RecipeOutput consumer) {
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!stack.has(FSAttachments.TILE)) {
            return;
        }

        CompoundTag tileTag = stack.getOrDefault(FSAttachments.TILE, new CompoundTag());
        appendContentsTooltip(tileTag, tooltip);
        appendUpgradesTooltip(tileTag, tooltip);
    }

    @Override
    public Collection<VoxelShape> getHitShapes(BlockState state) {
        return DrawerBlock.getDefaultHitShapes(drawerType, state);
    }

    private void appendContentsTooltip(CompoundTag tileTag, List<Component> tooltip) {
        tooltip.add(CONTENTS_HEADER);
        CompoundTag storageTag = tileTag.getCompound(GasDrawerStorage.COMPOUND_KEY_STORAGE);
        boolean hasContents = false;
        for (int slot = 0; slot < drawerType.getSlots(); slot++) {
            GasStack storedGas = GasDrawerStorage.readStoredGas(storageTag, slot, Utils.registryAccess());
            if (storedGas.isEmpty()) {
                continue;
            }

            hasContents = true;
            tooltip.add(Component.literal("- ").withStyle(ChatFormatting.GRAY).append(Component.literal(GasAmounts.formatCompact(storedGas.getAmount())).withStyle(ChatFormatting.YELLOW)).append(Component.literal(" ")).append(storedGas.getHoverName().copy().withStyle(ChatFormatting.GOLD)));
            if (storedGas.isComponentsPatchEmpty()) {
                continue;
            }

            tooltip.add(CCBLang.translateDirect("compat.functional_storage.gas_drawer.has_components").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (hasContents) {
            return;
        }

        tooltip.add(EMPTY_ENTRY);
    }
}
