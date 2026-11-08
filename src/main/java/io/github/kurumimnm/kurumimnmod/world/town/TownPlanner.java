package io.github.kurumimnm.kurumimnmod.world.town;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 街の設計図（{@link TownPlan}）を作る。街の種類（地図 JSON の街の {@code type}）ごとに、
 * 道・広場・城壁・建物の並べ方を変える。同じ街なら毎回同じ設計図になる（街の id から乱数を作る）。
 *
 * <h2>街の種類</h2>
 * <table>
 *   <tr><td>hidden_village</td><td>隠れ里</td><td>壁なし・家が少し・記念碑</td></tr>
 *   <tr><td>village</td><td>村</td><td>壁なし・家が少し・井戸・畑</td></tr>
 *   <tr><td>post_town</td><td>宿場町</td><td>大きな宿屋・家・井戸</td></tr>
 *   <tr><td>mining_town</td><td>鉱山町</td><td>柵・家・坑道の入口</td></tr>
 *   <tr><td>town</td><td>中位の街</td><td>城壁・家・噴水</td></tr>
 *   <tr><td>start_town</td><td>始まりの街</td><td>城壁と見張り塔・広場の噴水・ギルド（クエスト受付）・ショップと市場・宿屋・自分の家</td></tr>
 *   <tr><td>hub</td><td>中間拠点</td><td>壁なし・広場・市場・宿屋（出発ゲートは {@code QuestPlotFeature} が置く）</td></tr>
 *   <tr><td>castle_town</td><td>城下町</td><td>城壁・真ん中に城・家</td></tr>
 *   <tr><td>temple_city</td><td>神殿都市</td><td>城壁・神殿・記念碑</td></tr>
 *   <tr><td>city</td><td>大きな街</td><td>城壁・家がぎっしり・市場・噴水</td></tr>
 *   <tr><td>frontline_city</td><td>前線都市</td><td>高い城壁と見張り塔・兵舎・訓練場</td></tr>
 * </table>
 * ※ 試作。見た目は後で作り込む前提。
 */
public final class TownPlanner {

    private TownPlanner() {}

    /** 作った設計図（街ごと）。 */
    private static final Map<String, TownPlan> CACHE = new ConcurrentHashMap<>();

    public static TownPlan plan(TownSpec town) {
        return CACHE.computeIfAbsent(town.toString(), k -> build(town));
    }

    // 種類ごとの設定
    private record Kind(boolean walls, int wallHeight, int streetWidth, int plazaRadius, double density, int maxFloors) {}

    private static Kind kind(String type) {
        return switch (type) {
            case "hidden_village" -> new Kind(false, 0, 3, 7, 0.35, 1);
            case "village" -> new Kind(false, 0, 3, 6, 0.4, 1);
            case "post_town" -> new Kind(false, 0, 3, 7, 0.5, 2);
            case "mining_town" -> new Kind(false, 0, 3, 7, 0.55, 2);
            case "start_town" -> new Kind(true, 9, 5, 11, 0.75, 2);
            case "hub" -> new Kind(false, 0, 4, 9, 0.3, 1);
            case "castle_town" -> new Kind(true, 8, 5, 0, 0.7, 2);
            case "temple_city" -> new Kind(true, 7, 5, 9, 0.55, 2);
            case "city" -> new Kind(true, 8, 5, 10, 0.95, 3);
            case "frontline_city" -> new Kind(true, 11, 5, 8, 0.6, 2);
            default -> new Kind(true, 6, 4, 8, 0.65, 2); // town
        };
    }

