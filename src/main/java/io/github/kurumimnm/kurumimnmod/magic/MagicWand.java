package io.github.kurumimnm.kurumimnmod.magic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 「左クリックで魔法を発動する杖」の共通インターフェース。
 * <p>
 * 【操作】
 * <ul>
 *   <li>右クリック長押し … 着弾地点の照準（サークル）を表示（{@code client/magic/AoeIndicatorRenderer}）</li>
 *   <li>左クリック … 発動（照準を出していなくても発動できる）</li>
 * </ul>
 * 左クリックはクライアントで検知して {@code CastWandPayload} をサーバーへ送り、
 * サーバー側でこの {@link #cast} を呼ぶ（{@code client/magic/WandInputHandler}）。
 * 新しい杖を作るときはこのインターフェースを実装するだけで、左クリック発動に対応する。
 */
public interface MagicWand {

    /**
     * 魔法を発動する（サーバー側）。MP・クールダウンのチェックもこの中で行う。
     *
     * @param stack メインハンドに持っている杖
     */
    void cast(ServerPlayer player, ItemStack stack);

    /** 狙える最大距離（ブロック）。照準サークルの表示位置の計算に使う。 */
    double aimRange();

    /** 照準サークルの半径（ブロック）。0 以下ならサークルを表示しない。 */
    double aoeRadius();

    /** いま発動できるか（MP・クールダウン）。照準サークルの色分け用で、実際の判定は {@link #cast}。 */
    boolean canCast(Player player, ItemStack stack);
}
