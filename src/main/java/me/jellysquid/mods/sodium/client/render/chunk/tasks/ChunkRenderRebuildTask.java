package me.jellysquid.mods.sodium.client.render.chunk.tasks;

import me.jellysquid.mods.sodium.client.compat.FlywheelCompat;
import me.jellysquid.mods.sodium.client.gl.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheLocal;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

import java.util.EnumMap;
import java.util.Map;

/**
 * Rebuilds all the meshes of a chunk for each given render pass with non-occluded blocks. The result is then uploaded
 * to graphics memory on the main thread.
 *
 * This task takes a slice of the world from the thread it is created on. Since these slices require rather large
 * array allocations, they are pooled to ensure that the garbage collector doesn't become overloaded.
 */
public class ChunkRenderRebuildTask extends ChunkRenderBuildTask {
    private final RenderSection render;
    private final ChunkRenderContext renderContext;
    private final int frame;

    public ChunkRenderRebuildTask(RenderSection render, ChunkRenderContext renderContext, int frame) {
        this.render = render;
        this.renderContext = renderContext;
        this.frame = frame;
    }

    @Override
    public ChunkBuildResult performBuild(ChunkBuildContext buildContext, CancellationSource cancellationSource) {
        ChunkRenderData.Builder renderData = new ChunkRenderData.Builder();
        VisGraph occluder = new VisGraph();
        ChunkRenderBounds.Builder bounds = new ChunkRenderBounds.Builder();

        ChunkBuildBuffers buffers = buildContext.buffers;
        buffers.init(renderData, this.render.getChunkId());

        ChunkRenderCacheLocal cache = buildContext.cache;
        cache.init(this.renderContext);

        WorldSlice slice = cache.getWorldSlice();

        int minX = this.render.getOriginX();
        int minY = this.render.getOriginY();
        int minZ = this.render.getOriginZ();

        int maxX = minX + 16;
        int maxY = minY + 16;
        int maxZ = minZ + 16;

        assert Minecraft.getInstance().level != null;
        Map<BlockPos, IModelData> modelDataMap = ModelDataManager.getModelData(Minecraft.getInstance().level, new ChunkPos(SectionPos.blockToSectionCoord(minX), SectionPos.blockToSectionCoord(minZ)));

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos offset = new BlockPos.MutableBlockPos();

        for (int y = minY; y < maxY; y++) {
            if (cancellationSource.isCancelled()) {
                return null;
            }

            for (int z = minZ; z < maxZ; z++) {
                for (int x = minX; x < maxX; x++) {
                    BlockState blockState = slice.getBlockState(x, y, z);

                    if (blockState.isAir()) {
                        continue;
                    }

                    blockPos.set(x, y, z);
                    offset.set(x & 15, y & 15, z & 15);

                    boolean rendered = false;

                    if (blockState.getRenderShape() == RenderShape.MODEL) {

                        for (RenderType chunkBufferLayer : RenderType.chunkBufferLayers()) {
                            if (!ItemBlockRenderTypes.canRenderInLayer(blockState, chunkBufferLayer)) {
                                continue;
                            }

                            ForgeHooksClient.setRenderType(chunkBufferLayer);
                            IModelData modelData = modelDataMap.getOrDefault(blockPos, EmptyModelData.INSTANCE);

                            BakedModel model = cache.getBlockModels().getBlockModel(blockState);

                            long seed = blockState.getSeed(blockPos);

                            if (cache.getBlockRenderer().renderModel(slice, blockState, blockPos, offset, model, buffers.get(chunkBufferLayer), true, seed, modelData)) {
                                rendered = true;
                            }
                        }

                    }

                    FluidState fluidState = blockState.getFluidState();

                    if (!fluidState.isEmpty()) {
                        for (RenderType chunkBufferLayer : RenderType.chunkBufferLayers()) {
                            if (!ItemBlockRenderTypes.canRenderInLayer(fluidState, chunkBufferLayer)) {
                                continue;
                            }

                            ForgeHooksClient.setRenderType(chunkBufferLayer);

                            if (cache.getFluidRenderer().render(slice, fluidState, blockPos, offset, buffers.get(chunkBufferLayer))) {
                                rendered = true;
                            }

                        }
                    }

                    if (blockState.hasBlockEntity()) {
                        BlockEntity entity = slice.getBlockEntity(blockPos);

                        if (entity != null) {
                            BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity);

                            if (renderer != null && FlywheelCompat.addAndFilterBEs(entity)) {
                                renderData.addBlockEntity(entity, !renderer.shouldRenderOffScreen(entity));
                                rendered = true;
                            }
                        }
                    }

                    if (blockState.isSolidRender(slice, blockPos)) {
                        occluder.setOpaque(blockPos);
                    }

                    if (rendered) {
                        bounds.addBlock(x & 15, y & 15, z & 15);
                    }
                }
            }
        }

        ForgeHooksClient.setRenderType(null);

        Map<BlockRenderPass, ChunkMeshData> meshes = new EnumMap<>(BlockRenderPass.class);

        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            ChunkMeshData mesh = buffers.createMesh(pass);

            if (mesh != null) {
                meshes.put(pass, mesh);
            }
        }

        renderData.setOcclusionData(occluder.resolve());
        renderData.setBounds(bounds.build(this.render.getChunkPos()));

        return new ChunkBuildResult(this.render, renderData.build(), meshes, this.frame);
    }
}
