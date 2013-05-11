package bob_puyon.MineChairs;

import java.util.ArrayList;

import net.minecraft.server.v1_5_R3.DataWatcher;
import net.minecraft.server.v1_5_R3.WatchableObject;

public class ChairWatcher extends DataWatcher
{
	private byte metadata;

	public ChairWatcher(byte i)
	{
		this.metadata = i;
	}

	public ArrayList<WatchableObject> b()
	{
		ArrayList<WatchableObject> list = new ArrayList<WatchableObject>();
		WatchableObject wo = new WatchableObject(0, 0, Byte.valueOf(this.metadata));
		list.add(wo);
		return list;
	}
}

