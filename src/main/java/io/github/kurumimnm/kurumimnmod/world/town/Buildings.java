package io.github.kurumimnm.kurumimnmod.world.town;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.util.RandomSource;

/**
 * 街の建物・設備を設計図（{@link TownPlan.Builder}）に書き込む関数の集まり。
 * <p>
 * 家などは「正面（ドア）が北（-Z）を向いた向き」で作り、{@link Placer} で回転・移動して置く。
 * 高さ 0 = 街の地面のブロック（その上の 1 から建物が建つ）。
 */
public final class Buildings {

    private Buildings() {}

    /**
     * 建物を置く位置と向き。ox, oz = 建物の左手前の角、facing = 正面（ドア）が向く方角。
     * 建物の中の座標 (lx, lz) を、ワールドの座標に変換する。
     */
    public static final class Placer {
        private final TownPlan.Builder b;
        private final int ox;
        private final int oz;
        private final int width;
        private final int depth;
        private final Rotation rotation;

        public Placer(TownPlan.Builder b, int ox, int oz, int width, int depth, Direction facing) {
            this.b = b;
            this.ox = ox;
            this.oz = oz;
            this.width = width;
            this.depth = depth;
            this.rotation = switch (facing) {
                case EAST -> Rotation.CLOCKWISE_90;
                case SOUTH -> Rotation.CLOCKWISE_180;
                case WEST -> Rotation.COUNTERCLOCKWISE_90;
                default -> Rotation.NONE;
            };
        }

        /** 回転後の、建物が占める X 方向の幅。 */
        public int worldWidth() {
            return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90 ? depth : width;
        }

        public int worldDepth() {
            return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90 ? width : depth;
        }

        /** 建物の中の座標 (lx, y, lz) にブロックを置く。y は地面からの高さ。 */
        public void set(int lx, int y, int lz, BlockState state) {
            int wx;
            int wz;
            switch (rotation) {
                case CLOCKWISE_90 -> { wx = depth - 1 - lz; wz = lx; }
                case CLOCKWISE_180 -> { wx = width - 1 - lx; wz = depth - 1 - lz; }
                case COUNTERCLOCKWISE_90 -> { wx = lz; wz = width - 1 - lx; }
                default -> { wx = lx; wz = lz; }
            }
            b.set(ox + wx, b.groundY() + y, oz + wz, state.rotate(rotation));
        }

