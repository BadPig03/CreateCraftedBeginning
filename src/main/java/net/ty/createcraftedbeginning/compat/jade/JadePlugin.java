package net.ty.createcraftedbeginning.compat.jade;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@WailaPlugin
public class JadePlugin implements IWailaPlugin {
    public static final ResourceLocation GAS = CCBAPI.asResource("gas");
    public static final ResourceLocation GAS_STORAGE_BLOCK_TOOLTIP = CCBAPI.asResource("gas_storage_block_tooltip");
    public static final ResourceLocation GAS_STORAGE_CONTRAPTION_TOOLTIP = CCBAPI.asResource("gas_storage_contraption_tooltip");
    public static final ResourceLocation GAS_PIPE_TELEMETRY_TOOLTIP = CCBAPI.asResource("gas_pipe_telemetry_tooltip");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(GasStorageContraptionTooltipProvider.INSTANCE, AbstractContraptionEntity.class);
        registration.registerBlockDataProvider(GasStorageTooltipProvider.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(GasPipeTelemetryTooltipProvider.INSTANCE, BlockEntity.class);

        registration.registerBlockDataProvider(BreezeChamberProvider.INSTANCE, BreezeChamberBlockEntity.class);
        registration.registerBlockDataProvider(BreezeCoolerProvider.INSTANCE, BreezeCoolerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addConfig(GAS, true);
        registration.addTooltipCollectedCallback(JadeGoggleTooltip::onTooltipCollected);

        registration.registerEntityComponent(GasStorageContraptionTooltipProvider.INSTANCE, AbstractContraptionEntity.class);
        registration.registerBlockComponent(GasStorageTooltipProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(GasPipeTelemetryTooltipProvider.INSTANCE, Block.class);

        registration.registerBlockComponent(BreezeChamberProvider.INSTANCE, BreezeChamberBlock.class);
        registration.registerBlockComponent(BreezeCoolerProvider.INSTANCE, BreezeCoolerBlock.class);
    }
}
