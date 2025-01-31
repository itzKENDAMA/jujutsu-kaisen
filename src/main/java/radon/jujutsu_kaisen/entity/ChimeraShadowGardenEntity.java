package radon.jujutsu_kaisen.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import radon.jujutsu_kaisen.ability.base.DomainExpansion;
import radon.jujutsu_kaisen.block.*;
import radon.jujutsu_kaisen.block.entity.DomainBlockEntity;
import radon.jujutsu_kaisen.capability.data.SorcererDataHandler;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ChimeraShadowGardenEntity extends OpenDomainExpansionEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ChimeraShadowGardenEntity(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public boolean hasSureHitEffect() {
        return false;
    }

    @Override
    public AABB getBounds() {
        int width = this.getWidth();
        int height = this.getHeight();
        return new AABB(this.getX() - width, this.getY(), this.getZ() - width,
                this.getX() + width, this.getY() - height, this.getZ() + width);
    }

    @Override
    public boolean isInsideBarrier(BlockPos pos) {
        if (this.level().getBlockEntity(pos) instanceof DomainBlockEntity be && be.getIdentifier().equals(this.uuid)) return true;

        int width = this.getWidth();
        int height = this.getHeight();
        BlockPos center = this.blockPosition().below();
        BlockPos relative = pos.subtract(center);
        return relative.getY() >= 0 && relative.getY() <= height && relative.distSqr(Vec3i.ZERO) < width * width;
    }

    public ChimeraShadowGardenEntity(LivingEntity owner, DomainExpansion ability, int width, int height) {
        super(JJKEntities.CHIMERA_SHADOW_GARDEN.get(), owner, ability, width, height);
    }

    private void createBarrier(Entity owner) {
        BlockPos center = this.blockPosition().below();

        int width = this.getWidth();
        int height = this.getHeight();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int delay = i;

                int horizontal = i;
                int vertical = j;

                owner.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
                    cap.delayTickEvent(() -> {
                        for (int x = -horizontal; x <= horizontal; x++) {
                            for (int z = -horizontal; z <= horizontal; z++) {
                                double distance = Math.sqrt(x * x + -vertical * -vertical + z * z);

                                if (distance <= horizontal && distance >= horizontal - 1) {
                                    BlockPos pos = center.offset(x, -vertical, z);

                                    if (!this.isAffected(pos)) return;

                                    BlockState state = this.level().getBlockState(pos);

                                    if (!this.isRemoved()) {
                                        CompoundTag custom = null;

                                        BlockEntity existing = this.level().getBlockEntity(pos);

                                        if (existing instanceof DomainBlockEntity) {
                                            continue;
                                        } else if (existing != null) {
                                            custom = existing.saveWithFullMetadata();
                                        }

                                        Block block = JJKBlocks.CHIMERA_SHADOW_GARDEN.get();
                                        owner.level().setBlock(pos, block.defaultBlockState(),
                                                Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);

                                        if (this.level().getBlockEntity(pos) instanceof DomainBlockEntity be) {
                                            be.create(this.uuid, state, custom);
                                        }
                                    }
                                }
                            }
                        }
                    }, delay);
                });
            }
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return this.isAlive();
    }

    @Override
    protected void doPush(@NotNull Entity p_20971_) {

    }

    @Override
    public void tick() {
        this.refreshDimensions();

        LivingEntity owner = this.getOwner();

        if (owner != null) {
            if (!this.level().isClientSide) {
                if (this.getTime() == 0) {
                    this.createBarrier(owner);
                }
            }
        }
        super.tick();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