        public void fill(int lx0, int y0, int lz0, int lx1, int y1, int lz1, BlockState state) {
            for (int x = Math.min(lx0, lx1); x <= Math.max(lx0, lx1); x++) {
                for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                    for (int z = Math.min(lz0, lz1); z <= Math.max(lz0, lz1); z++) {
                        set(x, y, z, state);
                    }
                }
            }
        }
    }

    // =====================================================================
    // 家
    // =====================================================================

    /**
     * 切妻屋根の家。width = 正面の幅、depth = 奥行き、floors = 階数。
     * 正面中央にドア、壁の途中に窓、中に作業台・樽・ランタン。
     */
    public static void house(Placer p, TownStyle s, int width, int depth, int floors, RandomSource random) {
        int wallTop = floors * 4;
        BlockState planks = s.planks().defaultBlockState();
        BlockState log = s.log().defaultBlockState().trySetValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();

        // 土台・床
        p.fill(0, 0, 0, width - 1, 0, depth - 1, s.foundation().defaultBlockState());
        // 壁と中の空間
        for (int y = 1; y <= wallTop; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    boolean edgeX = x == 0 || x == width - 1;
                    boolean edgeZ = z == 0 || z == depth - 1;
                    if (edgeX && edgeZ) {
                        p.set(x, y, z, log);
                    } else if (edgeX || edgeZ) {
                        p.set(x, y, z, wall(s, x, y, z, edgeX, planks, log));
                    } else {
                        p.set(x, y, z, y % 4 == 0 ? planks : air); // 2 階以上の床
                    }
                }
            }
        }
        // 窓（各階の高さ 2）
        for (int f = 0; f < floors; f++) {
            int wy = f * 4 + 2;
            for (int x = 2; x < width - 2; x += 2) {
                p.set(x, wy, 0, glass);
                p.set(x, wy, depth - 1, glass);
            }
            for (int z = 2; z < depth - 2; z += 2) {
                p.set(0, wy, z, glass);
                p.set(width - 1, wy, z, glass);
            }
        }
        // ドア（正面中央）
        int doorX = width / 2;
        door(p, s.door(), doorX, 1, 0, Direction.NORTH);
        p.set(doorX, 3, 0, planks);
        // 屋根（棟は左右方向＝幅の方向）
        gableRoof(p, s, width, depth, wallTop + 1);
        // 中の家具
        p.set(1, 1, depth - 2, Blocks.CRAFTING_TABLE.defaultBlockState());
        p.set(width - 2, 1, depth - 2, Blocks.BARREL.defaultBlockState());
        p.set(width - 2, 1, 1, Blocks.LANTERN.defaultBlockState());
        if (floors >= 2) {
            // 2 階へのはしご
            for (int y = 1; y <= 4; y++) {
                p.set(1, y, 1, Blocks.LADDER.defaultBlockState().trySetValue(LadderBlock.FACING, Direction.SOUTH));
            }
        }
    }

    /**
     * 壁のブロック。中世の家（{@link TownStyle#lowerWall()}・{@link TownStyle#timbered()}）は
     * 1 階を石造りにし、2 階より上は漆喰の壁に木の梁（階の境目）と柱（3 ブロックおき）を見せる。
     */
    private static BlockState wall(TownStyle s, int x, int y, int z, boolean sideWall, BlockState planks, BlockState log) {
        if (y <= 4 && s.lowerWall() != null) {
            return s.lowerWall().defaultBlockState();
        }
        if (s.timbered()) {
            if (y % 4 == 0 || y % 4 == 1) {
                // 階の境目の梁（横向きの丸太）
                return s.log().defaultBlockState().trySetValue(RotatedPillarBlock.AXIS, sideWall ? Direction.Axis.Z : Direction.Axis.X);
            }
            if ((sideWall ? z : x) % 3 == 0) {
                return log; // 柱
            }
        }
        return planks;
    }

    /** 切妻屋根。棟は lx 方向。y0 = 屋根の一番下の高さ。 */
    public static void gableRoof(Placer p, TownStyle s, int width, int depth, int y0) {
        BlockState fillBlock = s.roofBlock().defaultBlockState();
        int layers = (depth + 1) / 2;
        for (int k = 0; k < layers; k++) {
            int y = y0 + k;
            int front = k;
            int back = depth - 1 - k;
            for (int x = -1; x <= width; x++) {
                if (front == back) {
                    p.set(x, y, front, s.roofBlock().defaultBlockState());
                    continue;
                }
                p.set(x, y, front, stairs(s.roofStairs(), Direction.SOUTH));
                p.set(x, y, back, stairs(s.roofStairs(), Direction.NORTH));
            }
            // 妻（三角の壁）
            for (int z = front + 1; z < back; z++) {
                p.set(0, y, z, fillBlock);
                p.set(width - 1, y, z, fillBlock);
            }
        }
    }

    /** 大きな建物（宿屋・兵舎・倉庫など）。家を大きくしたもの。 */
    public static void hall(Placer p, TownStyle s, int width, int depth, int floors, RandomSource random) {
        house(p, s, width, depth, floors, random);
        // 正面の両脇にランタン
        int doorX = width / 2;
        p.set(doorX - 1, 3, -1, Blocks.LANTERN.defaultBlockState().trySetValue(LanternBlock.HANGING, false));
        p.set(doorX + 1, 3, -1, Blocks.LANTERN.defaultBlockState().trySetValue(LanternBlock.HANGING, false));
    }

    /** 屋台（市場）。柵の柱と色の付いた布の屋根。 */
    public static void stall(Placer p, TownStyle s, String woolColor) {
        BlockState fence = s.fence().defaultBlockState();
        BlockState wool = TownStyle.state(woolColor + "_wool");
        for (int y = 1; y <= 3; y++) {
            p.set(0, y, 0, fence);
            p.set(4, y, 0, fence);
            p.set(0, y, 2, fence);
            p.set(4, y, 2, fence);
        }
        p.fill(0, 4, 0, 4, 4, 2, wool);
        p.fill(1, 1, 1, 3, 1, 1, Blocks.BARREL.defaultBlockState());
    }

    /** 見張り塔。size × size、高さ height。上に胸壁とランタン。 */
    public static void tower(Placer p, TownStyle s, int size, int height) {
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int y = 1; y <= height; y++) {
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    boolean edge = x == 0 || z == 0 || x == size - 1 || z == size - 1;
                    p.set(x, y, z, edge ? wall : (y == height ? s.planks().defaultBlockState() : air));
                }
            }
        }
        // 胸壁（ひとつおき）
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                boolean edge = x == 0 || z == 0 || x == size - 1 || z == size - 1;
                if (edge && (x + z) % 2 == 0) {
                    p.set(x, height + 1, z, wall);
                }
            }
        }
        p.set(size / 2, height + 1, size / 2, Blocks.LANTERN.defaultBlockState());
        // 入口とはしご
        door(p, Blocks.SPRUCE_DOOR, size / 2, 1, 0, Direction.NORTH);
        for (int y = 1; y < height; y++) {
            p.set(size / 2, y, size - 2, Blocks.LADDER.defaultBlockState().trySetValue(LadderBlock.FACING, Direction.NORTH));
        }
    }

    /** 灯台（港町）。 */
    public static void lighthouse(Placer p, int height) {
        BlockState white = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState red = TownStyle.state("red_terracotta");
        for (int y = 1; y <= height; y++) {
            BlockState ring = (y / 4) % 2 == 0 ? white : red;
            for (int x = 0; x < 5; x++) {
                for (int z = 0; z < 5; z++) {
                    boolean corner = (x == 0 || x == 4) && (z == 0 || z == 4);
                    boolean edge = x == 0 || z == 0 || x == 4 || z == 4;
                    if (!corner && edge) {
                        p.set(x, y, z, ring);
                    }
                }
            }
        }
        p.fill(1, height + 1, 1, 3, height + 1, 3, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        p.set(2, height + 2, 2, Blocks.GLOWSTONE.defaultBlockState());
        p.set(1, height + 2, 1, Blocks.GLASS.defaultBlockState());
        p.set(3, height + 2, 1, Blocks.GLASS.defaultBlockState());
        p.set(1, height + 2, 3, Blocks.GLASS.defaultBlockState());
        p.set(3, height + 2, 3, Blocks.GLASS.defaultBlockState());
        p.fill(1, height + 3, 1, 3, height + 3, 3, red);
    }

    /** 畑（村）。width × depth、真ん中の列に水。 */
    public static void farm(Placer p, int width, int depth, RandomSource random) {
        BlockState log = Blocks.OAK_LOG.defaultBlockState().trySetValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                boolean border = x == 0 || z == 0 || x == width - 1 || z == depth - 1;
                if (border) {
                    p.set(x, 0, z, log);
                } else if (z == depth / 2) {
                    p.set(x, 0, z, Blocks.WATER.defaultBlockState());
                } else {
                    p.set(x, 0, z, Blocks.FARMLAND.defaultBlockState());
                    p.set(x, 1, z, Blocks.WHEAT.defaultBlockState().trySetValue(CropBlock.AGE, 3 + random.nextInt(5)));
                }
            }
        }
    }

    // =====================================================================
    // 広場の真ん中
    // =====================================================================

    /** 井戸（村）。 */
    public static void well(TownPlan.Builder b, int cx, int cz) {
        int y = b.groundY();
        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                b.set(cx + dx, y, cz + dz, edge ? cobble : Blocks.WATER.defaultBlockState());
                b.set(cx + dx, y - 1, cz + dz, edge ? cobble : Blocks.WATER.defaultBlockState());
                b.set(cx + dx, y - 2, cz + dz, cobble);
                if (edge) {
                    b.set(cx + dx, y + 1, cz + dz, cobble);
                }
            }
        }
        for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            b.fill(cx + c[0], y + 2, cz + c[1], cx + c[0], y + 3, cz + c[1], Blocks.OAK_FENCE.defaultBlockState());
        }
        b.fill(cx - 2, y + 4, cz - 2, cx + 2, y + 4, cz + 2, slab(Blocks.COBBLESTONE_SLAB));
    }

    /** 噴水（街・都市・港町）。 */
    public static void fountain(TownPlan.Builder b, int cx, int cz) {
        int y = b.groundY();
        BlockState rim = Blocks.STONE_BRICKS.defaultBlockState();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= 4.5 && d > 3.5) {
                    b.set(cx + dx, y + 1, cz + dz, rim);
                } else if (d <= 3.5) {
                    b.set(cx + dx, y, cz + dz, rim);
                    b.set(cx + dx, y + 1, cz + dz, Blocks.WATER.defaultBlockState());
                }
            }
        }
        b.fill(cx, y + 1, cz, cx, y + 4, cz, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        b.set(cx, y + 5, cz, Blocks.WATER.defaultBlockState());
    }

    /** 記念碑（神殿都市・隠れ里）。 */
    public static void monument(TownPlan.Builder b, int cx, int cz, int height) {
        int y = b.groundY();
        b.fill(cx - 2, y + 1, cz - 2, cx + 2, y + 1, cz + 2, Blocks.QUARTZ_BLOCK.defaultBlockState());
        b.fill(cx - 1, y + 2, cz - 1, cx + 1, y + 2, cz + 1, Blocks.QUARTZ_BLOCK.defaultBlockState());
        b.fill(cx, y + 3, cz, cx, y + 3 + height, cz, Blocks.QUARTZ_PILLAR.defaultBlockState());
        b.set(cx, y + 4 + height, cz, Blocks.SEA_LANTERN.defaultBlockState());
    }

    /** たき火と訓練用の的（前線都市の広場）。 */
    public static void campfire(TownPlan.Builder b, int cx, int cz) {
        int y = b.groundY();
        b.set(cx, y + 1, cz, Blocks.CAMPFIRE.defaultBlockState());
        for (int[] c : new int[][]{{-4, 0}, {4, 0}, {0, -4}, {0, 4}}) {
            b.set(cx + c[0], y + 1, cz + c[1], Blocks.HAY_BLOCK.defaultBlockState());
            b.set(cx + c[0], y + 2, cz + c[1], Blocks.TARGET.defaultBlockState());
        }
    }

    // =====================================================================
    // 部品
    // =====================================================================

    public static BlockState stairs(Block block, Direction facing) {
        return block.defaultBlockState().trySetValue(StairBlock.FACING, facing);
    }

    public static BlockState slab(Block block) {
        return block.defaultBlockState().trySetValue(SlabBlock.TYPE, SlabType.BOTTOM);
    }

    /** ドア（下と上の 2 ブロック）。facing はドアが向く方角（建物の中の向き）。 */
    public static void door(Placer p, Block door, int lx, int y, int lz, Direction facing) {
        BlockState lower = door.defaultBlockState().trySetValue(DoorBlock.FACING, facing.getOpposite())
                .trySetValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        p.set(lx, y, lz, lower);
        p.set(lx, y + 1, lz, lower.trySetValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }
}
