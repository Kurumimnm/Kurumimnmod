package io.github.kurumimnm.kurumimnmod.quest;

import io.github.kurumimnm.kurumimnmod.config.ModConstants;

/**
 * フィールド（区画）の地面の高さを計算する。区画の seed が違えば形も変わる。
 * <ul>
 *   <li>style ごとの起伏：plains（なだらか）／hills（丘）／mountains（尾根のある山）／dunes（砂丘）／arena（平ら）</li>
 *   <li>正方形の縁は崖のように盛り上げる（外は奈落。見えない壁は {@link QuestPlotFeature} が置く）</li>
 *   <li>入口とゲートのまわりは基準の高さ（terrain.base）で平らにする</li>
 * </ul>
 * u, v は区画の中心からの距離（ブロック。北 = v マイナス）。
 */
public final class QuestTerrain {

    private QuestTerrain() {}

    public static double height(QuestPlots.Plot plot, double u, double v) {
        int half = plot.half();
        double edge = Math.min(half - Math.abs(u), half - Math.abs(v)); // 縁までの距離
        if (edge <= 0) {
            return QuestPlots.VOID;
        }
        QuestDefinition.Terrain t = plot.terrain();
        double base = t.base();
        double amp = t.amplitude();
        double scale = Math.max(8, t.scale());
        long seed = plot.seed();
        double nx = u / scale;
        double nz = v / scale;
        double h = switch (t.style()) {
            case "plains" -> base + amp * 0.5 * QuestNoise.fbm(nx, nz, 3, seed);
            case "mountains" -> {
                double ridge = 1 - Math.abs(QuestNoise.fbm(nx * 0.8, nz * 0.8, 3, seed + 5));
                double n = QuestNoise.fbm(nx, nz, 4, seed);
                yield base + amp * (0.45 * n + 0.9 * ridge * ridge - 0.35);
            }
            case "dunes" -> {
                double angle = (seed & 0xFF) / 255.0 * Math.PI;
                double along = (u * Math.cos(angle) + v * Math.sin(angle)) / scale;
                double warp = QuestNoise.fbm(nx * 0.7, nz * 0.7, 2, seed + 9) * 3;
                yield base + amp * (0.5 * Math.sin(along * Math.PI * 2 + warp) + 0.5 * QuestNoise.fbm(nx, nz, 3, seed));
            }
            case QuestDefinition.Terrain.ARENA -> base;
            default -> base + amp * QuestNoise.fbm(nx, nz, 4, seed); // hills
        };
        // 縁の崖（ボス部屋は壁を建てるので盛らない）
        int rim = ModConstants.QUEST_RIM_WIDTH;
        if (!plot.isArena() && edge < rim) {
            double k = 1 - edge / rim;
            h += ModConstants.QUEST_RIM_HEIGHT * k * k;
        }
        // 入口・ゲートのまわりは平らに
        h = flatten(h, base, u, v, 0, half - ModConstants.QUEST_GATE_INSET);
        h = flatten(h, base, u, v, 0, plot.isArena() ? -half / 2.0 : -half + ModConstants.QUEST_GATE_INSET);
        return h;
    }

    /** (px, pz) のまわり（半径 QUEST_FLAT_RADIUS）を base の高さにし、外側へなめらかにつなぐ。 */
    private static double flatten(double h, double base, double u, double v, double px, double pz) {
        double d = Math.hypot(u - px, v - pz);
        double r = ModConstants.QUEST_FLAT_RADIUS;
        double blend = ModConstants.QUEST_FLAT_BLEND;
        if (d >= r + blend) {
            return h;
        }
        double w = d <= r ? 0 : (d - r) / blend;
        w = w * w * (3 - 2 * w);
        return base + (h - base) * w;
    }
}
