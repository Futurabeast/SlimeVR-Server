package dev.slimevr.vr.trackers;

import io.eiren.util.collections.FastList;


public class Device implements IDevice {

	private final int id;
	private String customName;
	private String firmwareVersion;
	private final FastList<Tracker> trackers;

	public Device() {
		this.id = nextLocalDeviceId.incrementAndGet();
		this.trackers = new FastList<>();
	}

	public int getId() {
		return id;
	}

	public void setCustomName(String customName) {
		this.customName = customName;
	}

	@Override
	public String getManufacturer() {
		return null;
	}

	@Override
	public String getDisplayName() {
		return null;
	}

	@Override
	public String getFirmwareVersion() {
		return this.firmwareVersion;
	}

	public void setFirmwareVersion(String firmwareVersion) {
		this.firmwareVersion = firmwareVersion;
	}

	@Override
	public String getCustomName() {
		return this.customName;
	}

	@Override
	public FastList<Tracker> getTrackers() {
		return trackers;
	}

	public Tracker getTracker(int id) {
		if (id >= 0 && id < this.getTrackers().size())
			return this.getTrackers().get(id);
		return null;
	}
}

