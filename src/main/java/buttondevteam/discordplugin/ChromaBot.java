package buttondevteam.discordplugin;

import buttondevteam.discordplugin.mcchat.MCChatUtils;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.EmbedBuilder;

import javax.annotation.Nullable;
import java.awt.*;

public class ChromaBot {
	/**
	 * May be null if it's not initialized. Initialization happens after the server is done loading (using {@link BukkitScheduler#runTaskAsynchronously(org.bukkit.plugin.Plugin, Runnable)})
	 */
	private static @Getter ChromaBot instance;
	private DiscordPlugin dp;

	/**
	 * This will set the instance field.
	 * 
	 * @param dp
	 *            The Discord plugin
	 */
	ChromaBot(DiscordPlugin dp) {
		instance = this;
		this.dp = dp;
	}

	static void delete() {
		instance = null;
	}

	/**
	 * Send a message to the chat channel and private chats.
	 * 
	 * @param message
	 *            The message to send, duh
	 */
	public void sendMessage(String message) {
		MCChatUtils.forAllMCChat(ch -> DiscordPlugin.sendMessageToChannel(ch, message));
	}

	/**
	 * Send a message to the chat channels and private chats.
     *
	 * @param message
	 *            The message to send, duh
	 * @param embed
	 *            Custom fancy stuff, use {@link EmbedBuilder} to create one
	 */
	public void sendMessage(String message, EmbedObject embed) {
		MCChatUtils.forAllMCChat(ch -> DiscordPlugin.sendMessageToChannel(ch, message, embed));
	}

    /**
     * Send a message to the chat channels, private chats and custom chats.
     *
     * @param message The message to send, duh
     * @param embed   Custom fancy stuff, use {@link EmbedBuilder} to create one
     * @param toggle The toggle type for channelcon
     */
    public void sendMessageCustomAsWell(String message, EmbedObject embed, @Nullable ChannelconBroadcast toggle) {
	    MCChatUtils.forCustomAndAllMCChat(ch -> DiscordPlugin.sendMessageToChannel(ch, message, embed), toggle, false);
    }

	/**
	 * Send a message to an arbitrary channel. This will not send it to the private chats.
	 * 
	 * @param channel
	 *            The channel to send to, use the channel variables in {@link DiscordPlugin}
	 * @param message
	 *            The message to send, duh
	 * @param embed
	 *            Custom fancy stuff, use {@link EmbedBuilder} to create one
	 */
	public void sendMessage(IChannel channel, String message, EmbedObject embed) {
		DiscordPlugin.sendMessageToChannel(channel, message, embed);
	}

	/**
	 * Send a fancy message to the chat channels. This will show a bold text with a colored line.
	 * 
	 * @param message
	 *            The message to send, duh
	 * @param color
	 *            The color of the line before the text
	 */
	public void sendMessage(String message, Color color) {
		MCChatUtils.forAllMCChat(ch -> DiscordPlugin.sendMessageToChannel(ch, message,
				new EmbedBuilder().withTitle(message).withColor(color).build()));
	}

	/**
	 * Send a fancy message to the chat channels. This will show a bold text with a colored line.
	 * 
	 * @param message
	 *            The message to send, duh
	 * @param color
	 *            The color of the line before the text
	 * @param mcauthor
	 *            The name of the Minecraft player who is the author of this message
	 */
	public void sendMessage(String message, Color color, String mcauthor) {
		MCChatUtils.forAllMCChat(ch -> DiscordPlugin.sendMessageToChannel(ch, message,
				DPUtils.embedWithHead(new EmbedBuilder().withTitle(message).withColor(color), mcauthor).build()));
	}

	/**
	 * Send a fancy message to the chat channels. This will show a bold text with a colored line.
	 * 
	 * @param message
	 *            The message to send, duh
	 * @param color
	 *            The color of the line before the text
	 * @param authorname
	 *            The name of the author of this message
	 * @param authorimg
	 *            The URL of the avatar image for this message's author
	 */
	public void sendMessage(String message, Color color, String authorname, String authorimg) {
		MCChatUtils.forAllMCChat(ch -> DiscordPlugin.sendMessageToChannel(ch, message, new EmbedBuilder()
				.withTitle(message).withColor(color).withAuthorName(authorname).withAuthorIcon(authorimg).build()));
	}

	/**
	 * Send a message to the chat channels. This will show a bold text with a colored line.
	 * 
	 * @param message
	 *            The message to send, duh
	 * @param color
	 *            The color of the line before the text
	 * @param sender
	 *            The player who sends this message
	 */
	public void sendMessage(String message, Color color, Player sender) {
		MCChatUtils.forAllMCChat(ch -> DiscordPlugin.sendMessageToChannel(ch, message, DPUtils
				.embedWithHead(new EmbedBuilder().withTitle(message).withColor(color), sender.getName()).build()));
	}

	public void updatePlayerList() {
		MCChatUtils.updatePlayerList();
	}
}
