package net.okocraft.hugs;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@NullMarked
final class HugCommand {

    static void register(Commands commands, Predicate<Player> isInCoolDown, BiConsumer<CommandSender, List<? extends Entity>> doHug) {
        commands.register(
                Commands.literal("hug")
                        .requires(HugCommand::canUse)
                        .then(
                                Commands.argument("target", ArgumentTypes.players())
                                        .requires(HugCommand::canUse)
                                        .executes(context -> execute(context, isInCoolDown, doHug))
                        )
                        .build()
        );
    }

    static boolean canUse(CommandSourceStack sender) {
        return sender.getSender().hasPermission("hugs.command");
    }

    static int execute(CommandContext<CommandSourceStack> context, Predicate<Player> isInCoolDown, BiConsumer<CommandSender, List<? extends Entity>> doHug) {
        CommandSender source = context.getSource().getExecutor() != null ?
                context.getSource().getExecutor() :
                context.getSource().getSender();

        if (source instanceof Player player && isInCoolDown.test(player)) {
            context.getSource().getSender().sendMessage(Messages.COOL_DOWN);
            return 0;
        }

        List<Player> targets;
        try {
            targets = context.getArgument("target", PlayerSelectorArgumentResolver.class).resolve(context.getSource());
        } catch (CommandSyntaxException e) {
            context.getSource().getSender().sendMessage(Messages.PLAYER_NOT_FOUND);
            return 0;
        }

        doHug.accept(source, targets);
        return targets.size();
    }
}
