package mod.schnappdragon.habitat.common.levelgen.feature;

import com.mojang.serialization.Codec;
import mod.schnappdragon.habitat.common.block.FairyRingMushroomBlock;
import mod.schnappdragon.habitat.core.registry.HabitatBlocks;
import mod.schnappdragon.habitat.core.util.CompatHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;

public class HugeFairyRingMushroomFeature extends AbstractHugeMushroomFeature {
    public HugeFairyRingMushroomFeature(Codec<HugeMushroomFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<HugeMushroomFeatureConfiguration> context) {
        HugeMushroomFeatureConfiguration config = context.config();
        WorldGenLevel world = context.level();
        RandomSource random = context.random();
        BlockPos pos = context.origin();
        int i = this.getTreeHeight(random);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        if (!this.isValidPosition(world, pos, i, blockpos$mutableblockpos, config)) {
            return false;
        } else {
            this.makeCap(world, random, pos, i, blockpos$mutableblockpos, config);
            this.placeTrunk(world, random, pos, config, i, blockpos$mutableblockpos);
            return true;
        }
    }

    @Override
    protected int getTreeHeight(RandomSource rand) {
        int i = rand.nextInt(2) + 10;
        if (rand.nextInt(12) == 0)
            i *= 2;

        return i;
    }

    @Override
    protected void placeTrunk(LevelAccessor world, RandomSource rand, BlockPos pos, HugeMushroomFeatureConfiguration config, int i0, BlockPos.MutableBlockPos blockpos$mutable) {
        WeightedStateProvider mushroomProvider = new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder().add(HabitatBlocks.FAIRY_RING_MUSHROOM.get().defaultBlockState(), 1).add(HabitatBlocks.FAIRY_RING_MUSHROOM.get().defaultBlockState().setValue(FairyRingMushroomBlock.MUSHROOMS, 2), 2).add(HabitatBlocks.FAIRY_RING_MUSHROOM.get().defaultBlockState().setValue(FairyRingMushroomBlock.MUSHROOMS, 3), 3).add(HabitatBlocks.FAIRY_RING_MUSHROOM.get().defaultBlockState().setValue(FairyRingMushroomBlock.MUSHROOMS, 4), 3));

        BlockState stem = config.stemProvider.getState(rand, pos);
        if (CompatHelper.checkMods("enhanced_mushrooms") && stem.is(HabitatBlocks.FAIRY_RING_MUSHROOM_STEM.get()))
            stem = HabitatBlocks.ENHANCED_FAIRY_RING_MUSHROOM_STEM.get().defaultBlockState();

        for (int i = 0; i < i0; ++i) {
            blockpos$mutable.set(pos).move(Direction.UP, i);
            if (!world.getBlockState(blockpos$mutable).isSolidRender(world, blockpos$mutable)) {
                this.setBlock(world, blockpos$mutable, stem);
            }

            boolean breakFlag = false;
            if (i > i0 - 7) {
                for (int x = -1; x <= 1; ++x) {
                    for (int z = -1; z <= 1; ++z) {
                        BlockPos.MutableBlockPos inPos = new BlockPos.MutableBlockPos().setWithOffset(blockpos$mutable, x, 0, z);
                        if (!world.getBlockState(inPos).isSolidRender(world, inPos)) {
                            if (i > i0 - 6 && (x != 0 || z != 0) && rand.nextInt(12) == 0 && !world.getBlockState(inPos.below()).is(HabitatBlocks.FAIRYLIGHT.get())) {
                                this.setBlock(world, inPos, HabitatBlocks.FAIRYLIGHT.get().defaultBlockState());
                                breakFlag = true;
                                break;
                            }
                        }
                    }
                    if (breakFlag)
                        break;
                }
            }
        }

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int len = rand.nextInt(3) > 0 ? rand.nextBoolean() ? 1 + Mth.ceil((float) rand.nextInt(i0 - 7) / 2) : Mth.ceil((float) rand.nextInt(i0 - 7) / 2) : 0;
            blockpos$mutable.setWithOffset(pos, dir.getStepX(), -1, dir.getStepZ());

