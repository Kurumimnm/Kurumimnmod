package io.github.kurumimnm.kurumimnmod.client.quest;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.network.ChooseWeaponPayload;
import io.github.kurumimnm.kurumimnmod.quest.QuestDefinition;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * クエストを受ける前に、最初の武器を選ぶ画面（クエストの starter_weapons）。
 * ボタンを押すとその武器を持ってクエストが始まる。「やめる」で閉じるとクエストは受けない。
 */
public class WeaponSelectScreen extends Screen {

    private static final String KEY = "screen." + Kurumimnmod.MOD_ID + ".weapon";
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;
    private static final int COLOR_TITLE = 0xFFFFD94A;
    private static final int COLOR_NOTE = 0xFFA0A0A0;

    private final Identifier quest;
    private int top;

    public WeaponSelectScreen(Identifier quest) {
        super(Component.translatable(KEY));
        this.quest = quest;
    }

    @Override
    protected void init() {
        List<QuestDefinition.StarterWeapon> weapons = ClientQuest.definition(this.quest)
                .map(QuestDefinition::starterWeapons).orElse(List.of());
        int x = (this.width - BUTTON_WIDTH) / 2;
        this.top = (this.height - (weapons.size() + 1) * (BUTTON_HEIGHT + GAP)) / 2 + 10;
        int y = this.top;
        for (int i = 0; i < weapons.size(); i++) {
            int index = i;
            this.addRenderableWidget(Button.builder(label(weapons.get(i)), b -> {
                ClientPacketDistributor.sendToServer(new ChooseWeaponPayload(this.quest, index));
                this.onClose();
            }).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
            y += BUTTON_HEIGHT + GAP;
        }
        this.addRenderableWidget(Button.builder(Component.translatable(KEY + ".cancel"), b -> this.onClose())
                .bounds(x, y + 6, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    /** ボタンの名前：name の lang キー。なければ最初のアイテムの名前。 */
    private static Component label(QuestDefinition.StarterWeapon weapon) {
        if (weapon.name().isPresent()) {
            return Component.translatable(weapon.name().get());
        }
        if (weapon.items().isEmpty()) {
            return Component.literal("?");
        }
        return new ItemStack(BuiltInRegistries.ITEM.getValue(weapon.items().get(0).item())).getHoverName();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(g, mouseX, mouseY, partialTicks);
        int cx = this.width / 2;
        g.centeredText(this.font, Component.translatable(KEY + ".title"), cx, this.top - 34, COLOR_TITLE);
        g.centeredText(this.font, Component.translatable(KEY + ".note"), cx, this.top - 20, COLOR_NOTE);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
