package radon.jujutsu_kaisen.entity.ten_shadows;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.ability.Ability;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.Summon;
import radon.jujutsu_kaisen.entity.JJKEntities;
import radon.jujutsu_kaisen.entity.base.IJumpInputListener;
import radon.jujutsu_kaisen.entity.base.SorcererEntity;
import radon.jujutsu_kaisen.entity.base.TenShadowsSummon;
import radon.jujutsu_kaisen.util.HelperMethods;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class NueEntity extends TenShadowsSummon implements PlayerRideable, IJumpInputListener {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("move.fly");
    private static final RawAnimation SHOCK = RawAnimation.begin().thenPlay("attack.shock");

    private boolean jump;

    public NueEntity(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected boolean isCustom() {
        return false;
    }

    @Override
    protected boolean canFly() {
        return true;
    }

    @Override
    protected boolean canPerformSorcery() {
        return false;
    }

    @Override
    protected boolean hasMeleeAttack() {
        return true;
    }

    public NueEntity(LivingEntity owner, boolean tame) {
        this(JJKEntities.NUE.get(), owner.level());

        this.setTame(tame);
        this.setOwner(owner);

        Vec3 pos = owner.position()
                .subtract(HelperMethods.getLookAngle(owner).multiply(this.getBbWidth(), 0.0D, this.getBbWidth()));
        this.moveTo(pos.x(), pos.y(), pos.z(), owner.getYRot(), owner.getXRot());

        this.yHeadRot = this.getYRot();
        this.yHeadRotO = this.yHeadRot;

        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity pEntity) {
        if (super.doHurtTarget(pEntity)) {
            if (pEntity instanceof LivingEntity living) {
                Ability lightning = JJKAbilities.NUE_LIGHTNING.get();
                ((Ability.ITenShadowsAttack) lightning).perform(this, living);
            }
            return true;
        }
        return false;
    }

    @Override
    protected float getFlyingSpeed() {
        return this.getTarget() == null || this.isControlledByLocalInstance() ? 0.15F : 1.0F;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return SorcererEntity.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FLYING_SPEED)
                .add(Attributes.MAX_HEALTH, 2 * 20.0D)
                .add(Attributes.ATTACK_DAMAGE, 3 * 2.0D);
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level pLevel) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, pLevel);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    private PlayState flyIdlePredicate(AnimationState<NueEntity> animationState) {
        if (animationState.isMoving()) {
            return animationState.setAndContinue(FLY);
        } else {
            return animationState.setAndContinue(IDLE);
        }
    }

    private PlayState shockPredicate(AnimationState<NueEntity> animationState) {
        if (this.swinging) {
            return animationState.setAndContinue(SHOCK);
        }
        animationState.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "Fly/Idle", this::flyIdlePredicate));
        controllerRegistrar.add(new AnimationController<>(this, "Shock", this::shockPredicate));
    }

    @Override
    public Summon<?> getAbility() {
        return JJKAbilities.NUE.get();
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player pPlayer, @NotNull InteractionHand pHand) {
        if (pPlayer == this.getOwner() && this.isTame() && !this.isVehicle()) {
            if (pPlayer.startRiding(this)) {
                pPlayer.setYRot(this.getYRot());
                pPlayer.setXRot(this.getXRot());
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        } else {
            return super.mobInteract(pPlayer, pHand);
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public double getPassengersRidingOffset() {
        LivingEntity passenger = this.getControllingPassenger();
        if (passenger == null) return super.getPassengersRidingOffset();
        return -passenger.getBbHeight() + 0.8F;
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pPose) {
        EntityDimensions dimensions = super.getDimensions(pPose);

        LivingEntity passenger = this.getControllingPassenger();

        if (passenger != null) {
            return new EntityDimensions(dimensions.width, dimensions.height + passenger.getBbHeight(), dimensions.fixed);
        }
        return dimensions;
    }

    @Override
    protected @NotNull AABB makeBoundingBox() {
        AABB bounds = super.makeBoundingBox();

        LivingEntity passenger = this.getControllingPassenger();

        if (passenger != null) {
            return bounds.setMinY(bounds.minY - passenger.getBbHeight() / 2.0F - 0.4D)
                    .setMaxY(bounds.maxY - passenger.getBbHeight() / 2.0F);
        }
        return bounds;
    }

    @Override
    protected @NotNull Vec3 getRiddenInput(@NotNull Player pPlayer, @NotNull Vec3 pTravelVector) {
        if (this.onGround()) {
            return Vec3.ZERO;
        } else {
            float f = pPlayer.xxa * 0.5F;
            float f1 = pPlayer.zza;

            if (f1 <= 0.0F) {
                f1 *= 0.25F;
            }
            return new Vec3(f, 0.0D, f1);
        }
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity entity = this.getFirstPassenger();

        if (entity instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private Vec2 getRiddenRotation(LivingEntity pEntity) {
        return new Vec2(pEntity.getXRot() * 0.5F, pEntity.getYRot());
    }

    @Override
    protected void tickRidden(@NotNull Player pPlayer, @NotNull Vec3 pTravelVector) {
        super.tickRidden(pPlayer, pTravelVector);

        Vec2 vec2 = this.getRiddenRotation(pPlayer);
        this.setRot(vec2.y, vec2.x);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();

        if (this.jump) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, this.getFlyingSpeed(), 0.0D));
        }
    }

    @Override
    public void setJump(boolean jump) {
        this.jump = jump;
    }

    @Override
    public void tick() {
        super.tick();

        this.refreshDimensions();
    }
}
