package radon.jujutsu_kaisen.client.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Locale;

public class TravelParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final Vec3 center;

    protected TravelParticle(ClientLevel pLevel, double pX, double pY, double pZ, TravelParticleOptions options, SpriteSet pSprites) {
        super(pLevel, pX, pY, pZ);

        this.quadSize = Math.max(options.scalar(), (this.random.nextFloat() - 0.5F) * options.scalar());
        this.lifetime = options.lifetime();

        this.center = new Vec3(options.target());

        Vector3f color = options.color();
        this.rCol = color.x();
        this.gCol = color.y();
        this.bCol = color.z();

        this.alpha = options.opacity();

        this.sprites = pSprites;

        this.setSprite(this.sprites.get(this.level.random));
    }

    @Override
    public void tick() {
        super.tick();

        this.setSprite(this.sprites.get(this.level.random));

        Vec3 pos = new Vec3(this.x, this.y, this.z);
        Vec3 direction = this.center.subtract(pos).normalize();

        double remaining = this.center.distanceTo(pos);
        double distance = remaining / this.lifetime;

        Vec3 newPos = pos.add(direction.scale(distance));
        this.setPos(newPos.x(), newPos.y(), newPos.z());
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    public record TravelParticleOptions(Vector3f target, Vector3f color, float scalar, float opacity, int lifetime) implements ParticleOptions {
        public static Deserializer<TravelParticleOptions> DESERIALIZER = new Deserializer<>() {
            public @NotNull TravelParticle.TravelParticleOptions fromCommand(@NotNull ParticleType<TravelParticleOptions> type, @NotNull StringReader reader) throws CommandSyntaxException {
                Vector3f center = TravelParticleOptions.readCenterVector3f(reader);
                reader.expect(' ');
                Vector3f color = TravelParticleOptions.readColorVector3f(reader);
                reader.expect(' ');
                return new TravelParticleOptions(center, color, reader.readFloat(), reader.readFloat(), reader.readInt());
            }

            public @NotNull TravelParticle.TravelParticleOptions fromNetwork(@NotNull ParticleType<TravelParticleOptions> type, @NotNull FriendlyByteBuf buf) {
                return new TravelParticleOptions(TravelParticleOptions.readCenterFromNetwork(buf), TravelParticleOptions.readColorFromNetwork(buf), buf.readFloat(), buf.readFloat(), buf.readInt());
            }
        };

        @Override
        public @NotNull ParticleType<?> getType() {
            return JJKParticles.TRAVEL.get();
        }

        public static Vector3f readColorVector3f(StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float f0 = reader.readFloat();
            reader.expect(' ');
            float f1 = reader.readFloat();
            reader.expect(' ');
            float f2 = reader.readFloat();
            return new Vector3f(f0, f1, f2);
        }

        public static Vector3f readCenterVector3f(StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float f0 = reader.readFloat();
            reader.expect(' ');
            float f1 = reader.readFloat();
            reader.expect(' ');
            float f2 = reader.readFloat();
            return new Vector3f(f0, f1, f2);
        }

        public static Vector3f readColorFromNetwork(FriendlyByteBuf buf) {
            return new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
        }

        public static Vector3f readCenterFromNetwork(FriendlyByteBuf buf) {
            return new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
        }

        @Override
        public void writeToNetwork(FriendlyByteBuf buf) {
            buf.writeDouble(this.target.x());
            buf.writeDouble(this.target.y());
            buf.writeDouble(this.target.z());
            buf.writeFloat(this.color.x());
            buf.writeFloat(this.color.y());
            buf.writeFloat(this.color.z());
            buf.writeFloat(this.scalar);
            buf.writeFloat(this.opacity);
            buf.writeInt(this.lifetime);
        }

        @Override
        public @NotNull String writeToString() {
            return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f %.2f %.2f %.2f %.2f %d", BuiltInRegistries.PARTICLE_TYPE.getKey(this.getType()),
                    this.target.x(), this.target.y(), this.target.z(), this.color.x(), this.color.y(), this.color.z(), this.scalar, this.opacity, this.lifetime);
        }
    }

    public static class Provider implements ParticleProvider<TravelParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public TravelParticle createParticle(@NotNull TravelParticleOptions options, @NotNull ClientLevel level, double x, double y, double z,
                                             double xSpeed, double ySpeed, double zSpeed) {
            TravelParticle particle = new TravelParticle(level, x, y, z, options, this.sprites);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
