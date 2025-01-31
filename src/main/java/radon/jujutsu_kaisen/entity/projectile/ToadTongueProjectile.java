package radon.jujutsu_kaisen.entity.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import radon.jujutsu_kaisen.effect.JJKEffects;
import radon.jujutsu_kaisen.entity.JJKEntities;
import radon.jujutsu_kaisen.entity.base.JujutsuProjectile;
import radon.jujutsu_kaisen.entity.ten_shadows.ToadEntity;
import radon.jujutsu_kaisen.util.HelperMethods;

import java.util.UUID;

public class ToadTongueProjectile extends JujutsuProjectile {
    public static final float SPEED = 2.0F;

    private int range;
    private UUID target;
    private boolean grabbed;

    public ToadTongueProjectile(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        this.noCulling = true;
    }

    public ToadTongueProjectile(LivingEntity pShooter, int range, UUID target) {
        this(JJKEntities.TOAD_TONGUE.get(), pShooter.level());

        this.setOwner(pShooter);

        Vec3 spawn = new Vec3(pShooter.getX(), pShooter.getEyeY() - (this.getBbHeight() / 2.0F) - 0.1D, pShooter.getZ());
        this.setPos(spawn);

        this.setDeltaMovement(HelperMethods.getLookAngle(pShooter).scale(SPEED * (((ToadEntity) pShooter).hasWings() ? 5.0D : 1.0D)));

        this.target = target;
        this.range = range;
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);

        pCompound.putInt("range", this.range);
        pCompound.putUUID("target", this.target);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);

        this.range = pCompound.getInt("range");
        this.target = pCompound.getUUID("target");
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult pResult) {
        Entity target = pResult.getEntity();

        if (!target.getUUID().equals(this.target)) return;

        this.grabbed = true;
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult pResult) {
        this.discard();
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();

        Entity owner = this.getOwner();

        if (owner instanceof ToadEntity toad) {
            toad.setCanShoot(true);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            Entity owner = this.getOwner();

            if (owner != null) {
                if (this.grabbed) {
                    if (((ServerLevel) this.level()).getEntity(this.target) instanceof LivingEntity living) {
                        if (((ToadEntity) owner).getTarget() != living || living.isDeadOrDying() || living.isRemoved()) this.discard();

                        living.addEffect(new MobEffectInstance(JJKEffects.STUN.get(), 2, 0, false, false, false));
                        this.setPos(living.getX(), living.getY() + (living.getBbHeight() / 2.0F), living.getZ());
                    }
                    this.setDeltaMovement(Vec3.ZERO);
                } else {
                    if (this.distanceTo(owner) >= this.range) {
                        this.discard();
                    } else if (this.getDeltaMovement().lengthSqr() < 1.0E-7D) {
                        this.discard();
                    }
                }
            }
        }
    }
}
