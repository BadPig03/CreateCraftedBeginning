package net.ty.createcraftedbeginning.api.gascanisters;

import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.theme.Color;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterOverlayPacket;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.data.CCBDistExecutor;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CanisterContainerClients {
    public static final String COMPOUND_KEY_STORED_GAS_TYPE = "CreateCraftedBeginningStoredGasType";

    private static final int BAR_WIDTH = 13;

    private CanisterContainerClients() {
    }

    /**
     * Checks whether this value is bar visible.
     *
     * @return {@code true} if this value is bar visible; otherwise {@code false}
     */
    @OnlyIn(Dist.CLIENT)
    public static boolean isBarVisible() {
        DisplayedGasState state = getDisplayedGasState();
        return !state.content().isEmpty() && (state.creative() || state.capacity() > 0);
    }

    /**
     * Returns the bar color.
     *
     * @return the bar color
     */
    @OnlyIn(Dist.CLIENT)
    public static int getBarColor() {
        float ratio = getDisplayedGasRatio();
        if (ratio == 0) {
            return 0;
        }
        return Color.mixColors(GasCanisterUtils.COLOR_CYAN, GasCanisterUtils.COLOR_WHITE, ratio);
    }

    /**
     * Returns the bar width.
     *
     * @return the bar width
     */
    @OnlyIn(Dist.CLIENT)
    public static int getBarWidth() {
        float ratio = getDisplayedGasRatio();
        return ratio == 0 ? 0 : Math.round(BAR_WIDTH * ratio);
    }

    /**
     * Returns the displayed gas content.
     *
     * @return the displayed gas content
     */
    @OnlyIn(Dist.CLIENT)
    public static GasStack getDisplayedGasContent() {
        return getDisplayedGasState().content().copy();
    }

    @OnlyIn(Dist.CLIENT)
    private static float getDisplayedGasRatio() {
        DisplayedGasState state = getDisplayedGasState();
        if (state.content().isEmpty()) {
            return 0;
        }

        if (state.creative()) {
            return 1;
        }

        if (state.capacity() <= 0) {
            return 0;
        }
        return Mth.clamp((float) state.content().getAmount() / state.capacity(), 0, 1);
    }

    @OnlyIn(Dist.CLIENT)
    private static DisplayedGasState getDisplayedGasState() {
        Player player = CCBDistExecutor.getClientPlayer();
        if (player == null) {
            return DisplayedGasState.EMPTY;
        }

        CompoundTag data = player.getPersistentData();
        if (data.contains(GasCanisterOverlayPacket.COMPOUND_KEY_OVERLAY)) {
            CompoundTag overlay = data.getCompound(GasCanisterOverlayPacket.COMPOUND_KEY_OVERLAY);
            GasStack content = GasStack.parseOptional(player.level().registryAccess(), overlay.getCompound(GasCanisterOverlayPacket.COMPOUND_KEY_CONTENT));
            return new DisplayedGasState(content, overlay.getLong(GasCanisterOverlayPacket.COMPOUND_KEY_CAPACITY), overlay.getBoolean(GasCanisterOverlayPacket.COMPOUND_KEY_CREATIVE));
        }

        var fallback = CanisterContainerSuppliers.getFirstCanisterSupplierPair(player);
        GasStack content = fallback.getFirst();
        if (content.isEmpty()) {
            return DisplayedGasState.EMPTY;
        }

        return new DisplayedGasState(content, fallback.getSecond().getFirst(), fallback.getSecond().getSecond());
    }

    /**
     * Returns the bar color.
     *
     * @param canister the canister to use
     * @return the bar color
     */
    public static int getBarColor(ItemStack canister) {
        if (!(canister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return 0;
        }

        long amount = canisterContents.getGasInTank(0).getAmount();
        long capacity = canisterContents.getTankCapacity(0);
        if (amount == 0 || capacity == 0) {
            return 0;
        }
        float ratio = Mth.clamp((float) amount / capacity, 0, 1);
        return Color.mixColors(GasCanisterUtils.COLOR_CYAN, GasCanisterUtils.COLOR_WHITE, ratio);
    }

    /**
     * Returns the bar width.
     *
     * @param canister the canister to use
     * @return the bar width
     */
    public static int getBarWidth(ItemStack canister) {
        if (!(canister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return 0;
        }

        long amount = canisterContents.getGasInTank(0).getAmount();
        long capacity = canisterContents.getTankCapacity(0);
        if (amount == 0 || capacity == 0) {
            return 0;
        }
        float ratio = Mth.clamp((float) amount / capacity, 0, 1);
        return Math.round(BAR_WIDTH * ratio);
    }

    /**
     * Returns the stored gas type.
     *
     * @param player the player performing the operation
     * @return the stored gas type
     */
    public static Gas getStoredGasType(Player player) {
        ResourceLocation gasId = NBTHelper.readResourceLocation(player.getPersistentData(), COMPOUND_KEY_STORED_GAS_TYPE);
        return Gas.getGasTypeByName(gasId);
    }

    private record DisplayedGasState(GasStack content, long capacity, boolean creative) {
        private static final DisplayedGasState EMPTY = new DisplayedGasState(GasStack.EMPTY, -1, false);
    }
}
