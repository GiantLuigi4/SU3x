package tfc.smallerunits.core.data.access;

import tfc.smallerunits.core.networking.hackery.NetworkingHacks;

public interface LevelDescripted {
    NetworkingHacks.LevelDescriptor getDescriptor();
    void setDescriptor(NetworkingHacks.LevelDescriptor descriptor);
}
