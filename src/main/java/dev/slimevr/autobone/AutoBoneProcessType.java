package dev.slimevr.autobone;

import java.util.HashMap;
import java.util.Map;

public enum AutoBoneProcessType {
	RECORD(0),
	SAVE(1),
	PROCESS(2),
	APPLY(3);

	public final int id;
	
	private static final Map<Integer, AutoBoneProcessType> byId = new HashMap<>();
	
	private AutoBoneProcessType(int id) {
		this.id = id;
	}

	public static AutoBoneProcessType getById(int id) {
		return byId.get(id);
	}
	
	static {
		for(AutoBoneProcessType abpt : values()) {
			byId.put(abpt.id, abpt);
		}
	}
}
