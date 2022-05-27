package dev.slimevr.vr.trackers;

import io.eiren.util.collections.FastList;

import java.util.concurrent.atomic.AtomicInteger;


public interface IDevice {
	public static final AtomicInteger nextLocalDeviceId = new AtomicInteger();

	int getId();

	String getManufacturer();

	String getDisplayName();

	String getFirmwareVersion();

	String getCustomName();

	FastList<Tracker> getTrackers();

}
