package radon.jujutsu_kaisen.entity.ten_shadows;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Summon;
import radon.jujutsu_kaisen.entity.ai.goal.BetterFollowOwnerGoal;
import radon.jujutsu_kaisen.entity.ai.goal.LookAtTargetGoal;
import radon.jujutsu_kaisen.entity.base.SorcererEntity;
import radon.jujutsu_kaisen.entity.base.TenShadowsSummon;
import radon.jujutsu_kaisen.entity.projectile.ToadTongueProjectile;
import radon.jujutsu_kaisen.util.HelperMethods;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class ToadEntity extends TenShadowsSummon implements RangedAttackMob {
    private static final EntityDataAccessor<Integer> DATA_RITUAL = SynchedEntityData.defineId(ToadEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CAN_SHOOT = SynchedEntityData.defineId(ToadEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("move.walk");
    private static final RawAnimation TONGUE = RawAnimation.begin().thenPlayAndHold("attack.tongue");
    private static final RawAnimation HOWL = RawAnimation.begin().thenPlayAndHold("misc.howl");

    private static final int RANGE = 20;
    private static final int PULL_INTERVAL = 20;

    public ToadEntity(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected boolean isCustom() {
        return true;
    }

    @Override
    protected boolean canFly() {
        return false;
    }

    @Override
    protected boolean canPerformSorcery() {
        return false;
    }

    @Override
    protected boolean hasMeleeAttack() {
        return false;
    }

    public ToadEntity(EntityType<? extends TamableAnimal> type, LivingEntity owner, boolean ritual) {
        this(type, owner.level());

        this.setTame(true);
        this.setOwner(owner);

        Vec3 pos = ritual ? owner.position() : owner.position()
                .subtract(HelperMethods.getLookAngle(owner).multiply(this.getBbWidth(), 0.0D, this.getBbWidth()));
        this.moveTo(pos.x(), pos.y(), pos.z(), owner.getYRot(), owner.getXRot());

        this.yHeadRot = this.getYRot();
        this.yHeadRotO = this.yHeadRot;
    }

    @Override
    protected void registerGoals() {
        int goal = 1;

        this.goalSelector.addGoal(goal++, new FloatGoal(this));
        this.goalSelector.addGoal(goal++, new RangedAttackGoal(this, 1.0D, PULL_INTERVAL, RANGE));
        this.goalSelector.addGoal(goal++, new LookAtTargetGoal(this));
        this.goalSelector.addGoal(goal++, new BetterFollowOwnerGoal(this, 1.0D, 25.0F, 10.0F, this.canFly()));
        this.goalSelector.addGoal(goal, new RandomLookAroundGoal(this));
    }

    public boolean hasWings() {
        return false;
    }

    public void setRitual(int index, int duration) {
        this.setNoAi(true);
        this.entityData.set(DATA_RITUAL, duration);

        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        double distance = this.getBbWidth() * 2;
        Vec3 look = this.getLookAngle();
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side = look.cross(up);
        Vec3 offset = side.scale(distance * (index < 3 ? 1 : -1))
                .add(look.scale((index % 3) * 3.0D));
        this.setPos(x + offset.x(), y, z + offset.z());

        float yRot = this.getYRot();

        if (index < 3) {
            yRot -= 90.0F;
        } else {
            yRot += 90.0F;
        }
        this.setYRot(yRot);
        this.yHeadRot = this.getYRot();
        this.yHeadRotO = this.yHeadRot;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(DATA_RITUAL, 0);
        this.entityData.define(DATA_CAN_SHOOT, true);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);

        pCompound.putInt("ritual", this.entityData.get(DATA_RITUAL));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);

        this.entityData.set(DATA_RITUAL, pCompound.getInt("ritual"));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return SorcererEntity.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.MAX_HEALTH, 10.0D);
    }

    private PlayState walkPredicate(AnimationState<ToadEntity> animationState) {
        if (animationState.isMoving()) {
            return animationState.setAndContinue(WALK);
        }
        return PlayState.STOP;
    }

    private PlayState tonguePredicate(AnimationState<ToadEntity> animationState) {
        if (!this.entityData.get(DATA_CAN_SHOOT)) {
            return animationState.setAndContinue(TONGUE);
        }
        return PlayState.STOP;
    }

    private PlayState howlPredicate(AnimationState<ToadEntity> animationState) {
        if (this.entityData.get(DATA_RITUAL) > 0) {
            return animationState.setAndContinue(HOWL);
        }
        animationState.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "Walk", this::walkPredicate));
        controllerRegistrar.add(new AnimationController<>(this, "Tongue", this::tonguePredicate));
        controllerRegistrar.add(new AnimationController<>(this, "Howl", this::howlPredicate));
    }

    @Override
    public Summon<?> getAbility() {
        return this.hasWings() ? JJKAbilities.TOAD_TOTALITY.get() : JJKAbilities.TOAD.get();
    }

    public void setCanShoot(boolean canShoot) {
        this.entityData.set(DATA_CAN_SHOOT, canShoot);
    }

    @Override
    protected void customServerAiStep() {
        LivingEntity owner = this.getOwner();

        if (owner != null) {
            if (this.hasWings()) {
                for (Projectile projectile : this.level().getEntitiesOfClass(Projectile.class, owner.getBoundingBox().inflate(1.0D))) {
                    if (projectile instanceof AbstractArrow && projectile.getOwner() != this.getOwner()) {
                        this.shoot(projectile);
                        projectile.discard();
                    }
                }
            }
        }

        LivingEntity target = this.getTarget();

        if (target != null) {
            this.lookControl.setLookAt(target, 30.0F, 30.0F);
        }
    }

    @Override
    public void tick() {
        int ritual = this.entityData.get(DATA_RITUAL);

        if (ritual > 0) {
            this.entityData.set(DATA_RITUAL, --ritual);

            if (ritual == 0) {
                this.discard();
            }
        } else {
            super.tick();
        }
    }

    private void shoot(Entity target) {
        if (!this.entityData.get(DATA_CAN_SHOOT)) return;

        this.yHeadRot = HelperMethods.getYRotD(this, target.getEyePosition());
        this.yBodyRot = HelperMethods.getYRotD(this, target.getEyePosition());

        this.setXRot(HelperMethods.getXRotD(this, target.getEyePosition()));
        this.setYRot(HelperMethods.getYRotD(this, target.getEyePosition()));

        ToadTongueProjectile tongue = new ToadTongueProjectile(this, RANGE, target.getUUID());
        this.level().addFreshEntity(tongue);

        this.entityData.set(DATA_CAN_SHOOT, false);
    }

    @Override
    public void performRangedAttack(@NotNull LivingEntity pTarget, float pVelocity) {
        this.shoot(pTarget);
    }
}
