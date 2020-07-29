package buttondevteam.discordplugin.broadcaster;

import buttondevteam.discordplugin.mcchat.MCChatUtils;
import buttondevteam.lib.TBMCCoreAPI;
import lombok.val;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.matcher.ElementMatchers;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class PlayerListWatcher {
	private static Object plist;
	private static Object mock;

	/*public PlayerListWatcher(DedicatedServer minecraftserver) {
		super(minecraftserver); // <-- Does some init stuff and calls Bukkit.setServer() so we have to use Objenesis
	}

	public void sendAll(Packet<?> packet) {
		plist.sendAll(packet);
		try { // Some messages get sent by directly constructing a packet
			if (packet instanceof PacketPlayOutChat) {
				Field msgf = PacketPlayOutChat.class.getDeclaredField("a");
				msgf.setAccessible(true);
				MCChatUtils.forAllMCChat(MCChatUtils.send(((IChatBaseComponent) msgf.get(packet)).toPlainText()));
			}
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Failed to broadcast message sent to all players - hacking failed.", e);
		}
	}

	@Override
	public void sendMessage(IChatBaseComponent ichatbasecomponent, boolean flag) { // Needed so it calls the overridden method
		plist.getServer().sendMessage(ichatbasecomponent);
		ChatMessageType chatmessagetype = flag ? ChatMessageType.SYSTEM : ChatMessageType.CHAT;

		// CraftBukkit start - we run this through our processor first so we can get web links etc
		this.sendAll(new PacketPlayOutChat(CraftChatMessage.fixComponent(ichatbasecomponent), chatmessagetype));
		// CraftBukkit end
	}

	@Override
	public void sendMessage(IChatBaseComponent ichatbasecomponent) { // Needed so it calls the overriden method
		this.sendMessage(ichatbasecomponent, true);
	}

	@Override
	public void sendMessage(IChatBaseComponent[] iChatBaseComponents) { // Needed so it calls the overridden method
		for (IChatBaseComponent component : iChatBaseComponents) {
			sendMessage(component, true);
		}
	}*/

	static boolean hookUpDown(boolean up) throws Exception {
		val csc = Bukkit.getServer().getClass();
		Field conf = csc.getDeclaredField("console");
		conf.setAccessible(true);
		val server = conf.get(Bukkit.getServer());
		val nms = server.getClass().getPackage().getName();
		val dplc = Class.forName(nms + ".DedicatedPlayerList");
		val currentPL = server.getClass().getMethod("getPlayerList").invoke(server);
		if (up) {
			val icbcl = Class.forName(nms + ".IChatBaseComponent");
			Method sendMessageTemp;
			try {
				sendMessageTemp = server.getClass().getMethod("sendMessage", icbcl, UUID.class);
			} catch (NoSuchMethodException e) {
				sendMessageTemp = server.getClass().getMethod("sendMessage", icbcl);
			}
			val sendMessage = sendMessageTemp;
			val cmtcl = Class.forName(nms + ".ChatMessageType");
			val systemType = cmtcl.getDeclaredField("SYSTEM").get(null);
			val chatType = cmtcl.getDeclaredField("CHAT").get(null);

			val obc = csc.getPackage().getName();
			val ccmcl = Class.forName(obc + ".util.CraftChatMessage");
			val fixComponent = ccmcl.getMethod("fixComponent", icbcl);
			val ppoc = Class.forName(nms + ".PacketPlayOutChat");
			Constructor<?> ppocCTemp;
			try {
				ppocCTemp = ppoc.getConstructor(icbcl, cmtcl, UUID.class);
			} catch (Exception e) {
				ppocCTemp = ppoc.getConstructor(icbcl, cmtcl);
			}
			val ppocC = ppocCTemp;
			val sendAll = dplc.getMethod("sendAll", Class.forName(nms + ".Packet"));
			Method tpt;
			try {
				tpt = icbcl.getMethod("toPlainText");
			} catch (NoSuchMethodException e) {
				tpt = icbcl.getMethod("getString");
			}
			val toPlainText = tpt;
			val sysb = Class.forName(nms + ".SystemUtils").getField("b");

			//TODO: 1.16 only
			//String cbs = nms.replace("net.minecraft.server", "org.bukkit.craftbukkit");
			val epcl = Class.forName(nms + ".EntityPlayer");
			/*val getUUID = epcl.getMethod("getUniqueID");
			val getPlayer = Class.forName(cbs + ".CraftPlayer").getConstructors()[0];
			val getDataFixer = server.getClass().getMethod("getDataFixer");
			val getAdvancementData = server.getClass().getMethod("getAdvancementData");
			val adpcl = Class.forName(nms + ".AdvancementDataPlayer").getConstructors()[0];
			val setEPlayer = getAdvancementData.getReturnType().getMethod("a", epcl);*/
			val adpcl = Class.forName(nms + ".AdvancementDataPlayer");
			//Find the original method without overrides
			mock = Mockito.mock(dplc, Mockito.withSettings().defaultAnswer(new Answer<>() { // Cannot call super constructor
				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					final Method method = invocation.getMethod();
					if (!method.getName().equals("sendMessage")) {
						if (method.getName().equals("sendAll")) {
							sendAll(invocation.getArgument(0));
							return null;
						}
						//In 1.16 it passes a reference to the player list to advancement data for each player
						if (method.getName().equals("f") && method.getParameterCount() > 0 && method.getParameterTypes()[0].getSimpleName().equals("EntityPlayer")) {
							//val fHandle = MethodHandles.lookup().findSpecial(dplc, "f", MethodType.methodType(adpcl, epcl), mock.getClass());
							System.out.println("Declaring class: " + method.getDeclaringClass());
							method.setAccessible(true);
							//new ByteBuddy().subclass(dplc).method(ElementMatchers.named("f"))
							var lookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
							lookupConstructor.setAccessible(true); //Create lookup with a given class instead of caller
							var lookup = lookupConstructor.newInstance(mock.getClass());
							val fHandle = lookup.unreflectSpecial(method, mock.getClass()); //Special: super.method()
							return fHandle.invoke(mock, invocation.getArgument(0)); //Invoke with our instance, so it passes that to advancement data, we have the fields as well
						}
						return method.invoke(plist, invocation.getArguments());
					}
					val args = invocation.getArguments();
					val params = method.getParameterTypes();
					System.out.println("Method: " + method.getName());
					System.out.println("args: " + Arrays.toString(args));
					System.out.println("params: " + Arrays.toString(params));
					if (params.length == 0) {
						TBMCCoreAPI.SendException("Found a strange method",
							new Exception("Found a sendMessage() method without arguments."));
						return null;
					}
					if (params[0].getSimpleName().equals("IChatBaseComponent[]"))
						for (val arg : (Object[]) args[0])
							sendMessage(arg, true);
					else if (params[0].getSimpleName().equals("IChatBaseComponent"))
						if (params.length > 1 && params[1].getSimpleName().equalsIgnoreCase("boolean"))
							sendMessage(args[0], (Boolean) args[1]);
						else
							sendMessage(args[0], true);
					else
						TBMCCoreAPI.SendException("Found a method with interesting params",
							new Exception("Found a sendMessage(" + params[0].getSimpleName() + ") method"));
					return null;
				}

				private void sendMessage(Object chatComponent, boolean system) {
					try { //Converted to use reflection
						if (sendMessage.getParameterCount() == 2)
							sendMessage.invoke(server, chatComponent, sysb.get(null));
						else
							sendMessage.invoke(server, chatComponent);
						Object chatmessagetype = system ? systemType : chatType;

						// CraftBukkit start - we run this through our processor first so we can get web links etc
						var comp = fixComponent.invoke(null, chatComponent);
						var packet = ppocC.getParameterCount() == 3
							? ppocC.newInstance(comp, chatmessagetype, sysb.get(null))
							: ppocC.newInstance(comp, chatmessagetype);
						this.sendAll(packet);
						// CraftBukkit end
					} catch (Exception e) {
						TBMCCoreAPI.SendException("An error occurred while passing a vanilla message through the player list", e);
					}
				}

				private void sendAll(Object packet) {
					try { // Some messages get sent by directly constructing a packet
						sendAll.invoke(plist, packet);
						/*if(packet.getClass().getName().contains("Chat"))
							System.out.println("Chat packet: "+packet);
						if(packet.getClass().getName().contains("Advancement"))
							System.out.println("Advancement packet: "+packet);*/
						if (!packet.getClass().getName().contains("KeepAlive"))
							System.out.println("Packet: " + packet);
						if (packet.getClass() == ppoc) {
							//System.out.println("Indeed a chat packet");
							Field msgf = ppoc.getDeclaredField("a");
							msgf.setAccessible(true);
							MCChatUtils.forAllMCChat(MCChatUtils.send((String) toPlainText.invoke(msgf.get(packet))));
						}
					} catch (Exception e) {
						TBMCCoreAPI.SendException("Failed to broadcast message sent to all players - hacking failed.", e);
					}
				}

				/*public Object f(Object entityplayer) { //Returns advancement data
					try {
						UUID uuid = (UUID) getUUID.invoke(entityplayer);
						@SuppressWarnings("unchecked") Map<UUID, Object> map = (Map<UUID, Object>) dplc.getField("p").get(plist);
						var advancementdataplayer = map.get(uuid);

						if (advancementdataplayer == null) {
							var player = (Player) getPlayer.newInstance(Bukkit.getServer(), entityplayer);
							var file = new File(player.getWorld().getWorldFolder(), "advancements");
							File file1 = new File(file, uuid + ".json");

							var dataFixer = getDataFixer.invoke(server);
							var advData = getAdvancementData.invoke(server);
							advancementdataplayer = adpcl.newInstance(dataFixer, this, advData, file1, entityplayer);
							//advancementdataplayer = new AdvancementDataPlayer(this.server.getDataFixer(), this, this.server.getAdvancementData(), file1, entityplayer);
							map.put(uuid, advancementdataplayer);
						}

						setEPlayer.invoke(advancementdataplayer, entityplayer);
						return advancementdataplayer;
					} catch (Exception e) {
						TBMCCoreAPI.SendException("An error occurred while getting advancement data!", e);
					}
				}*/
			}).stubOnly());
			plist = currentPL;
			for (var plc = dplc; plc != null; plc = plc.getSuperclass()) { //Set all fields
				for (var f : plc.getDeclaredFields()) {
					f.setAccessible(true);
					Field modf = f.getClass().getDeclaredField("modifiers");
					modf.setAccessible(true);
					modf.set(f, f.getModifiers() & ~Modifier.FINAL);
					f.set(mock, f.get(plist));
				}
			}
		}
		try {
			server.getClass().getMethod("a", dplc).invoke(server, up ? mock : plist);
		} catch (NoSuchMethodException e) {
			server.getClass().getMethod("a", Class.forName(server.getClass().getPackage().getName() + ".PlayerList"))
				.invoke(server, up ? mock : plist);
		}
		Field pllf = csc.getDeclaredField("playerList");
		pllf.setAccessible(true);
		pllf.set(Bukkit.getServer(), up ? mock : plist);
		return true;
	}
}
