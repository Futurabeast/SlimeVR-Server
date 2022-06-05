package dev.slimevr.vr.trackers.udp;

import dev.slimevr.NetworkProtocol;
import dev.slimevr.vr.trackers.Device;

import java.net.InetAddress;
import java.net.SocketAddress;


public class TrackerUDPConnection {

//	public final int id;
//	public Map<Integer, IMUTracker> sensors = new HashMap<>();
	public SocketAddress address;
	public InetAddress ipAddress;
	public long lastPacket = System.currentTimeMillis();
	public int lastPingPacketId = -1;
	public long lastPingPacketTime = 0;
//	public String name;
//	public String descriptiveName;
	public StringBuilder serialBuffer = new StringBuilder();
	public long lastSerialUpdate = 0;
	public long lastPacketNumber = -1;
	public NetworkProtocol protocol = null;
	private int firmwareBuild = 0;
	public boolean timedOut = false;

	private final Device device;

	public TrackerUDPConnection(SocketAddress address, InetAddress ipAddress) {
		this.address = address;
		this.ipAddress = ipAddress;

		this.device = new Device();
//		this.id = TrackerUDPConnection.nextLocalDeviceId.incrementAndGet();
	}

	public boolean isNextPacket(long packetId) {
		if (packetId != 0 && packetId <= lastPacketNumber)
			return false;
		lastPacketNumber = packetId;
		return true;
	}

	@Override
	public String toString() {
		return "udp:/" + ipAddress;
	}

	public Device getDevice() {
		return device;
	}

	public void setFirmwareBuild(int firmwareBuild) {
		this.firmwareBuild = firmwareBuild;
		this.getDevice().setFirmwareVersion("v" + firmwareBuild);
	}

	public int getFirmwareBuild() {
		return firmwareBuild;
	}
}
