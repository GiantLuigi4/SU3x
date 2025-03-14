package tfc.smallerunits.core.utils;

import tfc.smallerunits.core.networking.hackery.NetworkHandlingContext;
import tfc.smallerunits.core.networking.hackery.NetworkingHacks;

public class AckClock {
	public int upTo;
	public final NetworkingHacks.LevelDescriptor descriptor;
	public final NetworkHandlingContext netCtx;
	
	public AckClock(NetworkingHacks.LevelDescriptor descriptor, NetworkHandlingContext netCtx) {
		this.descriptor = descriptor;
		this.netCtx = netCtx;
	}
}
