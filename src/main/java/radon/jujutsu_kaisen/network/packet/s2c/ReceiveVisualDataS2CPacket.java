package radon.jujutsu_kaisen.network.packet.s2c;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
import radon.jujutsu_kaisen.client.visual.ClientVisualHandler;

import java.util.UUID;

public class ReceiveVisualDataS2CPacket {
    private final UUID src;
    private final CompoundTag nbt;

    public ReceiveVisualDataS2CPacket(UUID src, CompoundTag nbt) {
        this.src = src;
        this.nbt = nbt;
    }

    public ReceiveVisualDataS2CPacket(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readNbt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.src);
        buf.writeNbt(this.nbt);
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientVisualHandler.VisualData data = ClientVisualHandler.VisualData.deserializeNBT(this.nbt);
            ClientVisualHandler.receive(this.src, data);
        }));
        ctx.setPacketHandled(true);
    }
}