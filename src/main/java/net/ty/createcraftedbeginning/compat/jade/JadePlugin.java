package net.ty.createcraftedbeginning.compat.jade;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import org.jetbrains.annotations.NotNull;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {
    public static final ResourceLocation GAS = CreateCraftedBeginning.asResource("gas");
    public static final ResourceLocation GAS_BLOCK_TOOLTIP = CreateCraftedBeginning.asResource("gas_block_tooltip");
    public static final ResourceLocation GAS_CONTRAPTION_TOOLTIP = CreateCraftedBeginning.asResource("gas_contraption_tooltip");

    @Override
    public void register(@NotNull IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(GasTooltipContraptionProvider.INSTANCE, AbstractContraptionEntity.class);
        registration.registerBlockDataProvider(GasTooltipProvider.INSTANCE, BlockEntity.class);

        registration.registerBlockDataProvider(BreezeChamberProvider.INSTANCE, BreezeChamberBlockEntity.class);
        registration.registerBlockDataProvider(BreezeCoolerProvider.INSTANCE, BreezeCoolerBlockEntity.class);
    }

    @Override
    public void registerClient(@NotNull IWailaClientRegistration registration) {
        registration.addConfig(GAS, true);

        registration.registerEntityComponent(GasTooltipContraptionProvider.INSTANCE, AbstractContraptionEntity.class);
        registration.registerBlockComponent(GasTooltipProvider.INSTANCE, Block.class);

        registration.registerBlockComponent(BreezeChamberProvider.INSTANCE, BreezeChamberBlock.class);
        registration.registerBlockComponent(BreezeCoolerProvider.INSTANCE, BreezeCoolerBlock.class);
    }
}
