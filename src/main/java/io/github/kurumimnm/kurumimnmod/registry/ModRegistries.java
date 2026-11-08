package io.github.kurumimnm.kurumimnmod.registry;

import net.neoforged.bus.api.IEventBus;

/**
 * 各 DeferredRegister を MOD イベントバスへまとめて登録するクラス。
 * <p>
 * 新しいレジストリ（エンティティ、サウンド、ブロックエンティティ等）を
 * 追加したら、ここに 1 行追加する。
 */
public final class ModRegistries {

    private ModRegistries() {}

    public static void register(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModWorldgen.register(modEventBus);
    }
}
