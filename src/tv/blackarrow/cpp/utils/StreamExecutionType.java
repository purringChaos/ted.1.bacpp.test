package tv.blackarrow.cpp.utils;

public enum StreamExecutionType {
	ENCODER_LEVEL("Encoder-level (global) execution"), MANIFEST_LEVEL("Viewer-specific execution");

	private final String value;

	private StreamExecutionType(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static StreamExecutionType fromValue(String v) {
		for (StreamExecutionType c : StreamExecutionType.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
