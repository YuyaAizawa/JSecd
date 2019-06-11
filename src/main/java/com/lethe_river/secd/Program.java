package com.lethe_river.secd;

import java.util.Map;

public final class Program {
	public final Map<Integer, String> labelName;
	public final int[] bin;
	public final int entryPoint;

	public Program(int[] bin, int entryPoint, Map<Integer, String> labelName) {
		this.labelName = labelName;
		this.entryPoint = entryPoint;
		this.bin = bin;
	}
}
