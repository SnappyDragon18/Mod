package mod.schnappdragon.habitat.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;

public class FairyRingSporeParticle extends TextureSheetParticle {
    private FairyRingSporeParticle(ClientLevel world, double x, double y, double z, double motionX, double motionY, double motionZ, SpriteSet spriteSet) {
        super(world, x, y, z);
        this.lifetime = (int) (60 + random.nextDouble() * 60);
        this.gravity = 0.0001F;
        this.friction = 0.99F;
        float f = 0.9F + this.random.nextFloat() * 0.1F;
        this.rCol = f;
        this.gCol = f * 0.98F;
        this.bCol = f * 0.98F;
        this.quadSize *= 0.8F;
        this.xd = motionX;
        this.yd = motionY;
        this.zd = motionZ;
        this.pickSprite(spriteSet);
    }

    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime || this.onGround || this.level.isFluidAtPosition(new BlockPos(this.x, this.y, this.z), (fluidState) -> !fluidState.isEmpty()))
            this.remove();
        else {
            this.yd -= this.gravity;
            this.move(this.xd, this.yd, this.zd);

            this.xd *= this.friction;
            this.yd *= this.friction;
            this.zd *= this.friction;
        }
    }

    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    public int getLightColor(float partialTick) {
        return 240;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new FairyRingSporeParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }
}