package com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.adaptations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yungnickyoung.minecraft.yungsapi.world.structure.YungJigsawStructure;
import com.yungnickyoung.minecraft.yungsapi.world.structure.terrainadaptation.aquiferoverride.AquiferOverride;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import org.jetbrains.annotations.NotNull;

/**
 * Extra alternatives to vanilla's {@link TerrainAdjustment}, for use with {@link YungJigsawStructure}.
 * Uses a 3-D kernel matrix to smoothly generate noise values for use when generating terrain arround structures.
 */
public abstract class EnhancedTerrainAdaptation {
    public static final EnhancedTerrainAdaptation NONE = new NoneAdaptation();

    /**
     * The action to perform on the top of the structure piece.
     * "Top" refers to any blocks above the piece's bounding box minimum y-value.
     */
    private final TerrainAction topAction;

    /**
     * The action to perform on the bottom of the structure piece.
     * "Bottom" refers to any blocks below the piece's bounding box minimum y-value.
     */
    private final TerrainAction bottomAction;

    /**
     * The length of each dimension in the kernel.
     **/
    private final int kernelSize;

    /**
     * Determines how smoothly kernel values transition from 0 to 1 across the kernel.
     * This is analogous to the standard deviation in Gaussian blur functions.
     */
    private final int kernelDistance;

    /**
     * Offsets the piece's "ground" position (the point at which carving and bearding meet).
     * By default, this offset is zero and "ground" equals the bounding box minimum y-value.
     */
    private final double bottomOffset;

    /**
     * Padding to apply to the piece's bounding box when applying the terrain adaptation.
     * This can be used to effectively change the size of the adaptation in specific dimensions.
     * For example, if you use a padding of top=1, the piece's bounding box will be temporarily expanded by 1 block in the y-axis,
     * effectively making the terrain adaptation an ellipsoid instead of a sphere.
     * Note that padding values can also be negative to shrink the adaptation in specific dimensions.
     */
    private final Padding padding;

    /**
     * Whether to keep liquid blocks placed by aquifers in block positions that are also carved by the enhanced terrain adaptation.
     * If unset (or set to AquiferOverride.NONE), liquid blocks will be unmodified. This is vanilla behavior.
     * This field can be useful for underground structures that use a carving TerrainAction without a wall/ceiling.
     */
    private final AquiferOverride aquiferOverride;

    /**
     * 3-dimensional kernel for smoothing terrain.
     * Values will vary from near-zero along the edges, to near-1 in the middle.
     * These values are used as noise to modify the noise density at specific positions during terrain generation.
     */
    private final float[] kernel;

    abstract public EnhancedTerrainAdaptationType<?> type();

    EnhancedTerrainAdaptation(int kernelSize, int kernelDistance, TerrainAction topAction, TerrainAction bottomAction,
                              double bottomOffset, Padding padding, AquiferOverride aquiferOverride) {
        this.kernelSize = kernelSize;
        this.kernelDistance = kernelDistance;
        this.topAction = topAction;
        this.bottomAction = bottomAction;
        this.bottomOffset = bottomOffset;
        this.padding = padding;
        this.aquiferOverride = aquiferOverride;
        int kernelRadius = this.getKernelRadius();
        this.kernel = Util.make(new float[kernelSize * kernelSize * kernelSize], (kernel) -> {
            for (int x = 0; x < kernelSize; ++x) {
                for (int y = 0; y < kernelSize; ++y) {
                    for (int z = 0; z < kernelSize; ++z) {
                        int i = index(x, y, z);
                        double kernelX = x - kernelRadius;
                        double kernelY = y - kernelRadius + 0.5;
                        double kernelZ = z - kernelRadius;
                        kernel[i] = computeKernelValue(kernelX, kernelY, kernelZ);
                    }
                }
            }
        });
    }

    /**
     * Computes the kernel value for a given x-, y-, and z-distance.
     *
     * @return A value from 0 to 1.0.
     */
    private float computeKernelValue(double xDistance, double yDistance, double zDistance) {
        double squaredDistance = Mth.lengthSquared(xDistance, yDistance, zDistance);
        return (float) Math.pow(Math.E, -squaredDistance / this.kernelDistance);
    }

    public TerrainAction topAction() {
        return this.topAction;
    }

    public TerrainAction bottomAction() {
        return this.bottomAction;
    }

    public double getBottomOffset() {
        return this.bottomOffset;
    }

    public Padding getPadding() {
        return this.padding;
    }

    public AquiferOverride getAquiferOverride() {
        return aquiferOverride;
    }

