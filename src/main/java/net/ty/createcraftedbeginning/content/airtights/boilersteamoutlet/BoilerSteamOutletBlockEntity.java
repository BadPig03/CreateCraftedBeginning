package net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BoilerSteamOutletBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private static final int LAZY_TICK_RATE = 20;
    private static final int GENERATION_BAR_SEGMENTS = 20;

    private final BoilerSteamOutletController controller;
    private final SteamOutletGasHandler exposedGasHandler;

    public BoilerSteamOutletBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        controller = new BoilerSteamOutletController(this);
        exposedGasHandler = new SteamOutletGasHandler(this);
        setLazyTickRate(LAZY_TICK_RATE);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasCapabilities.BLOCK, CCBBlockEntities.BOILER_STEAM_OUTLET.get(), (outlet, direction) -> {
            if (direction != BoilerSteamOutletBlock.getFacing(outlet.getBlockState())) {
                return null;
            }

            return outlet.exposedGasHandler;
        });
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        controller.tickServer();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        controller.lazyTickServer();
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        controller.write(compoundTag, clientPacket);
        exposedGasHandler.write(compoundTag, clientPacket);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        controller.read(compoundTag, clientPacket);
        exposedGasHandler.read(compoundTag, clientPacket);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CCBLang.translate("gui.boiler_steam_outlet.header").forGoggles(tooltip);
        int filledSegments = Mth.ceil(controller.getSteamGenerationRatio() * GENERATION_BAR_SEGMENTS);
        MutableComponent bar = Component.empty();
        for (int segment = 1; segment <= GENERATION_BAR_SEGMENTS; segment++) {
            ChatFormatting color;
            if (segment > filledSegments) {
                color = ChatFormatting.DARK_RED;
            }
            else if (segment == filledSegments) {
                color = ChatFormatting.GREEN;
            }
            else {
                color = ChatFormatting.DARK_GREEN;
            }
            bar.append(Component.literal("|").withStyle(color));
        }

        CCBLang.translate("gui.boiler_steam_outlet.steam_generation").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.builder().add(bar).forGoggles(tooltip, 1);
        return true;
    }

    void recordExtraction(GasStack drained, GasAction action) {
        controller.recordExtraction(drained, action);
    }

    void restoreExtraction(GasStack restored, GasAction action) {
        controller.restoreExtraction(restored, action);
    }

    void invalidateCurrentProduction() {
        controller.invalidateCurrentProduction();
    }

    long getMaximumOutputAmount() {
        return controller.getMaximumOutputAmount();
    }

    void ensureCurrentTick() {
        controller.ensureCurrentTick();
    }

    void addProducedSteam(long amount) {
        exposedGasHandler.addProducedSteam(amount);
    }

    void clearBufferedSteam() {
        exposedGasHandler.clearBufferedSteam();
    }
}
