package io.github.kurumimnm.kurumimnmod.quest;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

/**
 * クエストの最初の武器（クエストの JSON の starter_weapons）。
 * <ul>
 *   <li>受けるときに選択画面で 1 つ選ぶ → {@link #give} で渡す</li>
 *   <li>耐久があるものは<b>耐久無限</b>。目印（カスタムデータ {@value #MARK}）と説明（「クエスト終了時に消える」）を付ける</li>
 *   <li>クエストが終わったら（達成・辞退）目印の付いたアイテムを持ち物から消す（{@link #removeAll}）</li>
 *   <li>死んだときは落とさず消え、生き返ったときにクエストが続いていれば渡し直す（{@link QuestManager}）</li>
 * </ul>
 */
public final class QuestWeapons {

    private QuestWeapons() {}

    /** クエスト用のアイテムの目印（カスタムデータのキー）。 */
    public static final String MARK = Kurumimnmod.MOD_ID + "_quest_item";

    /** 番号 index の武器を渡す（持ち物がいっぱいなら足もとに落とす）。 */
    public static void give(ServerPlayer player, QuestDefinition def, int index) {
        if (index < 0 || index >= def.starterWeapons().size()) {
            return;
        }
        for (QuestDefinition.Grant grant : def.starterWeapons().get(index).items()) {
            Optional<Item> item = BuiltInRegistries.ITEM.getOptional(grant.item());
            if (item.isEmpty()) {
                Kurumimnmod.LOGGER.warn("Quest starter weapon: unknown item '{}' (skipped)", grant.item());
                continue;
            }
            ItemStack stack = new ItemStack(item.get(), grant.count());
            if (stack.isDamageableItem()) {
                stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
            }
            CompoundTag tag = new CompoundTag();
            tag.putBoolean(MARK, true);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            stack.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.translatable("item." + Kurumimnmod.MOD_ID + ".quest_item.lore").withStyle(ChatFormatting.GRAY))));
            if (!player.addItem(stack) && !stack.isEmpty()) {
                player.spawnAtLocation((ServerLevel) player.level(), stack); // 入りきらなかった分は足もとへ
            }
        }
    }

    /** クエスト用のアイテムか（目印が付いているか）。 */
    public static boolean isQuestItem(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(MARK);
    }

    /** 持ち物にクエスト用のアイテムがあるか。 */
    public static boolean hasAny(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isQuestItem(inv.getItem(i))) {
                return true;
            }
        }
        return false;
    }

    /** 持ち物からクエスト用のアイテムをすべて消す。 */
    public static void removeAll(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isQuestItem(inv.getItem(i))) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
    }
}
