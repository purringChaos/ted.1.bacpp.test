package tv.blackarrow.cpp.utils;

public enum AlternateContentVersion {
	ESNI_I02("ESNI I02"), ESNI_224("ESNI 224"), SCHEDULELESS("Scheduleless");
	private final String value;

	AlternateContentVersion(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static AlternateContentVersion fromValue(String v) {
		for (AlternateContentVersion c : AlternateContentVersion.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		return null;
	}
}
