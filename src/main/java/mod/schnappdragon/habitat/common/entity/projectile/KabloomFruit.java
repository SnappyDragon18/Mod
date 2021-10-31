package mod.schnappdragon.habitat.common.entity.projectile;

import mod.schnappdragon.habitat.core.misc.HabitatDamageSources;
import mod.schnappdragon.habitat.core.registry.HabitatEntityTypes;
import mod.schnappdragon.habitat.core.registry.HabitatItems;
import mod.schnappdragon.habitat.core.registry.HabitatSoundEvents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fmllegacy.network.NetworkHooks;

public class KabloomFruit extends ThrowableItemProjectile {
    public KabloomFruit(EntityType<? extends KabloomFruit> entity, Level world) {
        super(entity, world);
    }

    public KabloomFruit(Level worldIn, LivingEntity throwerIn) {
        super(HabitatEntityTypes.KABLOOM_FRUIT.get(), throwerIn, worldIn);
    }

    public KabloomFruit(Level worldIn, double x, double y, double z) {
        super(HabitatEntityTypes.KABLOOM_FRUIT.get(), x, y, z, worldIn);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            this.level.playLocalSound(this.getX(), this.getY(), this.getZ(), HabitatSoundEvents.ENTITY_KABLOOM_FRUIT_EXPLODE.get(), SoundSource.NEUTRAL, 1.0F, (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F), true);

            for(int i = 0; i < 8; ++i)
                this.level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, this.getItem()), this.getX(), this.getY(), this.getZ(), (this.random.nextFloat() - 0.5D) * 0.08D, (this.random.nextFloat() - 0.5D) * 0.8D, (this.random.nextFloat() - 0.5D) * 0.08D);

            this.level.addParticle(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 1.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        createExplosion();
    }

    @Override
    protected Item getDefaultItem() {
        return HabitatItems.KABLOOM_FRUIT.get();
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(HabitatItems.KABLOOM_FRUIT.get());
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected Vec3 limitPistonMovement(Vec3 pos) {
        createExplosion();
        return Vec3.ZERO;
    }

    private void createExplosion() {
        Vec3 vector3d = this.position();

        if (this.level.getGameRules().getRule(GameRules.RULE_DOENTITYDROPS).get()) {
            ItemEntity item = new ItemEntity(this.level, vector3d.x() + this.random.nextGaussian() / 2, vector3d.y() + this.random.nextDouble() / 2, vector3d.z() + this.random.nextGaussian() / 2, new ItemStack(HabitatItems.KABLOOM_PULP.get()));
            item.setDefaultPickUpDelay();
            this.level.addFreshEntity(item);
        }

        for (Entity entity : this.level.getEntities(null, this.getBoundingBox().inflate(0.75D))) {
            boolean flag = false;

            for (int i = 0; i < 2; ++i) {
                HitResult raytraceresult = this.level.clip(new ClipContext(vector3d, new Vec3(entity.getX(), entity.getY(0.5D * (double) i), entity.getZ()), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (raytraceresult.getType() == HitResult.Type.MISS) {
                    flag = true;
                    break;
                }
            }

            if (flag) {
                float dmg = 0;
                if (!entity.ignoreExplosion()) {
                    double dx = entity.getX() - this.getX();
                    double dy = entity.getEyeY() - this.getY();
                    double dz = entity.getZ() - this.getZ();
                    double dres = Mth.sqrt((float) (dx * dx + dy * dy + dz * dz));
                    if (dres != 0.0D) {
                        dx = dx / dres;
                        dy = dy / dres;
                        dz = dz / dres;
                        double df = this.distanceTo(entity) > 1.0F ? 0.25D : 0.5D;
                        dmg = 4.0F + 4.0F * ((float) df);
                        double dred = df;
                        if (entity instanceof LivingEntity) {
                            LivingEntity livingEntity = (LivingEntity) entity;
                            dred = ProtectionEnchantment.getExplosionKnockbackAfterDampener(livingEntity, df) * (1.0D - livingEntity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                        }

                        boolean knockback = true;
                        if (entity instanceof Player) {
                            Player playerentity = (Player) entity;
                            if (playerentity.isSpectator() || (playerentity.isCreative() && playerentity.getAbilities().flying)) {
                                knockback = false;
                            }
                        }

                        if (knockback)
                            entity.setDeltaMovement(entity.getDeltaMovement().add(dx * dred, dy * dred, dz * dred));
                    }
                }

                if (entity instanceof LivingEntity)
                    entity.hurt(HabitatDamageSources.causeKabloomDamage(this, this.getOwner()).setExplosion(), dmg);
                else if (entity.isAttackable())
                    entity.hurt(HabitatDamageSources.causeKabloomDamage(this, this.getOwner()), dmg);

                if (this.isOnFire() && !entity.fireImmune())
                    entity.setSecondsOnFire(1);
            }
        }

        if (!this.level.isClientSide) {
            this.level.broadcastEntityEvent(this, (byte) 3);
            this.kill();
        }
    }
}