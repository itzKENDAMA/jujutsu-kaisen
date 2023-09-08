package radon.jujutsu_kaisen.entity.projectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.SorcererDataHandler;
import radon.jujutsu_kaisen.client.particle.LightningParticle;
import radon.jujutsu_kaisen.client.particle.ParticleColors;
import radon.jujutsu_kaisen.damage.JJKDamageSources;
import radon.jujutsu_kaisen.entity.JJKEntities;
import radon.jujutsu_kaisen.entity.base.JujutsuProjectile;
import radon.jujutsu_kaisen.util.HelperMethods;

public class LightningProjectile extends JujutsuProjectile {
    private static final float DAMAGE = 15.0F;
    private static final float SPEED = 5.0F;
    private static final float EXPLOSIVE_POWER = 1.25F;
    public static final int DELAY = 20;
    public static final int STILL_FRAMES = 2;
    private static final double OFFSET = 2.0D;

    public int animation;

    public LightningProjectile(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public LightningProjectile(LivingEntity pShooter) {
        super(JJKEntities.LIGHTNING.get(), pShooter.level, pShooter);

        Vec3 look = HelperMethods.getLookAngle(pShooter);
        Vec3 spawn = new Vec3(pShooter.getX(), pShooter.getEyeY() - (this.getBbHeight() / 2.0F), pShooter.getZ()).add(look.scale(OFFSET));
        this.moveTo(spawn.x(), spawn.y(), spawn.z(), pShooter.getYRot(), pShooter.getXRot());
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult pResult) {
        super.onHitEntity(pResult);

        Entity entity = pResult.getEntity();

        if (this.getOwner() instanceof LivingEntity owner) {
            owner.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
                if ((entity instanceof LivingEntity living && owner.canAttack(living)) && entity != owner) {
                    entity.hurt(JJKDamageSources.indirectJujutsuAttack(this, owner, JJKAbilities.FIRE_ARROW.get()), DAMAGE * cap.getGrade().getPower(owner));
                }
            });
        }
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        super.onHit(result);

        if (this.level.isClientSide || !(this.getOwner() instanceof LivingEntity owner)) return;

        Vec3 dir = this.getDeltaMovement();

        for (int i = 0; i < 50; i++) {
            Vec3 yaw = dir.yRot(this.random.nextFloat() * 360.0F);
            Vec3 pitch = yaw.xRot(this.random.nextFloat() * 180.0F - 90.0F);

            double dx = pitch.x() + (this.random.nextDouble() - 0.5D) * 0.2D;
            double dy = pitch.y() + (this.random.nextDouble() - 0.5D) * 0.2D;
            double dz = pitch.z() + (this.random.nextDouble() - 0.5D) * 0.2D;

            ((ServerLevel) this.level).sendParticles(new LightningParticle.LightningParticleOptions(ParticleColors.getCursedEnergyColor(owner), 0.5F),
                    this.getX(), this.getY() + (this.getBbHeight() / 2.0F), this.getZ(),
                    0, dx, dy, dz, 1.0D);
        }

        owner.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
            Vec3 location = result.getLocation();
            this.level.explode(owner, JJKDamageSources.indirectJujutsuAttack(owner, owner, JJKAbilities.FIRE_ARROW.get()), null,
                    location.x(), location.y(), location.z(), EXPLOSIVE_POWER * cap.getGrade().getPower(owner), false,
                    this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
        });
        this.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER);
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.animation < STILL_FRAMES) {
            this.animation++;
        } else {
            this.animation = 0;
        }

        if (this.getOwner() instanceof LivingEntity owner) {
            for (int i = 0; i < 2; i++) {
                Vec3 dir = HelperMethods.getLookAngle(owner).reverse().scale(0.1D);
                double dx = dir.x() + ((this.random.nextDouble() - 0.5D) * 0.5D);
                double dy = dir.y() + ((this.random.nextDouble() - 0.5D) * 0.5D);
                double dz = dir.z() + ((this.random.nextDouble() - 0.5D) * 0.5D);

                this.level.addParticle(new LightningParticle.LightningParticleOptions(ParticleColors.getCursedEnergyColor(owner), 0.3F),
                        this.getX(), this.getY() + (this.getBbHeight() / 2.0F), this.getZ(), dx, dy, dz);
            }

            Vec3 look = HelperMethods.getLookAngle(owner);
            double d0 = look.horizontalDistance();
            this.setYRot((float) (Mth.atan2(look.x(), look.z()) * (double) (180.0F / (float) Math.PI)));
            this.setXRot((float) (Mth.atan2(look.y(), d0) * (double) (180.0F / (float) Math.PI)));
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();

            if (this.getTime() < DELAY) {
                if (!owner.isAlive()) {
                    this.discard();
                } else {
                    if (this.getTime() % 5 == 0) {
                        owner.swing(InteractionHand.MAIN_HAND);
                    }
                    Vec3 spawn = new Vec3(owner.getX(), owner.getEyeY() - (this.getBbHeight() / 2.0F), owner.getZ()).add(look.scale(OFFSET));
                    this.setPos(spawn.x(), spawn.y(), spawn.z());
                }
            } else if (this.getTime() >= DELAY) {
                if (this.getTime() == DELAY) {
                    this.setDeltaMovement(HelperMethods.getLookAngle(owner).scale(SPEED));
                } else if (this.getDeltaMovement().lengthSqr() < 1.0E-7D) {
                    this.discard();
                }
            }
        }
    }
}
