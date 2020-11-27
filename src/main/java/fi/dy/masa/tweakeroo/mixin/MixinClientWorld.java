package fi.dy.masa.tweakeroo.mixin;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import fi.dy.masa.tweakeroo.config.Configs;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld extends World
{
    protected MixinClientWorld(MutableWorldProperties properties, RegistryKey<World> registryKey, DimensionType dimensionType, Supplier<Profiler> supplier, boolean bl, boolean bl2, long l)
    {
        super(properties, registryKey, dimensionType, supplier, bl, bl2, l);
    }


    private Stream<BlockPos> allAdjacentBlocks(BlockPos pos) {
        return ImmutableList.of(pos.up(), pos.down(), pos.north(), pos.south(), pos.east(), pos.west()).stream();
    }

    private String getFullNamespace(Block b) {
        Identifier id = Registry.BLOCK.getId(b);
        return id.getNamespace() + ":" + id.getPath();
    }

    @Inject(method = "updateListeners", at = @At("TAIL"))
    private void updateListeners(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        if (!oldState.isAir() && newState.isAir()) {
            if (FeatureToggle.TWEAK_ANNOUNCE_EXPOSED_BLOCKS.getBooleanValue()) {

                if (allAdjacentBlocks(pos)
                        .filter(b -> Configs.Lists.ANNOUNCE_BLOCKS.getStrings().contains(getFullNamespace(getBlockState(b).getBlock())))
                        .anyMatch(b -> allAdjacentBlocks(b).filter(p -> getBlockState(p).isTranslucent(this, p) || getBlockState(p).isAir())
                                .count() < 2)) {
                    playSound(MinecraftClient.getInstance().player, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1f, 1f);
                }
            }
        }
    }

    @Override
    public void tickEntity(Consumer<Entity> consumer, Entity entity)
    {
        if (Configs.Disable.DISABLE_CLIENT_ENTITY_UPDATES.getBooleanValue() == false || entity instanceof PlayerEntity)
        {
            super.tickEntity(consumer, entity);
        }
    }

    @Inject(method = "addEntitiesToChunk", at = @At("HEAD"), cancellable = true)
    private void fixChunkEntityLeak(WorldChunk chunk, CallbackInfo ci)
    {
        if (Configs.Fixes.CLIENT_CHUNK_ENTITY_DUPE.getBooleanValue())
        {
            for (int y = 0; y < 16; ++y)
            {
                // The chunk already has entities, which means it's a re-used existing chunk,
                // in such a case we don't want to add the from the world entities again, otherwise
                // they are basically duped within the Chunk.
                if (chunk.getEntitySectionArray()[y].size() > 0)
                {
                    ci.cancel();
                    return;
                }
            }
        }
    }
}
