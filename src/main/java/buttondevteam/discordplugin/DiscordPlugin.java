package buttondevteam.discordplugin;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import buttondevteam.bucket.core.TBMCCoreAPI;
import sx.blah.discord.api.*;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IChannel;

/**
 * Hello world!
 *
 */
public class DiscordPlugin extends JavaPlugin implements IListener<ReadyEvent> {
	private static final String SubredditURL = "https://www.reddit.com/r/ChromaGamers";
	private static boolean stop = false;
	private static IDiscordClient dc;

	@Override
	public void onEnable() {
		try {
			Bukkit.getLogger().info("Initializing DiscordPlugin...");
			final File file = new File("TBMC", "DiscordRedditLastAnnouncement.txt");
			if (file.exists()) {
				BufferedReader reader = Files.newReader(file, StandardCharsets.UTF_8);
				String line = reader.readLine();
				lastannouncementtime = Integer.parseInt(line);
			}
			ClientBuilder cb = new ClientBuilder();
			cb.withToken(IOUtils.toString(getClass().getResourceAsStream("/Token.txt"), Charsets.UTF_8));
			dc = cb.login();
			dc.getDispatcher().registerListener(this);
		} catch (Exception e) {
			e.printStackTrace();
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}

	private IChannel channel;

	@Override
	public void handle(ReadyEvent event) {
		try {
			channel = event.getClient().getGuilds().get(0).getChannelsByName("bot").get(0);
			channel.sendMessage("Minecraft server started up");
			Runnable r = new Runnable() {
				public void run() {
					AnnouncementGetterThreadMethod();
				}
			};
			Thread t = new Thread(r);
			t.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDisable() {
		stop = true;
	}

	private long lastannouncementtime = 0;

	private void AnnouncementGetterThreadMethod() {
		while (!stop) {
			try {
				String body = TBMCCoreAPI.DownloadString(SubredditURL + "/new/.json?limit=10"); // TODO: Save last announcement
				JsonArray json = new JsonParser().parse(body).getAsJsonObject().get("data").getAsJsonObject()
						.get("children").getAsJsonArray();
				StringBuilder msgsb = new StringBuilder();
				for (Object obj : json) {
					JsonObject item = (JsonObject) obj;
					final JsonObject data = item.get("data").getAsJsonObject();
					String author = data.get("author").getAsString();
					String title = data.get("title").getAsString();
					// String stickied = data.get("stickied").getAsString();
					JsonElement flairjson = data.get("link_flair_text");
					String flair;
					if (flairjson.isJsonNull())
						flair = null;
					else
						flair = flairjson.getAsString();
					String distinguished = data.get("distinguished").getAsString();
					String url = data.get("url").getAsString();
					long date = data.get("created_utc").getAsLong();
					if (date <= lastannouncementtime)
						break;
					System.out.println("author: " + author);
					System.out.println("title: " + title);
					System.out.println("distinguished: " + distinguished);
					System.out.println("url: " + url);
					if (distinguished.equals("moderator"))
						msgsb.append("A new mod post was submitted to the subreddit");
					else
						msgsb.append("A new post was submitted to the subreddit");
					msgsb.append("\nAuthor: ").append(author).append("\nFlair: ").append(flair).append("\nTitle: ")
							.append(title).append("\n").append(url);
					lastannouncementtime = date;
					File file = new File("TBMC", "DiscordRedditLastAnnouncement.txt");
					Files.write(lastannouncementtime + "", file, StandardCharsets.UTF_8);
				}
				try {
					Thread.sleep(10000);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