    public int getKernelSize() {
        return this.kernelSize;
    }

    public int getKernelRadius() {
        return this.kernelSize / 2;
    }

    public int getKernelDistance() {
        return this.kernelDistance;
    }

    public float[] getKernel() {
        return this.kernel;
    }

    /**
     * Computes the noise density factor at a single location given the provided values.
     *
     * @param xDistance              Distance in the x-axis
     * @param yDistance              Distance in the y-axis
     * @param zDistance              Distance in the z-axis
     * @param yDistanceToPieceBottom Distance in the y-axis to the base of the piece,
     *                               the point at which carving and bearding meet
     * @return Noise density factor due to enhanced terrain adaptation
     */
    public double computeDensityFactor(
            int xDistance,
            int yDistance,
            int zDistance,
            int yDistanceToPieceBottom
    ) {
        int kernelRadius = this.getKernelRadius();
        int kernelX = xDistance + kernelRadius;
        int kernelY = yDistance + kernelRadius;
        int kernelZ = zDistance + kernelRadius;
        if (isInKernelRange(kernelX) && isInKernelRange(kernelY) && isInKernelRange(kernelZ)) {
            // Get kernel value for this distance.
            // Returns a value from 0 (high distance) to 1 (zero distance)
            int i = index(kernelX, kernelY, kernelZ);
            float kernelValue = this.getKernel()[i];

            double actualYDistanceToPieceBottom = (double) yDistanceToPieceBottom + 0.5;

            // Calculate squared distance from point to the beard base.
            // We use the bottom of the piece as our target since that is the y-position that determines using topAction vs bottomAction.
            double squaredDistance = Mth.lengthSquared(xDistance, actualYDistanceToPieceBottom, zDistance);

            // Calculate multiplier for final noise value.
            // The closer we are (i.e. smaller distance), the larger the multiplier.
            double multiplier = Math.abs(actualYDistanceToPieceBottom * Mth.invSqrt(squaredDistance / 2.0) / 2.0);

            // Get the density modifier for the action we are performing.
            boolean isAboveBeardBase = actualYDistanceToPieceBottom > 0;
            int densityModifier = isAboveBeardBase ? this.topAction.getDensityModifier() : this.bottomAction.getDensityModifier();

            return multiplier * kernelValue * densityModifier;
        } else {
            return 0;
        }
    }

    private boolean isInKernelRange(int i) {
        return i >= 0 && i < this.kernelSize;
    }

    private int index(int x, int y, int z) {
        return z * this.kernelSize * this.kernelSize + x * this.kernelSize + y;
    }

    /**
     * Padding to apply to the piece's bounding box when applying the terrain adaptation.
     * This can be used to effectively change the size of the adaptation in specific dimensions.
     * For example, if you use a padding of top=1, the piece's bounding box will be temporarily expanded by 1 block in the y-axis,
     * effectively making the terrain adaptation an ellipsoid instead of a sphere.
     * Note that padding values can also be negative to shrink the adaptation in specific dimensions.
     */
    public record Padding(int x, int top, int bottom, int z) {
            public static final Padding ZERO = new Padding(0, 0, 0, 0);
            public static final Codec<Padding> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
                    Codec.INT.optionalFieldOf("x", 0).forGetter((padding) -> padding.x),
                    Codec.INT.optionalFieldOf("top", 0).forGetter((padding) -> padding.top),
                    Codec.INT.optionalFieldOf("bottom", 0).forGetter((padding) -> padding.bottom),
                    Codec.INT.optionalFieldOf("z", 0).forGetter((padding) -> padding.z)
            ).apply(instance, Padding::new));
    }

    /**
     * Represents an action to take when adapting terrain around a structure piece.
     * This is used to modify the noise density at the position of the terrain action.
     */
    public enum TerrainAction implements StringRepresentable {
        CARVE("carve", -1),
        BURY("bury", 1),
        NONE("none", 0);

        public static final Codec<TerrainAction> CODEC = StringRepresentable.fromValues(TerrainAction::values);
        private final String name;

        /**
         * The modifier to apply to the noise density at the position of the terrain action.
         * This is a simple sign multiplier to the noise density.
         * Carving uses -1 to reduce the density, burying uses +1 to increase the density, and none uses 0 to keep the density the same.
         */
        private final int densityModifier;

        TerrainAction(String name, int densityModifier) {
            this.name = name;
            this.densityModifier = densityModifier;
        }

        public int getDensityModifier() {
            return densityModifier;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
