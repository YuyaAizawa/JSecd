package com.lethe_river.secd;

import java.util.Map;

public enum DataHeader implements AsmElement {
	// name(opcode, data size)
	INT  ( 1, 1),
	CELL ( 2, 2),
	FNP  ( 3, 1);

	private static final int MASK = 0b111;

	public final int bin;
	public final int dataSize;

	private DataHeader(int bin, int dataSize) {
		this.bin = bin;
		this.dataSize = dataSize;
	}

	@Override
	public int bin(Map<String, Integer> map) {
		return bin;
	}

	public static DataHeader fromBin(int bin) {
		return values()[(bin & MASK) - 1];
	}
}
