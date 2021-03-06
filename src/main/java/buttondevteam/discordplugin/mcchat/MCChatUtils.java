package buttondevteam.discordplugin.mcchat;

import buttondevteam.core.ComponentManager;
import buttondevteam.core.MainPlugin;
import buttondevteam.discordplugin.*;
import buttondevteam.discordplugin.broadcaster.GeneralEventBroadcasterModule;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.TBMCSystemChatEvent;
import com.google.common.collect.Sets;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
import io.netty.util.collection.LongObjectHashMap;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.AuthorNagException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MCChatUtils {
	/**
	 * May contain P&lt;DiscordID&gt; as key for public chat
	 */
	public static final ConcurrentHashMap<String, ConcurrentHashMap<Snowflake, DiscordSender>> UnconnectedSenders = new ConcurrentHashMap<>();
	public static final ConcurrentHashMap<String, ConcurrentHashMap<Snowflake, DiscordConnectedPlayer>> ConnectedSenders = new ConcurrentHashMap<>();
	/**
	 * May contain P&lt;DiscordID&gt; as key for public chat
	 */
	public static final ConcurrentHashMap<String, ConcurrentHashMap<Snowflake, DiscordPlayerSender>> OnlineSenders = new ConcurrentHashMap<>();
	public static final ConcurrentHashMap<UUID, DiscordConnectedPlayer> LoggedInPlayers = new ConcurrentHashMap<>();
	static @Nullable LastMsgData lastmsgdata;
	static LongObjectHashMap<Message> lastmsgfromd = new LongObjectHashMap<>(); // Last message sent by a Discord user, used for clearing checkmarks
	private static MinecraftChatModule module;
	private static final HashMap<Class<? extends Event>, HashSet<String>> staticExcludedPlugins = new HashMap<>();

	public static void updatePlayerList() {
		val mod = getModule();
		if (mod == null || !mod.showPlayerListOnDC.get()) return;
		if (lastmsgdata != null)
			updatePL(lastmsgdata);
		MCChatCustom.lastmsgCustom.forEach(MCChatUtils::updatePL);
	}

	private static boolean notEnabled() {
		return (module == null || !module.disabling) && getModule() == null; //Allow using things while disabling the module
	}

	private static MinecraftChatModule getModule() {
		if (module == null || !module.isEnabled()) module = ComponentManager.getIfEnabled(MinecraftChatModule.class);
		//If disabled, it will try to get it again because another instance may be enabled - useful for /discord restart
		return module;
	}

	private static void updatePL(LastMsgData lmd) {
		if (!(lmd.channel instanceof TextChannel)) {
			TBMCCoreAPI.SendException("Failed to update player list for channel " + lmd.channel.getId(),
				new Exception("The channel isn't a (guild) text channel."), getModule());
			return;
		}
		String topic = ((TextChannel) lmd.channel).getTopic().orElse("");
		if (topic.length() == 0)
			topic = ".\n----\nMinecraft chat\n----\n.";
		String[] s = topic.split("\\n----\\n");
		if (s.length < 3)
			return;
		String gid;
		if (lmd instanceof MCChatCustom.CustomLMD)
			gid = ((MCChatCustom.CustomLMD) lmd).groupID;
		else //If we're not using a custom chat then it's either can ("everyone") or can't (null) see at most
			gid = buttondevteam.core.component.channel.Channel.GROUP_EVERYONE; // (Though it's a public chat then rn)
		AtomicInteger C = new AtomicInteger();
		s[s.length - 1] = "Players: " + Bukkit.getOnlinePlayers().stream()
			.filter(p -> (lmd.mcchannel == null
				? gid.equals(buttondevteam.core.component.channel.Channel.GROUP_EVERYONE) //If null, allow if public (custom chats will have their channel stored anyway)
				: gid.equals(lmd.mcchannel.getGroupID(p)))) //If they can see it
			.filter(MCChatUtils::checkEssentials)
			.filter(p -> C.incrementAndGet() > 0) //Always true
			.map(p -> DPUtils.sanitizeString(p.getDisplayName())).collect(Collectors.joining(", "));
		s[0] = C + " player" + (C.get() != 1 ? "s" : "") + " online";
		((TextChannel) lmd.channel).edit(tce -> tce.setTopic(String.join("\n----\n", s)).setReason("Player list update")).subscribe(); //Don't wait
	}

	static boolean checkEssentials(Player p) {
		var ess = MainPlugin.ess;
		if (ess == null) return true;
		return !ess.getUser(p).isHidden();
	}

	public static <T extends DiscordSenderBase> T addSender(ConcurrentHashMap<String, ConcurrentHashMap<Snowflake, T>> senders,
	                                                        User user, T sender) {
		return addSender(senders, user.getId().asString(), sender);
	}

	public static <T extends DiscordSenderBase> T addSender(ConcurrentHashMap<String, ConcurrentHashMap<Snowflake, T>> senders,
	                                                        String did, T sender) {
		var map = senders.get(did);
		if (map == null)
			map = new ConcurrentHashMap<>();
		map.put(sender.getChannel().getId(), sender);
		senders.put(did, map);
		return sender;
	}

	public static <T extends DiscordSenderBase> T getSender(ConcurrentHashMap<String, ConcurrentHashMap<Snowflake, T>> senders,
	                                                        Snowflake channel, User user) {
		var map = senders.get(user.getId().asString());
		if (map != null)
			return map.get(channel);
		return null;
	}

	public static <T extends DiscordSenderBase> T removeSender(ConcurrentHashMap<String, ConcurrentHashMap<Snowflake, T>> senders,
	                                                           Snowflake channel, User user) {
		var map = senders.get(user.getId().asString());
		if (map != null)
			return map.remove(channel);
		return null;
	}

	public static Mono<?> forPublicPrivateChat(Function<Mono<MessageChannel>, Mono<?>> action) {
		if (notEnabled()) return Mono.empty();
		var list = new ArrayList<Mono<?>>();
		list.add(action.apply(module.chatChannelMono()));
		for (LastMsgData data : MCChatPrivate.lastmsgPerUser)
			list.add(action.apply(Mono.just(data.channel)));
		// lastmsgCustom.forEach(cc -> action.accept(cc.channel)); - Only send relevant messages to custom chat
		return Mono.whenDelayError(list);
	}

	/**
	 * For custom and all MC chat
	 *
	 * @param action  The action to act (cannot complete empty)
	 * @param toggle  The toggle to check
	 * @param hookmsg Whether the message is also sent from the hook
	 */
	public static Mono<?> forCustomAndAllMCChat(Function<Mono<MessageChannel>, Mono<?>> action, @Nullable ChannelconBroadcast toggle, boolean hookmsg) {
		if (notEnabled()) return Mono.empty();
		var list = new ArrayList<Publisher<?>>();
		if (!GeneralEventBroadcasterModule.isHooked() || !hookmsg)
			list.add(forPublicPrivateChat(action));
		final Function<MCChatCustom.CustomLMD, Publisher<?>> customLMDFunction = cc -> action.apply(Mono.just(cc.channel));
		if (toggle == null)
			MCChatCustom.lastmsgCustom.stream().map(customLMDFunction).forEach(list::add);
		else
			MCChatCustom.lastmsgCustom.stream().filter(cc -> (cc.toggles & toggle.flag) != 0).map(customLMDFunction).forEach(list::add);
		return Mono.whenDelayError(list);
	}

	/**
	 * Do the {@code action} for each custom chat the {@code sender} have access to and has that broadcast type enabled.
	 *
	 * @param action The action to do
	 * @param sender The sender to check perms of or null to send to all that has it toggled
	 * @param toggle The toggle to check or null to send to all allowed
	 */
	public static Mono<?> forAllowedCustomMCChat(Function<Mono<MessageChannel>, Mono<?>> action, @Nullable CommandSender sender, @Nullable ChannelconBroadcast toggle) {
		if (notEnabled()) return Mono.empty();
		Stream<Publisher<?>> st = MCChatCustom.lastmsgCustom.stream().filter(clmd -> {
			//new TBMCChannelConnectFakeEvent(sender, clmd.mcchannel).shouldSendTo(clmd.dcp) - Thought it was this simple hehe - Wait, it *should* be this simple
			if (toggle != null && (clmd.toggles & toggle.flag) == 0)
				return false; //If null then allow
			if (sender == null)
				return true;
			return clmd.groupID.equals(clmd.mcchannel.getGroupID(sender));
		}).map(cc -> action.apply(Mono.just(cc.channel))); //TODO: Send error messages on channel connect
		return Mono.whenDelayError(st::iterator); //Can't convert as an iterator or inside the stream, but I can convert it as a stream
	}

	/**
	 * Do the {@code action} for each custom chat the {@code sender} have access to and has that broadcast type enabled.
	 *
	 * @param action  The action to do
	 * @param sender  The sender to check perms of or null to send to all that has it toggled
	 * @param toggle  The toggle to check or null to send to all allowed
	 * @param hookmsg Whether the message is also sent from the hook
	 */
	public static Mono<?> forAllowedCustomAndAllMCChat(Function<Mono<MessageChannel>, Mono<?>> action, @Nullable CommandSender sender, @Nullable ChannelconBroadcast toggle, boolean hookmsg) {
		if (notEnabled()) return Mono.empty();
		var cc = forAllowedCustomMCChat(action, sender, toggle);
		if (!GeneralEventBroadcasterModule.isHooked() || !hookmsg)
			return Mono.whenDelayError(forPublicPrivateChat(action), cc);
		return Mono.whenDelayError(cc);
	}

	public static Function<Mono<MessageChannel>, Mono<?>> send(String message) {
		return ch -> ch.flatMap(mc -> {
			resetLastMessage(mc);
			return mc.createMessage(DPUtils.sanitizeString(message));
		});
	}

	public static Mono<?> forAllowedMCChat(Function<Mono<MessageChannel>, Mono<?>> action, TBMCSystemChatEvent event) {
		if (notEnabled()) return Mono.empty();
		var list = new ArrayList<Mono<?>>();
		if (event.getChannel().isGlobal())
			list.add(action.apply(module.chatChannelMono()));
		for (LastMsgData data : MCChatPrivate.lastmsgPerUser)
			if (event.shouldSendTo(getSender(data.channel.getId(), data.user)))
				list.add(action.apply(Mono.just(data.channel))); //TODO: Only store ID?
		MCChatCustom.lastmsgCustom.stream().filter(clmd -> {
			if (!clmd.brtoggles.contains(event.getTarget()))
				return false;
			return event.shouldSendTo(clmd.dcp);
		}).map(clmd -> action.apply(Mono.just(clmd.channel))).forEach(list::add);
		return Mono.whenDelayError(list);
	}

	/**
	 * This method will find the best sender to use: if the player is online, use that, if not but connected then use that etc.
	 */
	static DiscordSenderBase getSender(Snowflake channel, final User author) {
		//noinspection OptionalGetWithoutIsPresent
		return Stream.<Supplier<Optional<DiscordSenderBase>>>of( // https://stackoverflow.com/a/28833677/2703239
			() -> Optional.ofNullable(getSender(OnlineSenders, channel, author)), // Find first non-null
			() -> Optional.ofNullable(getSender(ConnectedSenders, channel, author)), // This doesn't support the public chat, but it'll always return null for it
			() -> Optional.ofNullable(getSender(UnconnectedSenders, channel, author)), //
			() -> Optional.of(addSender(UnconnectedSenders, author,
				new DiscordSender(author, (MessageChannel) DiscordPlugin.dc.getChannelById(channel).block())))).map(Supplier::get).filter(Optional::isPresent).map(Optional::get).findFirst().get();
	}

	/**
	 * Resets the last message, so it will start a new one instead of appending to it.
	 * This is used when someone (even the bot) sends a message to the channel.
	 *
	 * @param channel The channel to reset in - the process is slightly different for the public, private and custom chats
	 */
	public static void resetLastMessage(Channel channel) {
		if (notEnabled()) return;
		if (channel.getId().asLong() == module.chatChannel.get().asLong()) {
			(lastmsgdata == null ? lastmsgdata = new LastMsgData(module.chatChannelMono().block(), null)
				: lastmsgdata).message = null;
			return;
		} // Don't set the whole object to null, the player and channel information should be preserved
		for (LastMsgData data : channel instanceof PrivateChannel ? MCChatPrivate.lastmsgPerUser : MCChatCustom.lastmsgCustom) {
			if (data.channel.getId().asLong() == channel.getId().asLong()) {
				data.message = null;
				return;
			}
		}
		//If it gets here, it's sending a message to a non-chat channel
	}

	public static void addStaticExcludedPlugin(Class<? extends Event> event, String plugin) {
		staticExcludedPlugins.compute(event, (e, hs) -> hs == null
			? Sets.newHashSet(plugin)
			: (hs.add(plugin) ? hs : hs));
	}

	public static void callEventExcludingSome(Event event) {
		if (notEnabled()) return;
		val second = staticExcludedPlugins.get(event.getClass());
		String[] first = module.excludedPlugins.get();
		String[] both = second == null ? first
			: Arrays.copyOf(first, first.length + second.size());
		int i = first.length;
		if (second != null)
			for (String plugin : second)
				both[i++] = plugin;
		callEventExcluding(event, false, both);
	}

	/**
	 * Calls an event with the given details.
	 * <p>
	 * This method only synchronizes when the event is not asynchronous.
	 *
	 * @param event   Event details
	 * @param only    Flips the operation and <b>includes</b> the listed plugins
	 * @param plugins The plugins to exclude. Not case sensitive.
	 */
	public static void callEventExcluding(Event event, boolean only, String... plugins) { // Copied from Spigot-API and modified a bit
		if (event.isAsynchronous()) {
			if (Thread.holdsLock(Bukkit.getPluginManager())) {
				throw new IllegalStateException(
					event.getEventName() + " cannot be triggered asynchronously from inside synchronized code.");
			}
			if (Bukkit.getServer().isPrimaryThread()) {
				throw new IllegalStateException(
					event.getEventName() + " cannot be triggered asynchronously from primary server thread.");
			}
			fireEventExcluding(event, only, plugins);
		} else {
			synchronized (Bukkit.getPluginManager()) {
				fireEventExcluding(event, only, plugins);
			}
		}
	}

	private static void fireEventExcluding(Event event, boolean only, String... plugins) {
		HandlerList handlers = event.getHandlers(); // Code taken from SimplePluginManager in Spigot-API
		RegisteredListener[] listeners = handlers.getRegisteredListeners();
		val server = Bukkit.getServer();

		for (RegisteredListener registration : listeners) {
			if (!registration.getPlugin().isEnabled()
				|| Arrays.stream(plugins).anyMatch(p -> only ^ p.equalsIgnoreCase(registration.getPlugin().getName())))
				continue; // Modified to exclude plugins

			try {
				registration.callEvent(event);
			} catch (AuthorNagException ex) {
				Plugin plugin = registration.getPlugin();

				if (plugin.isNaggable()) {
					plugin.setNaggable(false);

					server.getLogger().log(Level.SEVERE,
						String.format("Nag author(s): '%s' of '%s' about the following: %s",
							plugin.getDescription().getAuthors(), plugin.getDescription().getFullName(),
							ex.getMessage()));
				}
			} catch (Throwable ex) {
				server.getLogger().log(Level.SEVERE, "Could not pass event " + event.getEventName() + " to "
					+ registration.getPlugin().getDescription().getFullName(), ex);
			}
		}
	}

	/**
	 * Call it from an async thread.
	 */
	public static void callLoginEvents(DiscordConnectedPlayer dcp) {
		Consumer<Supplier<String>> loginFail = kickMsg -> {
			dcp.sendMessage("Minecraft chat disabled, as the login failed: " + kickMsg.get());
			MCChatPrivate.privateMCChat(dcp.getChannel(), false, dcp.getUser(), dcp.getChromaUser());
		}; //Probably also happens if the user is banned or so
		val event = new AsyncPlayerPreLoginEvent(dcp.getName(), InetAddress.getLoopbackAddress(), dcp.getUniqueId());
		callEventExcludingSome(event);
		if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
			loginFail.accept(event::getKickMessage);
			return;
		}
		Bukkit.getScheduler().runTask(DiscordPlugin.plugin, () -> {
			val ev = new PlayerLoginEvent(dcp, "localhost", InetAddress.getLoopbackAddress());
			callEventExcludingSome(ev);
			if (ev.getResult() != PlayerLoginEvent.Result.ALLOWED) {
				loginFail.accept(ev::getKickMessage);
				return;
			}
			callEventExcludingSome(new PlayerJoinEvent(dcp, ""));
			dcp.setLoggedIn(true);
			if (module != null) {
				if (module.serverWatcher != null)
					module.serverWatcher.fakePlayers.add(dcp);
				module.log(dcp.getName() + " (" + dcp.getUniqueId() + ") logged in from Discord");
			}
		});
	}

	/**
	 * Only calls the events if the player is actually logged in
	 *
	 * @param dcp       The player
	 * @param needsSync Whether we're in an async thread
	 */
	public static void callLogoutEvent(DiscordConnectedPlayer dcp, boolean needsSync) {
		if (!dcp.isLoggedIn()) return;
		val event = new PlayerQuitEvent(dcp, "");
		if (needsSync) callEventSync(event);
		else callEventExcludingSome(event);
		dcp.setLoggedIn(false);
		if (module != null) {
			module.log(dcp.getName() + " (" + dcp.getUniqueId() + ") logged out from Discord");
			if (module.serverWatcher != null)
				module.serverWatcher.fakePlayers.remove(dcp);
		}
	}

	static void callEventSync(Event event) {
		Bukkit.getScheduler().runTask(DiscordPlugin.plugin, () -> callEventExcludingSome(event));
	}

	@RequiredArgsConstructor
	public static class LastMsgData {
		public Message message;
		public long time;
		public String content;
		public final MessageChannel channel;
		public buttondevteam.core.component.channel.Channel mcchannel;
		public final User user;
	}
}
