package buttondevteam.discordplugin.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.DiscordCommandBase;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.channel.message.MentionEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;

public class CommandListener {

	private static final String[] serverReadyStrings = new String[] { "In one week from now", // Ali
			"Between now and the heat-death of the universe.", // Ghostise
			"Soon™", "Ask again this time next month", // Ghostise
			"In about 3 seconds", // Nicolai
			"Right after we finish coding 7 plugins", // Ali
			"It'll be done tomorrow.", // Ali
			"We just need  to complete one tiiiny feature", // Ali
			"In 18 commits", // Ali
			"After we finish strangling Towny", // Ali
			"When we kill every bug in the system", // Ali
			"Once the server stops screaming error messages.",// Ali
	};

	private static final String[] serverReadyQuestions = new String[] { "when will the server be open",
			"when will the server be ready", "when's the server ready", "when's the server open" };

	private static final Random serverReadyRandom = new Random();
	private static final ArrayList<Short> usableServerReadyStrings = new ArrayList<Short>(serverReadyStrings.length) {
		private static final long serialVersionUID = 2213771460909848770L;
		{
			createUsableServerReadyStrings(this);
		}
	};

	private static void createUsableServerReadyStrings(ArrayList<Short> list) {
		for (short i = 0; i < serverReadyStrings.length; i++)
			list.add(i);
	}

	public static IListener<?>[] getListeners() {
		return new IListener[] { new IListener<MentionEvent>() {
			@Override
			public void handle(MentionEvent event) {
				if (DiscordPlugin.SafeMode)
					return;
				if (event.getMessage().getAuthor().isBot())
					return;
				final IChannel channel = event.getMessage().getChannel();
				if (!channel.getStringID().equals(DiscordPlugin.botchannel.getStringID()) && !channel.isPrivate())
					return;
				if (channel.getStringID().equals(DiscordPlugin.chatchannel.getStringID()))
					return; // The chat code already handles this - Right now while testing botchannel is the same as chatchannel
				runCommand(event.getMessage(), true);
			}
		}, new IListener<MessageReceivedEvent>() {
			@Override
			public void handle(MessageReceivedEvent event) {
				if (DiscordPlugin.SafeMode)
					return;
				final String msglowercase = event.getMessage().getContent().toLowerCase();
				if (Arrays.stream(serverReadyQuestions).anyMatch(s -> msglowercase.contains(s))) {
					int next;
					if (usableServerReadyStrings.size() == 0)
						createUsableServerReadyStrings(usableServerReadyStrings);
					next = usableServerReadyStrings.remove(serverReadyRandom.nextInt(usableServerReadyStrings.size()));
					DiscordPlugin.sendMessageToChannel(event.getMessage().getChannel(), serverReadyStrings[next]);
				}
				if (!event.getMessage().getChannel().isPrivate())
					return;
				if (event.getMessage().getAuthor().isBot())
					return;
				runCommand(event.getMessage(), false);
			}
		} };
	}

	/**
	 * Runs a ChromaBot command.
	 * 
	 * @param message
	 *            The Discord message
	 * @param mentionedonly
	 *            Only run the command if ChromaBot is mentioned at the start of the message
	 * @return Whether it ran the command (always true if mentionedonly is false)
	 */
	public static boolean runCommand(IMessage message, boolean mentionedonly) {
		if (DiscordPlugin.SafeMode)
			return true;
		message.getChannel().setTypingStatus(true);
		final StringBuilder cmdwithargs = new StringBuilder(message.getContent());
		final String mention = DiscordPlugin.dc.getOurUser().mention(false);
		final String mentionNick = DiscordPlugin.dc.getOurUser().mention(true);
		boolean gotmention = checkanddeletemention(cmdwithargs, mention, message);
		gotmention = checkanddeletemention(cmdwithargs, mentionNick, message) || gotmention;
		for (String mentionRole : (Iterable<String>) message.getRoleMentions().stream().map(r -> r.mention())::iterator)
			gotmention = checkanddeletemention(cmdwithargs, mentionRole, message) || gotmention; // Delete all mentions
		if (mentionedonly && !gotmention) {
			message.getChannel().setTypingStatus(false);
			return false;
		}
		int index = cmdwithargs.indexOf(" ");
		String cmd;
		String args;
		if (index == -1) {
			cmd = cmdwithargs.toString();
			args = "";
		} else {
			cmd = cmdwithargs.substring(0, index);
			args = cmdwithargs.substring(index + 1);
		}
		DiscordCommandBase.runCommand(cmd, args, message);
		message.getChannel().setTypingStatus(false);
		return true;
	}

	private static boolean checkanddeletemention(StringBuilder cmdwithargs, String mention, IMessage message) {
		if (message.getContent().startsWith(mention)) // TODO: Resolve mentions: Compound arguments, either a mention or text
			if (cmdwithargs.length() > mention.length() + 1)
				cmdwithargs = cmdwithargs.delete(0,
						cmdwithargs.charAt(mention.length()) == ' ' ? mention.length() + 1 : mention.length());
			else
				cmdwithargs.replace(0, cmdwithargs.length(), "help");
		else
			return false;
		if (cmdwithargs.length() == 0)
			cmdwithargs.replace(0, cmdwithargs.length(), "help");
		return true;
	}
}