    private static TownPlan build(TownSpec town) {
        int groundY = town.groundY();
        TownPlan.Builder b = new TownPlan.Builder(groundY);
        RandomSource random = RandomSource.create(town.id().hashCode() * 31L);
        Kind kind = kind(town.type());
        TownStyle style = TownStyle.of(town.style());
        int cx = town.x();
        int cz = town.z();
        int r = town.radius();
        Occupancy occupancy = new Occupancy();

        // 1. 片付け（地面より上の木・草を取り除く）
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                if ((x - cx) * (x - cx) + (z - cz) * (z - cz) <= r * r) {
                    b.clearColumn(x, z);
                }
            }
        }

        // 2. 道（東西南北の十字）
        List<Double> angles = town.streetAngles();
        BlockState street = style.street().defaultBlockState();
        for (double a : angles) {
            int x1 = (int) Math.round(cx + Math.cos(a) * (r + 2));
            int z1 = (int) Math.round(cz + Math.sin(a) * (r + 2));
            line(b, cx, cz, x1, z1, kind.streetWidth(), street);
            occupancy.addLine(cx, cz, x1, z1, kind.streetWidth() / 2 + 1);
        }

        // 4. 広場と真ん中の設備
        if (kind.plazaRadius() > 0) {
            disc(b, cx, cz, kind.plazaRadius(), Blocks.POLISHED_ANDESITE.defaultBlockState());
            occupancy.addBox(cx - kind.plazaRadius() - 1, cz - kind.plazaRadius() - 1,
                    cx + kind.plazaRadius() + 1, cz + kind.plazaRadius() + 1);
        }
        switch (town.type()) {
            case "village", "post_town" -> Buildings.well(b, cx, cz);
            case "hidden_village", "temple_city" -> Buildings.monument(b, cx, cz, town.type().equals("temple_city") ? 8 : 4);
            case "frontline_city" -> Buildings.campfire(b, cx, cz);
            case "castle_town" -> castle(b, cx, cz, angles.get(0), occupancy);
            case "mining_town" -> mineEntrance(b, cx + kind.plazaRadius() + 2, cz - 2, style, occupancy);
            default -> Buildings.fountain(b, cx, cz);
        }

        // 5. 城壁（道が出入りするところは門）
        if (kind.walls()) {
            walls(b, town, kind, angles, town.type().equals("frontline_city") || town.type().equals("start_town"), occupancy);
        } else if (town.type().equals("mining_town")) {
            fenceRing(b, town, style, angles);
        }

        // 6. 種類ごとの特別な建物
        switch (town.type()) {
            case "temple_city" -> temple(b, cx, cz - kind.plazaRadius() - 18, style, occupancy);
            case "post_town" -> placeBuilding(b, occupancy, cx + kind.plazaRadius() + 2, cz - 6, 13, 9, Direction.WEST,
                    p -> Buildings.hall(p, style, 13, 9, 2, random));
            case "frontline_city" -> {
                for (int i = 0; i < 2; i++) {
                    int bx = cx - 20 + i * 26;
                    placeBuilding(b, occupancy, bx, cz + kind.plazaRadius() + 4, 15, 7, Direction.NORTH,
                            p -> Buildings.hall(p, style, 15, 7, 1, random));
                }
            }
            case "city" -> market(b, cx, cz, kind.plazaRadius(), style, random);
            case "start_town" -> startTown(b, town, style, occupancy, random);
            case "hub" -> hub(b, town, kind, style, occupancy, random);
            default -> { }
        }

        // 7. 家（道に沿って並べる）
        houses(b, town, kind, style, angles, occupancy, random);

        // 8. 村の畑
        if (town.type().equals("village") || town.type().equals("hidden_village")) {
            for (int i = 0; i < 3; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                int fx = (int) (cx + Math.cos(a) * r * 0.6) - 4;
                int fz = (int) (cz + Math.sin(a) * r * 0.6) - 3;
                if (occupancy.free(fx - 1, fz - 1, fx + 9, fz + 7) && insideTown(town, fx, fz, 9, 7, 3)) {
                    occupancy.addBox(fx - 1, fz - 1, fx + 9, fz + 7);
                    Buildings.farm(new Buildings.Placer(b, fx, fz, 9, 7, Direction.NORTH), 9, 7, random);
                }
            }
        }

        // 9. 街灯（道に沿って）
        lamps(b, town, kind, style, angles);
        return b.build();
    }

    // =====================================================================
    // 道・広場
    // =====================================================================

    private static double angleDistance(double a, double b) {
        double d = Math.abs(a - b) % (Math.PI * 2);
        return d > Math.PI ? Math.PI * 2 - d : d;
    }

    /** 太さ width の道（地面のブロックを置き換える）。港の水の上は橋（板）。 */
    private static void line(TownPlan.Builder b, int x0, int z0, int x1, int z1, int width, BlockState state) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
        double half = width / 2.0;
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : i / (double) steps;
            double px = x0 + (x1 - x0) * t;
            double pz = z0 + (z1 - z0) * t;
            for (int dx = (int) -Math.ceil(half); dx <= Math.ceil(half); dx++) {
                for (int dz = (int) -Math.ceil(half); dz <= Math.ceil(half); dz++) {
                    if (dx * dx + dz * dz <= half * half + 0.25) {
                        int x = (int) Math.round(px + dx);
                        int z = (int) Math.round(pz + dz);
                        b.set(x, b.groundY(), z, state);
                    }
                }
            }
        }
    }

    private static void disc(TownPlan.Builder b, int cx, int cz, int radius, BlockState state) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius) {
                    b.set(cx + dx, b.groundY(), cz + dz, state);
                }
            }
        }
    }

    private static void lamps(TownPlan.Builder b, TownSpec town, Kind kind, TownStyle style, List<Double> angles) {
        int y = b.groundY();
        for (double a : angles) {
            double side = kind.streetWidth() / 2.0 + 1;
            for (double d = Math.max(kind.plazaRadius(), 6) + 4; d < town.radius() - 6; d += 9) {
                int x = (int) Math.round(town.x() + Math.cos(a) * d - Math.sin(a) * side);
                int z = (int) Math.round(town.z() + Math.sin(a) * d + Math.cos(a) * side);
                b.fill(x, y + 1, z, x, y + 3, z, style.fence().defaultBlockState());
                b.set(x, y + 4, z, Blocks.LANTERN.defaultBlockState());
            }
        }
    }

    // =====================================================================
    // 城壁
    // =====================================================================

    private static void walls(TownPlan.Builder b, TownSpec town, Kind kind, List<Double> angles, boolean towers,
                              Occupancy occupancy) {
        int y = b.groundY();
        int r = town.radius() - 3;
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState top = Blocks.STONE_BRICK_WALL.defaultBlockState();
        double gateHalf = (kind.streetWidth() / 2.0 + 1.5) / r; // 門の幅（角度）
        int steps = (int) Math.ceil(Math.PI * 2 * r * 2);
        for (int i = 0; i < steps; i++) {
            double a = i * Math.PI * 2 / steps;
            boolean gate = false;
            for (double s : angles) {
                if (angleDistance(a, s) < gateHalf) {
                    gate = true;
                    break;
                }
            }
            for (int t = 0; t < 2; t++) {
                int x = (int) Math.round(town.x() + Math.cos(a) * (r + t));
                int z = (int) Math.round(town.z() + Math.sin(a) * (r + t));
                if (gate) {
                    // 門：上だけつなぐ（アーチ）
                    b.fill(x, y + kind.wallHeight() - 1, z, x, y + kind.wallHeight(), z, wall);
                } else {
                    b.fill(x, y + 1, z, x, y + kind.wallHeight(), z, wall);
                    if (t == 1 && i % 2 == 0) {
                        b.set(x, y + kind.wallHeight() + 1, z, top);
                    }
                }
            }
        }
        occupancy.addRing(town.x(), town.z(), r - 2, r + 3);
        if (towers) {
            // 見張り塔（30 度ごと。門のところは避ける）
            for (int i = 0; i < 12; i++) {
                double a = angles.get(0) + Math.PI / 12 + i * Math.PI / 6;
                boolean nearGate = angles.stream().anyMatch(s -> angleDistance(a, s) < gateHalf * 3);
                if (nearGate) {
                    continue;
                }
                int tx = (int) Math.round(town.x() + Math.cos(a) * r) - 3;
                int tz = (int) Math.round(town.z() + Math.sin(a) * r) - 3;
                Buildings.tower(new Buildings.Placer(b, tx, tz, 7, 7, Direction.NORTH), TownStyle.of("spruce"), 7,
                        kind.wallHeight() + 7);
            }
        }
    }

    private static void fenceRing(TownPlan.Builder b, TownSpec town, TownStyle style, List<Double> angles) {
        int y = b.groundY();
        int r = town.radius() - 2;
        int steps = (int) Math.ceil(Math.PI * 2 * r * 2);
        for (int i = 0; i < steps; i++) {
            double a = i * Math.PI * 2 / steps;
            boolean gate = angles.stream().anyMatch(s -> angleDistance(a, s) < 4.0 / r);
            if (!gate) {
                b.set((int) Math.round(town.x() + Math.cos(a) * r), y + 1, (int) Math.round(town.z() + Math.sin(a) * r),
                        style.fence().defaultBlockState());
            }
        }
    }

    // =====================================================================
    // 家
    // =====================================================================

    private static void houses(TownPlan.Builder b, TownSpec town, Kind kind, TownStyle style, List<Double> angles,
                               Occupancy occupancy, RandomSource random) {
        int start = Math.max(kind.plazaRadius(), 6) + 3;
        int end = town.radius() - (kind.walls() ? 7 : 4);
        for (double a : angles) {
            for (int side = -1; side <= 1; side += 2) {
                for (double d = start; d < end; d += 3) {
                    if (random.nextDouble() > kind.density()) {
                        d += 4;
                        continue;
                    }
                    int width = 5 + random.nextInt(3) * 2;   // 5, 7, 9（ドアを中央にするため奇数）
                    int depth = 5 + random.nextInt(3);
                    int floors = 1 + random.nextInt(kind.maxFloors());
                    // 道の向きに一番近い東西南北を、家の正面にする
                    Direction toStreet = facingToward(a + (side > 0 ? -Math.PI / 2 : Math.PI / 2));
                    double off = kind.streetWidth() / 2.0 + 2 + depth / 2.0;
                    double hx = town.x() + Math.cos(a) * (d + width / 2.0) + Math.cos(a + side * Math.PI / 2) * off;
                    double hz = town.z() + Math.sin(a) * (d + width / 2.0) + Math.sin(a + side * Math.PI / 2) * off;
                    Buildings.Placer probe = new Buildings.Placer(b, 0, 0, width, depth, toStreet);
                    int ox = (int) Math.round(hx - probe.worldWidth() / 2.0);
                    int oz = (int) Math.round(hz - probe.worldDepth() / 2.0);
                    int ww = probe.worldWidth();
                    int wd = probe.worldDepth();
                    if (!insideTown(town, ox, oz, ww, wd, kind.walls() ? 6 : 3)
                            || !occupancy.free(ox - 1, oz - 1, ox + ww, oz + wd)) {
                        continue;
                    }
                    occupancy.addBox(ox - 1, oz - 1, ox + ww, oz + wd);
                    Buildings.house(new Buildings.Placer(b, ox, oz, width, depth, toStreet), style, width, depth, floors, random);
                    d += width;
                }
            }
        }
        // 大きな街は、すき間にも家を建てる
        if (kind.density() > 0.9) {
            for (int i = 0; i < 160; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double d = start + random.nextDouble() * (end - start);
                int width = 5 + random.nextInt(3) * 2;
                int depth = 5 + random.nextInt(3);
                Direction facing = Direction.from2DDataValue(random.nextInt(4));
                Buildings.Placer probe = new Buildings.Placer(b, 0, 0, width, depth, facing);
                int ox = (int) Math.round(town.x() + Math.cos(a) * d);
                int oz = (int) Math.round(town.z() + Math.sin(a) * d);
                if (!insideTown(town, ox, oz, probe.worldWidth(), probe.worldDepth(), 6)
                        || !occupancy.free(ox - 2, oz - 2, ox + probe.worldWidth() + 1, oz + probe.worldDepth() + 1)) {
                    continue;
                }
                occupancy.addBox(ox - 1, oz - 1, ox + probe.worldWidth(), oz + probe.worldDepth());
                Buildings.house(new Buildings.Placer(b, ox, oz, width, depth, facing), style, width, depth,
                        1 + random.nextInt(kind.maxFloors()), random);
            }
        }
    }

    private static Direction facingToward(double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        if (Math.abs(c) > Math.abs(s)) {
            return c > 0 ? Direction.EAST : Direction.WEST;
        }
        return s > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static boolean insideTown(TownSpec town, int ox, int oz, int w, int d, int margin) {
        double limit = town.radius() - margin;
        int[][] corners = {{ox, oz}, {ox + w, oz}, {ox, oz + d}, {ox + w, oz + d}};
        for (int[] c : corners) {
            double dx = c[0] - town.x();
            double dz = c[1] - town.z();
            if (dx * dx + dz * dz > limit * limit) {
                return false;
            }
        }
        return true;
    }

    private static void placeBuilding(TownPlan.Builder b, Occupancy occupancy, int ox, int oz, int width, int depth,
                                      Direction facing, java.util.function.Consumer<Buildings.Placer> build) {
        Buildings.Placer placer = new Buildings.Placer(b, ox, oz, width, depth, facing);
        if (!occupancy.free(ox - 1, oz - 1, ox + placer.worldWidth(), oz + placer.worldDepth())) {
            return;
        }
        occupancy.addBox(ox - 1, oz - 1, ox + placer.worldWidth(), oz + placer.worldDepth());
        build.accept(placer);
    }

    // =====================================================================
    // 種類ごとの特別な建物
    // =====================================================================

    /** 城（城下町の真ん中）。外壁 25×25、四隅に塔、真ん中に天守。 */
    private static void castle(TownPlan.Builder b, int cx, int cz, double gateAngle, Occupancy occupancy) {
        int y = b.groundY();
        int h = 12;
        int half = 12;
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState dark = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                boolean edge = Math.abs(dx) == half || Math.abs(dz) == half;
                b.set(cx + dx, y, cz + dz, edge ? wall : Blocks.GRASS_BLOCK.defaultBlockState());
                if (edge) {
                    b.fill(cx + dx, y + 1, cz + dz, cx + dx, y + h, cz + dz, wall);
                    if ((dx + dz) % 2 == 0) {
                        b.set(cx + dx, y + h + 1, cz + dz, wall);
                    }
                }
            }
        }
        // 門（最初の道の向き）
        Direction gate = facingToward(gateAngle);
        int gx = cx + gate.getStepX() * half;
        int gz = cz + gate.getStepZ() * half;
        for (int s = -1; s <= 1; s++) {
            int x = gate.getAxis() == Direction.Axis.X ? gx : gx + s;
            int z = gate.getAxis() == Direction.Axis.X ? gz + s : gz;
            b.fill(x, y + 1, z, x, y + 4, z, air);
        }
        // 四隅の塔
        for (int[] c : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            int tx = cx + c[0] * half;
            int tz = cz + c[1] * half;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    b.fill(tx + dx, y + 1, tz + dz, tx + dx, y + h + 6, tz + dz, wall);
                }
            }
            b.fill(tx - 2, y + h + 7, tz - 2, tx + 2, y + h + 7, tz + 2, dark);
        }
        // 天守
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                boolean edge = Math.abs(dx) == 5 || Math.abs(dz) == 5;
                for (int yy = 1; yy <= 22; yy++) {
                    b.set(cx + dx, y + yy, cz + dz, edge ? (yy % 6 == 3 && (dx + dz) % 3 == 0 ? Blocks.GLASS.defaultBlockState() : wall)
                            : (yy % 6 == 0 ? Blocks.SPRUCE_PLANKS.defaultBlockState() : air));
                }
                b.set(cx + dx, y + 23, cz + dz, dark);
            }
        }
        for (int s = -1; s <= 1; s++) {
            int x = gate.getAxis() == Direction.Axis.X ? cx + gate.getStepX() * 5 : cx + s;
            int z = gate.getAxis() == Direction.Axis.X ? cz + s : cz + gate.getStepZ() * 5;
            b.fill(x, y + 1, z, x, y + 3, z, air);
        }
        occupancy.addBox(cx - half - 3, cz - half - 3, cx + half + 3, cz + half + 3);
    }

    /** 神殿（神殿都市）。石英の柱が並ぶ建物。 */
    private static void temple(TownPlan.Builder b, int cx, int cz, TownStyle style, Occupancy occupancy) {
        int y = b.groundY();
        int hw = 7;
        int hd = 11;
        if (!occupancy.free(cx - hw - 2, cz - hd - 2, cx + hw + 2, cz + hd + 2)) {
            return;
        }
        BlockState quartz = Blocks.QUARTZ_BLOCK.defaultBlockState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.defaultBlockState();
        b.fill(cx - hw - 1, y + 1, cz - hd - 1, cx + hw + 1, y + 1, cz + hd + 1, quartz);
        b.fill(cx - hw, y + 2, cz - hd, cx + hw, y + 2, cz + hd, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        for (int dx = -hw; dx <= hw; dx += 2) {
            for (int dz : new int[]{-hd, hd}) {
                b.fill(cx + dx, y + 3, cz + dz, cx + dx, y + 10, cz + dz, pillar);
            }
        }
        for (int dz = -hd; dz <= hd; dz += 2) {
            for (int dx : new int[]{-hw, hw}) {
                b.fill(cx + dx, y + 3, cz + dz, cx + dx, y + 10, cz + dz, pillar);
            }
        }
        b.fill(cx - hw, y + 11, cz - hd, cx + hw, y + 11, cz + hd, quartz);
        for (int k = 1; k <= 3; k++) {
            b.fill(cx - hw + k * 2, y + 11 + k, cz - hd, cx + hw - k * 2, y + 11 + k, cz + hd, quartz);
        }
        b.set(cx, y + 3, cz, Blocks.SEA_LANTERN.defaultBlockState());
        occupancy.addBox(cx - hw - 2, cz - hd - 2, cx + hw + 2, cz + hd + 2);
    }

    /** 坑道の入口（鉱山町）。丸太の櫓と、地下へ下りる縦穴。 */
    private static void mineEntrance(TownPlan.Builder b, int x, int z, TownStyle style, Occupancy occupancy) {
        int y = b.groundY();
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
                boolean edge = dx == 0 || dz == 0 || dx == 4 || dz == 4;
                for (int yy = -24; yy <= 0; yy++) {
                    b.set(x + dx, y + yy, z + dz, edge ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int yy = -24; yy <= 0; yy++) {
            b.set(x + 2, y + yy, z + 1, Blocks.LADDER.defaultBlockState()
                    .trySetValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.SOUTH));
        }
        b.set(x + 2, y + 1, z + 1, Blocks.AIR.defaultBlockState());
        for (int[] c : new int[][]{{0, 0}, {4, 0}, {0, 4}, {4, 4}}) {
            b.fill(x + c[0], y + 1, z + c[1], x + c[0], y + 5, z + c[1], log);
        }
        b.fill(x, y + 6, z, x + 4, y + 6, z + 4, style.planks().defaultBlockState());
        b.set(x + 2, y + 5, z + 2, Blocks.LANTERN.defaultBlockState()
                .trySetValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        occupancy.addBox(x - 1, z - 1, x + 5, z + 5);
    }

    /**
     * 始まりの街の主な建物（広場のまわり）。
     * <pre>
     *            北の門
     *   ショップ │ ギルド（クエスト受付）
     *  ──────広場（噴水・市場）──────
     *   宿屋     │ 自分の家
     *            南の門
     * </pre>
     * ※ NPC・クエスト受付・ショップの中身は後で作る。今は建物だけ。
     */
    private static void startTown(TownPlan.Builder b, TownSpec town, TownStyle style, Occupancy occupancy, RandomSource random) {
        int cx = town.x();
        int cz = town.z();
        int street = 5;   // 道の中心から建物までの間（道の幅 5 ＋ すき間）
        int plaza = 14;   // 広場の中心から建物までの間
        // ギルド（北東、正面は西の北通り）：幅 15・奥行き 11・2 階 → 地図上は 11 × 15
        placeBuilding(b, occupancy, cx + street, cz - plaza - 15, 15, 11, Direction.WEST,
                p -> Buildings.hall(p, style, 15, 11, 2, random));
        // ショップ（北西、正面は東の北通り）：13 × 9 → 地図上は 9 × 13
        placeBuilding(b, occupancy, cx - street - 9, cz - plaza - 13, 13, 9, Direction.EAST,
                p -> Buildings.hall(p, style, 13, 9, 2, random));
        // 宿屋（南西）
        placeBuilding(b, occupancy, cx - street - 9, cz + plaza, 13, 9, Direction.EAST,
                p -> Buildings.hall(p, style, 13, 9, 2, random));
        // 自分の家（南東）：9 × 7 → 地図上は 7 × 9
        placeBuilding(b, occupancy, cx + street, cz + plaza, 9, 7, Direction.WEST,
                p -> Buildings.house(p, style, 9, 7, 2, random));
        // 広場の市場（屋台）
        market(b, cx, cz, 11, style, random);
    }

    /** 中間拠点の建物。北の出発ゲートのまわりは空けておく。 */
    private static void hub(TownPlan.Builder b, TownSpec town, Kind kind, TownStyle style, Occupancy occupancy, RandomSource random) {
        int cx = town.x();
        int cz = town.z();
        occupancy.addBox(cx - 5, cz - 20, cx + 5, cz - kind.plazaRadius());
        market(b, cx, cz, kind.plazaRadius(), style, random);
        // 宿屋（南西）とショップ（南東）
        placeBuilding(b, occupancy, cx - 5 - 9, cz + kind.plazaRadius() + 3, 13, 9, Direction.EAST,
                p -> Buildings.hall(p, style, 13, 9, 1, random));
        placeBuilding(b, occupancy, cx + 5, cz + kind.plazaRadius() + 3, 13, 9, Direction.WEST,
                p -> Buildings.hall(p, style, 13, 9, 1, random));
    }

    /** 市場（大きな街の広場の周り）。 */
    private static void market(TownPlan.Builder b, int cx, int cz, int plaza, TownStyle style, RandomSource random) {
        String[] colors = {"red", "blue", "yellow", "white", "green", "orange"};
        for (int i = 0; i < 6; i++) {
            double a = Math.PI / 6 + i * Math.PI / 3;
            int x = (int) Math.round(cx + Math.cos(a) * (plaza - 3)) - 2;
            int z = (int) Math.round(cz + Math.sin(a) * (plaza - 3)) - 1;
            Buildings.stall(new Buildings.Placer(b, x, z, 5, 3, Direction.NORTH), style, colors[i % colors.length]);
        }
    }

    // =====================================================================
    // 使用中の場所の管理（建物が重ならないように）
    // =====================================================================

    private static final class Occupancy {
        private record Box(int x0, int z0, int x1, int z1) {}
        private record Line(int x0, int z0, int x1, int z1, double half) {}
        private record Ring(int cx, int cz, double inner, double outer) {}
        private record Circle(int cx, int cz, double r) {}

        private final List<Box> boxes = new ArrayList<>();
        private final List<Line> lines = new ArrayList<>();
        private final List<Ring> rings = new ArrayList<>();
        private final List<Circle> circles = new ArrayList<>();

        void addBox(int x0, int z0, int x1, int z1) {
            boxes.add(new Box(Math.min(x0, x1), Math.min(z0, z1), Math.max(x0, x1), Math.max(z0, z1)));
        }

        void addLine(int x0, int z0, int x1, int z1, double half) {
            lines.add(new Line(x0, z0, x1, z1, half));
        }

        void addRing(int cx, int cz, double inner, double outer) {
            rings.add(new Ring(cx, cz, inner, outer));
        }

        void addCircle(int cx, int cz, double r) {
            circles.add(new Circle(cx, cz, r));
        }

        boolean free(int x0, int z0, int x1, int z1) {
            for (Box bx : boxes) {
                if (x0 <= bx.x1() && x1 >= bx.x0() && z0 <= bx.z1() && z1 >= bx.z0()) {
                    return false;
                }
            }
            // 四角の角と辺の中点で調べる（道・城壁・港は形が複雑なので近似）
            int mx = (x0 + x1) / 2;
            int mz = (z0 + z1) / 2;
            int[][] pts = {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}, {mx, z0}, {mx, z1}, {x0, mz}, {x1, mz}, {mx, mz}};
            for (int[] p : pts) {
                for (Line l : lines) {
                    if (segmentDistance(p[0], p[1], l) <= l.half()) {
                        return false;
                    }
                }
                for (Ring r : rings) {
                    double d = Math.hypot(p[0] - r.cx(), p[1] - r.cz());
                    if (d >= r.inner() && d <= r.outer()) {
                        return false;
                    }
                }
                for (Circle c : circles) {
                    if (Math.hypot(p[0] - c.cx(), p[1] - c.cz()) <= c.r()) {
                        return false;
                    }
                }
            }
            return true;
        }

        private static double segmentDistance(double x, double z, Line l) {
            double vx = l.x1() - l.x0();
            double vz = l.z1() - l.z0();
            double len = vx * vx + vz * vz;
            double t = len == 0 ? 0 : Math.max(0, Math.min(1, ((x - l.x0()) * vx + (z - l.z0()) * vz) / len));
            return Math.hypot(x - (l.x0() + vx * t), z - (l.z0() + vz * t));
        }
    }
}
