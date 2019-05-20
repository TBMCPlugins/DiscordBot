package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.Command2DCSender;
import buttondevteam.lib.TBMCCoreAPI;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.PrivateChannel;
import discord4j.core.object.entity.Role;
import lombok.val;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

public class CommandListener {
	/**
	 * Runs a ChromaBot command. If mentionedonly is false, it will only execute the command if it was in #bot with the correct prefix or in private.
	 *
	 * @param message       The Discord message
	 * @param mentionedonly Only run the command if ChromaBot is mentioned at the start of the message
	 * @return Whether it <b>did not run</b> the command
	 */
	public static Mono<Boolean> runCommand(Message message, MessageChannel channel, boolean mentionedonly) {
		Mono<Boolean> ret = Mono.just(true);
		if (!message.getContent().isPresent())
			return ret; //Pin messages and such, let the mcchat listener deal with it
		val content = message.getContent().get();
		Mono<?> tmp = ret;
		if (!mentionedonly) { //mentionedonly conditions are in CommonListeners
			if (!(channel instanceof PrivateChannel)
				&& !(content.charAt(0) == DiscordPlugin.getPrefix()
				&& channel.getId().asString().equals(DiscordPlugin.plugin.commandChannel().get().asString()))) //
				return ret;
			tmp = ret.then(channel.type()); // Fun
		}
		final StringBuilder cmdwithargs = new StringBuilder(content);
		val gotmention = new AtomicBoolean();
		return tmp.flatMapMany(x ->
			DiscordPlugin.dc.getSelf().flatMap(self -> self.asMember(DiscordPlugin.mainServer.getId()))
				.flatMapMany(self -> {
					gotmention.set(checkanddeletemention(cmdwithargs, self.getMention(), message));
					gotmention.set(checkanddeletemention(cmdwithargs, self.getNicknameMention(), message) || gotmention.get());
					val mentions = message.getRoleMentions();
					return self.getRoles().filterWhen(r -> mentions.any(rr -> rr.getName().equals(r.getName())))
						.map(Role::getMention);
				}).map(mentionRole -> {
				gotmention.set(checkanddeletemention(cmdwithargs, mentionRole, message) || gotmention.get()); // Delete all mentions
				return !mentionedonly || gotmention.get(); //Stops here if false
			})).filter(b -> b).last(false).flatMap(b -> channel.type()).flatMap(v -> {
			String cmdwithargsString = cmdwithargs.toString();
			try {
				if (!DiscordPlugin.plugin.getManager().handleCommand(new Command2DCSender(message), cmdwithargsString))
					return DPUtils.reply(message, channel, "Unknown command. Do " + DiscordPlugin.getPrefix() + "help for help.\n" + cmdwithargsString);
			} catch (Exception e) {
				TBMCCoreAPI.SendException("Failed to process Discord command: " + cmdwithargsString, e);
			}
			return Mono.empty();
		}).map(m -> false).defaultIfEmpty(true);
	}

	private static boolean checkanddeletemention(StringBuilder cmdwithargs, String mention, Message message) {
		final char prefix = DiscordPlugin.getPrefix();
		if (message.getContent().orElse("").startsWith(mention)) // TODO: Resolve mentions: Compound arguments, either a mention or text
			if (cmdwithargs.length() > mention.length() + 1) {
				int i = cmdwithargs.indexOf(" ", mention.length());
				if (i == -1)
					i = mention.length();
				else
					//noinspection StatementWithEmptyBody
					for (; i < cmdwithargs.length() && cmdwithargs.charAt(i) == ' '; i++)
						; //Removes any space before the command
				cmdwithargs.delete(0, i);
				cmdwithargs.insert(0, prefix); //Always use the prefix for processing
			} else
				cmdwithargs.replace(0, cmdwithargs.length(), prefix + "help");
		else {
			if (cmdwithargs.length() == 0)
				cmdwithargs.replace(0, cmdwithargs.length(), prefix + "help");
			else if (cmdwithargs.charAt(0) != prefix)
				cmdwithargs.insert(0, prefix);
			return false; //Don't treat / as mention, mentions can be used in public mcchat
		}
		return true;
	}
}
