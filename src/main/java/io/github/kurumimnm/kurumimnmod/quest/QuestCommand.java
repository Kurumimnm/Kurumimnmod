package io.github.kurumimnm.kurumimnmod.quest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.level.LevelCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * クエスト・所持金のテスト用コマンド（OP のみ）。
 * <ul>
 *   <li>{@code /kmod quest list} … クエストの一覧</li>
 *   <li>{@code /kmod quest accept <クエスト>} … 受注（すぐ最初のフィールドへ）</li>
 *   <li>{@code /kmod quest abandon} … 辞退（受けた場所へ戻る）</li>
 *   <li>{@code /kmod quest clear} … 今いるフィールドをクリアにする（ゲートが開く）</li>
 *   <li>{@code /kmod quest hub} … 中間拠点へ移動</li>
 *   <li>{@code /kmod money get|add|set <金額>} … 所持金</li>
 * </ul>
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class QuestCommand {

    private QuestCommand() {}

    private static final String KEY = "commands." + Kurumimnmod.MOD_ID;

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(LevelCommand.COMMAND_NAME)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("quest")
                        .then(Commands.literal("list").executes(QuestCommand::list))
                        .then(Commands.literal("accept")
                                .then(Commands.argument("quest", IdentifierArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                QuestManager.registry(ctx.getSource().getServer())
                                                        .map(r -> r.keySet().stream()).orElseGet(java.util.stream.Stream::empty), builder))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            return QuestManager.accept(player, IdentifierArgument.getId(ctx, "quest")) ? 1 : 0;
                                        })))
                        .then(Commands.literal("abandon").executes(ctx -> {
                            QuestManager.abandon(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("clear").executes(QuestCommand::clear))
                        .then(Commands.literal("hub").executes(ctx -> {
                            QuestManager.goHub(ctx.getSource().getPlayerOrException());
                            return 1;
                        })))
                .then(Commands.literal("money")
                        .then(Commands.literal("get").executes(QuestCommand::moneyGet))
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", LongArgumentType.longArg())
                                        .executes(ctx -> moneyChange(ctx, LongArgumentType.getLong(ctx, "amount"), false))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                        .executes(ctx -> moneyChange(ctx, LongArgumentType.getLong(ctx, "amount"), true))))));
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        var registry = QuestManager.registry(ctx.getSource().getServer());
        if (registry.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (Identifier id : registry.get().keySet()) {
            ctx.getSource().sendSuccess(() -> Component.translatable(KEY + ".quest.entry", id.toString(),
                    Component.translatable(QuestDefinition.nameKey(id))), false);
            n++;
        }
        return n;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerQuest q = QuestManager.get(player);
        if (!q.inField()) {
            ctx.getSource().sendFailure(Component.translatable(KEY + ".quest.not_in_field"));
            return 0;
        }
        QuestManager.clearField(player, q.location());
        return 1;
    }

    private static int moneyGet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        long money = Money.get(ctx.getSource().getPlayerOrException());
        ctx.getSource().sendSuccess(() -> Component.translatable(KEY + ".money.get", money), false);
        return (int) Math.min(Integer.MAX_VALUE, money);
    }

    private static int moneyChange(CommandContext<CommandSourceStack> ctx, long amount, boolean absolute) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (absolute) {
            Money.set(player, amount);
        } else {
            Money.add(player, amount);
        }
        long money = Money.get(player);
        ctx.getSource().sendSuccess(() -> Component.translatable(KEY + ".money.get", money), true);
        return 1;
    }
}
