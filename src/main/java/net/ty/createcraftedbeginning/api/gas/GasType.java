package net.ty.createcraftedbeginning.api.gas;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.ty.createcraftedbeginning.api.gas.interfaces.IHasTextComponent;
import net.ty.createcraftedbeginning.api.gas.interfaces.IHasTranslationKey;
import net.ty.createcraftedbeginning.data.CCBDeferredHolder;
import org.jetbrains.annotations.NotNull;

public class GasType<TYPE extends Gas> extends CCBDeferredHolder<Gas, TYPE> implements IHasTextComponent, IHasTranslationKey {
    public GasType(ResourceKey<Gas> key) {
        super(key);
    }

    @NotNull
    @Override
    public Component getTextComponent() {
        return get().getTextComponent();
    }

    @NotNull
    @Override
    public String getTranslationKey() {
        return get().getTranslationKey();
    }
}
