package io.github.kurumimnm.kurumimnmod.world.terrain;

import io.github.kurumimnm.kurumimnmod.world.terrain.TerrainDesign.Landform;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 設計図の部品を、計算しやすい形（点の配列・外接矩形など）にしたもの。シード値で山並みの点をずらす。
 * <p>
 * 計算（{@link TerrainBaker}）では {@link #height} で「設計の高さ」を作る。
 * 地形の生成中（{@link TerrainBake}）は、粗いマスでは表せない切り立った峡谷の壁・川の水路を
 * {@link #sharpen}・{@link #designedWater} でブロック単位に作り直す。
 */
final class Design {

    // 各マスの印
    static final int SEA = 1;    // 海
    static final int HARD = 2;   // 崩さない（台地の崖・峡谷の壁）
    static final int GORGE = 4;  // 峡谷（崩れる角度をほぼ垂直に）
    static final int DRY = 8;    // 設計した窪み（湖にしない・埋めない）
    static final int LAKE = 16;  // 設計した湖
    static final int FILL = 32;  // 自然の湖を作らない（窪みは埋める。砂漠・荒地など）
    /** 設計した川の土手を、岸（bank）の外へなだらかにつなぐ幅（ブロック） */
    static final double RIVER_LEVEE = 48;

    /** 部品 1 つ。xs/zs＝点、bounds＝影響する範囲（外接矩形）。 */
    record Shape(Landform form, double[] xs, double[] zs, double[] bounds, long salt) {}

    private final List<Shape> shapes;
    private final List<Shape> canyons = new ArrayList<>();
    private final List<Shape> rivers = new ArrayList<>();
    private final List<Shape> bridges = new ArrayList<>();
    private Shape coast;

    private Design(List<Shape> shapes) {
        this.shapes = shapes;
        for (Shape s : shapes) {
            switch (s.form().type()) {
                case "canyon" -> canyons.add(s);
                case "river" -> rivers.add(s);
                case "bridge" -> bridges.add(s);
                case "coast" -> coast = s;
                default -> {}
            }
        }
    }

    List<Shape> bridges() {
        return bridges;
    }

    double seaLevel() {
        return coast == null ? 63 : coast.form().num("sea_level", 63);
    }

    static Design prepare(TerrainDesign design, Random random) {
        List<Shape> shapes = new ArrayList<>();
        long salt = 1000;
        for (Landform form : design.landforms()) {
            salt += 101;
            List<double[]> pts = new ArrayList<>();
            String type = form.type();
            if (type.equals("arc")) {
                double phase = random.nextDouble() * Math.PI * 2;
                int count = 10;
                for (int k = 0; k < count; k++) {
                    double a = Math.toRadians(form.num("from", 0) + (form.num("to", 360) - form.num("from", 0)) * k / (count - 1.0));
                    double r = form.num("radius", 500) + form.num("wobble", 0) * Math.sin(a * 3.0 + phase);
                    pts.add(new double[]{form.cx() + Math.cos(a) * r, form.cz() + Math.sin(a) * r});
                }
            } else {
                for (List<Double> p : form.points()) {
                    pts.add(new double[]{p.get(0), p.get(1)});
                }
            }
            // シードでずらすのは山並みだけ（海岸・台地・谷・川・橋は設計どおり）
            if (type.equals("range") || type.equals("arc")) {
                for (double[] p : pts) {
                    p[0] += (random.nextDouble() * 2 - 1) * design.jitter();
                    p[1] += (random.nextDouble() * 2 - 1) * design.jitter();
                }
            }
            double[] xs = pts.stream().mapToDouble(p -> p[0]).toArray();
            double[] zs = pts.stream().mapToDouble(p -> p[1]).toArray();
            double reach = reach(form);
            double[] bounds;
            if (xs.length > 0) {
                bounds = Shapes2D.bounds(xs, zs, reach);
            } else {
                double r = form.num("radius", Math.max(form.num("rx", 0), form.num("rz", 0))) + reach;
                bounds = new double[]{form.cx() - r, form.cz() - r, form.cx() + r, form.cz() + r};
            }
            if (type.equals("coast")) {
                bounds = new double[]{-Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
            }
            shapes.add(new Shape(form, xs, zs, bounds, salt));
        }
        return new Design(shapes);
    }

    /** 部品が影響する、形の外側への広がり（ブロック）。 */
    private static double reach(Landform f) {
        return switch (f.type()) {
            case "range", "arc" -> f.num("width", 500);
            case "area" -> f.num("edge", 200) + f.num("wobble", 0) * 1.1 + f.num("edge_noise", f.num("edge", 200) * 0.5);
            case "basin" -> f.num("width", 300);
            case "crater" -> f.num("radius", 200) * 0.6;
            case "canyon" -> f.num("width", 200) / 2 + f.num("wall", 10) + 64;
            case "gorge" -> f.num("width", 60) + f.num("meander", 0);
            case "river" -> f.num("width", 20) / 2 + f.num("bank", 24) + RIVER_LEVEE + 16;
            case "lake" -> 0;
            default -> 0;
        };
    }

    // =====================================================================
    // 設計の高さ（計算のときに使う）
    // =====================================================================

    double height(TerrainDesign design, BakeNoise noise, double x, double z, int[] mask, boolean bake) {
        double base = design.baseHeight();
        double wx = x + 320 * noise.fbm(x, z, 1700, 4, 1);
        double wz = z + 320 * noise.fbm(x, z, 1700, 4, 2);
        double mx = wx + 180 * noise.fbm(x, z, 420, 4, 40);
        double mz = wz + 180 * noise.fbm(x, z, 420, 4, 41);
        double h = base + design.baseNoise() * noise.fbm(wx, wz, 1300, 5, 3) + 0.8 * design.baseNoise() * noise.ridged(wx, wz, 600, 5, 4);
        for (Shape s : shapes) {
            if (!Shapes2D.inBounds(s.bounds(), x, z) && !s.form().type().equals("range") && !s.form().type().equals("arc")) {
                continue;
            }
            Landform f = s.form();
            switch (f.type()) {
                case "area" -> h = area(s, noise, x, z, h, mask);
                case "range", "arc" -> {
                    if (!Shapes2D.inBounds(s.bounds(), wx, wz)) {
                        continue;
                    }
                    double width = f.num("width", 500);
                    double d = Shapes2D.polylineDistance(wx, wz, s.xs(), s.zs(), null);
                    if (d >= width) {
                        continue;
                    }
                    double prof = Math.pow(1 - BakeNoise.smooth(d / width), 1.8);
                    double rough = f.num("rough", 0.6);
                    double rr = 0.6 * noise.ridged(mx, mz, width * 0.75, 6, s.salt()) + 0.4 * noise.ridged(mx, mz, 260, 5, s.salt() + 5);
                    double add = (f.num("peak", 200) - base) * prof * (1 - rough + rough * rr) * (0.9 + 0.1 * prof);
                    h = Math.max(h, base + add);
                }
                case "basin" -> {
                    double bx = x + 50 * noise.fbm(x, z, 400, 3, s.salt());
                    double bz = z + 50 * noise.fbm(x, z, 400, 3, s.salt() + 1);
                    double rb = Math.hypot(bx - f.cx(), bz - f.cz());
                    double w = 1 - BakeNoise.smooth((rb - f.num("radius", 300)) / Math.max(1, f.num("width", 300)));
                    if (w > 0) {
                        double floor = f.num("floor", 60) + f.num("noise", 6) * noise.fbm(x, z, 200, 3, s.salt() + 2);
                        h = h * (1 - w) + floor * w;
                        if (w > 0.3) {
                            mask[0] |= DRY;
                        }
                    }
                }
                case "crater" -> {
                    double r = f.num("radius", 200);
                    double d = Math.max(0, Math.hypot(x - f.cx(), z - f.cz()) + 0.08 * r * noise.fbm(x, z, r * 0.6, 3, s.salt()));
                    double rimUp = f.num("rim", 6);
                    if (d < r) {
                        double floor = f.num("floor", h - 20);
                        double rimTop = h + rimUp;
                        h = Math.min(h, floor + (rimTop - floor) * Math.pow(d / r, 2.2));
                        mask[0] |= DRY;
                    } else if (d < r * 1.6) {
                        h += rimUp * (1 - BakeNoise.smooth((d - r) / (0.6 * r)));
                    }
                }
                case "volcano" -> {
                    double d = Math.hypot(x - f.cx(), z - f.cz()) + 40 * noise.fbm(x, z, 300, 3, s.salt());
                    double radius = f.num("radius", 2000);
                    double crater = f.num("crater", 350);
                    double peak = f.num("peak", 240);
                    double foot = f.num("base", base);
                    if (d < radius) {
                        double cone;
                        if (d > crater) {
                            cone = foot + (peak - foot) * Math.pow(1 - (d - crater) / (radius - crater), 1.7);
                            cone += 6 * noise.ridged(x, z, 160, 4, s.salt() + 3) * (1 - (d - crater) / (radius - crater));
                        } else {
                            double floor = f.num("floor", 120);
                            cone = floor + (peak - floor) * Math.pow(d / crater, 4);
                            mask[0] |= DRY;
                        }
                        double w = BakeNoise.smooth((radius - d) / (radius * 0.25));
                        h = d <= crater ? cone : Math.max(h, h * (1 - w) + cone * w);
                    }
                }
                case "gorge" -> {
                    double gx = x + f.num("meander", 0) * noise.fbm(x, z, 360, 2, s.salt());
                    double gz = z + f.num("meander", 0) * noise.fbm(x, z, 360, 2, s.salt() + 1);
                    double[] t = new double[1];
                    double width = f.num("width", 60);
                    double d = Shapes2D.polylineDistance(gx, gz, s.xs(), s.zs(), t);
                    double slot = 1 - BakeNoise.smooth((d - width * 0.5) / (width * 0.5));
                    if (slot > 0) {
                        double floor = f.num("floor", 80) + (f.num("floor_end", f.num("floor", 80)) - f.num("floor", 80)) * t[0];
                        h = h * (1 - slot) + Math.min(h, floor) * slot;
                        if (slot > 0.05) {
                            mask[0] |= GORGE;
                        }
                    }
                }
                case "canyon" -> {
                    double[] t = new double[1];
                    double d = Shapes2D.polylineDistance(x, z, s.xs(), s.zs(), t);
                    double half = f.num("width", 200) / 2;
                    double wall = Math.max(f.num("wall", 10), bake ? 24 : 1);
                    if (d < half + wall) {
                        double floor = canyonFloor(f, t[0]);
                        double rim = f.has("rim") ? f.num("rim", h) : h;
                        double target = d <= half ? floor : floor + (rim - floor) * BakeNoise.smooth((d - half) / wall);
                        h = Math.min(h, target);
                        mask[0] |= DRY | GORGE | HARD;
                    } else if (f.has("rim") && d < half + wall + 96) {
                        // 縁の近くは台地の高さにそろえる
                        double w = 1 - BakeNoise.smooth((d - half - wall) / 96);
                        h = h + (f.num("rim", h) - h) * w;
                        mask[0] |= HARD;
                    }
                }
                case "river" -> {
                    // 設計した川の両岸：地面が水面より低い所は土手を盛る（水が宙に浮かないように）。水路は生成中に掘る（sharpen）
                    double[] t = new double[1];
                    double d = Shapes2D.polylineDistance(x, z, s.xs(), s.zs(), t);
                    double inner = f.num("width", 20) / 2 + f.num("bank", 24);
                    if (d < inner + RIVER_LEVEE) {
                        double bankTop = riverLevel(f, t[0]) + 2;
                        double w = 1 - BakeNoise.smooth((d - inner) / RIVER_LEVEE);
                        if (h < bankTop && (mask[0] & GORGE) == 0) {
                            h = h + (bankTop - h) * w;
                        }
                        if (w > 0.5) {
                            mask[0] |= HARD;
                        }
                    }
                }
                case "lake" -> {
                    double e = Math.hypot((x - f.cx()) / f.num("rx", 100), (z - f.cz()) / f.num("rz", 100)) + 0.2 * noise.fbm(x, z, 120, 3, s.salt());
                    if (e < 1) {
                        h -= f.num("depth", 6) * (1 - BakeNoise.smooth((e - 0.6) / 0.4));
                        mask[0] |= LAKE;
                    }
                }
                default -> {}
            }
        }
        // 細かい不規則さ（水が自然な道筋を見つけられるように）
        h += (h - base) * 0.04 * noise.fbm(x, z, 140, 4, 90) + 1.2 * noise.fbm(x, z, 50, 3, 91);
        // 海岸：外は海。岸の近くは海面の少し上までなだらかに下げる
        if (coast != null) {
            Landform f = coast.form();
            double sea = f.num("sea_level", 63);
            double c = Shapes2D.coastDistance(x, z, coast.xs(), coast.zs());
            double beach = f.num("beach", 160);
            if (c < 0) {
                double floor = f.num("ocean_floor", 30);
                double oceanH = Math.max(floor, sea - 3 - (-c) * f.num("ocean_slope", 0.06));
                h = oceanH;
                mask[0] |= SEA;
            } else if (c < beach) {
                double t = BakeNoise.smooth(c / beach);
                double shore = sea + 1 + 2 * noise.fbm(x, z, 80, 2, 4403);
                h = shore + (h - shore) * t;
            }
        }
        return h;
    }

    private static double area(Shape s, BakeNoise noise, double x, double z, double h, int[] mask) {
        Landform f = s.form();
        double edge = Math.max(1, f.num("edge", 200));
        double sd;
        if (s.xs().length >= 3) {
            sd = Shapes2D.signedPolygonDistance(x, z, s.xs(), s.zs());
        } else {
            sd = f.num("radius", 500) - Math.hypot(x - f.cx(), z - f.cz());
        }
        sd += f.num("edge_noise", edge * 0.5) * noise.fbm(x, z, Math.max(80, edge * 3), 3, s.salt() + 9);
        // 輪郭の大きなうねり（四角い範囲でも自然な形にする）
        // （fbm の値はほぼ ±0.4 に収まるので 2.5 倍して、wobble ≒ 最大のずれ にする）
        sd += f.num("wobble", 0) * 2.5 * noise.fbm(x, z, f.num("wobble_scale", 600), 2, s.salt() + 11);
        double w = BakeNoise.smooth((sd + edge / 2) / edge);
        if (w <= 0) {
            return h;
        }
        if (f.num("lakes", 1) == 0 && w > 0.5) {
            mask[0] |= FILL;
        }
        String style = f.str("style", "plain");
        double target = f.num("height", 72) + f.num("noise", 8) * noise.fbm(x, z, f.num("noise_scale", 500), 4, s.salt());
        switch (style) {
            case "dunes" -> {
                double angle = f.num("dune_angle", 30) * Math.PI / 180;
                double u = x * Math.cos(angle) + z * Math.sin(angle);
                double v = -x * Math.sin(angle) + z * Math.cos(angle);
                target += f.num("dune", 14) * noise.ridged(u * 1.8 + 60 * noise.fbm(x, z, 300, 2, s.salt() + 4), v * 0.6, 260, 3, s.salt() + 5);
            }
            case "mesas" -> {
                double m = noise.fbm(x, z, f.num("mesa_scale", 500), 3, s.salt() + 6);
                double thr = f.num("mesa_threshold", 0.25);
                if (m > thr - 0.04) {
                    double t = BakeNoise.smooth((m - thr) / 0.04);
                    // 段々（地層）
                    double top = f.num("mesa", 30);
                    double lift = top * t;
                    lift = Math.floor(lift / 6) * 6 + (lift % 6) * 0.3;
                    target += lift;
                    if (t > 0 && t < 1) {
                        mask[0] |= HARD;
                    }
                }
            }
            default -> {}
        }
        if (style.equals("plateau") && w > 0.02 && w < 0.98) {
            mask[0] |= HARD;
        }
        return h + (target - h) * w;
    }

    static double canyonFloor(Landform f, double t) {
        double a = f.num("floor", 60);
        return a + (f.num("floor_end", a) - a) * t;
    }

    // =====================================================================
    // 生成中（ブロック単位）の仕上げ
    // =====================================================================

    /** 峡谷の壁を垂直に、底を平らにし、設計した川の水路を掘る。h＝補間した高さ。 */
    double sharpen(BakeNoise noise, double x, double z, double h) {
        for (Shape s : canyons) {
            if (!Shapes2D.inBounds(s.bounds(), x, z)) {
                continue;
            }
            Landform f = s.form();
            double[] t = new double[1];
            double d = Shapes2D.polylineDistance(x, z, s.xs(), s.zs(), t);
            double half = f.num("width", 200) / 2 + 6 * noise.fbm(x, z, 40, 2, s.salt() + 7);
            double wall = f.num("wall", 10);
            double floor = canyonFloor(f, t[0]) + 1.5 * noise.fbm(x, z, 30, 2, s.salt() + 8);
            double rim = f.has("rim") ? f.num("rim", h) : h;
            if (d <= half) {
                h = floor;
            } else if (d < half + wall) {
                double u = (d - half) / wall;
                // 壁：ほぼ垂直。ところどころに段（足場）
                double ledge = Math.floor(u * 6) / 6;
                h = floor + (rim - floor) * (0.85 * u + 0.15 * ledge);
            } else if (f.has("rim") && d < half + wall + 64) {
                double w = 1 - BakeNoise.smooth((d - half - wall) / 64);
                h = h + (rim - h) * w;
            }
        }
        for (Shape s : rivers) {
            if (!Shapes2D.inBounds(s.bounds(), x, z)) {
                continue;
            }
            Landform f = s.form();
            double[] t = new double[1];
            double d = Shapes2D.polylineDistance(x, z, s.xs(), s.zs(), t);
            double half = f.num("width", 20) / 2 + 2 * noise.fbm(x, z, 50, 2, s.salt() + 7);
            double bank = f.num("bank", 24);
            if (d < half + bank) {
                double level = riverLevel(f, t[0]);
                double bed = level - f.num("depth", 3);
                if (d <= half) {
                    double centre = 1 - d / half;
                    h = Math.min(h, bed - 1.5 * centre + 1.0);
                } else {
                    double w = BakeNoise.smooth((d - half) / bank);
                    double bankTop = level + 1;
                    h = Math.min(h, bankTop + (h - bankTop) * w);
                }
            }
        }
        return h;
    }

    /** 設計した川・峡谷の沼の水面（なければ NaN）。 */
    double designedWater(BakeNoise noise, double x, double z) {
        for (Shape s : rivers) {
            if (!Shapes2D.inBounds(s.bounds(), x, z)) {
                continue;
            }
            Landform f = s.form();
            double[] t = new double[1];
            double d = Shapes2D.polylineDistance(x, z, s.xs(), s.zs(), t);
            double half = f.num("width", 20) / 2 + 2 * noise.fbm(x, z, 50, 2, s.salt() + 7);
            if (d <= half + 0.5 && !inCanyon(x, z)) {
                return riverLevel(f, t[0]);
            }
        }
        for (Shape s : canyons) {
            Landform f = s.form();
            if (!f.has("water") || !Shapes2D.inBounds(s.bounds(), x, z)) {
                continue;
            }
            double d = Shapes2D.polylineDistance(x, z, s.xs(), s.zs(), null);
            if (d <= f.num("width", 200) / 2) {
                return f.num("water", 55);
            }
        }
        return Double.NaN;
    }

    /** 峡谷（壁を含む）の中か。川が峡谷へ流れ落ちる所では、川の水面を峡谷の中に置かない（滝として壁を落ちる）。 */
    private boolean inCanyon(double x, double z) {
        for (Shape s : canyons) {
            if (!Shapes2D.inBounds(s.bounds(), x, z)) {
                continue;
            }
            Landform f = s.form();
            double d = Shapes2D.polylineDistance(x, z, s.xs(), s.zs(), null);
            if (d < f.num("width", 200) / 2 + f.num("wall", 10) + 2) {
                return true;
            }
        }
        return false;
    }

    static double riverLevel(Landform f, double t) {
        double a = f.num("level", 70);
        return a + (f.num("level_end", a) - a) * t;
    }
}
