package io.github.kurumimnm.kurumimnmod.quest;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

/**
 * クエスト用ディメンションの「地面の高さ（Y）」（{@link QuestPlots#height}）。
 * <p>
 * 地形の密度は「地面の高さ − Y」で作る（noise_settings/quest.json の final_density）ので、この値より下が地面、上が空になる。
 * 区画の外は {@link QuestPlots#VOID} を返すので、何もない（奈落）。
 * <pre>{@code { "type": "kurumimnmod:quest_height" } }</pre>
 */
public final class QuestHeightFunction implements DensityFunction {

    public static final QuestHeightFunction INSTANCE = new QuestHeightFunction();
    public static final MapCodec<QuestHeightFunction> CODEC = MapCodec.unit(INSTANCE);

    private QuestHeightFunction() {}

    @Override
    public DensitySampler compileSampler(DensityFunction.CompileContext context) {
        return Sampler.INSTANCE;
    }

    @Override
    public DensityFunction rewriteChildren(DfRewriteRule rule) {
        return this;
    }

    @Override
    public Interval range() {
        return Interval.of((float) QuestPlots.VOID, 320.0F);
    }

    @Override
    public int domainAxes() {
        return DensityFunction.AXIS_X | DensityFunction.AXIS_Z;
    }

    @Override
    public MapCodec<QuestHeightFunction> codec() {
        return CODEC;
    }

    private enum Sampler implements DensitySampler {
        INSTANCE;

        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
            // 高さ方向には同じ値なので、柱ごとに 1 回だけ計算する（並び順はバニラと同じ z → x → y）
            int index = 0;
            for (int z = 0; z < volume.sizeZ(); ++z) {
                int blockZ = volume.blockZ(z);
                for (int x = 0; x < volume.sizeX(); ++x) {
                    float value = (float) QuestPlots.height(volume.blockX(x), blockZ);
                    for (int y = 0; y < volume.sizeY(); ++y) {
                        outputBuffer.set(index++, value);
                    }
                }
            }
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            return (float) QuestPlots.height(blockX, blockZ);
        }
    }
}
