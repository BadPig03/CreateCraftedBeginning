package net.ty.createcraftedbeginning.content.breezes;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BreezeCoreItem extends Item {
    public BreezeCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
		Level level = entity.level();
        if (level.isClientSide || entity.isNoGravity()) {
			return false;
		}

        entity.setNoGravity(true);
		return false;
	}
}
