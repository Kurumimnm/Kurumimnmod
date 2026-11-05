package io.github.kurumimnm.kurumimnmod.world;

import com.mojang.brigadier.CommandDispatcher;
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
 * /kmod area   … 今いるフィールド・エリア・危険度を表示
 * /kmod towns  … 街の番号と座標の一覧を表示（/tp で移動するとき用）
 * </pre>
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class WorldCommand {

    private WorldCommand() {}

    private static final String KEY = "commands." + Kurumimnmod.MOD_ID;

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(LevelCommand.COMMAND_NAME)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("area").executes(WorldCommand::area))
                .then(Commands.literal("towns").executes(WorldCommand::towns)));
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

    private static int towns(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Optional<FieldMap> map = FieldLookup.map(source.getLevel());
        if (map.isEmpty()) {
            source.sendFailure(Component.translatable(KEY + ".area.not_field_world"));
            return 0;
        }
        for (FieldMap.Town town : map.get().towns()) {
            source.sendSuccess(() -> Component.translatable(KEY + ".towns.entry",
                    town.number(), town.x(), town.z()), false);
        }
        return map.get().towns().size();
    }
}
