package bob_puyon.MineChairs;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChairsCommand
implements CommandExecutor
{
	private final MineChairs plugin;
	public ChairsIgnoreList ignoreList;

	public ChairsCommand(MineChairs instance, ChairsIgnoreList ignoreList)
	{
		this.plugin = instance;
		this.ignoreList = ignoreList;
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (args.length == 0) {
			return false;
		}
		if (args[0].equalsIgnoreCase("reload")) {
			if ((sender.hasPermission("chairs.reload")) || (!(sender instanceof Player))) {
				this.plugin.reloadConfig();
				this.plugin.loadConfig();
				this.plugin.restartEffectsTask();
				if (!this.plugin.msgReloaded.isEmpty()) {
					sender.sendMessage(this.plugin.msgReloaded);
				}
			}
			else if (!this.plugin.msgNoPerm.isEmpty()) {
				sender.sendMessage(this.plugin.msgNoPerm);
			}
		}

		if ((sender instanceof Player)) {
			Player p = (Player)sender;
			if (args[0].equalsIgnoreCase("on")) {
				if ((p.hasPermission("chairs.self")) || (!this.plugin.permissions)) {
					this.ignoreList.removePlayer(p.getName());
					if (!this.plugin.msgEnabled.isEmpty()) {
						p.sendMessage(this.plugin.msgEnabled);
					}
				}
				else if (!this.plugin.msgNoPerm.isEmpty()) {
					p.sendMessage(this.plugin.msgNoPerm);
				}
			}

			if (args[0].equalsIgnoreCase("off")) {
				if ((p.hasPermission("chairs.self")) || (!this.plugin.permissions)) {
					this.ignoreList.addPlayer(p.getName());
					if (!this.plugin.msgDisabled.isEmpty()) {
						p.sendMessage(this.plugin.msgDisabled);
					}
				}
				else if (!this.plugin.msgNoPerm.isEmpty()) {
					p.sendMessage(this.plugin.msgNoPerm);
				}
			}
		}

		return true;
	}
}

