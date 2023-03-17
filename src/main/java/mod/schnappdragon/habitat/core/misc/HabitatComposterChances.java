package mod.schnappdragon.habitat.core.misc;

import mod.schnappdragon.habitat.core.registry.HabitatItems;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.ComposterBlock;

public class HabitatComposterChances {
    public static void registerComposterChances() {
        put(HabitatItems.RAFFLESIA.get(), 0.65F);

        put(HabitatItems.KABLOOM_PULP.get(), 0.3F);
        put(HabitatItems.KABLOOM_FRUIT.get(), 0.5F);
        put(HabitatItems.KABLOOM_FRUIT_PILE.get(), 1.0F);
        put(HabitatItems.KABLOOM_PULP_BLOCK.get(), 1.0F);

        put(HabitatItems.SLIME_FERN.get(), 0.65F);

        put(HabitatItems.ORANGE_BALL_CACTUS_FLOWER.get(), 0.3F);
        put(HabitatItems.PINK_BALL_CACTUS_FLOWER.get(), 0.3F);
        put(HabitatItems.RED_BALL_CACTUS_FLOWER.get(), 0.3F);
        put(HabitatItems.YELLOW_BALL_CACTUS_FLOWER.get(), 0.3F);
        put(HabitatItems.ORANGE_BALL_CACTUS.get(), 0.5F);
        put(HabitatItems.PINK_BALL_CACTUS.get(), 0.5F);
        put(HabitatItems.RED_BALL_CACTUS.get(), 0.5F);
        put(HabitatItems.YELLOW_BALL_CACTUS.get(), 0.5F);
        put(HabitatItems.DRIED_BALL_CACTUS.get(), 0.3F);

        put(HabitatItems.FAIRY_RING_MUSHROOM.get(), 0.65F);
        put(HabitatItems.FAIRY_RING_MUSHROOM_BLOCK.get(), 0.65F);
        put(HabitatItems.FAIRY_RING_MUSHROOM_STEM.get(), 0.65F);
        put(HabitatItems.FAIRYLIGHT.get(), 0.65F);

        put(HabitatItems.EDELWEISS.get(), 0.65F);

        put(HabitatItems.ORANGE_BALL_CACTUS_BLOCK.get(), 0.5F);
        put(HabitatItems.PINK_BALL_CACTUS_BLOCK.get(), 0.5F);
        put(HabitatItems.RED_BALL_CACTUS_BLOCK.get(), 0.5F);
        put(HabitatItems.YELLOW_BALL_CACTUS_BLOCK.get(), 0.5F);
        put(HabitatItems.FLOWERING_ORANGE_BALL_CACTUS_BLOCK.get(), 0.5F);
        put(HabitatItems.FLOWERING_PINK_BALL_CACTUS_BLOCK.get(), 0.5F);
        put(HabitatItems.FLOWERING_RED_BALL_CACTUS_BLOCK.get(), 0.5F);
        put(HabitatItems.FLOWERING_YELLOW_BALL_CACTUS_BLOCK.get(), 0.5F);
        put(HabitatItems.DRIED_BALL_CACTUS_BLOCK.get(), 0.5F);
    }

    private static void put(ItemLike item, float value) {
        ComposterBlock.COMPOSTABLES.put(item, value);
    }
}