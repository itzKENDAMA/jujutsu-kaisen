package radon.jujutsu_kaisen.ability.projection_sorcery;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.JujutsuKaisen;
import radon.jujutsu_kaisen.ability.Ability;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.ISorcererData;
import radon.jujutsu_kaisen.capability.data.SorcererDataHandler;
import radon.jujutsu_kaisen.damage.JJKDamageSources;
import radon.jujutsu_kaisen.entity.effect.ProjectionFrameEntity;
import radon.jujutsu_kaisen.network.PacketHandler;
import radon.jujutsu_kaisen.network.packet.s2c.ScreenFlashS2CPacket;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class TwentyFourFrameRule extends Ability implements Ability.IToggled {
    private static final float DAMAGE = 15.0F;
    private static final int INVULNERABLE_TIME = 5 * 20;

    private static final Map<UUID, Long> invulnerable = new HashMap<>();

    @Override
    public boolean shouldTrigger(PathfinderMob owner, @Nullable LivingEntity target) {
        return false;
    }

    @Override
    public ActivationType getActivationType(LivingEntity owner) {
        return ActivationType.TOGGLED;
    }

    @Override
    public void run(LivingEntity owner) {

    }

    @Override
    public int getCooldown() {
        return 10 * 20;
    }

    @Override
    public float getCost(LivingEntity owner) {
        return 1.0F;
    }

    @Override
    public void onEnabled(LivingEntity owner) {

    }

    @Override
    public void onDisabled(LivingEntity owner) {

    }

    @Mod.EventBusSubscriber(modid = JujutsuKaisen.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onLivingHurt(LivingHurtEvent event) {
            DamageSource source = event.getSource();
            if (!(source.getEntity() instanceof LivingEntity attacker)) return;

            LivingEntity victim = event.getEntity();

            if (victim.level().isClientSide) return;

            for (ProjectionFrameEntity frame : victim.level().getEntitiesOfClass(ProjectionFrameEntity.class, AABB.ofSize(victim.position(),
                    8.0D, 8.0D, 8.0D))) {
                if (frame.getVictim() != victim) continue;

                Vec3 center = new Vec3(frame.getX(), frame.getY(), frame.getZ());
                ((ServerLevel) frame.level()).sendParticles(ParticleTypes.EXPLOSION, center.x(), center.y(), center.z(), 0, 1.0D, 0.0D, 0.0D, 1.0D);

                frame.level().playSound(null, frame.getX(), frame.getY(), frame.getZ(), SoundEvents.GLASS_BREAK, SoundSource.MASTER, 1.0F, 1.0F);
                frame.discard();

                LivingEntity owner = frame.getOwner();

                if (owner != null) {
                    ISorcererData cap = owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElseThrow();
                    victim.hurt(JJKDamageSources.indirectJujutsuAttack(frame, attacker, JJKAbilities.PROJECTION_SORCERY.get()), DAMAGE * cap.getAbilityPower(owner));

                    invulnerable.put(victim.getUUID(), victim.level().getGameTime());
                }
                return;
            }

            boolean melee = !source.isIndirect() && (source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.PLAYER_ATTACK));

            if (!melee) return;

            Iterator<Map.Entry<UUID, Long>> iter = invulnerable.entrySet().iterator();

            while (iter.hasNext()) {
                Map.Entry<UUID, Long> entry = iter.next();

                if (victim.level().getGameTime() - entry.getValue() >= INVULNERABLE_TIME) {
                    iter.remove();
                } else if (entry.getKey() == victim.getUUID()) {
                    return;
                }
            }

            if (source instanceof JJKDamageSources.JujutsuDamageSource jujutsu && jujutsu.getAbility() == JJKAbilities.PROJECTION_SORCERY.get()) return;

            if (JJKAbilities.hasToggled(attacker, JJKAbilities.TWENTY_FOUR_FRAME_RULE.get())) {
                ISorcererData cap = attacker.getCapability(SorcererDataHandler.INSTANCE).resolve().orElseThrow();
                cap.addSpeedStack();
                
                attacker.level().addFreshEntity(new ProjectionFrameEntity(attacker, victim));

                if (victim instanceof ServerPlayer player) {
                    PacketHandler.sendToClient(new ScreenFlashS2CPacket(), player);
                }
            }
        }
    }
}
