package dev.slimevr.vr;

import solarxr_protocol.data_feed.device_data.DeviceDataT;
import solarxr_protocol.datatypes.DeviceIdT;
import solarxr_protocol.datatypes.hardware_info.HardwareAddressT;
import solarxr_protocol.datatypes.hardware_info.HardwareInfoT;

import java.util.concurrent.atomic.AtomicInteger;

public class Device {

	public static final AtomicInteger nextLocalDeviceId = new AtomicInteger();

	static HardwareInfoT createHardwareInfo(
		String displayName,
		String firmwareVersion,
		String manufacturer,
		String model,
		String hardwareRevision,
		HardwareAddressT hardwareAddressT,
		int mcuId
	) {
		HardwareInfoT hardwareInfoT = new HardwareInfoT();

		hardwareInfoT.setMcuId(mcuId);
		hardwareInfoT.setFirmwareVersion(firmwareVersion);
		hardwareInfoT.setManufacturer(manufacturer);
		hardwareInfoT.setModel(model);
		hardwareInfoT.setDisplayName(displayName);
		hardwareInfoT.setHardwareAddress(hardwareAddressT);
		hardwareInfoT.setHardwareRevision(hardwareRevision);

		return hardwareInfoT;
	}

	static DeviceDataT createDevice(
		String name,
		HardwareInfoT hardwareInfoT
	) {
		DeviceDataT deviceData = new DeviceDataT();
		DeviceIdT deviceIdT = new DeviceIdT();

		deviceData.setCustomName(name);
		deviceData.setHardwareInfo(hardwareInfoT);
		deviceIdT.setId(nextLocalDeviceId.incrementAndGet());
		deviceData.setId(deviceIdT);

		return deviceData;
	}
}
