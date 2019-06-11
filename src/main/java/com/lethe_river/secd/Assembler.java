package com.lethe_river.secd;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Assembler {
	private final Map<String, Integer> labelMap = new HashMap<>();
	private final AsmElement[] asms;

	public Assembler(AsmElement... asms) {
		this.asms = asms;
	}

	public Program assembl(String entryPoint) {
		for (int i = 0; i < asms.length; i++) {
			AsmElement e = asms[i];
			String label = e.getLabel();
			if(label != null) {
				labelMap.put(label, i);
			}
		}

		int[] bin = new int[asms.length];
		for (int i = 0; i < asms.length; i++) {
			bin[i] = asms[i].bin(labelMap);
		}
		return new Program(
				bin,
				labelMap.get(entryPoint),
				labelMap.entrySet()
				.stream()
				.collect(Collectors.toMap(
						e -> e.getValue(),
						e -> e.getKey())));
	}

	public static AsmElement REF(String label) {
		return new AsmElement() {
			@Override
			public int bin(Map<String, Integer> map) {
				return map.get(label);
			}
		};
	}

	public static AsmElement VALUE(int value) {
		return new AsmElement() {
			@Override
			public int bin(Map<String, Integer> map) {
				return value;
			}
		};
	}
}
