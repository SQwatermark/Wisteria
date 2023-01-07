package me.jellysquid.mods.sodium.mixin.features.buffer_builder.fast_advance;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder extends DefaultedVertexConsumer implements BufferVertexConsumer {
    @Shadow
    private VertexFormat format;

    @Shadow
    private VertexFormatElement currentElement;

    @Shadow
    private int nextElementByte;

    @Shadow
    private int elementIndex;

    @Shadow
    @Final
    private List<BufferBuilder.DrawState> drawStates;

    /**
     * @author JellySquid
     * @reason Remove modulo operations and recursion
     */
    @Override
    @Overwrite
    public void nextElement() {
        ImmutableList<VertexFormatElement> elements = this.format.getElements();

        do {
            this.nextElementByte += this.currentElement.getByteSize();

            // Wrap around the element pointer without using modulo
            if (++this.elementIndex >= elements.size()) {
                this.elementIndex -= elements.size();
            }

            this.currentElement = elements.get(this.elementIndex);
        } while (this.currentElement.getUsage() == VertexFormatElement.Usage.PADDING);

        if (this.defaultColorSet && this.currentElement.getUsage() == VertexFormatElement.Usage.COLOR) {
            BufferVertexConsumer.super.color(this.defaultR, this.defaultG, this.defaultB, this.defaultA);
        }
    }
}
