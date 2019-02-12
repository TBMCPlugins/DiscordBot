package buttondevteam.discordplugin.role;

import buttondevteam.core.ComponentManager;
import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.DiscordCommandBase;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ConfigData;
import lombok.val;
import org.bukkit.Bukkit;
import sx.blah.discord.handle.impl.events.guild.role.RoleCreateEvent;
import sx.blah.discord.handle.impl.events.guild.role.RoleDeleteEvent;
import sx.blah.discord.handle.impl.events.guild.role.RoleEvent;
import sx.blah.discord.handle.impl.events.guild.role.RoleUpdateEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IRole;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class GameRoleModule extends Component {
	public List<String> GameRoles;

	@Override
	protected void enable() {
		DiscordCommandBase.registerCommand("role", new RoleCommand(this));
		GameRoles = DiscordPlugin.mainServer.getRoles().stream().filter(this::isGameRole).map(IRole::getName).collect(Collectors.toList());
	}

	@Override
	protected void disable() {

	}

	private ConfigData<IChannel> logChannel() {
		return DPUtils.channelData(getConfig(), "logChannel", 239519012529111040L);
	}

	public static void handleRoleEvent(RoleEvent roleEvent) {
		val grm = ComponentManager.getIfEnabled(GameRoleModule.class);
		if (grm == null) return;
		val GameRoles = grm.GameRoles;
		if (roleEvent instanceof RoleCreateEvent) {
			Bukkit.getScheduler().runTaskLaterAsynchronously(DiscordPlugin.plugin, () -> {
				if (roleEvent.getRole().isDeleted() || !grm.isGameRole(roleEvent.getRole()))
					return; //Deleted or not a game role
				GameRoles.add(roleEvent.getRole().getName());
				DiscordPlugin.sendMessageToChannel(grm.logChannel().get(), "Added " + roleEvent.getRole().getName() + " as game role. If you don't want this, change the role's color from the default.");
			}, 100);
		} else if (roleEvent instanceof RoleDeleteEvent) {
			if (GameRoles.remove(roleEvent.getRole().getName()))
				DiscordPlugin.sendMessageToChannel(grm.logChannel().get(), "Removed " + roleEvent.getRole().getName() + " as a game role.");
		} else if (roleEvent instanceof RoleUpdateEvent) {
			val event = (RoleUpdateEvent) roleEvent;
			if (!grm.isGameRole(event.getNewRole())) {
				if (GameRoles.remove(event.getOldRole().getName()))
					DiscordPlugin.sendMessageToChannel(grm.logChannel().get(), "Removed " + event.getOldRole().getName() + " as a game role because it's color changed.");
			} else {
				if (GameRoles.contains(event.getOldRole().getName()) && event.getOldRole().getName().equals(event.getNewRole().getName()))
					return;
				boolean removed = GameRoles.remove(event.getOldRole().getName()); //Regardless of whether it was a game role
				GameRoles.add(event.getNewRole().getName()); //Add it because it has no color
				if (removed)
					DiscordPlugin.sendMessageToChannel(grm.logChannel().get(), "Changed game role from " + event.getOldRole().getName() + " to " + event.getNewRole().getName() + ".");
				else
					DiscordPlugin.sendMessageToChannel(grm.logChannel().get(), "Added " + event.getNewRole().getName() + " as game role because it has the default color.");
			}
		}
	}

	private boolean isGameRole(IRole r) {
		if (r.getGuild().getLongID() != DiscordPlugin.mainServer.getLongID())
			return false; //Only allow on the main server
		val rc = new Color(149, 165, 166, 0);
		return r.getColor().equals(rc)
			&& r.getPosition() < DiscordPlugin.mainServer.getRoleByID(234343495735836672L).getPosition(); //Below the ChromaBot role
	}
}