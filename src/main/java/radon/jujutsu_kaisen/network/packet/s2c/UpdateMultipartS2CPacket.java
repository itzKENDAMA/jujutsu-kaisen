package radon.jujutsu_kaisen.network.packet.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.network.NetworkEvent;
import radon.jujutsu_kaisen.client.ClientWrapper;
import radon.jujutsu_kaisen.entity.base.JJKPartEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class UpdateMultipartS2CPacket {
    private int id;
    private Entity entity;
    private final List<PartDataHolder> data = new ArrayList<>();

    public UpdateMultipartS2CPacket(FriendlyByteBuf buf) {
        this.id = buf.readInt();
        int len = buf.readInt();

        for (int i = 0; i < len; i++) {
            if (buf.readBoolean()) {
                this.data.add(PartDataHolder.decode(buf));
            }
        }
    }

    public UpdateMultipartS2CPacket(Entity entity) {
        this.entity = entity;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.entity.getId());
        PartEntity<?>[] parts = this.entity.getParts();

        if (parts != null) {
            buf.writeInt(parts.length);

            for (PartEntity<?> part : parts) {
                if (part instanceof JJKPartEntity<?>) {
                    buf.writeBoolean(true);
                    ((JJKPartEntity<?>) part).writeData().encode(buf);
                } else {
                    buf.writeBoolean(false);
                }
            }
        } else {
            buf.writeInt(0);
        }
    }

    public static class Handler {
        public static void onMessage(UpdateMultipartS2CPacket message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context ctx = supplier.get();

            ctx.enqueueWork(() -> {
                Level level = ClientWrapper.getLevel();

                if (level == null) return;

                Entity entity = level.getEntity(message.id);

                if (entity != null && entity.isMultipartEntity()) {
                    PartEntity<?>[] parts = entity.getParts();

                    if (parts == null) return;

                    int index = 0;

                    for (PartEntity<?> part : parts) {
                        if (part instanceof JJKPartEntity<?>) {
                            ((JJKPartEntity<?>) part).readData(message.data.get(index));
                            index++;
                        }
                    }
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    public record PartDataHolder(double x, double y, double z, float yRot, float xRot, float width, float height, boolean fixed, boolean dirty, List<SynchedEntityData.DataValue<?>> data) {
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeDouble(this.x);
            buffer.writeDouble(this.y);
            buffer.writeDouble(this.z);
            buffer.writeFloat(this.yRot);
            buffer.writeFloat(this.xRot);
            buffer.writeFloat(this.width);
            buffer.writeFloat(this.height);
            buffer.writeBoolean(this.fixed);
            buffer.writeBoolean(this.dirty);

            if (this.dirty) {
                for (SynchedEntityData.DataValue<?> datavalue : this.data) {
                    datavalue.write(buffer);
                }
                buffer.writeByte(255);
            }
        }

        static PartDataHolder decode(FriendlyByteBuf buffer) {
            boolean dirty;

            return new PartDataHolder(
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readBoolean(),
                    dirty = buffer.readBoolean(),
                    dirty ? unpack(buffer) : null
            );
        }

        private static List<SynchedEntityData.DataValue<?>> unpack(FriendlyByteBuf buf) {
            List<SynchedEntityData.DataValue<?>> result = new ArrayList<>();

            int i;

            while ((i = buf.readUnsignedByte()) != 255) {
                result.add(SynchedEntityData.DataValue.read(buf, i));
            }
            return result;
        }
    }
}
