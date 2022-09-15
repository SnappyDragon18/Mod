package mod.schnappdragon.habitat.common.entity.monster;

import mod.schnappdragon.habitat.common.entity.IHabitatShearable;
import mod.schnappdragon.habitat.core.HabitatConfig;
import mod.schnappdragon.habitat.core.misc.HabitatCriterionTriggers;
import mod.schnappdragon.habitat.core.registry.*;
import mod.schnappdragon.habitat.core.tags.HabitatEntityTypeTags;
import mod.schnappdragon.habitat.core.tags.HabitatItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public class Pooka extends Rabbit implements Enemy, IHabitatShearable {
    private static final EntityDataAccessor<Integer> DATA_STATE_ID = SynchedEntityData.defineId(Pooka.class, EntityDataSerializers.INT);
    private int aidId;
    private int aidDuration;
    private int ailmentId;
    private int ailmentDuration;
    private int forgiveTicks;
    private int aidTicks;

    public Pooka(EntityType<? extends Pooka> entityType, Level world) {
        super(entityType, world);
        this.xpReward = 3;
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(1, new Pooka.PookaPanicGoal(2.2D));
        this.targetSelector.addGoal(1, (new Pooka.PookaHurtByTargetGoal()).setAlertOthers());
        this.targetSelector.addGoal(2, new Pooka.PookaNearestAttackableTargetGoal<>(Player.class));
        this.targetSelector.addGoal(2, new Pooka.PookaNearestAttackableTargetGoal<>(Mob.class, mob -> mob.getType().is(HabitatEntityTypeTags.POOKA_ATTACK_TARGETS)));
        this.goalSelector.addGoal(2, new BreedGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new Pooka.PookaTemptGoal(1.25D, Ingredient.of(HabitatItemTags.POOKA_FOOD), false));
        this.goalSelector.addGoal(4, new Pooka.PookaMeleeAttackGoal());
        this.goalSelector.addGoal(4, new Pooka.PookaAvoidEntityGoal<>(Mob.class, 10.0F, 2.2D, 2.2D, mob -> mob.getType().is(HabitatEntityTypeTags.PACIFIED_POOKA_SCARED_BY)));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 10.0F));
    }

    public static AttributeSupplier.Builder registerAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3F)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ARMOR, 8.0D);
    }

    @Override
    protected int getExperienceReward(Player player) {
        return this.xpReward;
    }

    /*
     * Data Methods
     */

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STATE_ID, 0);
    }

    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("AidId", this.aidId);
        compound.putInt("AidDuration", this.aidDuration);
        compound.putInt("AilmentId", this.ailmentId);
        compound.putInt("AilmentDuration", this.ailmentDuration);
        compound.putInt("ForgiveTicks", this.forgiveTicks);
        compound.putInt("AidTicks", this.aidTicks);
        compound.putInt("State", this.getStateId());
    }

    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setAidAndAilment(
                compound.getInt("AidId"),
                compound.getInt("AidDuration"),
                compound.getInt("AilmentId"),
                compound.getInt("AilmentDuration")
        );
        this.forgiveTicks = compound.getInt("ForgiveTicks");
        this.aidTicks = compound.getInt("AidTicks");
        this.setStateId(compound.getInt("State"));
    }

    private void setAidAndAilment(int aidI, int aidD, int ailI, int ailD) {
        this.aidId = aidI;
        this.aidDuration = aidD;
        this.ailmentId = ailI;
        this.ailmentDuration = ailD;
    }

    private void setStateId(int state) {
        this.entityData.set(DATA_STATE_ID, state);
    }

    public int getStateId() {
        return Mth.clamp(this.entityData.get(DATA_STATE_ID), 0, 2);
    }

    private void setState(State state) {
        this.setStateId(state.ordinal());
    }

    public State getState() {
        return State.getStateById(this.getStateId());
    }

    public boolean isHostile() {
        return this.getState().equals(State.HOSTILE);
    }

    public boolean isPacified() {
        return this.getState().equals(State.PACIFIED);
    }

    private void setForgiveTimer() {
        this.forgiveTicks = 12000;
    }

    private void setAidTimer() {
        this.aidTicks = (int) ((20.0F + this.random.nextFloat() * 4.0F) * (float) HabitatConfig.COMMON.pookaAidCooldown.get());
    }

    private void resetAidTimer() {
        this.aidTicks = 0;
    }

    /*
     * Update AI Tasks
     */

    @Override
    public void jumpFromGround() {
        if (!this.level.isClientSide)
            this.level.broadcastEntityEvent(this, (byte) 14);

        super.jumpFromGround();
    }

    @Override
    public void customServerAiStep() {
        if (this.forgiveTicks > 0)
            forgiveTicks--;

        if (this.aidTicks > 0)
            aidTicks--;

        if (this.onGround && this.isHostile() && this.jumpDelayTicks == 0) {
            LivingEntity livingentity = this.getTarget();
            if (livingentity != null && this.distanceToSqr(livingentity) < 16.0D) {
                this.facePoint(livingentity.getX(), livingentity.getZ());
                this.moveControl.setWantedPosition(livingentity.getX(), livingentity.getY(), livingentity.getZ(), this.moveControl.getSpeedModifier());
                this.startJumping();
                this.wasOnGround = true;
            }
        }

        super.customServerAiStep();
    }

    private void facePoint(double x, double z) {
        this.setYRot((float) (Mth.atan2(z - this.getZ(), x - this.getX()) * (double) (180F / (float) Math.PI)) - 90.0F);
    }

    /*
     * Leash Methods
     */

    @Override
    public boolean canBeLeashed(Player player) {
        return !this.isHostile();
    }

    @Override
    protected void tickLeash() {
        super.tickLeash();

        if (this.isHostile())
            this.dropLeash(true, true);
    }

    /*
     * Conversion Methods
     */

    @Override
    public boolean isShearable(@Nonnull ItemStack item, Level world, BlockPos pos) {
        return this.isAlive() && !this.isHostile() && !this.isBaby();
    }

    @Nonnull
    @Override
    public List<ItemStack> onSheared(@Nullable Player player, @Nonnull ItemStack item, Level world, BlockPos pos, int fortune) {
        return onSheared(player, item, world, pos, fortune, SoundSource.PLAYERS);
    }

    @Nonnull
    @Override
    public List<ItemStack> onSheared(@Nullable Player player, @Nonnull ItemStack item, Level world, BlockPos pos, int fortune, SoundSource source) {
        this.level.gameEvent(player, GameEvent.SHEAR, pos);
        world.playSound(null, this, HabitatSoundEvents.POOKA_SHEAR.get(), source, 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
        if (!this.level.isClientSide()) {
            ((ServerLevel) this.level).sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(0.5D), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            this.discard();
            world.addFreshEntity(convertPookaToRabbit(this));
        }
        return Collections.emptyList();
    }

    public static Rabbit convertPookaToRabbit(Pooka pooka) {
        Rabbit rabbit = EntityType.RABBIT.create(pooka.level);
        rabbit.moveTo(pooka.getX(), pooka.getY(), pooka.getZ(), pooka.getYRot(), pooka.getXRot());
        rabbit.setHealth(pooka.getHealth());
        rabbit.yBodyRot = pooka.yBodyRot;
        if (pooka.hasCustomName()) {
            rabbit.setCustomName(pooka.getCustomName());
            rabbit.setCustomNameVisible(pooka.isCustomNameVisible());
        }

        if (pooka.isPersistenceRequired())
            rabbit.setPersistenceRequired();

        rabbit.setRabbitType(pooka.getRabbitType());
        rabbit.setBaby(pooka.isBaby());
        rabbit.setInvulnerable(pooka.isInvulnerable());
        return rabbit;
    }

    public static Pooka convertRabbitToPooka(Rabbit rabbit) {
        Pooka pooka = HabitatEntityTypes.POOKA.get().create(rabbit.level);
        pooka.moveTo(rabbit.getX(), rabbit.getY(), rabbit.getZ(), rabbit.getYRot(), rabbit.getXRot());
        pooka.setHealth(rabbit.getHealth());
        pooka.yBodyRot = rabbit.yBodyRot;
        if (rabbit.hasCustomName()) {
            pooka.setCustomName(rabbit.getCustomName());
            pooka.setCustomNameVisible(rabbit.isCustomNameVisible());
        }

        pooka.setPersistenceRequired();
        pooka.setForgiveTimer();

        Pair<Integer, Integer> aid = pooka.getRandomAid();
        Pair<Integer, Integer> ailment = pooka.getRandomAilment();
        pooka.setAidAndAilment(aid.getLeft(), aid.getRight(), ailment.getLeft(), ailment.getRight());

        pooka.setRabbitType(rabbit.getRabbitType());
        pooka.setBaby(rabbit.isBaby());
        pooka.setInvulnerable(rabbit.isInvulnerable());
        return pooka;
    }

    /*
     * Sound Methods
     */

    public SoundSource getSoundSource() {
        return this.isHostile() ? SoundSource.HOSTILE : SoundSource.NEUTRAL;
    }

    protected SoundEvent getJumpSound() {
        return HabitatSoundEvents.POOKA_JUMP.get();
    }

    protected SoundEvent getAmbientSound() {
        return HabitatSoundEvents.POOKA_AMBIENT.get();
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return HabitatSoundEvents.POOKA_HURT.get();
    }

    protected SoundEvent getDeathSound() {
        return HabitatSoundEvents.POOKA_DEATH.get();
    }

    /*
     * Taming Methods
     */

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.isFood(stack)) {
            if (this.isHostile()) {
                if (!this.level.isClientSide) {
                    this.setPersistenceRequired();
                    this.usePlayerItem(player, hand, stack);

                    if (this.forgiveTicks == 0) {
                        int roll = random.nextInt(5);

                        if ((this.isBaby() && roll > 0 || roll == 0) && this.isAlone()) {
                            this.setState(State.PACIFIED);
                            this.playSound(HabitatSoundEvents.POOKA_PACIFY.get(), 1.0F, 1.0F);
                            HabitatCriterionTriggers.PACIFY_POOKA.trigger((ServerPlayer) player);
                            this.navigation.stop();
                            this.setTarget(null);
                            this.setLastHurtByMob(null);
                            this.level.broadcastEntityEvent(this, (byte) 18);
                        } else
                            this.level.broadcastEntityEvent(this, (byte) 12);
                    } else {
                        this.forgiveTicks -= (double) this.forgiveTicks * 0.1D;
                        this.level.broadcastEntityEvent(this, (byte) 12);
                    }

                    this.gameEvent(GameEvent.MOB_INTERACT, this.eyeBlockPosition());
                }

                return InteractionResult.sidedSuccess(this.level.isClientSide);
            }
            else if (this.getHealth() < this.getMaxHealth() && stack.isEdible()) {
                if (!this.level.isClientSide) {
                    this.usePlayerItem(player, hand, stack);
                    this.heal(stack.getItem().getFoodProperties().getNutrition());
                    this.gameEvent(GameEvent.MOB_INTERACT, this.eyeBlockPosition());
                }

                return InteractionResult.sidedSuccess(this.level.isClientSide);
            }
        }

        InteractionResult result = super.mobInteract(player, hand);
        if (result.consumesAction())
            this.setPersistenceRequired();

        return result;
    }

    protected void usePlayerItem(Player player, InteractionHand hand, ItemStack stack) {
        if (this.isFood(stack))
            this.playSound(HabitatSoundEvents.POOKA_EAT.get(), 1.0F, 1.0F);

        super.usePlayerItem(player, hand, stack);
    }

    private boolean isAlone() {
        return this.level.getEntitiesOfClass(Pooka.class, this.getBoundingBox().inflate(16.0D, 10.0D, 16.0D), Pooka::isHostile).size() == 1;
    }

    public void unpacify() {
        this.resetLove();
        this.resetAidTimer();
        this.setForgiveTimer();
        this.setState(State.HOSTILE);
        this.level.broadcastEntityEvent(this, (byte) 13);
    }

    /*
     * Breeding Methods
     */

    @Override
    public Pooka getBreedOffspring(ServerLevel serverWorld, AgeableMob entity) {
        Pooka pooka = HabitatEntityTypes.POOKA.get().create(serverWorld);
        State state = State.HOSTILE;
        int i = this.getRandomRabbitType(serverWorld);

        Pair<Integer, Integer> aid = this.getRandomAid();
        int aidI = aid.getLeft();
        int aidD = aid.getRight();

        Pair<Integer, Integer> ailment = this.getRandomAilment();
        int ailI = ailment.getLeft();
        int ailD = ailment.getRight();

        if (entity instanceof Pooka parent) {
            if (!this.isHostile() && !parent.isHostile()) state = State.PASSIVE;

            if (this.random.nextInt(20) != 0) {
                if (this.random.nextBoolean())
                    i = parent.getRabbitType();
                else
                    i = this.getRabbitType();
            }

            if (this.random.nextInt(20) != 0) {
                if (this.random.nextBoolean()) {
                    aidI = parent.aidId;
                    aidD = parent.aidDuration;
                } else {
                    aidI = this.aidId;
                    aidD = this.aidDuration;
                }
            }

            if (this.random.nextInt(20) != 0) {
                if (this.random.nextBoolean()) {
                    ailI = parent.ailmentId;
                    ailD = parent.ailmentDuration;
                } else {
                    ailI = this.ailmentId;
                    ailD = this.ailmentDuration;
                }
            }
        }

        pooka.setState(state);
        pooka.setRabbitType(i);
        pooka.setAidAndAilment(aidI, aidD, ailI, ailD);
        pooka.setPersistenceRequired();
        return pooka;
    }

    public boolean isFood(ItemStack stack) {
        return stack.is(HabitatItemTags.POOKA_FOOD);
    }

    public boolean canMate(Animal animal) {
        return animal instanceof Pooka pooka && !this.isHostile() && !pooka.isHostile() && super.canMate(animal);
    }

    /*
     * Spawn Methods
     */

    public static boolean checkPookaSpawnRules(EntityType<Pooka> pooka, LevelAccessor world, MobSpawnType reason, BlockPos pos, Random rand) {
        return world.getDifficulty() != Difficulty.PEACEFUL && world.getBlockState(pos.below()).is(BlockTags.RABBITS_SPAWNABLE_ON) && world.getSkyDarken() >= 5 && world.dimensionType().hasSkyLight();
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
        Pair<Integer, Integer> aid = this.getRandomAid();
        Pair<Integer, Integer> ailment = this.getRandomAilment();
        int i = this.getRandomRabbitType(worldIn);

        if (spawnDataIn instanceof Rabbit.RabbitGroupData data)
            i = data.rabbitType;
        else
            spawnDataIn = new Rabbit.RabbitGroupData(i);

        this.setRabbitType(i);
        this.setState(State.HOSTILE);
        this.setAidAndAilment(aid.getLeft(), aid.getRight(), ailment.getLeft(), ailment.getRight());
        return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    private int getRandomRabbitType(LevelAccessor world) {
        Holder<Biome> biomeHolder = world.getBiome(this.blockPosition());
        Biome biome = biomeHolder.value();
        int i = this.random.nextInt(100);
        if (biome.getPrecipitation() == Biome.Precipitation.SNOW)
            return i < 80 ? 1 : 3;
        else if (Biome.getBiomeCategory(biomeHolder) == Biome.BiomeCategory.DESERT)
            return 4;
        else
            return i < 50 ? 0 : (i < 90 ? 5 : 2);
    }

    private Pair<Integer, Integer> getRandomAid() {
        return getEffect(HabitatConfig.COMMON.pookaPositiveEffects);
    }

    private Pair<Integer, Integer> getRandomAilment() {
        return getEffect(HabitatConfig.COMMON.pookaNegativeEffects);
    }

    private Pair<Integer, Integer> getEffect(ForgeConfigSpec.ConfigValue<String> config) {
        List<String> stewEffectPairs = Arrays.asList(StringUtils.deleteWhitespace(config.get()).split(","));
        String[] pair = stewEffectPairs.get(this.random.nextInt(stewEffectPairs.size())).split(":");

        return Pair.of(Integer.parseInt(pair[0]), Integer.parseInt(pair[1]) * 20);
    }

    /*
     * Damage Methods
     */

    public boolean doHurtTarget(Entity entityIn) {
        if (entityIn.getType() == EntityType.RABBIT && entityIn.isAlive() && !entityIn.isInvulnerableTo(DamageSource.mobAttack(this))) {
            this.playSound(HabitatSoundEvents.POOKA_ATTACK.get(), 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            this.gameEvent(GameEvent.ENTITY_DAMAGED, this);

            Rabbit rabbit = (Rabbit) entityIn;
            rabbit.playSound(HabitatSoundEvents.RABBIT_CONVERTED_TO_POOKA.get(), 1.0F, rabbit.isBaby() ? (rabbit.getRandom().nextFloat() - rabbit.getRandom().nextFloat()) * 0.2F + 1.5F : (rabbit.getRandom().nextFloat() - rabbit.getRandom().nextFloat()) * 0.2F + 1.0F);
            rabbit.discard();
            this.level.addFreshEntity(convertRabbitToPooka(rabbit));

            for (int i = 0; i < 8; i++)
                ((ServerLevel) this.level).sendParticles(HabitatParticleTypes.FAIRY_RING_SPORE.get(), rabbit.getRandomX(0.5D), rabbit.getY(0.5D), rabbit.getRandomZ(0.5D), 0, rabbit.getRandom().nextGaussian(), 0.0D, rabbit.getRandom().nextGaussian(), 0.01D);
            return false;
        }

        boolean flag = entityIn.hurt(DamageSource.mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
        if (flag) {
            this.doEnchantDamageEffects(this, entityIn);
            this.setLastHurtMob(entityIn);
            this.playSound(HabitatSoundEvents.POOKA_ATTACK.get(), 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);

            if (!this.isBaby() && entityIn instanceof LivingEntity) {
                MobEffect effect = MobEffect.byId(ailmentId);

                if (effect != null)
                    ((LivingEntity) entityIn).addEffect(new MobEffectInstance(effect, ailmentDuration * (this.level.getDifficulty() == Difficulty.HARD ? 2 : 1)));
            }
        }

        return flag;
    }

    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source))
            return false;
        else {
            if (!this.level.isClientSide && this.isAlive()) {
                MobEffect effect = MobEffect.byId(aidId);
                if (!this.isBaby() && effect != null)
                    this.addEffect(new MobEffectInstance(effect, aidDuration));

                if (this.isPacified() && source.getEntity() instanceof Player && !source.isCreativePlayer())
                    this.unpacify();
            }

            return super.hurt(source, amount);
        }
    }

    /*
     * Particle Status Updates
     */

    public void handleEntityEvent(byte id) {
        switch (id) {
            case 12 -> spawnParticles(ParticleTypes.SMOKE, 7, true);
            case 13 -> spawnParticles(ParticleTypes.ANGRY_VILLAGER, 7, true);
            case 14 -> spawnParticles(HabitatParticleTypes.FAIRY_RING_SPORE.get(), 1, false);
            case 15 -> spawnParticles(HabitatParticleTypes.FAIRY_RING_SPORE.get(), 8, false);
            default -> super.handleEntityEvent(id);
        }
    }

    protected void spawnParticles(ParticleOptions particle, int number, boolean vanillaPresets) {
        for (int i = 0; i < number; i++) {
            double d0 = this.random.nextGaussian() * (vanillaPresets ? 0.02D : 0.01D);
            double d1 = vanillaPresets ? this.random.nextGaussian() * 0.02D : 0.0D;
            double d2 = this.random.nextGaussian() * (vanillaPresets ? 0.02D : 0.01D);
            double d3 = vanillaPresets ? 0.5D : 0.0D;
            this.level.addParticle(particle, this.getRandomX(0.5D + d3), this.getRandomY() + d3, this.getRandomZ(0.5D + d3), d0, d1, d2);
        }
    }

    /*
     * State
     */

    public enum State {
        HOSTILE,
        PACIFIED,
        PASSIVE;

        private static final State[] STATES = State.values();

        public static State getStateById(int id) {
            return STATES[Mth.clamp(id, 0, 2)];
        }
    }

    /*
     * AI Goals
     */

    class PookaPanicGoal extends PanicGoal {
        public PookaPanicGoal(double speedIn) {
            super(Pooka.this, speedIn);
        }

        @Override
        public void tick() {
            super.tick();
            Pooka.this.setSpeedModifier(this.speedModifier);
        }
    }

    class PookaTemptGoal extends TemptGoal {
        public PookaTemptGoal(double speed, Ingredient temptItem, boolean scaredByMovement) {
            super(Pooka.this, speed, temptItem, scaredByMovement);
        }

        @Override
        public boolean canUse() {
            return !Pooka.this.isHostile() && super.canUse();
        }

        @Override
        public void tick() {
            super.tick();
            MobEffect aid = MobEffect.byId(Pooka.this.aidId);

            if (!Pooka.this.isBaby() && Pooka.this.aidTicks == 0 && aid != null) {
                this.player.addEffect(new MobEffectInstance(aid, Pooka.this.aidDuration * 2));
                Pooka.this.setAidTimer();
            }
        }
    }

    class PookaHurtByTargetGoal extends HurtByTargetGoal {
        private int timestamp;

        public PookaHurtByTargetGoal() {
            super(Pooka.this);
        }

        @Override
        public void start() {
            this.timestamp = this.mob.getLastHurtByMobTimestamp();
            super.start();
        }

        @Override
        public boolean canUse() {
            return this.mob.getLastHurtByMobTimestamp() != this.timestamp && this.mob.getLastHurtByMob() != null && this.canAttack(this.mob.getLastHurtByMob(), TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting());
        }

        @Override
        public boolean canContinueToUse() {
            return Pooka.this.isHostile() && super.canContinueToUse();
        }

        @Override
        protected void alertOther(Mob mob, LivingEntity target) {
            if (mob instanceof Pooka pooka && this.mob.hasLineOfSight(target)) {
                if (pooka.isHostile())
                    super.alertOther(mob, target);
                else if (pooka.isPacified() && target instanceof Player) {
                    pooka.unpacify();
                    super.alertOther(mob, target);
                }
            }
        }
    }

    class PookaNearestAttackableTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
        public PookaNearestAttackableTargetGoal(Class<T> targetClassIn, @Nullable Predicate<LivingEntity> targetPredicate) {
            super(Pooka.this, targetClassIn, 10, true, false, targetPredicate);
        }

        public PookaNearestAttackableTargetGoal(Class<T> targetClassIn) {
            super(Pooka.this, targetClassIn, true);
        }

        @Override
        public boolean canUse() {
            return Pooka.this.isHostile() && super.canUse();
        }
    }

    class PookaAvoidEntityGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
        public PookaAvoidEntityGoal(Class<T> entity, float range, double v1, double v2, Predicate<LivingEntity> predicate) {
            super(Pooka.this, entity, range, v1, v2, predicate);
        }

        @Override
        public boolean canUse() {
            return !Pooka.this.isHostile() && super.canUse();
        }
    }

    class PookaMeleeAttackGoal extends MeleeAttackGoal {
        public PookaMeleeAttackGoal() {
            super(Pooka.this, 1.4D, true);
        }

        @Override
        protected double getAttackReachSqr(LivingEntity attackTarget) {
            return 4.0F + attackTarget.getBbWidth();
        }

        @Override
        public boolean canUse() {
            return Pooka.this.isHostile() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return Pooka.this.isHostile() && super.canContinueToUse();
        }
    }
}