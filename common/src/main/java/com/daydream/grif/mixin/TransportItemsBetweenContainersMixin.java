package com.daydream.grif.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(TransportItemsBetweenContainers.class)
public class TransportItemsBetweenContainersMixin {
    private static Stream<ItemFrame> getAttachedItemFrames(Level level, BlockPos pos) {
        AABB box = new AABB(pos).inflate(1.0);

        Predicate<Entity> attachedToPos = frame -> {
            Direction facing = frame.getDirection();
            BlockPos attached = frame.blockPosition().relative(facing.getOpposite());
            return attached.equals(pos);
        };

        return Stream.concat(
                level.getEntitiesOfClass(ItemFrame.class, box, attachedToPos).stream(),
                level.getEntitiesOfClass(GlowItemFrame.class, box, attachedToPos).stream()
                        .map(gf -> (ItemFrame) gf)
        );
    }
    private static boolean mobCanPlaceInContainerCheck(PathfinderMob mob, Container container) {
        if (container instanceof BlockEntity blockEntityContainer) {
            assert blockEntityContainer.getLevel() != null;
            var frameItems = getAttachedItemFrames(blockEntityContainer.getLevel(), blockEntityContainer.getBlockPos())
                    .map(ItemFrame::getItem)
                    .filter(item -> !item.isEmpty()).toList();

            for (var item : frameItems) {
                if (ItemStack.isSameItem(item, mob.getMainHandItem())) {
                    return true;
                }
            }

            if (!frameItems.isEmpty()) return false; // We don't want to do the default action if there is a filter.
        }
        if (container.isEmpty()) return true;
        return TransportItemsBetweenContainersAccessor.hasItemMatchingHandItem(mob, container);
    }

    @Inject(at = @At("HEAD"), method = "doReachedTargetInteraction", cancellable = true)
    private void overrideDoReachedTargetInteraction(PathfinderMob mob, Container container, BiConsumer<PathfinderMob,
        Container> pickupItem, BiConsumer<PathfinderMob, Container> pickupNoItem, BiConsumer<PathfinderMob, Container> placeItem,
        BiConsumer<PathfinderMob, Container> placeNoItem, CallbackInfo info) {

        if (TransportItemsBetweenContainersAccessor.isPickingUpItems(mob)) {
            if (TransportItemsBetweenContainersAccessor.matchesGettingItemsRequirement(container)) {
                pickupItem.accept(mob, container);
            } else {
                pickupNoItem.accept(mob, container);
            }
        } else if (mobCanPlaceInContainerCheck(mob, container)) {
            placeItem.accept(mob, container);
        } else {
            placeNoItem.accept(mob, container);
        }

        info.cancel();
    }

    @Inject(at = @At("TAIL"), method = "isTargetValidToPick", cancellable = true)
    private void appendIsTargetValidToPick(PathfinderMob mob, Level level, BlockEntity blockEntity, Set<GlobalPos> visited,
        Set<GlobalPos> unreachable, AABB searchArea, CallbackInfoReturnable<TransportItemsBetweenContainers.TransportItemTarget> info) {
        var returned = info.getReturnValue();
        if (returned != null) {
            if (!mobCanPlaceInContainerCheck(mob, returned.container())) {
                info.setReturnValue(null); // Don't go to a chest we can't place into.
            }
        }
    }
}
