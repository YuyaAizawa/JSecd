package com.lethe_river.secd;

import static com.lethe_river.secd.DataHeader.*;

import java.util.Arrays;

import com.lethe_river.secd.Secd.Stack;

public class Gc {
	private static final int CELL_SIZE = 3;

	private static final int MARK = 0b1000;

	private final int[] memory;
	private final int heapBegin;
	private final Stack stack;
	private final Stack environment;
	private final Stack dump;

	private int f;

	// 直前にallocしたデータがGCに巻き込まれないように
	private final int[] guard = new int[3];
	private int guardIdx = 0;

	private int marked = 0;
	private int sweeped = 0;

	Gc(int[] memory, int heapBegin, Stack stack, Stack environment, Stack dump) {
		this.memory = memory;
		this.heapBegin = heapBegin;
		this.stack = stack;
		this.environment = environment;
		this.dump = dump;
	}

	void init() {
		sweep();
	}

	int allocInt(int value) {
		if(f == 0) {
			gc();
		}
		if(f == 0) {
			throw new RuntimeException("Out of memory!");
		}
		int addr = f;
		f = memory[f+1];

		memory[addr-1] = INT.bin;
		memory[addr  ] = value;
		memory[addr+1] = 0;

		guard(addr);

		return addr;
	}

	int allocFnp(int ptr) {
		if(f == 0) {
			gc();
		}
		if(f == 0) {
			throw new RuntimeException("Out of memory!");
		}
		int addr = f;
		f = memory[f+1];

		memory[addr-1] = FNP.bin;
		memory[addr  ] = ptr;
		memory[addr+1] = 0;

		guard(addr);

		return addr;
	}

	int allocCons(int car, int cdr) {
		if(f == 0) {
			gc();
		}
		if(f == 0) {
			throw new RuntimeException("Out of memory!");
		}
		int addr = f;
		f = memory[f+1];

		memory[addr-1] = CELL.bin;
		memory[addr  ] = car;
		memory[addr+1] = cdr;

		guard(addr);

		return addr;
	}

	private void guard(int addr) {
		guard[guardIdx] = addr;

		if(++guardIdx == guard.length) {
			guardIdx = 0;
		}
	}

	void gc() {
		marked = 0;
		sweeped = 0;

		mark(stack.top);
		mark(environment.top);
		mark(dump.top);
		for (int i = 0; i < guard.length; i++) {
			mark(guard[i]);
		}
		sweep();

//		info();
	}

	void mark(int addr) {
		if(addr < heapBegin+1) {
			return;
		}
		if((memory[addr - 1] & MARK) == 0) {
			memory[addr - 1] |= MARK;

			marked++;
			if(DataHeader.fromBin(memory[addr - 1]) == CELL) {
				mark(memory[addr  ]);
				mark(memory[addr+1]);
			}
		}
	}

	void sweep() {
		f = 0;
		for(int addr = heapBegin+1; addr+CELL_SIZE <= memory.length; addr += CELL_SIZE) {
			if((memory[addr - 1] & MARK) == 0) {
				free(addr);
				sweeped++;
			} else {
				memory[addr - 1] &= ~MARK;
			}
		}
	}

	void free(int addr) {
		memory[addr-1] = CELL.bin;
		memory[addr  ] = 0;
		memory[addr+1] = f;
		f = addr;
	}

	void info() {
		System.out.println("-- GC result ----------");
		System.out.println(" marked: "+marked);
		System.out.println("sweeped: "+sweeped);
		System.out.println("-----------------------");
	}
}
