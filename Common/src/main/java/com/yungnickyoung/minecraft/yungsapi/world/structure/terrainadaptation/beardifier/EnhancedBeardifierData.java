package com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.beardifier;

import com.yungnickyoung.minecraft.yungsapi.mixin.BeardifierMixin;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.world.level.levelgen.NoiseChunk;

import javax.annotation.Nullable;

/**
 * Utility interface for use with {@link BeardifierMixin}.
 */
public interface EnhancedBeardifierData {
    ObjectListIterator<EnhancedBeardifierRigid> getEnhancedPieceIterator();
    void setEnhancedPieceIterator(ObjectListIterator<EnhancedBeardifierRigid> enhancedPieceIterator);

    ObjectListIterator<EnhancedJigsawJunction> getEnhancedJunctionIterator();
    void setEnhancedJunctionIterator(ObjectListIterator<EnhancedJigsawJunction> enhancedJunctionIterator);

    @Nullable NoiseChunk getNoiseChunk();
    void setNoiseChunk(NoiseChunk noiseChunk);
}
