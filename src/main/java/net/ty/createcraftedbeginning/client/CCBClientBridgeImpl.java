package net.ty.createcraftedbeginning.client;

import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonRenderHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlock;
import net.ty.createcraftedbeginning.content.airtights.gas.GasAreaOutlinePacket;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeScreen;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlock;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.CCBClientBridge.CCBService;
import net.ty.createcraftedbeginning.platform.CCBClientBridge.ScreenTarget;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class CCBClientBridgeImpl implements CCBService {
    private static final int COLOR_RED = 0xFFFF5D6C;

    @Override
    public @Nullable Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }

    @Override
    public boolean addAlignedTooltipBars(List<Component> tooltip, int indent, List<? extends Component> labels, List<? extends Component> bars) {
        CCBTooltipBarAlignment.addAlignedBars(tooltip, indent, labels, bars);
        return true;
    }

    @Override
    public void dontAnimateAirtightCannon(InteractionHand hand) {
        AirtightCannonRenderHandler.INSTANCE.dontAnimateItem(hand);
    }

    @Override
    public void showAirtightForgingPressPlacementBounds(BlockPlaceContext context) {
        if (!(context.getPlayer() instanceof LocalPlayer localPlayer) || !(context.getItemInHand().getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof AirtightForgingPressBlock)) {
            return;
        }

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Outliner.getInstance().showAABB(Pair.of("airtight_forging_press", pos), new AABB(pos).inflate(1)).colored(COLOR_RED);
        CCBLang.translate("gui.warnings.clear_blocks_for_placement").color(COLOR_RED).sendStatus(localPlayer);
    }

    @Override
    public void showAirtightReactorKettlePlacementBounds(BlockPlaceContext context) {
        if (!(context.getPlayer() instanceof LocalPlayer localPlayer) || !(context.getItemInHand().getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof AirtightReactorKettleBlock)) {
            return;
        }

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Outliner.getInstance().showAABB(Pair.of("airtight_reactor_kettle", pos), new AABB(pos).inflate(1)).colored(COLOR_RED);
        CCBLang.translate("gui.warnings.clear_blocks_for_placement").color(COLOR_RED).sendStatus(localPlayer);
    }

    @Override
    public void showTeslaTurbinePlacementBounds(BlockPlaceContext context) {
        if (!(context.getPlayer() instanceof LocalPlayer localPlayer) || !(context.getItemInHand().getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof TeslaTurbineBlock turbine)) {
            return;
        }

        Axis axis = turbine.getAxisForPlacement(context);
        if (axis == null) {
            return;
        }

        Vec3 contract = Vec3.atLowerCornerOf(Direction.get(AxisDirection.POSITIVE, axis).getNormal());
        BlockPos pos = context.getClickedPos();
        Outliner.getInstance().showAABB(Pair.of("tesla_turbine", pos), new AABB(pos).inflate(1).deflate(contract.x, contract.y, contract.z)).colored(COLOR_RED);
        CCBLang.translate("gui.warnings.clear_blocks_for_placement").color(COLOR_RED).sendStatus(localPlayer);
    }

    @Override
    public void showGasAreaOutline(Player player, BlockPos pos, Direction direction, float inflation, int color) {
        if (!GogglesItem.isWearingGoggles(player) || !CCBConfig.client().enableGasAreaOutline.get()) {
            return;
        }

        AABB area = new AABB(pos.relative(direction)).inflate(inflation);
        Object outlineSlot = Pair.of(GasAreaOutlinePacket.class, Pair.of(pos, direction));
        Outliner.getInstance().chaseAABB(outlineSlot, area).colored(color).withFaceTextures(AllSpecialTextures.CHECKERED, AllSpecialTextures.HIGHLIGHT_CHECKERED).lineWidth(0.0625f);
    }

    @Override
    public boolean isOverstressedTooltipEnabled() {
        return AllConfigs.client().enableOverstressedTooltip.get();
    }

    @Override
    public int getMaxItemStackDisplay() {
        return CCBConfig.client().maxItemStackDisplay.get();
    }

    @Override
    public float getFilterItemRenderDistance() {
        return AllConfigs.client().filterItemRenderDistance.getF();
    }

    @Override
    public void openGasFactoryGaugeScreen(ScreenTarget target, Player player) {
        if (!(player instanceof LocalPlayer) || !(target instanceof GasFactoryGaugeBehaviour behaviour)) {
            return;
        }

        ScreenOpener.open(new GasFactoryGaugeScreen(behaviour));
    }

    @Override
    public @Nullable ScreenTarget createGasFactoryGaugeBehaviour(RegistryFriendlyByteBuf extraData) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }

        FactoryPanelPosition position = FactoryPanelPosition.STREAM_CODEC.decode(extraData);
        FactoryPanelBehaviour panel = FactoryPanelBehaviour.at(level, position);
        return panel instanceof GasFactoryGaugeBehaviour gasGauge ? gasGauge : null;
    }
}
