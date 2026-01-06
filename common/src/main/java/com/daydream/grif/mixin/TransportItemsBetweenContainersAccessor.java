package com.daydream.grif.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TransportItemsBetweenContainers.class)
public interface TransportItemsBetweenContainersAccessor {
    @Invoker("isPickingUpItems")
    static boolean isPickingUpItems(PathfinderMob mob) {
        throw new UnsupportedOperationException();
    }
    @Invoker("matchesGettingItemsRequirement")
    static boolean matchesGettingItemsRequirement(Container container) {
        throw new UnsupportedOperationException();
    }

    @Invoker("hasItemMatchingHandItem")
    static boolean hasItemMatchingHandItem(PathfinderMob mob, Container container) {
        throw new UnsupportedOperationException();
    }

}
