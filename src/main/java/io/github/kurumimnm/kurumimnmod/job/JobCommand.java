package io.github.kurumimnm.kurumimnmod.job;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.level.LevelCommand;
import java.util.Collection;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 職業のコマンド（OP 権限レベル 2 以上・テスト用）。
 * <pre>
 * /kmod job unlock &lt;対象&gt; … 職業を解放して選択画面を開く（チュートリアルを達成したのと同じ）
 * /kmod job reset &lt;対象&gt;  … 選んだ職業を取り消して、選択画面を開く
 * </pre>
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class JobCommand {

    private JobCommand() {}

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
                                        .executes(ctx -> run(ctx, JobManager::reset, "reset"))))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, Consumer<ServerPlayer> action, String key)
            throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        targets.forEach(action);
        int count = targets.size();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands." + Kurumimnmod.MOD_ID + ".job." + key, count), true);
        return count;
    }
}
