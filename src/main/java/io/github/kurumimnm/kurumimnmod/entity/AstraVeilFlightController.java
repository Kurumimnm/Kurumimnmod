package io.github.kurumimnm.kurumimnmod.entity;

import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * アストラ・ヴェイルの空中移動（サーバー側）。
 * <p>
 * 巨大な海洋生物が空を漂うように動かすため、バニラの経路探索・MoveControl は使わず、
 * 「向き・速さ・上下の速さ」をそれぞれ少しずつ変える方式で動かす（急な方向転換・急停止をしない）。
 * <ul>
 *   <li>出現した場所（home）のまわり {@link ModConstants#ASTRA_VEIL_WANDER_RADIUS} の中で、次の目的地をランダムに選ぶ</li>
 *   <li>目的地の方角へゆっくり向きを変えながら進む。進む向きは少しゆらして、一直線にならないようにする</li>
 *   <li>高さは「地面の高さ + 目的地ごとの高さ」へゆっくり合わせる（上昇・下降）</li>
 *   <li>目的地に着いたら、そのまま次へ向かうか、減速して止まり、しばらくその場に漂う</li>
 * </ul>
 * 地形はすり抜ける（noPhysics）。地面の高さは読み込み済みのチャンクだけを調べる（チャンクを新しく読み込ませない）。
 * <p>
 * 【拡張】攻撃などで移動を止めたい・特定の場所へ向かわせたいときは、{@link #hold}・{@link #flyTo} を使う。
 */
public final class AstraVeilFlightController {

    private final AstraVeilEntity entity;

    private boolean hasHome;
    private double homeX, homeZ;

    private boolean hasWaypoint;
    private double waypointX, waypointZ;
    /** 今の目的地で使う、地面からの高さ。 */
    private double flightHeight = ModConstants.ASTRA_VEIL_FLIGHT_HEIGHT_MIN;
    /** 今の目的地に着いたら止まるか。 */
    private boolean stopAtWaypoint;
    /** 止まって漂う残り時間（tick）。 */
    private int hoverTicks;

    private float yaw;
    private float turnRate;
    private double speed;
    private double verticalSpeed;
    private double lastGround = Double.NaN;
    private boolean yawInitialized;

    public AstraVeilFlightController(AstraVeilEntity entity) {
        this.entity = entity;
    }

    // =====================================================================
    // 毎 tick の更新（サーバー側。AstraVeilEntity#tick から呼ぶ）
    // =====================================================================

    public void tick() {
        Level level = this.entity.level();
        RandomSource random = this.entity.getRandom();
        double x = this.entity.getX(), y = this.entity.getY(), z = this.entity.getZ();
        if (!this.hasHome) {
            this.hasHome = true;
            this.homeX = x; this.homeZ = z;
        }
        if (!this.yawInitialized) {
            this.yawInitialized = true;
            this.yaw = this.entity.getYRot();
        }

        // ---- 目的地・速さの目標 ----
        double targetSpeed;
        float targetTurn;
        if (this.hoverTicks > 0) {
            // 漂っている：その場にとどまる（向きはそのまま）
            this.hoverTicks--;
            targetSpeed = 0;
            targetTurn = 0;
            if (this.hoverTicks == 0) {
                pickWaypoint(random);
            }
        } else {
            if (!this.hasWaypoint) {
                pickWaypoint(random);
            }
            double dx = this.waypointX - x, dz = this.waypointZ - z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance < ModConstants.ASTRA_VEIL_ARRIVE_DISTANCE) {
                if (this.stopAtWaypoint) {
                    this.hoverTicks = ModConstants.ASTRA_VEIL_HOVER_MIN_TICKS
                            + random.nextInt(ModConstants.ASTRA_VEIL_HOVER_MAX_TICKS - ModConstants.ASTRA_VEIL_HOVER_MIN_TICKS + 1);
                    this.hasWaypoint = false;
                } else {
                    pickWaypoint(random);
                    dx = this.waypointX - x; dz = this.waypointZ - z;
                    distance = Math.sqrt(dx * dx + dz * dz);
                }
            }
            // 目的地の方角（バニラの向き：0 = 南 +Z、90 = 西 -X）に、ゆるやかなゆらぎを足す
            float desired = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
            float meander = Mth.sin(this.entity.tickCount * Mth.TWO_PI / ModConstants.ASTRA_VEIL_MEANDER_PERIOD_TICKS)
                    * ModConstants.ASTRA_VEIL_MEANDER_DEGREES;
            float error = Mth.wrapDegrees(desired + meander - this.yaw);
            targetTurn = Mth.clamp(error * ModConstants.ASTRA_VEIL_TURN_GAIN,
                    -ModConstants.ASTRA_VEIL_MAX_TURN_RATE, ModConstants.ASTRA_VEIL_MAX_TURN_RATE);
            targetSpeed = ModConstants.ASTRA_VEIL_CRUISE_SPEED;
            if (this.stopAtWaypoint) {
                // 止まる目的地の手前では、距離に合わせて速さを落とす
                targetSpeed *= Mth.clamp(distance / ModConstants.ASTRA_VEIL_BRAKE_DISTANCE, 0.12, 1.0);
            }
            // 大きく向きを変える間は速さを落とす（大回りしすぎて、目的地のまわりを回り続けないように）
            targetSpeed *= 1.0 - 0.6 * Math.min(1.0, Math.abs(error) / 120.0);
        }

        // ---- 向き・速さを少しずつ目標へ ----
        this.turnRate += Mth.clamp(targetTurn - this.turnRate,
                -ModConstants.ASTRA_VEIL_TURN_ACCELERATION, ModConstants.ASTRA_VEIL_TURN_ACCELERATION);
        this.yaw = Mth.wrapDegrees(this.yaw + this.turnRate);
        this.speed += Mth.clamp(targetSpeed - this.speed, -ModConstants.ASTRA_VEIL_ACCELERATION, ModConstants.ASTRA_VEIL_ACCELERATION);

        // ---- 高さ：地面の高さ + 目的地ごとの高さへ ----
        int bx = Mth.floor(x), bz = Mth.floor(z);
        if (level.hasChunk(bx >> 4, bz >> 4)) {
            double ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
            // 地面の高さはなめらかに追う（崖の上を通ったとたんに急上昇しないように）
            this.lastGround = Double.isNaN(this.lastGround) ? ground : this.lastGround + (ground - this.lastGround) * 0.01;
        }
        double targetVertical = 0;
        if (!Double.isNaN(this.lastGround)) {
            double targetY = this.lastGround + this.flightHeight;
            targetVertical = Mth.clamp((targetY - y) * 0.012, -ModConstants.ASTRA_VEIL_VERTICAL_SPEED, ModConstants.ASTRA_VEIL_VERTICAL_SPEED);
        }
        this.verticalSpeed += Mth.clamp(targetVertical - this.verticalSpeed,
                -ModConstants.ASTRA_VEIL_VERTICAL_ACCELERATION, ModConstants.ASTRA_VEIL_VERTICAL_ACCELERATION);

        // ---- 移動（向いている方向へ進む） ----
        float rad = this.yaw * Mth.DEG_TO_RAD;
        double nx = x - Mth.sin(rad) * this.speed;
        double nz = z + Mth.cos(rad) * this.speed;
        this.entity.applyFlight(nx, y + this.verticalSpeed, nz, this.yaw);
    }

    /** 次の目的地を選ぶ（home のまわり。今の場所から十分に離れた場所）。 */
    private void pickWaypoint(RandomSource random) {
        double x = this.entity.getX(), z = this.entity.getZ();
        double radius = ModConstants.ASTRA_VEIL_WANDER_RADIUS;
        double bestX = this.homeX, bestZ = this.homeZ, bestDistance = -1;
        for (int i = 0; i < 12; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double r = radius * Math.sqrt(random.nextDouble());
            double cx = this.homeX + Math.cos(angle) * r, cz = this.homeZ + Math.sin(angle) * r;
            double d = Math.sqrt((cx - x) * (cx - x) + (cz - z) * (cz - z));
            if (d > bestDistance) {
                bestDistance = d; bestX = cx; bestZ = cz;
            }
            if (d >= ModConstants.ASTRA_VEIL_WAYPOINT_MIN_DISTANCE) {
                break;
            }
        }
        this.waypointX = bestX; this.waypointZ = bestZ;
        this.hasWaypoint = true;
        this.stopAtWaypoint = random.nextFloat() < ModConstants.ASTRA_VEIL_HOVER_CHANCE;
        this.flightHeight = ModConstants.ASTRA_VEIL_FLIGHT_HEIGHT_MIN
                + random.nextDouble() * (ModConstants.ASTRA_VEIL_FLIGHT_HEIGHT_MAX - ModConstants.ASTRA_VEIL_FLIGHT_HEIGHT_MIN);
    }

    // =====================================================================
    // 外から使う操作（将来の攻撃・演出用）
    // =====================================================================

    /** その場に止まって漂わせる（tick 数）。減速してなめらかに止まる。 */
    public void hold(int ticks) {
        this.hoverTicks = Math.max(this.hoverTicks, ticks);
    }

    /** 指定した場所（水平位置）へ向かわせる。stop = true なら着いたら止まる。 */
    public void flyTo(double x, double z, boolean stop) {
        this.waypointX = x; this.waypointZ = z;
        this.hasWaypoint = true;
        this.stopAtWaypoint = stop;
        this.hoverTicks = 0;
    }

    /** うろつく範囲の中心を変える。 */
    public void setHome(double x, double z) {
        this.hasHome = true;
        this.homeX = x; this.homeZ = z;
    }

    /** 今の水平の速さ（ブロック / tick）。 */
    public double speed() {
        return this.speed;
    }

    /** 止まって漂っているか。 */
    public boolean isHovering() {
        return this.hoverTicks > 0;
    }

    // =====================================================================
    // 保存・読み込み
    // =====================================================================

    public void save(ValueOutput output) {
        if (this.hasHome) {
            output.putDouble("HomeX", this.homeX);
            output.putDouble("HomeZ", this.homeZ);
        }
    }

    public void load(ValueInput input) {
        double hx = input.getDoubleOr("HomeX", Double.NaN), hz = input.getDoubleOr("HomeZ", Double.NaN);
        if (!Double.isNaN(hx) && !Double.isNaN(hz)) {
            setHome(hx, hz);
        }
    }
}
