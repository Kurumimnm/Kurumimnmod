package io.github.kurumimnm.kurumimnmod.job;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.level.LevelCommand;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 職業のコマンド（OP 権限レベル 2 以上）。
 * <pre>
 * /kmod job unlock &lt;対象&gt;        … 職業を解放して選択画面を開く（チュートリアルを達成したのと同じ）
 * /kmod job reset &lt;対象&gt;         … 選んだ職業を取り消して、選択画面を開く
 * /kmod job set &lt;対象&gt; &lt;職業&gt;   … 職業を強制的に変える（剣聖・拳聖・魔導などの上位職もこれで解放）
 * </pre>
 * set は「今のレベルから新しい職業になる」扱い。それまでのレベルアップ上昇は前の職業のまま残る。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class JobCommand {

    private JobCommand() {}

    private static final String KEY = "commands." + Kurumimnmod.MOD_ID + ".job.";

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(LevelCommand.COMMAND_NAME)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("job")
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> run(ctx, JobManager::unlock, "unlock"))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> run(ctx, JobManager::reset, "reset"))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("job", IdentifierArgument.id())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                        ctx.getSource().getServer().registryAccess().lookup(JobRegistry.JOB)
                                                                .map(r -> r.keySet().stream()).orElseGet(Stream::empty), builder))
                                                .executes(JobCommand::set))))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, Consumer<ServerPlayer> action, String key)
            throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        targets.forEach(action);
        int count = targets.size();
        ctx.getSource().sendSuccess(() -> Component.translatable(KEY + key, count), true);
        return count;
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        Identifier id = IdentifierArgument.getId(ctx, "job");
        int count = 0;
        for (ServerPlayer player : targets) {
            if (JobManager.forceSet(player, id)) {
                count++;
            }
        }
        if (count == 0) {
            ctx.getSource().sendFailure(Component.translatable(KEY + "unknown", id.toString()));
            return 0;
        }
        int n = count;
        Component name = Component.translatable(JobDefinition.nameKey(id));
        ctx.getSource().sendSuccess(() -> Component.translatable(KEY + "set", n, name), true);
        return n;
    }
}
