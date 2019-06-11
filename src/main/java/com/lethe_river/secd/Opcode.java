package com.lethe_river.secd;

import java.util.Map;

public enum Opcode implements AsmElement {
	// name(opcode, args)
	STOP ( 0, 0),
	NIL  ( 1, 0),
	LDC  ( 2, 1),
	LD   ( 3, 1),
	CAR  ( 4, 0),
	CDR  ( 5, 0),
	ATOM ( 6, 0),
	CONS ( 7, 0),
	EQ   ( 8, 0),
	LEQ  ( 9, 0),
	ADD  (10, 0),
	SUB  (11, 0),
	MUL  (12, 0),
	DIV  (13, 0),
	REM  (14, 0),
	SEL  (15, 2),
	JOIN (16, 0),
	LDF  (17, 1),
	AP   (18, 0),
	RTN  (19, 0),
	DUM  (20, 0),
	RAP  (21, 0),
	DUP  (22, 0);

	public final int bin;
	public final int operands;

	private Opcode(int bin, int operands) {
		this.bin = bin;
		this.operands = operands;
	}

	public static Opcode fromBin(int bin) {
		return Opcode.values()[bin];
	}

	@Override
	public int bin(Map<String, Integer> map) {
		return bin;
	}
}