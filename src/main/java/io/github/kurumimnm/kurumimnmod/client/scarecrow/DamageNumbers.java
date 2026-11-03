package io.github.kurumimnm.kurumimnmod.client.scarecrow;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModClientConfigs;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.network.ScarecrowDamagePayload;
import io.github.kurumimnm.kurumimnmod.scarecrow.DamageKind;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ThreadLocalRandom;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 表示中のダメージ数字（クライアント専用）。エンティティ ID ごとに保持し、描画は {@link ScarecrowRenderer} が行う。
 * 時間は実時間（ミリ秒）で管理するので、ゲームの一時停止やラグの影響を受けない。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class DamageNumbers {

    private DamageNumbers() {}

    /**
     * 1 つのダメージ数字。
     *
     * @param offsetX 左右のばらつき（ブロック）
     */
    public record Entry(float amount, DamageKind kind, long startMillis, float offsetX) {}

    private static final Int2ObjectMap<Deque<Entry>> ENTRIES = new Int2ObjectOpenHashMap<>();
    private static int tickCounter;

    /** サーバーから届いたダメージを追加する。 */
    static void add(ScarecrowDamagePayload payload) {
        if (!ModClientConfigs.SHOW_DAMAGE_NUMBERS.get()) {
            return;
        }
        float spread = ModConstants.DAMAGE_NUMBER_SPREAD;
        float offsetX = (ThreadLocalRandom.current().nextFloat() - 0.5f) * spread;
        Deque<Entry> deque = ENTRIES.computeIfAbsent(payload.entityId(), id -> new ArrayDeque<>());
        deque.addLast(new Entry(payload.amount(), DamageKind.byId(payload.kind()), System.currentTimeMillis(), offsetX));
        while (deque.size() > ModConstants.DAMAGE_NUMBER_MAX_PER_ENTITY) {
            deque.removeFirst();
        }
    }

    /** 表示時間（ミリ秒）。 */
    static long durationMillis() {
        return Math.round(ModClientConfigs.DAMAGE_NUMBER_DURATION.get() * 1000);
    }

    /** 指定エンティティの表示中の数字（古い順）。期限切れはここで取り除く。無ければ null。 */
    static Deque<Entry> get(int entityId) {
        Deque<Entry> deque = ENTRIES.get(entityId);
        if (deque == null) {
            return null;
        }
        long expireBefore = System.currentTimeMillis() - durationMillis();
        while (!deque.isEmpty() && deque.peekFirst().startMillis() < expireBefore) {
            deque.removeFirst();
        }
        return deque.isEmpty() ? null : deque;
    }

    /** 画面外のカカシの数字が残り続けないよう、1 秒ごとに期限切れをまとめて掃除する。 */
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (++tickCounter % 20 != 0 || ENTRIES.isEmpty()) {
            return;
        }
        long expireBefore = System.currentTimeMillis() - durationMillis();
        ENTRIES.values().removeIf(deque -> {
            while (!deque.isEmpty() && deque.peekFirst().startMillis() < expireBefore) {
                deque.removeFirst();
            }
            return deque.isEmpty();
        });
    }
}
