package net.ty.createcraftedbeginning.content.gasinjectionchamber;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.AdvancementBehaviour;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

public class GasInjectionChamberBlock extends Block implements IBE<GasInjectionChamberBlockEntity> {
    public GasInjectionChamberBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        AdvancementBehaviour.setPlacedBy(world, pos, placer);
    }

    @Override
    public Class<GasInjectionChamberBlockEntity> getBlockEntityClass() {
        return GasInjectionChamberBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GasInjectionChamberBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.GAS_INJECTION_CHAMBER.get();
    }
}
