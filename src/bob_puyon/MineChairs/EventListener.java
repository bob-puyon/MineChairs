package bob_puyon.MineChairs;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.material.Stairs;
import org.bukkit.material.Step;
import org.bukkit.material.WoodenStep;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public class EventListener
implements Listener
{
	public MineChairs plugin;
	public ChairsIgnoreList ignoreList;

	public EventListener(MineChairs plugin, ChairsIgnoreList ignoreList)
	{
		this.plugin = plugin;
		this.ignoreList = ignoreList;
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		String pname = player.getName();
		if (this.plugin.sit.containsKey(player.getName())) {
			Location from = player.getLocation();
			Location to = (Location)this.plugin.sit.get(pname);
			if (from.getWorld() == to.getWorld()) {
				if (from.distance(to) > 1.5D)
					this.plugin.sendStand(player);
				else
					this.plugin.sendSit(player);
			}
			else
				this.plugin.sendStand(player);
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		delayedSitTask();
	}

	private void delayedSitTask() {
		this.plugin.getServer().getScheduler().runTaskLater( this.plugin, new Runnable()
		{
			public void run() {
				plugin.sendSitAll();
			}
		}
		, 20L);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();
		if (this.plugin.sit.containsKey(player.getName())) {
			this.plugin.sendStand(player);
			Location loc = player.getLocation().clone();
			loc.setY(loc.getY() + 1.0D);
			player.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
		}
	}

	@EventHandler
	public void onBlockDestroy(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (!this.plugin.sit.isEmpty()) {
			ArrayList<String> standList = new ArrayList<String>();
			for (String s : this.plugin.sit.keySet()) {
				if (((Location)this.plugin.sit.get(s)).equals(block.getLocation())) {
					standList.add(s);
				}
			}
			for (String s : standList) {
				Player player = Bukkit.getPlayer(s);
				this.plugin.sendStand(player);
			}
			standList.clear();
		}
	}

	public boolean isValidChair(Block block) {
		for (ChairBlock cb : this.plugin.allowedBlocks) {
			if (cb.getMat().equals(block.getType())) {
				return true;
			}
		}
		return false;
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if ((event.getPlayer().getItemInHand().getType().isBlock()) && (event.getPlayer().getItemInHand().getTypeId() != 0) && (this.plugin.ignoreIfBlockInHand))
		{
			return;
		}
		if ((event.hasBlock()) && (event.getAction() == Action.RIGHT_CLICK_BLOCK))
		{
			Block block = event.getClickedBlock();
			Stairs stairs = null;
			Step step = null;
			WoodenStep woodenstep = null;
			double sh = this.plugin.sittingHeight;
			boolean blockOkay = false;

			Player player = event.getPlayer();
			if (this.ignoreList.isIgnored(player.getName())) {
				return;
			}

			if ((this.plugin.permissions) &&
					(!player.hasPermission("chairs.sit"))) {
				return;
			}

			if (this.plugin.perItemPerms) {
				if (this.plugin.pm.getPermission("chairs.sit." + block.getTypeId()) == null) {
					this.plugin.pm.addPermission(new Permission("chairs.sit." + block.getTypeId(), "Allow players to sit on a '" + block.getType().name() + "'", PermissionDefault.FALSE));
				}

				if (this.plugin.pm.getPermission("chairs.sit." + block.getType().toString()) == null) {
					this.plugin.pm.addPermission(new Permission("chairs.sit." + block.getType().toString(), "Allow players to sit on a '" + block.getType().name() + "'", PermissionDefault.FALSE));
				}

			}

			for (ChairBlock cb : this.plugin.allowedBlocks) {
				if (cb.getMat().equals(block.getType())) {
					blockOkay = true;
					sh = cb.getSitHeight();
				}

				// ** Sorry, Debug Code **
				//Bukkit.getServer().broadcastMessage("DATA : " + block.getType() );
				//Bukkit.getServer().broadcastMessage("DATA : " + block.getTypeId() );
				//Bukkit.getServer().broadcastMessage("DATA : " + block.getData() );
			}
			if ((blockOkay) || ((player.hasPermission("chairs.sit." + block.getTypeId())) && (this.plugin.perItemPerms)) || ((player.hasPermission("chairs.sit." + block.getType().toString())) && (this.plugin.perItemPerms)))
			{
				if ((block.getState().getData() instanceof Stairs))
					stairs = (Stairs)block.getState().getData();
				else if ((block.getState().getData() instanceof Step))
					step = (Step)block.getState().getData();
				else if ((block.getState().getData() instanceof WoodenStep))
					woodenstep = (WoodenStep)block.getState().getData();
				else {
					sh += this.plugin.sittingHeightAdj;
				}

				int chairwidth = 1;

				if (block.getRelative(BlockFace.DOWN).isLiquid()) {
					return;
				}
				if (block.getRelative(BlockFace.DOWN).isEmpty()) {
					return;
				}
				if (!block.getRelative(BlockFace.DOWN).getType().isSolid()) {
					return;
				}

				if (this.plugin.sit.containsKey(event.getPlayer().getName())) {
					this.plugin.sit.remove(player.getName());
					event.setCancelled(true);
					if ((this.plugin.notifyplayer) && (!this.plugin.msgStanding.isEmpty())) {
						player.sendMessage(this.plugin.msgStanding);
					}
					this.plugin.sendStand(player);
					return;
				}

				if ((this.plugin.distance > 0.0D) && (player.getLocation().distance(block.getLocation().add(0.5D, 0.0D, 0.5D)) > this.plugin.distance)) {
					return;
				}

				if ((stairs != null) &&
						(stairs.isInverted()) && (this.plugin.invertedStairCheck)) {
					return;
				}

				if (step != null){
					if( this.plugin.invertedStepCheck ){ return; }
					if( step.isInverted() ){
						sh += 0.5D;
					}
				}

				if (woodenstep != null){
					if( this.plugin.invertedStepCheck ){ return; }
					if( woodenstep.isInverted() ){
						sh += 0.3D;
					}
				}

				if ((this.plugin.signCheck == true) && (stairs != null)) {
					boolean sign1 = false;
					boolean sign2 = false;

					if ((stairs.getDescendingDirection() == BlockFace.NORTH) || (stairs.getDescendingDirection() == BlockFace.SOUTH)) {
						sign1 = (checkSign(block, BlockFace.EAST)) || (checkFrame(block, BlockFace.EAST, player));
						sign2 = (checkSign(block, BlockFace.WEST)) || (checkFrame(block, BlockFace.WEST, player));
					} else if ((stairs.getDescendingDirection() == BlockFace.EAST) || (stairs.getDescendingDirection() == BlockFace.WEST)) {
						sign1 = (checkSign(block, BlockFace.NORTH)) || (checkFrame(block, BlockFace.NORTH, player));
						sign2 = (checkSign(block, BlockFace.SOUTH)) || (checkFrame(block, BlockFace.SOUTH, player));
					}

					if ((sign1 != true) || (sign2 != true)) {
						return;
					}

				}

				if ((this.plugin.maxChairWidth > 0) && (stairs != null)) {
					if ((stairs.getDescendingDirection() == BlockFace.NORTH) || (stairs.getDescendingDirection() == BlockFace.SOUTH)) {
						chairwidth += getChairWidth(block, BlockFace.EAST);
						chairwidth += getChairWidth(block, BlockFace.WEST);
					} else if ((stairs.getDescendingDirection() == BlockFace.EAST) || (stairs.getDescendingDirection() == BlockFace.WEST)) {
						chairwidth += getChairWidth(block, BlockFace.NORTH);
						chairwidth += getChairWidth(block, BlockFace.SOUTH);
					}

					if (chairwidth > this.plugin.maxChairWidth) {
						return;
					}

				}

				if ((!this.plugin.sneaking) || ((this.plugin.sneaking) && (event.getPlayer().isSneaking()))) {
					if ((this.plugin.seatOccupiedCheck) &&
							(!this.plugin.sit.isEmpty())) {
						for (String s : this.plugin.sit.keySet()) {
							if (((Location)this.plugin.sit.get(s)).equals(block.getLocation())) {
								if (!this.plugin.msgOccupied.isEmpty()) {
									player.sendMessage(this.plugin.msgOccupied.replaceAll("%PLAYER%", s));
								}
								return;
							}
						}

					}

					if (player.getVehicle() != null) {
						player.getVehicle().remove();
					}

					if ((this.plugin.autoRotate) && (stairs != null)) {
						Location plocation = block.getLocation().clone();
						plocation.add(0.5D, sh - 0.5D, 0.5D);
						switch ( stairs.getDescendingDirection() ) {
						case NORTH:
							plocation.setYaw(180.0F);
							break;
						case EAST:
							plocation.setYaw(-90.0F);
							break;
						case SOUTH:
							plocation.setYaw(0.0F);
							break;
						case WEST:
							plocation.setYaw(90.0F);
						default:
							break;
						}
						player.teleport(plocation);
					} else {
						Location plocation = block.getLocation().clone();
						plocation.setYaw(player.getLocation().getYaw());
						player.sendMessage( "sit_hight : " + sh );
						player.teleport(plocation.add(0.5D, sh - 0.5D, 0.5D));
					}
					player.setSneaking(true);
					if ((this.plugin.notifyplayer) && (!this.plugin.msgSitting.isEmpty())) {
						player.sendMessage(this.plugin.msgSitting);
					}
					this.plugin.sit.put(player.getName(), block.getLocation());
					event.setUseInteractedBlock(Result.DENY);

					delayedSitTask();
					//plugin.layDown(player, block);
				}
			}
		}
	}

	// 「Leave Bed」が押下されベッドから起き上がる時のイベント
	@EventHandler
	public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
		//plugin.sendWakeUp( event.getPlayer(), event.getBed() );
	}

	private int getChairWidth(Block block, BlockFace face) {
		int width = 0;

		for (int i = 1; i <= this.plugin.maxChairWidth; i++) {
			Block relative = block.getRelative(face, i);
			if ((relative.getState().getData() instanceof Stairs)) {
				if ((!isValidChair(relative)) || (((Stairs)relative.getState().getData()).getDescendingDirection() != ((Stairs)block.getState().getData()).getDescendingDirection())) break;
				width++;
			}

		}
		return width;
	}

	private boolean checkSign(Block block, BlockFace face)
	{
		for (int i = 1; i <= 100; i++) {
			Block relative = block.getRelative(face, i);
			if ((!isValidChair(relative)) || (((block.getState().getData() instanceof Stairs)) && (((Stairs)relative.getState().getData()).getDescendingDirection() != ((Stairs)block.getState().getData()).getDescendingDirection())))
			{
				if (this.plugin.validSigns.contains(relative.getType())) {
					return true;
				}
				return false;
			}
		}

		return false;
	}

	private boolean checkFrame(Block block, BlockFace face, Player player)
	{
		for (int i = 1; i <= 100; i++) {
			Block relative = block.getRelative(face, i);
			int x = relative.getLocation().getBlockX();
			int y = relative.getLocation().getBlockY();
			int z = relative.getLocation().getBlockZ();
			if ((!isValidChair(relative)) || (((block.getState().getData() instanceof Stairs)) && (((Stairs)relative.getState().getData()).getDescendingDirection() != ((Stairs)block.getState().getData()).getDescendingDirection())))
			{
				if (relative.getType().equals(Material.AIR)) {
					for (Entity e : player.getNearbyEntities(this.plugin.distance, this.plugin.distance, this.plugin.distance)) {
						if (((e instanceof ItemFrame)) && (this.plugin.validSigns.contains(Material.ITEM_FRAME))) {
							int x2 = e.getLocation().getBlockX();
							int y2 = e.getLocation().getBlockY();
							int z2 = e.getLocation().getBlockZ();
							if ((x == x2) && (y == y2) && (z == z2))
								return true;
						}
					}
				}
				else {
					return false;
				}
			}
		}
		return false;
	}
}

