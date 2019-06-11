package com.lethe_river.secd;

import java.util.Map;

public interface AsmElement {
	int bin(Map<String, Integer> map);

	default String getLabel() {
		return null;
	}
	default AsmElement label(String name) {
		return new AsmElement() {
			@Override
			public String getLabel() {
				return name;
			}
			@Override
			public int bin(Map<String, Integer> map) {
				return AsmElement.this.bin(map);
			}
		};
	}
}
