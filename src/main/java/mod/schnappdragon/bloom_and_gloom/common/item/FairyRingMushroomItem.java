package mod.schnappdragon.bloom_and_gloom.common.item;

import net.minecraft.block.Block;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;

public class FairyRingMushroomItem extends BlockItem {
    public FairyRingMushroomItem(Block blockIn, Properties builder) {
        super(blockIn, builder);
    }

    @Override
    public ActionResultType itemInteractionForEntity(ItemStack stack, PlayerEntity playerIn, LivingEntity target, Hand hand) {
        if (target instanceof MooshroomEntity && ((MooshroomEntity) target).getMooshroomType() == MooshroomEntity.Type.BROWN) {
            MooshroomEntity mooshroom = (MooshroomEntity) target;
            if (mooshroom.hasStewEffect != null) {
                mooshroom.effectDuration *= 2;
                if (!playerIn.abilities.isCreativeMode) {
                    stack.shrink(1);
                }
                //capability

                for(int j = 0; j < 4; ++j) {
                    mooshroom.world.addParticle(ParticleTypes.EFFECT, mooshroom.getPosX() + mooshroom.getRNG().nextDouble() / 2.0D, mooshroom.getPosYHeight(0.5D), mooshroom.getPosZ() + mooshroom.getRNG().nextDouble() / 2.0D, 0.0D, mooshroom.getRNG().nextDouble() / 5.0D, 0.0D);
                }
            }
            else
                for (int i = 0; i < 2; ++i) {
                    mooshroom.world.addParticle(ParticleTypes.SMOKE, mooshroom.getPosX() + mooshroom.getRNG().nextDouble() / 2.0D, mooshroom.getPosYHeight(0.5D), mooshroom.getPosZ() + mooshroom.getRNG().nextDouble() / 2.0D, 0.0D, mooshroom.getRNG().nextDouble() / 5.0D, 0.0D);
                }

            return ActionResultType.func_233537_a_(mooshroom.world.isRemote);
        }

        return super.itemInteractionForEntity(stack, playerIn, target, hand);
    }
}