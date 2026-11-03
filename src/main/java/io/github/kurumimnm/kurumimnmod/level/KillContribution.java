package io.github.kurumimnm.kurumimnmod.level;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 「どのプレイヤーが、最後にいつ関わったか」を記録する入れ物（アシスト判定用）。
 * <p>
 * 保存はしない一時データ（ワールドを再読み込みすると消える）。用途は 3 つ:
 * <ul>
 *   <li>敵側 {@code damagers}  … その敵にダメージを与えたプレイヤー</li>
 *   <li>敵側 {@code debuffers} … その敵にデバフ（有害なエフェクト）を付けたプレイヤー</li>
 *   <li>プレイヤー側 {@code supporters} … そのプレイヤーにバフ（有益なエフェクト）を付けた他のプレイヤー</li>
 *   <li>ボス側 {@code supporters} … ボスを攻撃した人にバフを付けていたプレイヤー（攻撃した時点で記録）</li>
 * </ul>
 * 時刻はワールドのゲーム時間（tick）。同じプレイヤーが再び関わると時刻が上書きされ、
 * 有効時間（Config: assistWindowSeconds）がそこから数え直しになる。
 */
public final class KillContribution {

    private final Map<UUID, Long> damagers = new HashMap<>();
    private final Map<UUID, Long> debuffers = new HashMap<>();
    private final Map<UUID, Long> supporters = new HashMap<>();

    public void recordDamage(UUID player, long gameTime) {
        damagers.put(player, gameTime);
    }

    public void recordDebuff(UUID player, long gameTime) {
        debuffers.put(player, gameTime);
    }

    /** バフを付けたプレイヤーを記録する。古い記録はついでに掃除する。 */
    public void recordSupport(UUID player, long gameTime, long windowTicks) {
        supporters.values().removeIf(time -> gameTime - time > windowTicks);
        supporters.put(player, gameTime);
    }

    /** 支援者をまとめて記録する（ボス用。時間で掃除しない）。 */
    public void recordSupports(Set<UUID> players, long gameTime) {
        for (UUID player : players) {
            supporters.put(player, gameTime);
        }
    }

    /** 有効時間内にダメージを与えたプレイヤー。 */
    public Set<UUID> recentDamagers(long gameTime, long windowTicks) {
        return recent(damagers, gameTime, windowTicks);
    }

    /** 有効時間内にデバフを付けたプレイヤー。 */
    public Set<UUID> recentDebuffers(long gameTime, long windowTicks) {
        return recent(debuffers, gameTime, windowTicks);
    }

    /** 有効時間内にバフをくれたプレイヤー。 */
    public Set<UUID> recentSupporters(long gameTime, long windowTicks) {
        return recent(supporters, gameTime, windowTicks);
    }

    private static Set<UUID> recent(Map<UUID, Long> map, long gameTime, long windowTicks) {
        return map.entrySet().stream()
                .filter(e -> gameTime - e.getValue() <= windowTicks)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
