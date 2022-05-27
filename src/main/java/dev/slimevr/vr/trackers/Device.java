package dev.slimevr.vr.trackers;

public class Device implements IDevice {

	private final int id;
	private final String name;

	public Device(String name) {
		this.id = nextLocalDeviceId.incrementAndGet();
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
