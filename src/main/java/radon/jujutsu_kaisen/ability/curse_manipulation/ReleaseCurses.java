package radon.jujutsu_kaisen.ability.curse_manipulation;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.ability.Ability;
import radon.jujutsu_kaisen.capability.data.SorcererDataHandler;
import radon.jujutsu_kaisen.entity.base.CursedSpirit;
import radon.jujutsu_kaisen.network.PacketHandler;
import radon.jujutsu_kaisen.network.packet.s2c.SyncSorcererDataS2CPacket;
import radon.jujutsu_kaisen.util.HelperMethods;

public class ReleaseCurses extends Ability {
    @Override
    public boolean shouldTrigger(PathfinderMob owner, @Nullable LivingEntity target) {
        return target == null;
    }

    @Override
    public ActivationType getActivationType(LivingEntity owner) {
        return ActivationType.INSTANT;
    }

    private static void makePoofParticles(Entity entity) {
        for (int i = 0; i < 20; ++i) {
            double d0 = HelperMethods.RANDOM.nextGaussian() * 0.02D;
            double d1 = HelperMethods.RANDOM.nextGaussian() * 0.02D;
            double d2 = HelperMethods.RANDOM.nextGaussian() * 0.02D;
            ((ServerLevel) entity.level()).sendParticles(ParticleTypes.POOF, entity.getRandomX(1.0D), entity.getRandomY(), entity.getRandomZ(1.0D),
                    0, d0, d1, d2, 1.0D);
        }
    }

    @Override
    public void run(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel level)) return;

        Registry<EntityType<?>> registry = owner.level().registryAccess().registryOrThrow(Registries.ENTITY_TYPE);

        owner.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
            for (Entity summon : cap.getSummons(level)) {
                if (!(summon instanceof CursedSpirit curse)) continue;

                cap.removeSummon(curse);
                cap.addCurse(registry, curse.getType());

                if (owner instanceof ServerPlayer player) {
                    PacketHandler.sendToClient(new SyncSorcererDataS2CPacket(cap.serializeNBT()), player);
                }

                if (!owner.level().isClientSide) {
                    makePoofParticles(curse);
                }
                curse.discard();
            }
        });
    }

    @Override
    public float getCost(LivingEntity owner) {
        return 0;
    }

    @Override
    public boolean isTechnique() {
        return true;
    }
}
