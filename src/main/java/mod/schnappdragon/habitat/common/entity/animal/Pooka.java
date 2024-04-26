package mod.schnappdragon.habitat.common.entity.animal;

import mod.schnappdragon.habitat.core.registry.HabitatSoundEvents;
import mod.schnappdragon.habitat.core.tags.HabitatItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class Pooka extends Animal {
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
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ClimbOnTopOfPowderSnowGoal(this, this.level()));
        this.goalSelector.addGoal(2, new BreedGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.0D, Ingredient.of(HabitatItemTags.POOKA_FOOD), false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 10.0F));
    }

    protected float getJumpPower() {
        float f = 0.6F;
        if (this.horizontalCollision || this.moveControl.hasWanted() && this.moveControl.getWantedY() > this.getY() + 0.5D) {
            f = 1.0F;
        }

        Path path = this.navigation.getPath();
        if (path != null && !path.isDone()) {
            Vec3 vec3 = path.getNextEntityPos(this);
            if (vec3.y > this.getY() + 0.5D) {
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

    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 3.0D).add(Attributes.MOVEMENT_SPEED, 0.3F);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        return null;
    }

    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
    }

    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
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