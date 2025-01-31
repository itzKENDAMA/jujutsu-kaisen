package radon.jujutsu_kaisen.entity.effect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.SorcererDataHandler;
import radon.jujutsu_kaisen.damage.JJKDamageSources;
import radon.jujutsu_kaisen.entity.JJKEntities;
import radon.jujutsu_kaisen.entity.base.JujutsuProjectile;
import radon.jujutsu_kaisen.util.HelperMethods;

public class ForestSpikeEntity extends JujutsuProjectile {
    private static final int DURATION = 5 * 20;
    private static final float DAMAGE = 10.0F;

    public ForestSpikeEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public ForestSpikeEntity(LivingEntity pShooter) {
        super(JJKEntities.FOREST_SPIKE.get(), pShooter.level(), pShooter);
    }

    @Override
    public @NotNull Vec3 getDeltaMovement() {
        return Vec3.ZERO;
    }

    @Override
    public void tick() {
        if (this.getTime() >= DURATION) {
            this.discard();
        } else if (this.getTime() == 0) {
            if (!(this.getOwner() instanceof LivingEntity owner)) return;

            if (this.level().isClientSide) return;

            owner.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
                for (Entity entity : HelperMethods.getEntityCollisions(this.level(), this.getBoundingBox().expandTowards(this.getLookAngle().scale(3.0D)))) {
                    if (entity == this || entity == owner) continue;
                    entity.hurt(JJKDamageSources.indirectJujutsuAttack(this, owner, JJKAbilities.FOREST_SPIKES.get()), DAMAGE * cap.getAbilityPower(owner));
                }
            });
        }
        super.tick();
    }
}
