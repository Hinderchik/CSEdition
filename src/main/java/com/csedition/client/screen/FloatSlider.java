package com.csedition.client.screen;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Ползунок с плавающей точкой для экрана настроек прицела.
 */
public class FloatSlider extends AbstractSliderButton {
    private final String label;
    private final float min, max;
    private final java.util.function.Consumer<Float> onChanged;
    private float value;
    private float normValue = 0f;

    public FloatSlider(int x, int y, int w, int h, String label,
                       float initialValue, float min, float max,
                       java.util.function.Consumer<Float> onChanged) {
        super(x, y, w, h, Component.literal(label), 0.0f);
        this.label = label;
        this.min = min;
        this.max = max;
        this.onChanged = onChanged;
        this.value = Mth.clamp(initialValue, min, max);
        this.normValue = (this.value - min) / Math.max(0.001f, (max - min));
        updateMessage();
    }

    @Override
    protected void applyValue() {
        this.value = Mth.clamp(min + this.normValue * (max - min), min, max);
        updateMessage();
        if (onChanged != null) onChanged.accept(this.value);
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.literal(label + ": " + String.format("%.2f", value)));
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        float f = (float) (mouseX - (double) getX() - 4.0F) / (float) (width - 8.0F);
        f = Mth.clamp(f, 0.0F, 1.0F);
        this.normValue = f;
        applyValue();
    }
}