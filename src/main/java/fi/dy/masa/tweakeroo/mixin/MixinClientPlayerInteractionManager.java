package fi.dy.masa.tweakeroo.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import fi.dy.masa.tweakeroo.config.Configs;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import fi.dy.masa.tweakeroo.tweaks.PlacementTweaks;
import fi.dy.masa.tweakeroo.util.InventoryUtils;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManager
{
    @Inject(method = "interactItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;syncSelectedSlot()V"),
            cancellable = true)
    private void onProcessRightClickFirst(PlayerEntity player, World worldIn, Hand hand, CallbackInfoReturnable<ActionResult> cir)
    {
        if (PlacementTweaks.onProcessRightClickPre(player, hand))
        {
            cir.setReturnValue(ActionResult.PASS);
            cir.cancel();
        }
    }

    @Inject(method = "interactItem",
            slice = @Slice(from = @At(value = "INVOKE", 
                                      target = "Lnet/minecraft/item/ItemStack;use(" +
                                               "Lnet/minecraft/world/World;" +
                                               "Lnet/minecraft/entity/player/PlayerEntity;" +
                                               "Lnet/minecraft/util/Hand;" +
                                               ")Lnet/minecraft/util/TypedActionResult;")),
            at = @At("RETURN"))
    private void onProcessRightClickPost(PlayerEntity player, World worldIn, Hand hand, CallbackInfoReturnable<ActionResult> cir)
    {
        PlacementTweaks.onProcessRightClickPost(player, hand);
    }

    @Inject(method = "interactEntity(" +
                     "Lnet/minecraft/entity/player/PlayerEntity;" +
                     "Lnet/minecraft/entity/Entity;" +
                     "Lnet/minecraft/util/Hand;" +
                     ")Lnet/minecraft/util/ActionResult;",
            at = @At("HEAD"),
            cancellable = true)
    private void onRightClickMouseOnEntityPre1(PlayerEntity player, Entity target, Hand hand, CallbackInfoReturnable<ActionResult> cir)
    {
        if (PlacementTweaks.onProcessRightClickPre(player, hand))
        {
            cir.setReturnValue(ActionResult.PASS);
            cir.cancel();
        }
    }

    @Inject(method = "interactEntityAtLocation(" +
                     "Lnet/minecraft/entity/player/PlayerEntity;" +
                     "Lnet/minecraft/entity/Entity;" +
                     "Lnet/minecraft/util/hit/EntityHitResult;" +
                     "Lnet/minecraft/util/Hand;" +
                     ")Lnet/minecraft/util/ActionResult;",
            at = @At("HEAD"),
            cancellable = true)
    private void onRightClickMouseOnEntityPre2(PlayerEntity player, Entity target, EntityHitResult trace, Hand hand, CallbackInfoReturnable<ActionResult> cir)
    {
        if (PlacementTweaks.onProcessRightClickPre(player, hand))
        {
            cir.setReturnValue(ActionResult.PASS);
            cir.cancel();
        }
    }

    @Inject(method = "attackBlock",
            slice = @Slice(from = @At(value = "FIELD", ordinal = 0,
                                      target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;breakingBlock:Z")),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getBlockState(" +
                                                "Lnet/minecraft/util/math/BlockPos;" +
                                                ")Lnet/minecraft/block/BlockState;", ordinal = 0))
    private void onClickBlockPre(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir)
    {
        InventoryUtils.trySwitchToEffectiveTool(pos);
        PlacementTweaks.cacheStackInHand(Hand.MAIN_HAND);
    }


    @Inject(method = "interactBlock",
            at = @At(value = "HEAD"))
    private void onClickBlockPre(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<Boolean> cir)
    {
        InventoryUtils.trySwitchToBlock(hitResult.getBlockPos());
        PlacementTweaks.cacheStackInHand(Hand.MAIN_HAND);
    }

    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void handleBreakingRestriction1(BlockPos pos, Direction side, CallbackInfoReturnable<Boolean> cir)
    {
        if (PlacementTweaks.isPositionAllowedByBreakingRestriction(pos, side) == false)
        {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"), cancellable = true) // MCP: onPlayerDamageBlock
    private void handleBreakingRestriction2(BlockPos pos, Direction side, CallbackInfoReturnable<Boolean> cir)
    {
        if (PlacementTweaks.isPositionAllowedByBreakingRestriction(pos, side) == false)
        {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getReachDistance", at = @At("HEAD"), cancellable = true)
    private void overrideReachDistance(CallbackInfoReturnable<Float> cir)
    {
        if (FeatureToggle.TWEAK_BLOCK_REACH_OVERRIDE.getBooleanValue())
        {
            cir.setReturnValue((float) Configs.Generic.BLOCK_REACH_DISTANCE.getDoubleValue());
            cir.cancel();
        }
    }

    @Inject(method = "hasExtendedReach", at = @At("HEAD"), cancellable = true)
    private void overrideExtendedReach(CallbackInfoReturnable<Boolean> cir)
    {
        if (FeatureToggle.TWEAK_BLOCK_REACH_OVERRIDE.getBooleanValue())
        {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
