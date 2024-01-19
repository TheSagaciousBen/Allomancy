package com.legobmw99.allomancy.modules.powers.client.util;

import com.legobmw99.allomancy.api.data.IAllomancerData;
import com.legobmw99.allomancy.api.enums.Metal;
import com.legobmw99.allomancy.modules.powers.PowerUtils;
import com.legobmw99.allomancy.modules.powers.PowersConfig;
import com.legobmw99.allomancy.modules.powers.data.AllomancerAttachment;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Consumer;

public class SensoryTracking {

    private final List<Entity> metal_entities = new ArrayList<>();
    private final List<MetalBlockBlob> metal_blobs = new ArrayList<>();
    private final List<Player> nearby_allomancers = new ArrayList<>();

    private int tickOffset = 0;
    private final Deque<BlockPos> to_consider = new ArrayDeque<>(20 * 20);
    // trick taken from BlockPos#breadthFirstTraversal used in SpongeBlock
    private final Set<Long> seen = new LongOpenHashSet(20 * 20);

    public void tick() {
        this.tickOffset = (this.tickOffset + 1) % 2;
        if (this.tickOffset == 0) {
            populateSensoryLists();
        }
    }

    public void forEachSeeked(Consumer<Player> f) {
        this.nearby_allomancers.forEach(f);
    }

    public void forEachMetalicEntity(Consumer<Entity> f) {
        this.metal_entities.forEach(f);
    }

    public void forEachMetalBlob(Consumer<MetalBlockBlob> f) {
        this.metal_blobs.forEach(f);
    }

    private void populateSensoryLists() {
        Player player = Minecraft.getInstance().player;
        IAllomancerData data = player.getData(AllomancerAttachment.ALLOMANCY_DATA);

        this.metal_blobs.clear();
        this.metal_entities.clear();
        if (data.isBurning(Metal.IRON) || data.isBurning(Metal.STEEL)) {
            int max = PowersConfig.max_metal_detection.get();
            var negative = player.blockPosition().offset(-max, -max, -max);
            var positive = player.blockPosition().offset(max, max, max);

            // Add metal entities to metal list
            this.metal_entities.addAll(
                    player.level().getEntitiesOfClass(Entity.class, AABB.encapsulatingFullBlocks(negative, positive), e -> PowerUtils.isEntityMetal(e) && !e.equals(player)));

            // Add metal blobs to metal list
            this.seen.clear();
            BlockPos
                    .betweenClosed(negative.getX(), negative.getY(), negative.getZ(), positive.getX(), positive.getY(), positive.getZ())
                    .forEach(starter -> searchNearbyMetalBlocks(player.blockPosition(), max, starter, player.level()));
        }

        // Populate our list of nearby allomancy users
        this.nearby_allomancers.clear();
        if (data.isBurning(Metal.BRONZE) && (data.isEnhanced() || !data.isBurning(Metal.COPPER))) {
            // Add metal burners to a list
            var negative = player.position().add(-30, -30, -30);
            var positive = player.position().add(30, 30, 30);


            var nearby_players = player.level().getEntitiesOfClass(Player.class, new AABB(negative, positive), entity -> entity != null && entity != player);

            for (Player otherPlayer : nearby_players) {
                if (!addSeeked(data, otherPlayer)) {
                    this.nearby_allomancers.clear();
                    break;
                }
            }
        }
    }

    /**
     * A sort of BFS with a global seen list
     */
    private void searchNearbyMetalBlocks(BlockPos origin, int range, BlockPos starter, Level level) {
        var starterState = level.getBlockState(starter);
        if (!this.seen.add(starter.asLong()) || !PowerUtils.isBlockStateMetal(starterState)) {
            return;
        }
        var blob = new MetalBlockBlob(starter, starterState);


        int range_sqr = 4 * range * range;

        this.to_consider.clear();
        this.to_consider.addFirst(starter);


        while (!this.to_consider.isEmpty()) {
            var pos = this.to_consider.removeLast();
            for (var next : BlockPos.withinManhattan(pos, 1, 1, 1)) {
                if (this.seen.add(next.asLong()) && origin.distToCenterSqr(next.getCenter()) < range_sqr) {
                    var nextState = level.getBlockState(next);
                    if (PowerUtils.isBlockStateMetal(nextState)) {
                        blob.add(next, nextState);
                        this.to_consider.add(next.immutable());
                    }
                }
            }
        }
        this.metal_blobs.add(blob);
    }

    private boolean addSeeked(IAllomancerData data, Player otherPlayer) {
        var otherData = otherPlayer.getData(AllomancerAttachment.ALLOMANCY_DATA);
        if (otherData.isBurning(Metal.COPPER) && (!data.isEnhanced() || otherData.isEnhanced())) {
            return false;
        }

        if (Arrays.stream(Metal.values()).anyMatch(otherData::isBurning)) {
            this.nearby_allomancers.add(otherPlayer);
        }

        return true;
    }


    public static class MetalBlockBlob {

        private static final Level level = Minecraft.getInstance().level;
        private int blocks;
        private Vec3 center;

        public MetalBlockBlob(BlockPos initial, BlockState initialState) {
            this.blocks = 1;
            this.center = getCenterOfBlock(initial, initialState);
        }

        private static Vec3 getCenterOfBlock(BlockPos pos, BlockState state) {
            var shape = state.getShape(level, pos);
            if (shape.isEmpty()) {
                return Vec3.atCenterOf(pos);
            }
            return Vec3.atLowerCornerOf(pos).add(shape.bounds().getCenter());
        }

        public int size() {
            return this.blocks;
        }

        public void add(BlockPos pos, BlockState state) {
            this.blocks += 1;
            this.center = this.center.scale(this.blocks - 1).add(getCenterOfBlock(pos, state)).scale(1.0D / this.blocks);
        }

        public Vec3 getCenter() {
            return this.center;
        }

        @Override
        public String toString() {
            return "MetalBlockBlob{" + "blocks=" + this.blocks + ", center=" + this.center + '}';
        }
    }
}
