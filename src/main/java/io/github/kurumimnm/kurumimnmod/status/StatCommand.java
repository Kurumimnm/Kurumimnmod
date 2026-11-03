package io.github.kurumimnm.kurumimnmod.status;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.level.LevelCommand;
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
 * ステータス関連のコマンド（OP 権限レベル 2 以上）。
 * <pre>
 * /kmod stat reset &lt;対象&gt; … 振り分けをすべて取り消す（ポイントは全額戻る）
 * </pre>
 * /kmod は {@link LevelCommand} と共通（Brigadier が同じ名前のコマンドを自動でまとめる）。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class StatCommand {

    private StatCommand() {}

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(LevelCommand.COMMAND_NAME)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stat")
                        .then(Commands.literal("reset")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(StatCommand::reset)))));
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer player : targets) {
            StatManager.reset(player);
        }
        if (targets.size() == 1) {
            Component name = targets.iterator().next().getDisplayName();
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.kurumimnmod.stat.reset.single", name), true);
        } else {
            int count = targets.size();
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.kurumimnmod.stat.reset.multiple", count), true);
        }
        return targets.size();
    }
}
