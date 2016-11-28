package buttondevteam.discordplugin;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import buttondevteam.lib.TBMCCoreAPI;
import sx.blah.discord.handle.obj.IUser;

public class DiscordSender implements CommandSender {
	private PermissibleBase perm = new PermissibleBase(this);
	private IUser user;

	public DiscordSender(IUser user) {
		this.user = user;
	}

	@Override
	public boolean isPermissionSet(String name) {
		return perm.isPermissionSet(name);
	}

	@Override
	public boolean isPermissionSet(Permission perm) {
		return this.perm.isPermissionSet(perm);
	}

	@Override
	public boolean hasPermission(String name) {
		return perm.hasPermission(name);
	}

	@Override
	public boolean hasPermission(Permission perm) {
		return this.perm.hasPermission(perm);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
		return perm.addAttachment(plugin, name, value);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin) {
		return perm.addAttachment(plugin);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
		return perm.addAttachment(plugin, name, value, ticks);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
		return perm.addAttachment(plugin, ticks);
	}

	@Override
	public void removeAttachment(PermissionAttachment attachment) {
		perm.removeAttachment(attachment);
	}

	@Override
	public void recalculatePermissions() {
		perm.recalculatePermissions();
	}

	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		return perm.getEffectivePermissions();
	}

	@Override
	public boolean isOp() {
		return perm.isOp();
	}

	@Override
	public void setOp(boolean value) {
		perm.setOp(value);
	}

	@Override
	public void sendMessage(String message) {
		try {
			DiscordPlugin.sendMessageToChannel(user.getOrCreatePMChannel(), message);
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while sending message to DiscordSender", e);
		}
	}

	@Override
	public void sendMessage(String[] messages) {
		for (String message : messages)
			sendMessage(message);
	}

	@Override
	public Server getServer() {
		return Bukkit.getServer();
	}

	@Override
	public String getName() {
		return user.getDisplayName(DiscordPlugin.mainServer);
	}

}
