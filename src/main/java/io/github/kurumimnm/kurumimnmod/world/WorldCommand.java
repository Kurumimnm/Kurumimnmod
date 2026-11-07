package io.github.kurumimnm.kurumimnmod.world;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kurumimnm.kurumimnmod.registry.ModAttachments;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.brigadier.context.CommandContext;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.level.LevelCommand;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * フィールドマップ確認用のコマンド（OP 権限レベル 2 以上）。
 * <pre>
 * /kmod area                 … 今いるフィールド・エリア・危険度を表示
 * /kmod towns                … 街の番号と座標の一覧を表示（/tp で移動するとき用）
 * /kmod flag grant &lt;フラグ|all&gt;  … 進行フラグを持たせる（実績を付与。ゲートのテスト用。例：gate）
 * /kmod flag revoke &lt;フラグ|all&gt; … 進行フラグを取り消す
 * /kmod explore all          … すべての街・エリアに行ったことにする（地図のテスト用）
 * /kmod explore reset        … 探索の記録を消す
 * </pre>
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class WorldCommand {

    private WorldCommand() {}

    private static final String KEY = "commands." + Kurumimnmod.MOD_ID;
    /** 進行フラグの実績の条件名（JSON の criteria と同じにする）。 */
    private static final String FLAG_CRITERION = "done";

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(LevelCommand.COMMAND_NAME)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("area").executes(WorldCommand::area))
                .then(Commands.literal("towns").executes(WorldCommand::towns))
                .then(Commands.literal("flag")
                        .then(Commands.literal("grant")
                                .then(Commands.argument("flag", StringArgumentType.word())
                                        .executes(ctx -> setFlag(ctx, StringArgumentType.getString(ctx, "flag"), true))))
                        .then(Commands.literal("revoke")
                                .then(Commands.argument("flag", StringArgumentType.word())
                                        .executes(ctx -> setFlag(ctx, StringArgumentType.getString(ctx, "flag"), false)))))
                .then(Commands.literal("explore")
                        .then(Commands.literal("all").executes(WorldCommand::exploreAll))
                        .then(Commands.literal("reset").executes(WorldCommand::exploreReset))));
    }

    private static int area(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Vec3 pos = source.getPosition();
        Optional<FieldMap.Location> location = FieldLookup.locate(source.getLevel(), pos.x, pos.z);
        if (location.isEmpty()) {
            source.sendFailure(Component.translatable(KEY + ".area.not_field_world"));
            return 0;
        }
        FieldMap.Location loc = location.get();
        // フィールド名はバイオーム名の翻訳キー（biome.kurumimnmod.xxx）を使う
        Component fieldName = loc.field().biome().unwrapKey()
                .map(key -> (Component) Component.translatable(key.identifier().toLanguageKey("biome")))
                .orElse(Component.literal(loc.field().id()));
        source.sendSuccess(() -> Component.translatable(KEY + ".area.info",
                loc.field().number(), fieldName, loc.area(), loc.danger()), false);
        if (loc.inMountains()) {
            source.sendSuccess(() -> Component.translatable(KEY + ".area.mountains"), false);
        }
        return loc.danger();
    }

    /** 進行フラグを付ける／外す。flag = all ならすべて。 */
    private static int setFlag(CommandContext<CommandSourceStack> ctx, String flag, boolean grant) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Optional<FieldMap> map = FieldLookup.map(player.level());
        if (map.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable(KEY + ".area.not_field_world"));
            return 0;
        }
        int count = 0;
        for (String f : map.get().flags()) {
            if (!flag.equals("all") && !f.equals(flag)) {
                continue;
            }
            AdvancementHolder holder = player.level().getServer().getAdvancements().get(ExplorationTracker.flagAdvancementId(f));
            if (holder != null) {
                if (grant) {
                    player.getAdvancements().award(holder, FLAG_CRITERION);
                } else {
                    player.getAdvancements().revoke(holder, FLAG_CRITERION);
                }
                count++;
            }
        }
        ExplorationTracker.syncFlagsFromAdvancements(player);
        int result = count;
        ctx.getSource().sendSuccess(() -> Component.translatable(KEY + (grant ? ".flag.granted" : ".flag.revoked"), result, flag), true);
        return count;
    }

    private static int exploreAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Optional<FieldMap> map = FieldLookup.map(player.level());
        if (map.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable(KEY + ".area.not_field_world"));
            return 0;
        }
        Set<String> towns = new HashSet<>();
        Set<Integer> fields = new HashSet<>();
        Set<Integer> areas = new HashSet<>();
        map.get().towns().forEach(t -> towns.add(t.id()));
        for (FieldMap.Field f : map.get().fields()) {
            fields.add(f.number());
            f.areas().forEach(a -> areas.add(FieldRegion.areaKey(f.number(), a.area())));
        }
        player.setData(ModAttachments.EXPLORATION, player.getData(ModAttachments.EXPLORATION).withVisited(towns, fields, areas));
        ctx.getSource().sendSuccess(() -> Component.translatable(KEY + ".explore.all"), true);
        return 1;
    }

    private static int exploreReset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        player.setData(ModAttachments.EXPLORATION, player.getData(ModAttachments.EXPLORATION).withVisited(Set.of(), Set.of(), Set.of()));
        ExplorationTracker.resetTrack(player); // 今いる場所は記録し直す
        ctx.getSource().sendSuccess(() -> Component.translatable(KEY + ".explore.reset"), true);
        return 1;
    }

    private static int towns(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Optional<FieldMap> map = FieldLookup.map(source.getLevel());
        if (map.isEmpty()) {
            source.sendFailure(Component.translatable(KEY + ".area.not_field_world"));
            return 0;
        }
        for (FieldMap.Town town : map.get().towns()) {
            source.sendSuccess(() -> Component.translatable(KEY + ".towns.entry",
                    Component.translatable("town." + Kurumimnmod.MOD_ID + "." + town.id()), town.x(), town.z()), false);
        }
        return map.get().towns().size();
    }
}
