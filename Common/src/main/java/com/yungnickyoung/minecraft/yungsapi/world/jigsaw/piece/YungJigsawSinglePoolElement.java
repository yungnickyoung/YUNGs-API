package com.yungnickyoung.minecraft.yungsapi.world.jigsaw.piece;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yungnickyoung.minecraft.yungsapi.module.StructurePoolElementTypeModule;
import com.yungnickyoung.minecraft.yungsapi.world.condition.ConditionContext;
import com.yungnickyoung.minecraft.yungsapi.world.condition.StructureCondition;
import com.yungnickyoung.minecraft.yungsapi.world.condition.StructureConditionType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import com.yungnickyoung.minecraft.yungsapi.world.condition.AnyOfCondition;
import com.yungnickyoung.minecraft.yungsapi.world.condition.AllOfCondition;

import java.util.Optional;

/**
 * Custom {@link SinglePoolElement} with support for many additional settings.
 */
public class YungJigsawSinglePoolElement extends SinglePoolElement {
    public static final Codec<YungJigsawSinglePoolElement> CODEC = RecordCodecBuilder.create((builder) -> builder
            .group(
                    templateCodec(),
                    processorsCodec(),
                    projectionCodec(),
                    Codec.STRING.optionalFieldOf("name").forGetter(element -> element.name),
                    ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("max_count").forGetter(element -> element.maxCount),
                    ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("min_required_depth").forGetter(element -> element.minRequiredDepth),
                    ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("max_possible_depth").forGetter(element -> element.maxPossibleDepth),
                    Codec.BOOL.optionalFieldOf("is_priority", false).forGetter(element -> element.isPriority),
                    Codec.BOOL.optionalFieldOf("ignore_bounds", false).forGetter(element -> element.ignoreBounds),
                    StructureConditionType.CONDITION_CODEC.optionalFieldOf("condition").forGetter(element -> element.condition)
            ).apply(builder, YungJigsawSinglePoolElement::new));

    /**
     * The maximum possible number of pieces with this name.
     * If this setting is used, the 'name' MUST be specified.
     */
    public final Optional<Integer> maxCount;

    /**
     * The name of this piece.
     * Used to uniquely identify pieces for the sake of tracking max_count.
     */
    public final Optional<String> name;

    /**
     * The minimum required distance (in jigsaw pieces) from the structure start
     * at which this piece can spawn.
     */
    public final Optional<Integer> minRequiredDepth;

    /**
     * The maximum allowed distance (in jigsaw pieces) from the structure start
     * at which this piece will no longer spawn.
     */
    public final Optional<Integer> maxPossibleDepth;

    /**
     * Whether this is a priority piece.
     * Priority pieces attempt to generate before all other pieces in the pool,
     * regardless of weight.
     */
    public final boolean isPriority;

    /**
     * Whether this piece should ignore the usual piece boundary checks.
     * Enabling this allows this piece to spawn partially overlap other pieces.
     */
    public final boolean ignoreBounds;

    /**
     * The condition required for this piece to spawn.
     * Can be any {@link StructureCondition}, including compound conditions ({@link AnyOfCondition}, {@link AllOfCondition}
     */
    public final Optional<StructureCondition> condition;

    public YungJigsawSinglePoolElement(
            Either<ResourceLocation, StructureTemplate> resourceLocation,
            Holder<StructureProcessorList> processors,
            StructureTemplatePool.Projection projection,
            Optional<String> name,
            Optional<Integer> maxCount,
            Optional<Integer> minRequiredDepth,
            Optional<Integer> maxPossibleDepth,
            boolean isPriority,
            boolean ignoreBounds,
            Optional<StructureCondition> condition
    ) {
        super(resourceLocation, processors, projection);
        this.maxCount = maxCount;
        this.name = name;
        this.minRequiredDepth = minRequiredDepth;
        this.maxPossibleDepth = maxPossibleDepth;
        this.isPriority = isPriority;
        this.ignoreBounds = ignoreBounds;
        this.condition = condition;
    }

    public StructurePoolElementType<?> getType() {
        return StructurePoolElementTypeModule.YUNG_SINGLE_ELEMENT;
    }

    public boolean isPriorityPiece() {
        return isPriority;
    }

    public boolean ignoresBounds() {
        return this.ignoreBounds;
    }

    public boolean isAtValidDepth(int depth) {
        boolean isAtMinRequiredDepth = minRequiredDepth.isEmpty() || minRequiredDepth.get() <= depth;
        boolean isAtMaxAllowableDepth = maxPossibleDepth.isEmpty() || maxPossibleDepth.get() >= depth;
        return isAtMinRequiredDepth && isAtMaxAllowableDepth;
    }

    public boolean passesConditions(ConditionContext ctx) {
        return this.condition.isEmpty() || this.condition.get().passes(ctx);
    }

    public String toString() {
        return String.format("YungJigsawSingle[%s][%s][%s][%s][%s][%s]",
                this.name,
                this.template,
                this.maxCount.isPresent() ? maxCount.get() : "N/A",
                this.minRequiredDepth.isPresent() ? "" + minRequiredDepth.get() : "N/A",
                this.maxPossibleDepth.isPresent() ? "" + maxPossibleDepth.get() : "N/A",
                this.isPriority);
    }
}