            for (int i = 0; i < len + 1; ++i) {
                blockpos$mutable.move(Direction.UP);
                if (!world.getBlockState(blockpos$mutable).isSolidRender(world, blockpos$mutable) && world.getBlockState(blockpos$mutable.below()).isSolidRender(world, blockpos$mutable.below())) {
                    if (i < len) {
                        BlockState stemState = stem;
                        if (stemState.hasProperty(HugeMushroomBlock.WEST) && stemState.hasProperty(HugeMushroomBlock.EAST) && stemState.hasProperty(HugeMushroomBlock.NORTH) && stemState.hasProperty(HugeMushroomBlock.SOUTH) && stemState.hasProperty(HugeMushroomBlock.UP)) {
                            stemState = stemState.setValue(HugeMushroomBlock.UP, i == len - 1);
                            if (world.getBlockState(blockpos$mutable.relative(dir.getOpposite())).is(stemState.getBlock())) {
                                stemState = stemState.setValue(getPropertyFromDirection(dir.getOpposite()), false);
                                this.setBlock(world, blockpos$mutable.relative(dir.getOpposite()), world.getBlockState(blockpos$mutable.relative(dir.getOpposite())).setValue(getPropertyFromDirection(dir), false));
                            }
                        }
                        this.setBlock(world, blockpos$mutable, stemState);
                    } else if (world.getBlockState(blockpos$mutable).isAir() && rand.nextInt(3) == 0)
                        this.setBlock(world, blockpos$mutable, mushroomProvider.getState(rand, blockpos$mutable));
                }
            }
        }

        for (int i = -1; i <= 1; i += 2) {
            for (int k = -1; k <= 1; k += 2) {
                for (int j = -1; j <= 1; ++j) {
                    blockpos$mutable.setWithOffset(pos, i, j, k);

                    if (world.getBlockState(blockpos$mutable).isAir() && world.getBlockState(blockpos$mutable.below()).isSolidRender(world, blockpos$mutable.below())) {
                        if (rand.nextInt(3) == 0)
                            this.setBlock(world, blockpos$mutable, mushroomProvider.getState(rand, blockpos$mutable));
                        break;
                    }
                }
            }
        }
    }

    private BooleanProperty getPropertyFromDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> HugeMushroomBlock.NORTH;
            case EAST -> HugeMushroomBlock.EAST;
            case SOUTH -> HugeMushroomBlock.SOUTH;
            case WEST -> HugeMushroomBlock.WEST;
            default -> null;
        };
    }

    protected void makeCap(LevelAccessor world, RandomSource rand, BlockPos pos, int i0, BlockPos.MutableBlockPos blockpos$mutable, HugeMushroomFeatureConfiguration config) {
        for (int i = i0 - 6; i <= i0; ++i) {
            int j = i < i0 ? config.foliageRadius : config.foliageRadius - 1;
            int k = config.foliageRadius - 2;

            for (int l = -j; l <= j; ++l) {
                for (int i1 = -j; i1 <= j; ++i1) {
                    boolean flag = l == -j;
                    boolean flag1 = l == j;
                    boolean flag2 = i1 == -j;
                    boolean flag3 = i1 == j;
                    boolean flag4 = flag || flag1;
                    boolean flag5 = flag2 || flag3;
                    if (i >= i0 || flag4 != flag5) {
                        blockpos$mutable.setWithOffset(pos, l, i, i1);
                        if (!world.getBlockState(blockpos$mutable).isSolidRender(world, blockpos$mutable)) {
                            BlockState blockstate = config.capProvider.getState(rand, pos);
                            if (blockstate.hasProperty(HugeMushroomBlock.WEST) && blockstate.hasProperty(HugeMushroomBlock.EAST) && blockstate.hasProperty(HugeMushroomBlock.NORTH) && blockstate.hasProperty(HugeMushroomBlock.SOUTH) && blockstate.hasProperty(HugeMushroomBlock.UP)) {
                                blockstate = blockstate.setValue(HugeMushroomBlock.UP, i >= i0 - 1).setValue(HugeMushroomBlock.WEST, l < -k).setValue(HugeMushroomBlock.EAST, l > k).setValue(HugeMushroomBlock.NORTH, i1 < -k).setValue(HugeMushroomBlock.SOUTH, i1 > k);
                            }

                            this.setBlock(world, blockpos$mutable, blockstate);
                        }
                    }
                }
            }
        }
    }

    protected int getTreeRadiusForHeight(int i1, int i2, int i3, int i4) {
        return i4 < i2 && i4 >= i2 - 3 || i4 == i2 ? i3 : 0;
    }
}