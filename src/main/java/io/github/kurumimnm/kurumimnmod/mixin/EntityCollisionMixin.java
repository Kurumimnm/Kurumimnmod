package io.github.kurumimnm.kurumimnmod.mixin;

import com.google.common.collect.ImmutableList;
import io.github.kurumimnm.kurumimnmod.world.Borders;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ボーダー（赤い壁）を本物の壁にする。
 * <p>
 * バニラはエンティティが動くとき、周りのブロック・ワールドボーダーの形を集めてぶつかり判定をする
 * （{@code Entity#collectCollidersIgnoringWorldBorder}）。そこへ「入れない場所の柱」の形を足すので、
 * 壁に当たると止まり、壁に沿って滑るように歩ける（バニラのブロックの壁と同じ感触）。
 * プレイヤー・モブ・乗り物・アイテムなど、普通に動くものはすべて対象（ルールは {@link Borders}）。
 */
@Mixin(Entity.class)
public abstract class EntityCollisionMixin {

    @Inject(method = "collectCollidersIgnoringWorldBorder(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private static void kurumimnmod$addBorderWalls(@Nullable Entity source, Level level, List<VoxelShape> entityColliders, AABB boundingBox,
                                                   CallbackInfoReturnable<List<VoxelShape>> cir) {
        if (source == null) {
            return;
        }
        List<VoxelShape> walls = Borders.wallShapes(source, level, boundingBox);
        if (walls.isEmpty()) {
            return;
        }
        cir.setReturnValue(ImmutableList.<VoxelShape>builderWithExpectedSize(cir.getReturnValue().size() + walls.size())
                .addAll(cir.getReturnValue()).addAll(walls).build());
    }
}
