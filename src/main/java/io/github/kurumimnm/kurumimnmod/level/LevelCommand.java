package io.github.kurumimnm.kurumimnmod.level;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * レベル関連のコマンド（OP 権限レベル 2 以上）。
 * <pre>
 * /kurumimnmod exp add &lt;対象&gt; &lt;量&gt;     … 経験値を付与（レベルアップ処理あり）
 * /kurumimnmod level set &lt;対象&gt; &lt;レベル&gt; … レベルを直接設定（経験値は 0 に戻る）
 * /kurumimnmod level query &lt;対象&gt;       … レベルと経験値を表示
 * </pre>
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class LevelCommand {

    private LevelCommand() {}

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(Kurumimnmod.MOD_ID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                // ---- /kurumimnmod exp add <targets> <amount> ----
                .then(Commands.literal("exp")
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                                .executes(LevelCommand::addExp)))))
                .then(Commands.literal("level")
                        // ---- /kurumimnmod level set <targets> <level> ----
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("level", IntegerArgumentType.integer(ModConstants.INITIAL_LEVEL))
                                                .executes(LevelCommand::setLevel))))
                        // ---- /kurumimnmod level query <target> ----
                        .then(Commands.literal("query")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(LevelCommand::query)))));
    }

    private static int addExp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        long amount = LongArgumentType.getLong(ctx, "amount");
        for (ServerPlayer player : targets) {
            LevelManager.addExp(player, amount, LevelManager.Reason.COMMAND);
        }
        sendResult(ctx, "commands.kurumimnmod.exp.add", targets, amount);
        return targets.size();
    }

    private static int setLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        int level = LevelCurve.clampLevel(IntegerArgumentType.getInteger(ctx, "level"));
        for (ServerPlayer player : targets) {
            LevelManager.setLevel(player, level);
        }
        sendResult(ctx, "commands.kurumimnmod.level.set", targets, level);
        return targets.size();
    }

    private static int query(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
        PlayerLevel data = LevelManager.get(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.kurumimnmod.level.query",
                player.getDisplayName(), data.level(), data.exp(), data.requiredExp()), false);
        return data.level();
    }

    /** 対象が 1 人なら名前、複数なら人数を表示する（バニラの /xp と同じ形式）。 */
    private static void sendResult(CommandContext<CommandSourceStack> ctx, String key, Collection<ServerPlayer> targets, Object value) {
        if (targets.size() == 1) {
            Component name = targets.iterator().next().getDisplayName();
            ctx.getSource().sendSuccess(() -> Component.translatable(key + ".single", value, name), true);
        } else {
            int count = targets.size();
            ctx.getSource().sendSuccess(() -> Component.translatable(key + ".multiple", value, count), true);
        }
    }
}
