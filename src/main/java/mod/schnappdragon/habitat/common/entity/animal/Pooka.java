package mod.schnappdragon.habitat.common.entity.animal;

import mod.schnappdragon.habitat.core.registry.HabitatParticleTypes;
import mod.schnappdragon.habitat.core.registry.HabitatSoundEvents;
import mod.schnappdragon.habitat.core.tags.HabitatItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class Pooka extends TamableAnimal {
    private static final EntityDataAccessor<Integer> DATA_SOCKS_COLOR = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.INT);
    private int jumpTicks;
    private int jumpDuration;
    public boolean wasOnGround;
    public int jumpDelayTicks;

    public Pooka(EntityType<Pooka> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.jumpControl = new PookaJumpControl(this);
        this.moveControl = new PookaMoveControl(this);
        this.setSpeedModifier(0.0D);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25D));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ClimbOnTopOfPowderSnowGoal(this, this.level()));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0D, 5.0F, 1.0F, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10.0F));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 3.0D).add(Attributes.MOVEMENT_SPEED, 0.3F);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SOCKS_COLOR, DyeColor.RED.getId());
    }

    public DyeColor getSocksColor() {
        return DyeColor.byId(this.entityData.get(DATA_SOCKS_COLOR));
    }

    public void setSocksColor(DyeColor pCollarColor) {
        this.entityData.set(DATA_SOCKS_COLOR, pCollarColor.getId());
    }

    public void addAdditionalSaveData(CompoundTag pCompound) {
        pCompound.putByte("SocksColor", (byte) this.getSocksColor().getId());
    }
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);

        if (pCompound.contains("CollarColor", 99)) {
            this.setSocksColor(DyeColor.byId(pCompound.getInt("SocksColor")));
        }
    }

    protected float getJumpPower() {
        float f = 0.6F;
        if (this.horizontalCollision || this.moveControl.hasWanted() && this.moveControl.getWantedY() > this.getY()) {
            f = 1.0F;
        }

        Path path = this.navigation.getPath();
        if (path != null && !path.isDone()) {
            Vec3 vec3 = path.getNextEntityPos(this);
            if (vec3.y > this.getY()) {
                f = 1.0F;
            }
        }

        if (this.moveControl.getSpeedModifier() <= 0.6D) {
            f = 0.4F;
        }

        return f + this.getJumpBoostPower();
    }

    protected void jumpFromGround() {
        super.jumpFromGround();
        double d0 = this.moveControl.getSpeedModifier();
        if (d0 > 0.0D) {
            double d1 = this.getDeltaMovement().horizontalDistanceSqr();
            if (d1 < 0.01D) {
                this.moveRelative(0.1F, new Vec3(0.0D, 0.0D, 1.0D));
            }
        }

        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte) 1);
        }

    }

    public float getJumpCompletion(float pPartialTick) {
        return this.jumpDuration == 0 ? 0.0F : ((float) this.jumpTicks + pPartialTick) / (float) this.jumpDuration;
    }

    public void setSpeedModifier(double pSpeedModifier) {
        this.getNavigation().setSpeedModifier(pSpeedModifier);
        this.moveControl.setWantedPosition(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ(), pSpeedModifier);
    }

    public void setJumping(boolean pJumping) {
        super.setJumping(pJumping);
        if (pJumping) {
            this.playSound(this.getJumpSound(), this.getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) * 0.8F);
        }

    }

    public void startJumping() {
        this.setJumping(true);
        this.jumpDuration = 10;
        this.jumpTicks = 0;
    }

    public void customServerAiStep() {
        if (this.jumpDelayTicks > 0) {
            --this.jumpDelayTicks;
        }

        if (this.onGround()) {
            if (!this.wasOnGround) {
                this.setJumping(false);
                this.checkLandingDelay();
            }

            PookaJumpControl pookaJumpControl = (PookaJumpControl) this.jumpControl;
            if (!pookaJumpControl.wantJump()) {
                if (this.moveControl.hasWanted() && this.jumpDelayTicks == 0) {
                    Path path = this.navigation.getPath();
                    Vec3 vec3 = new Vec3(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ());
                    if (path != null && !path.isDone()) {
                        vec3 = path.getNextEntityPos(this);
                    }

                    this.facePoint(vec3.x, vec3.z);
                    this.startJumping();
                }
            } else if (!pookaJumpControl.canJump()) {
                this.enableJumpControl();
            }
        }

        this.wasOnGround = this.onGround();
    }

    public boolean canSpawnSprintParticle() {
        return false;
    }

    private void facePoint(double pX, double pZ) {
        this.setYRot((float) (Mth.atan2(pZ - this.getZ(), pX - this.getX()) * (double) (180F / (float) Math.PI)) - 90.0F);
    }

    private void enableJumpControl() {
        ((PookaJumpControl) this.jumpControl).setCanJump(true);
    }

    private void disableJumpControl() {
        ((PookaJumpControl) this.jumpControl).setCanJump(false);
    }

    private void setLandingDelay() {
        if (this.moveControl.getSpeedModifier() < 2.2D) {
            this.jumpDelayTicks = 10;
        } else {
            this.jumpDelayTicks = 1;
        }

    }

    private void checkLandingDelay() {
        this.setLandingDelay();
        this.disableJumpControl();
    }

    public void aiStep() {
        super.aiStep();
        if (this.jumpTicks != this.jumpDuration) {
            ++this.jumpTicks;
        } else if (this.jumpDuration != 0) {
            this.jumpTicks = 0;
            this.jumpDuration = 0;
            this.setJumping(false);
        }

        Vec3 vec3 = this.getDeltaMovement();
        if (!this.onGround() && vec3.y < 0.0D) {
            this.setDeltaMovement(vec3.multiply(1.0D, 0.6D, 1.0D));
        }

        if (this.level().isClientSide) {
            if (this.random.nextInt(16) == 0) {
                this.level().addParticle(HabitatParticleTypes.FAIRY_RING_SPORE.get(), this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), this.random.nextGaussian() * (0.01D), 0.0D, this.random.nextGaussian() * (0.01D));
            }
        }
    }

    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        Item item = itemstack.getItem();

        if (this.level().isClientSide) {
            boolean flag = this.isOwnedBy(pPlayer) || this.isTame() || this.isFood(itemstack);
            return flag ? InteractionResult.CONSUME : InteractionResult.PASS;
        } else if (this.isTame()) {
            if (this.isFood(itemstack) && this.getHealth() < this.getMaxHealth()) {
                this.heal((float)itemstack.getFoodProperties(this).getNutrition());
                if (!pPlayer.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }

                this.gameEvent(GameEvent.EAT, this);
                return InteractionResult.SUCCESS;
            } else {
                if (item instanceof DyeItem) {
                    DyeItem dyeitem = (DyeItem)item;
                    if (this.isOwnedBy(pPlayer)) {
                        DyeColor dyecolor = dyeitem.getDyeColor();
                        if (dyecolor != this.getSocksColor()) {
                            this.setSocksColor(dyecolor);
                            if (!pPlayer.getAbilities().instabuild) {
                                itemstack.shrink(1);
                            }

                            return InteractionResult.SUCCESS;
                        }

                        return super.mobInteract(pPlayer, pHand);
                    }
                }

                InteractionResult interactionresult = super.mobInteract(pPlayer, pHand);
                if ((!interactionresult.consumesAction() || this.isBaby()) && this.isOwnedBy(pPlayer)) {
                    this.setOrderedToSit(!this.isOrderedToSit());
                    this.jumping = false;
                    this.navigation.stop();
                    return InteractionResult.SUCCESS;
                } else {
                    return interactionresult;
                }
            }
        } else if (this.isFood(itemstack)) {
            if (!pPlayer.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            if (this.random.nextInt(3) == 0 && !net.minecraftforge.event.ForgeEventFactory.onAnimalTame(this, pPlayer)) {
                this.tame(pPlayer);
                this.navigation.stop();
                this.setOrderedToSit(true);
                this.level().broadcastEntityEvent(this, (byte)7);
            } else {
                this.level().broadcastEntityEvent(this, (byte)6);
            }

            return InteractionResult.SUCCESS;
        } else {
            return super.mobInteract(pPlayer, pHand);
        }
    }
    
    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        return null;
    }

    protected SoundEvent getJumpSound() {
        return HabitatSoundEvents.POOKA_JUMP.get();
    }

    protected SoundEvent getAmbientSound() {
        return HabitatSoundEvents.POOKA_AMBIENT.get();
    }

    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return HabitatSoundEvents.POOKA_HURT.get();
    }

    protected SoundEvent getDeathSound() {
        return HabitatSoundEvents.POOKA_DEATH.get();
    }

    public boolean isFood(ItemStack stack) {
        return stack.is(HabitatItemTags.POOKA_FOOD);
    }

    public static boolean checkPookaSpawnRules(EntityType<Pooka> entityType, LevelAccessor world, MobSpawnType spawnType, BlockPos pos, RandomSource rand) {
        return world.getBlockState(pos.below()).is(BlockTags.RABBITS_SPAWNABLE_ON) && isBrightEnoughToSpawn(world, pos);
    }

    public void handleEntityEvent(byte pId) {
        if (pId == 1) {
            this.spawnSprintParticle();
            this.jumpDuration = 10;
            this.jumpTicks = 0;
        } else {
            super.handleEntityEvent(pId);
        }
    }

    class PookaJumpControl extends JumpControl {
        private boolean canJump;

        public PookaJumpControl(Pooka pooka) {
            super(pooka);
        }

        public boolean wantJump() {
            return this.jump;
        }

        public boolean canJump() {
            return this.canJump;
        }

        public void setCanJump(boolean pCanJump) {
            this.canJump = pCanJump;
        }

        public void tick() {
            if (this.jump) {
                Pooka.this.startJumping();
                this.jump = false;
            }
        }
    }

    class PookaMoveControl extends MoveControl {
        private double nextJumpSpeed;

        public PookaMoveControl(Pooka pooka) {
            super(pooka);
        }

        public void tick() {
            if (Pooka.this.onGround() && !Pooka.this.jumping && !((PookaJumpControl) Pooka.this.jumpControl).wantJump()) {
                Pooka.this.setSpeedModifier(0.0D);
            } else if (this.hasWanted()) {
                Pooka.this.setSpeedModifier(this.nextJumpSpeed);
            }

            super.tick();
        }

        public void setWantedPosition(double pX, double pY, double pZ, double pSpeed) {
            if (Pooka.this.isInWater()) {
                pSpeed = 1.5D;
            }

            super.setWantedPosition(pX, pY, pZ, pSpeed);
            if (pSpeed > 0.0D) {
                this.nextJumpSpeed = pSpeed;
            }
        }
    }
}