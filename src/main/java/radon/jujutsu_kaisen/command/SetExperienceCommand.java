package radon.jujutsu_kaisen.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import radon.jujutsu_kaisen.capability.data.SorcererDataHandler;
import radon.jujutsu_kaisen.network.PacketHandler;
import radon.jujutsu_kaisen.network.packet.s2c.SyncSorcererDataS2CPacket;

public class SetExperienceCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> node = dispatcher.register(Commands.literal("setexperience")
                .requires((player) -> player.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.entity()).then(Commands.argument("experience", FloatArgumentType.floatArg()).executes((ctx) ->
                        setGrade(EntityArgument.getPlayer(ctx, "player"), FloatArgumentType.getFloat(ctx, "experience"))))));

        dispatcher.register(Commands.literal("setexperience").requires((player) -> player.hasPermission(2)).redirect(node));
    }

    public static int setGrade(ServerPlayer player, float experience) {
        player.getCapability(SorcererDataHandler.INSTANCE).ifPresent(cap -> {
            cap.setExperience(experience);
            PacketHandler.sendToClient(new SyncSorcererDataS2CPacket(cap.serializeNBT()), player);
        });
        return 1;
    }
}
