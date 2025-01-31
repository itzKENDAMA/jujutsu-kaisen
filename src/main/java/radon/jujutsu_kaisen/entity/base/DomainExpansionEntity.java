package radon.jujutsu_kaisen.entity.base;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import radon.jujutsu_kaisen.ability.Ability;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.DomainExpansion;
import radon.jujutsu_kaisen.capability.data.ISorcererData;
import radon.jujutsu_kaisen.capability.data.SorcererDataHandler;
import radon.jujutsu_kaisen.util.HelperMethods;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public abstract class DomainExpansionEntity extends Mob {
    private static final EntityDataAccessor<Integer> DATA_TIME = SynchedEntityData.defineId(DomainExpansionEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUUID;
    @Nullable
    private LivingEntity cachedOwner;

    protected DomainExpansion ability;
    protected boolean first = true;

    protected DomainExpansionEntity(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public DomainExpansionEntity(EntityType<? extends Mob> pEntityType, LivingEntity owner, DomainExpansion ability) {
        super(pEntityType, owner.level());

        this.setOwner(owner);

        this.ability = ability;
    }

    public boolean hasSureHitEffect() {
        return true;
    }

    public abstract boolean checkSureHitEffect();

    public Ability getAbility() {
        return this.ability;
    }

    @Override
    public @NotNull Vec3 getDeltaMovement() {
        return Vec3.ZERO;
    }

    @Override
    protected boolean updateInWaterStateAndDoFluidPushing() {
        return false;
    }

    @Nullable
    public DomainExpansionCenterEntity getDomainCenter() {
        List<DomainExpansionCenterEntity> collisions = HelperMethods.getEntityCollisionsOfClass(DomainExpansionCenterEntity.class, this.level(), this.getBounds());

        for (DomainExpansionCenterEntity collision : collisions) {
            if (collision.getDomain() == this) {
                return collision;
            }
        }
        return null;
    }

    public void setOwner(@Nullable LivingEntity pOwner) {
        if (pOwner != null) {
            this.ownerUUID = pOwner.getUUID();
            this.cachedOwner = pOwner;
        }
    }

    @Nullable
    public LivingEntity getOwner() {
        if (this.cachedOwner != null && !this.cachedOwner.isRemoved()) {
            return this.cachedOwner;
        } else if (this.ownerUUID != null && this.level() instanceof ServerLevel) {
            this.cachedOwner = (LivingEntity) ((ServerLevel) this.level()).getEntity(this.ownerUUID);
            return this.cachedOwner;
        } else {
            return null;
        }
    }

    public abstract AABB getBounds();
    public abstract boolean isInsideBarrier(BlockPos pos);
    public abstract void warn();

    @Override
    public boolean isInWall() {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity owner = this.getOwner();

        if (!this.level().isClientSide && (owner == null || owner.isRemoved() || !owner.isAlive())) {
            this.discard();
        } else {
            super.tick();

            if (!this.level().isClientSide) {
                int time = this.getTime();

                if (owner != null) {
                    if (!owner.getCapability(SorcererDataHandler.INSTANCE).isPresent()) return;
                    ISorcererData cap = owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElseThrow();

                    if (!cap.hasToggled(this.ability)) {
                        this.discard();
                        return;
                    }
                }
                this.setTime(++time);
            }
        }
    }

    protected boolean isAffected(BlockPos pos) {
        return this.isInsideBarrier(pos);
    }

    protected boolean isAffected(Entity entity) {
        LivingEntity owner = this.getOwner();

        if (owner == null || entity == owner) {
            return false;
        }

        if (entity instanceof TamableAnimal tamable && tamable.isTame() && tamable.getOwner() == owner) return false;

        if (entity instanceof LivingEntity living) {
            if (!owner.canAttack(living)) return false;

            if (living.getCapability(SorcererDataHandler.INSTANCE).isPresent()) {
                ISorcererData cap = living.getCapability(SorcererDataHandler.INSTANCE).resolve().orElseThrow();

                if (cap.hasToggled(JJKAbilities.SIMPLE_DOMAIN.get())) {
                    return false;
                }
            }
        }
        return this.isAffected(entity.blockPosition());
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);

        if (this.ownerUUID != null) {
            pCompound.putUUID("owner", this.ownerUUID);
        }
        pCompound.putInt("time", this.entityData.get(DATA_TIME));
        pCompound.putString("ability", JJKAbilities.getKey(this.ability).toString());
        pCompound.putBoolean("first", this.first);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);

        if (pCompound.hasUUID("owner")) {
            this.ownerUUID = pCompound.getUUID("owner");
        }
        this.entityData.set(DATA_TIME, pCompound.getInt("time"));
        this.ability = (DomainExpansion) JJKAbilities.getValue(new ResourceLocation(pCompound.getString("ability")));
        this.first = pCompound.getBoolean("first");
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(DATA_TIME, 0);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        Entity entity = this.getOwner();
        return new ClientboundAddEntityPacket(this, entity == null ? 0 : entity.getId());
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket pPacket) {
        super.recreateFromPacket(pPacket);

        LivingEntity owner = (LivingEntity) this.level().getEntity(pPacket.getData());

        if (owner != null) {
            this.setOwner(owner);
        }
    }

    // If strength is more than 75% larger than this
    public boolean shouldCollapse(float strength) {
        return ((strength - this.getStrength()) / this.getStrength()) >= 0.75F;
    }

    public float getStrength() {
        LivingEntity owner = this.getOwner();
        if (owner == null) return 0.0F;
        ISorcererData cap = owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElseThrow();
        return cap.getAbilityPower(owner) * (owner.getHealth() / owner.getMaxHealth());
    }

    public int getTime() {
        return this.entityData.get(DATA_TIME);
    }

    private void setTime(int time) {
        this.entityData.set(DATA_TIME, time);
    }
}
