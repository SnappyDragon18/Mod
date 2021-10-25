package mod.schnappdragon.habitat.common.item;

import mod.schnappdragon.habitat.common.entity.monster.PookaEntity;
import mod.schnappdragon.habitat.core.HabitatConfig;
import mod.schnappdragon.habitat.core.registry.HabitatParticleTypes;
import mod.schnappdragon.habitat.core.registry.HabitatSoundEvents;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.Effects;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

public class FairyRingMushroomItem extends BlockItem {
    public FairyRingMushroomItem(Block blockIn, Properties builder) {
        super(blockIn, builder);
    }

    @Override
    public ActionResultType itemInteractionForEntity(ItemStack stack, PlayerEntity playerIn, LivingEntity target, Hand hand) {
        if (!playerIn.world.isRemote) {
            if (target instanceof MooshroomEntity && ((MooshroomEntity) target).getMooshroomType() == MooshroomEntity.Type.BROWN) {
                MooshroomEntity mooshroom = (MooshroomEntity) target;

                if (mooshroom.hasStewEffect == null) {
                    Pair<Effect, Integer> effect = getStewEffect();

                    mooshroom.hasStewEffect = effect.getLeft();
                    mooshroom.effectDuration = effect.getRight();

                    if (!playerIn.abilities.isCreativeMode)
                        stack.shrink(1);

                    for (int i = 0; i < 4; ++i) {
                        ((ServerWorld) playerIn.world).spawnParticle(ParticleTypes.EFFECT, mooshroom.getPosXRandom(0.5D), mooshroom.getPosYHeight(0.5D), mooshroom.getPosZRandom(0.5D), 0, 0.0D, mooshroom.getRNG().nextDouble(), 0.0D, 0.2D);
                        ((ServerWorld) playerIn.world).spawnParticle(HabitatParticleTypes.FAIRY_RING_SPORE.get(), mooshroom.getPosX() + mooshroom.getRNG().nextDouble() / 2.0D, mooshroom.getPosYHeight(0.5D), mooshroom.getPosZ() + mooshroom.getRNG().nextDouble() / 2.0D, 0, mooshroom.getRNG().nextGaussian(), 0.0D, mooshroom.getRNG().nextGaussian(), 0.01D);
                    }

                    playerIn.world.playMovingSound(null, mooshroom, SoundEvents.ENTITY_MOOSHROOM_EAT, mooshroom.getSoundCategory(), 2.0F, 1.0F);
                    return ActionResultType.SUCCESS;
                }

                for (int i = 0; i < 2; ++i)
                    ((ServerWorld) playerIn.world).spawnParticle(ParticleTypes.SMOKE, mooshroom.getPosX() + mooshroom.getRNG().nextDouble() / 2.0D, mooshroom.getPosYHeight(0.5D), mooshroom.getPosZ() + mooshroom.getRNG().nextDouble() / 2.0D, 0, 0.0D, mooshroom.getRNG().nextDouble(), 0.0D, 0.2D);
                return ActionResultType.SUCCESS;
            }
            else if (target.getType() == EntityType.RABBIT) {
                RabbitEntity rabbit = (RabbitEntity) target;
                rabbit.playSound(HabitatSoundEvents.ENTITY_RABBIT_CONVERTED_TO_POOKA.get(), 1.0F, rabbit.isChild() ? (rabbit.getRNG().nextFloat() - rabbit.getRNG().nextFloat()) * 0.2F + 1.5F : (rabbit.getRNG().nextFloat() - rabbit.getRNG().nextFloat()) * 0.2F + 1.0F);
                rabbit.remove();
                PookaEntity pooka = PookaEntity.convertRabbit(rabbit);
                playerIn.world.addEntity(pooka);
                playerIn.world.setEntityState(pooka, (byte) 15);

                if (!playerIn.abilities.isCreativeMode)
                    stack.shrink(1);

                return ActionResultType.SUCCESS;
            }
        }

        return super.itemInteractionForEntity(stack, playerIn, target, hand);
    }

    public static Pair<Effect, Integer> getStewEffect() {
        List<String> stewEffectPairs = Arrays.asList(StringUtils.deleteWhitespace(HabitatConfig.COMMON.suspiciousStewEffects.get()).split(","));
        String[] pair = stewEffectPairs.get((int) (Math.random() * stewEffectPairs.size())).split(":");
        Effect effect = Effect.get(Integer.parseInt(pair[0]));

        return Pair.of(effect != null ? effect : Effects.GLOWING, Integer.parseInt(pair[1]) * 20);
    }
}