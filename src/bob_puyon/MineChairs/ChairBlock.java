package bob_puyon.MineChairs;

import org.bukkit.Material;

public class ChairBlock
{
	private Material mat;
	private double sitHeight;

	public ChairBlock(Material m, double d)
	{
		this.mat = m;
		this.sitHeight = d;
	}

	public Material getMat() {
		return this.mat;
	}

	public double getSitHeight() {
		return this.sitHeight;
	}
}

