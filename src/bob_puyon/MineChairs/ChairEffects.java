package bob_puyon.MineChairs;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ChairEffects
{
	MineChairs plugin;
	int taskID;

	public ChairEffects(MineChairs plugin)
	{
		this.plugin = plugin;
		effectsTask();
	}

	public void cancel() {
		this.plugin.getServer().getScheduler().cancelTask(this.taskID);
		this.taskID = 0;
	}

	public void restart() {
		cancel();
		effectsTask();
	}

	private void effectsTask() {
		this.taskID = this.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, new Runnable()
		{
			public void run() {
				for (Player p : Bukkit.getOnlinePlayers()) {
					String pName = p.getName();
					if ((ChairEffects.this.plugin.sit.containsKey(pName)) &&
							(p.hasPermission("chairs.sit.health"))) {
						double pHealthPcnt = p.getHealth() / p.getMaxHealth() * 100.0D;
						if ((pHealthPcnt < ChairEffects.this.plugin.sitMaxHealth) && (p.getHealth() < p.getMaxHealth()))
						{
							int newHealth = ChairEffects.this.plugin.sitHealthPerInterval + p.getHealth();
							if (newHealth > p.getMaxHealth()) {
								newHealth = p.getMaxHealth();
							}
							p.setHealth(newHealth);
						}
					}
				}
			}
		}
		, this.plugin.sitEffectInterval, this.plugin.sitEffectInterval);
	}
}

