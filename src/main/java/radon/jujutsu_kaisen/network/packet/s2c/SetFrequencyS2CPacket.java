package radon.jujutsu_kaisen.network.packet.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
import radon.jujutsu_kaisen.client.gui.screen.VeilRodScreen;

public class SetFrequencyS2CPacket {
    private final int frequency;

    public SetFrequencyS2CPacket(int frequency) {
        this.frequency = frequency;
    }

    public SetFrequencyS2CPacket(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.frequency);
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();

            if (mc.screen instanceof VeilRodScreen screen) {
                screen.setFrequency(this.frequency);
            }
        }));
        ctx.setPacketHandled(true);
    }
}