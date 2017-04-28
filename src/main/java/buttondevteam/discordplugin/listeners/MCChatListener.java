package buttondevteam.discordplugin.listeners;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import buttondevteam.discordplugin.*;
import buttondevteam.lib.*;
import buttondevteam.lib.chat.Channel;
import buttondevteam.lib.chat.TBMCChatAPI;
import buttondevteam.lib.player.ChromaGamerBase;
import buttondevteam.lib.player.TBMCPlayer;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;

public class MCChatListener implements Listener, IListener<MessageReceivedEvent> {
	@EventHandler // Minecraft
	public void onMCChat(TBMCChatEvent e) {
		if (e.isCancelled())
			return;
		if (e.getSender() instanceof DiscordSender || e.getSender() instanceof DiscordPlayerSender)
			return;
		if (!e.getChannel().equals(Channel.GlobalChat))
			return;
		synchronized (this) {
			final String authorPlayer = DiscordPlugin.sanitizeString(e.getSender() instanceof Player //
					? ((Player) e.getSender()).getDisplayName() //
					: e.getSender().getName());
			final EmbedBuilder embed = new EmbedBuilder().withAuthorName(authorPlayer).withDescription(e.getMessage());
			final EmbedObject embedObject = e.getSender() instanceof Player
					? embed.withAuthorIcon(
							"https://minotar.net/avatar/" + ((Player) e.getSender()).getName() + "/32.png").build()
					: embed.build();
			final long nanoTime = System.nanoTime();
			if (lastmessage == null || lastmessage.isDeleted()
					|| !authorPlayer.equals(lastmessage.getEmbedded().get(0).getAuthor().getName())
					|| lastmsgtime / 1000000000f < nanoTime / 1000000000f - 120) {
				lastmessage = DiscordPlugin.sendMessageToChannel(DiscordPlugin.chatchannel, "", embedObject);
				lastmsgtime = nanoTime;
			} else
				try {
					embedObject.description = lastmessage.getEmbedded().get(0).getDescription() + "\n"
							+ embedObject.description;
					DiscordPlugin.perform(() -> lastmessage.edit("", embedObject));
				} catch (MissingPermissionsException | DiscordException e1) {
					TBMCCoreAPI.SendException("An error occured while editing chat message!", e1);
				}
		} // TODO: Author URL
	}

	private static final String[] UnconnectedCmds = new String[] { "list", "u", "shrug", "tableflip", "unflip", "mwiki",
			"yeehaw" };

	private static IMessage lastmessage = null;
	private static long lastmsgtime = 0;
	private static short lastlist = 0;
	private static short lastlistp = 0;

	public static final HashMap<String, DiscordSender> UnconnectedSenders = new HashMap<>();
	public static final HashMap<String, DiscordPlayerSender> ConnectedSenders = new HashMap<>();
	public static short ListC = 0;

	public static void resetLastMessage() {
		lastmessage = null;
	}

	@Override // Discord
	public void handle(MessageReceivedEvent event) {
		final IUser author = event.getMessage().getAuthor();
		if (!event.getMessage().getChannel().getID().equals(DiscordPlugin.chatchannel.getID())
		/* && !(event.getMessage().getChannel().isPrivate() && privatechat) */)
			return;
		lastmessage = null;
		lastlist++;
		if (author.isBot())
			return;
		if (CommandListener.runCommand(event.getMessage(), true))
			return;
		String dmessage = event.getMessage().getContent();
		synchronized (this) {
			try {
				DiscordPlayer dp = ChromaGamerBase.getUser(author.getID(), DiscordPlayer.class);
				final DiscordSenderBase dsender;
				Player mcp = null; // Offline players can't really run commands
				final String cid;
				if ((cid = dp.getConnectedID(TBMCPlayer.class)) != null // Connected?
						&& (mcp = Bukkit.getPlayer(cid)) != null) { // Execute as ingame player, if online
					if (!ConnectedSenders.containsKey(author.getID()))
						ConnectedSenders.put(author.getID(),
								new DiscordPlayerSender(author, event.getMessage().getChannel(), mcp));
					dsender = ConnectedSenders.get(author.getID());
				} else {
					TBMCPlayer p = dp.getAs(TBMCPlayer.class);
					if (!UnconnectedSenders.containsKey(author.getID()))
						UnconnectedSenders.put(author.getID(), new DiscordSender(author,
								event.getMessage().getChannel(), p == null ? null : p.PlayerName().get())); // Display the playername, if found
					dsender = UnconnectedSenders.get(author.getID());
				}

				for (IUser u : event.getMessage().getMentions()) {
					dmessage = dmessage.replace(u.mention(false), "@" + u.getName()); // TODO: IG Formatting
					final Optional<String> nick = u.getNicknameForGuild(DiscordPlugin.mainServer);
					dmessage = dmessage.replace(u.mention(true), "@" + (nick.isPresent() ? nick.get() : u.getName()));
				}

				if (dmessage.startsWith("/")) {
					final String cmd = dmessage.substring(1).toLowerCase();
					if (mcp == null && !Arrays.stream(UnconnectedCmds)
							.anyMatch(s -> cmd.equals(s) || cmd.startsWith(s + " "))) {
						// Command not whitelisted
						DiscordPlugin.sendMessageToChannel(event.getMessage().getChannel(), // TODO
								"Sorry, you need to be online on the server and have your accounts connected, you can only access these commands:\n"
										+ Arrays.stream(UnconnectedCmds).map(uc -> "/" + uc)
												.collect(Collectors.joining(", "))
										+ "\nTo connect your accounts, use @ChromaBot connect in "
										+ DiscordPlugin.botchannel.mention());
						return;
					}
					if (lastlist > 5) {
						ListC = 0;
						lastlist = 0;
					}
					if (cmd.equals("list") && Bukkit.getOnlinePlayers().size() == lastlistp && ListC++ > 2) // Lowered already
					{
						dsender.sendMessage("Stop it. You know the answer.");
						lastlist = 0;
					} else
						Bukkit.dispatchCommand(dsender, cmd);
					lastlistp = (short) Bukkit.getOnlinePlayers().size();
					if (!event.getMessage().isDeleted())
						event.getMessage().delete();
				} else {
					TBMCChatAPI.SendChatMessage(Channel.GlobalChat, dsender,
							dmessage + (event.getMessage().getAttachments().size() > 0 ? "\n" + event.getMessage()
									.getAttachments().stream().map(a -> a.getUrl()).collect(Collectors.joining("\n"))
									: ""));
					event.getMessage().getChannel().getMessages().stream().forEach(m -> {
						try {
							final IReaction reaction = m.getReactionByName(DiscordPlugin.DELIVERED_REACTION);
							if (reaction != null)
								DiscordPlugin.perform(() -> m.removeReaction(reaction));
						} catch (Exception e) {
							TBMCCoreAPI.SendException("An error occured while removing reactions from chat!", e);
						}
					});
					DiscordPlugin.perform(() -> event.getMessage().addReaction(DiscordPlugin.DELIVERED_REACTION));
				}
			} catch (Exception e) {
				TBMCCoreAPI.SendException("An error occured while handling message \"" + dmessage + "\"!", e);
				return;
			}
		}
	}
}
