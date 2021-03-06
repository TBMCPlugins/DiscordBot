package buttondevteam.discordplugin.broadcaster;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ComponentMetadata;
import lombok.Getter;

/**
 * Uses a bit of a hacky method of getting all broadcasted messages, including advancements and any other message that's for everyone.
 * If this component is enabled then these messages will show up on Discord.
 */
@ComponentMetadata(enabledByDefault = false)
public class GeneralEventBroadcasterModule extends Component<DiscordPlugin> {
	private static @Getter boolean hooked = false;

	@Override
	protected void enable() {
		try {
			PlayerListWatcher.hookUpDown(true, this);
			log("Finished hooking into the player list");
			hooked = true;
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Error while hacking the player list! Disable this module if you're on an incompatible version.", e, this);
		} catch (NoClassDefFoundError e) {
			logWarn("Error while hacking the player list! Disable this module if you're on an incompatible version.");
		}

	}

	@Override
	protected void disable() {
		try {
			if (!hooked) return;
			if (PlayerListWatcher.hookUpDown(false, this))
				log("Finished unhooking the player list!");
			else
				log("Didn't have the player list hooked.");
			hooked = false;
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Error while hacking the player list!", e, this);
		} catch (NoClassDefFoundError ignored) {
		}
	}
}
