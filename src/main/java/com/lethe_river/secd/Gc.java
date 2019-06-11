package com.lethe_river.secd;

import static com.lethe_river.secd.DataHeader.*;

public class Gc {
	private static final int CELL_SIZE = 3;

	private static final int MARK = 010;

	private final int[] memory;
	private final int heapBegin;

	private int f;

	Gc(int[] memory, int heapBegin) {
		this.memory = memory;
		this.heapBegin = heapBegin;
	}

	void init() {
		f = 0;
		for(int i = heapBegin; i+CELL_SIZE <= memory.length; i += CELL_SIZE) {
			memory[i] = CELL.bin;
			memory[i+2] = f;
			f = i+1;
		}
	}

	int allocInt(int value) {
		if(f == 0) {
			throw new RuntimeException("Out of memory!");
		}
		int h = f - 1;
		f = memory[f+1];

		memory[h  ] = INT.bin;
		memory[h+1] = value;
		memory[h+2] = 0;

		return h+1;
	}

	int allocFnp(int addr) {
		if(f == 0) {
			throw new RuntimeException("Out of memory!");
		}
		int h = f - 1;
		f = memory[f+1];

		memory[h  ] = FNP.bin;
		memory[h+1] = addr;
		memory[h+2] = 0;

		return h+1;
	}

	int allocCons(int car, int cdr) {
		if(f == 0) {
			throw new RuntimeException("Out of memory!");
		}
		int h = f - 1;
		f = memory[f+1];

		memory[h  ] = CELL.bin;
		memory[h+1] = car;
		memory[h+2] = cdr;

		return h+1;
	}
}
