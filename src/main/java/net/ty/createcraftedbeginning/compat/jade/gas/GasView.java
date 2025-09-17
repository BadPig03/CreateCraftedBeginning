package net.ty.createcraftedbeginning.compat.jade.gas;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.ui.IElement;

import java.util.Objects;

public class GasView {
    public IElement overlay;
    public String current;
    public String max;
    public float ratio;
    @Nullable
    public Component gasName;
    @Nullable
    public Component overrideText;
    public boolean creative;

    public GasView(IElement overlay) {
        this.overlay = overlay;
        Objects.requireNonNull(overlay);
    }
}
