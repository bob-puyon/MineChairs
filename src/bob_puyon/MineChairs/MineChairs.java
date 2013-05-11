package bob_puyon.MineChairs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraft.server.v1_5_R3.Packet40EntityMetadata;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_5_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MineChairs extends JavaPlugin
{
	private static MineChairs instance = null;
	public static ChairEffects chairEffects;
	public List<ChairBlock> allowedBlocks = new ArrayList<ChairBlock>();
	public List<Material> validSigns = new ArrayList<Material>();
	public boolean sneaking;
	public boolean autoRotate;
	public boolean signCheck;
	public boolean permissions;
	public boolean notifyplayer;
	public boolean opsOverridePerms;
	public boolean invertedStairCheck;
	public boolean seatOccupiedCheck;
	public boolean invertedStepCheck;
	public boolean perItemPerms;
	public boolean ignoreIfBlockInHand;
	public boolean sitEffectsEnabled;
	public double sittingHeight;
	public double sittingHeightAdj;
	public double distance;
	public int maxChairWidth;
	public int sitMaxHealth;
	public int sitHealthPerInterval;
	public int sitEffectInterval;
	private File pluginFolder;
	private File configFile;
	public byte metadata;
	public HashMap<String, Location> sit = new HashMap<String, Location>();
	public static final String PLUGIN_NAME = "Chairs";
	public static final String LOG_HEADER = "[Chairs]";
	static final Logger log = Logger.getLogger("Minecraft");
	public PluginManager pm;
	public static ChairsIgnoreList ignoreList;
	public String msgSitting;
	public String msgStanding;
	public String msgOccupied;
	public String msgNoPerm;
	public String msgReloaded;
	public String msgDisabled;
	public String msgEnabled;

	public void onEnable()
	{

		instance = this;
		ignoreList = new ChairsIgnoreList();
		ignoreList.load();
		this.pm = getServer().getPluginManager();
		this.pluginFolder = getDataFolder();
		this.configFile = new File(this.pluginFolder, "config.yml");
		createConfig();
		getConfig().options().copyDefaults(true);
		saveConfig();
		loadConfig();
		getServer().getPluginManager().registerEvents(new EventListener(this, ignoreList), this);
		getCommand("chairs").setExecutor(new ChairsCommand(this, ignoreList));
		if (this.sitEffectsEnabled) {
			logInfo("Enabling sitting effects.");
			chairEffects = new ChairEffects(this);
		}
	}

	public void onDisable()
	{
		for (String pName : this.sit.keySet()) {
			Player player = getServer().getPlayer(pName);
			Location loc = player.getLocation().clone();
			loc.setY(loc.getY() + 1.0D);
			player.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
		}

		if (ignoreList != null) {
			ignoreList.save();
		}
		if (chairEffects != null)
			chairEffects.cancel();
	}

	public void restartEffectsTask()
	{
		if (chairEffects != null)
			chairEffects.restart();
	}

	private void createConfig()
	{
		if (!this.pluginFolder.exists()) {
			try {
				this.pluginFolder.mkdir();
			} catch (Exception e) {
				logInfo("ERROR: " + e.getMessage());
			}
		}

		if (!this.configFile.exists())
			try {
				this.configFile.createNewFile();
			} catch (Exception e) {
				logInfo("ERROR: " + e.getMessage());
			}
	}

	public void loadConfig()
	{
		this.autoRotate = getConfig().getBoolean("auto-rotate");
		this.sneaking = getConfig().getBoolean("sneaking");
		this.signCheck = getConfig().getBoolean("sign-check");
		this.sittingHeight = getConfig().getDouble("sitting-height");
		this.sittingHeightAdj = getConfig().getDouble("sitting-height-adj");
		this.distance = getConfig().getDouble("distance");
		this.maxChairWidth = getConfig().getInt("max-chair-width");
		this.permissions = getConfig().getBoolean("permissions");
		this.notifyplayer = getConfig().getBoolean("notify-player");
		this.invertedStairCheck = getConfig().getBoolean("upside-down-check");
		this.seatOccupiedCheck = getConfig().getBoolean("seat-occupied-check");
		this.invertedStepCheck = getConfig().getBoolean("upper-step-check");
		this.perItemPerms = getConfig().getBoolean("per-item-perms");
		this.opsOverridePerms = getConfig().getBoolean("ops-override-perms");
		this.ignoreIfBlockInHand = getConfig().getBoolean("ignore-if-block-in-hand");

		this.sitEffectsEnabled = getConfig().getBoolean("sit-effects.enabled", false);
		this.sitEffectInterval = getConfig().getInt("sit-effects.interval", 20);
		this.sitMaxHealth = getConfig().getInt("sit-effects.healing.max-percent", 100);
		this.sitHealthPerInterval = getConfig().getInt("sit-effects.healing.amount", 1);

		this.msgSitting = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.sitting"));
		this.msgStanding = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.standing"));
		this.msgOccupied = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.occupied"));
		this.msgNoPerm = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.no-permission"));
		this.msgEnabled = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.enabled"));
		this.msgDisabled = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.disabled"));
		this.msgReloaded = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.reloaded"));

		for (String s : getConfig().getStringList("allowed-blocks"))
		{
			String def_blk = s;
			String type;
			String data;

			double sh = this.sittingHeight;

			String[] tmp_high;
			String[] tmp_type;

			// ブロックの種類に応じて@デリミタで高さが割り当てされている場合
			if (def_blk.contains("@")){
				tmp_high = def_blk.split("@",2);
				sh = Double.valueOf( tmp_high[1] );
				def_blk = tmp_high[0];
			}
			// ハーフブロック等ブロックにDataValueが指定されている場合
			// TODO:ハーフブロック郡(STEP,WOOD_STEP)についてはそれ専用のHashMapで処理
			if (def_blk.contains(":")) {
				tmp_type = def_blk.split(":", 2);
				type = tmp_type[0];
				data = tmp_type[1];
			} else {
				type = s;
			}

			try{
				Material mat;
				if (type.matches("\\d+"))
					mat = Material.getMaterial(Integer.parseInt(type));
				else {
					mat = Material.matchMaterial(type);
				}

				if (mat != null) {
					logInfo("Allowed block: " + mat.toString() + " => " + sh);
					this.allowedBlocks.add(new ChairBlock(mat, sh));
				} else {
					logError("Invalid block: " + type);
				}
			}
			catch (Exception e) {
				logError(e.getMessage());
			}
		}

		for (String type : getConfig().getStringList("valid-signs")) {
			try {
				if (type.matches("\\d+"))
					this.validSigns.add(Material.getMaterial(Integer.parseInt(type)));
				else
					this.validSigns.add(Material.matchMaterial(type));
			}
			catch (Exception e)
			{
				logError(e.getMessage());
			}
		}

		ArrayList<String> perms = new ArrayList<String>();
		perms.add("chairs.sit");
		perms.add("chairs.reload");
		perms.add("chairs.self");
		perms.add("chairs.sit.health");
		for (String s : perms)
			if (this.pm.getPermission(s) != null)
				this.pm.removePermission(s);
		PermissionDefault pd;
		if (this.opsOverridePerms)
			pd = PermissionDefault.OP;
		else {
			pd = PermissionDefault.FALSE;
		}

		this.pm.addPermission(new Permission("chairs.sit", "Allow player to sit on a block.", pd));
		this.pm.addPermission(new Permission("chairs.reload", "Allow player to reload the Chairs configuration.", pd));
		this.pm.addPermission(new Permission("chairs.self", "Allow player to self disable or enable sitting.", pd));
	}

	/*
	private void sendPacketToPlayers(PacketContainer pc, Player p) {
		for (Player onlinePlayer : Bukkit.getOnlinePlayers())
			if ((onlinePlayer.canSee(p)) &&
					(onlinePlayer.getWorld().equals(p.getWorld())))
				try {
					this.protocolManager.sendServerPacket(onlinePlayer, pc);
				}
		catch (Exception ex)
		{
		}
	}
	 */

	public void sendSitAll()
	{
		for (String s : this.sit.keySet()) {
			Player p = Bukkit.getPlayer(s);
			if (p != null)
				sendSit(p);
		}
	}

	public void sendSit(Player player)
	{
		//Packet40EntityMetadata packet = new Packet40EntityMetadata(p.getPlayer().getEntityId(), new ChairWatcher((byte)4), false);
		//for (Player play : Bukkit.getOnlinePlayers())
		//パケットの送信
		((CraftPlayer)player).getHandle().playerConnection.sendPacket( generateSitPacket(player) );
	}

	public void sendStand(Player player)
	{
		if (this.sit.containsKey(player.getName())) {
			if (this.notifyplayer) {
				player.sendMessage(ChatColor.GRAY + "You are no longer sitting.");
			}
			this.sit.remove(player.getName());
		}
		//for (Player play : Bukkit.getOnlinePlayers())
		//パケットの送信
		((CraftPlayer)player).getHandle().playerConnection.sendPacket( generateStandPacket(player) );
	}

	/*
	public void sendStand(Player p)
	{
		if (this.sit.containsKey(p.getName())) {
			if ((this.notifyplayer) && (!this.msgStanding.isEmpty())) {
				p.sendMessage(this.msgStanding);
			}
			this.sit.remove(p.getName());
		}
		sendPacketToPlayers(getStandPacket(p), p);
	}
	 */
	private Packet40EntityMetadata generateSitPacket(Player p){
		return new Packet40EntityMetadata(p.getPlayer().getEntityId(), new ChairWatcher((byte)4), false);
	}

	private Packet40EntityMetadata generateStandPacket(Player p){
		return new Packet40EntityMetadata(p.getPlayer().getEntityId(), new ChairWatcher((byte)0), false);
	}


	public void logInfo(String _message) {
		log.log(Level.INFO, String.format("%s %s", new Object[] { "[MineChairs]", _message }));
	}

	public void logError(String _message) {
		log.log(Level.SEVERE, String.format("%s %s", new Object[] { "[MineChairs]", _message }));
	}

	public static MineChairs get() {
		return instance;
	}
}

