package me.jellysquid.mods.sodium.client.model.quad.blender;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.jetbrains.annotations.Nullable;

public interface ColorSampler<T> {
    int getColor(T var1, @Nullable BlockAndTintGetter var2, @Nullable BlockPos var3, int var4);
}
