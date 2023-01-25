package me.jellysquid.mods.sodium.client.compat;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderRegistry;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collection;

/**
 * from rubidium
 */
public class FlywheelCompat {

	public static boolean addAndFilterBEs(BlockEntity be) {
		if (SodiumClientMod.flywheelLoaded) {
            if (Backend.canUseInstancing(be.getLevel())) {
                if (InstancedRenderRegistry.canInstance(be.getType()))
                    InstancedRenderDispatcher.getBlockEntities(be.getLevel()).queueAdd(be);
                return !InstancedRenderRegistry.shouldSkipRender(be);
            }
        }
        return true;
    }
	
    public static void filterBlockEntityList(Collection<BlockEntity> blockEntities) {
        if (SodiumClientMod.flywheelLoaded) {
            blockEntities.removeIf(InstancedRenderRegistry::shouldSkipRender);
        }
    }
	
}
