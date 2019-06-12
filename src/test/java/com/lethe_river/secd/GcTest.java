package com.lethe_river.secd;

import static com.lethe_river.secd.Assembler.*;
import static com.lethe_river.secd.DataHeader.*;
import static com.lethe_river.secd.Opcode.*;
import static org.junit.Assert.assertEquals;

import org.junit.*;

public class GcTest {
	static Program fact;

	@BeforeClass
	public static void preparePrograms() {
		fact = new Assembler(
				INT, VALUE(1).label("ONE"),
				INT, VALUE(4).label("FOUR"),
				FNP, REF("FACT").label("FACT_P"),
				INT, VALUE(0).label("ZERO"),
				CELL,REF("ZERO").label("ZZ"), REF("ZERO"),

				NIL.label("ENTRY"),
				LDC, REF("FOUR"),
				CONS,
				LDF, REF("FACT_P"),
				AP,
				STOP,

				NIL.label("FACT"),
				LD,  REF("ZZ"),
				LDC, REF("ZERO"),
				EQ,
				SEL, REF("FACT_T"), REF("FACT_F"),
				RTN,

				LDC.label("FACT_T"), REF("ONE"),
				JOIN,

				LDC.label("FACT_F"), REF("ONE"),
				LD, REF("ZZ"),
				SUB,
				CONS,
				LDF, REF("FACT_P"),
				AP,
				LD,  REF("ZZ"),
				MUL,
				JOIN
		).assembl("ENTRY");
	}

	@Test
	public void factTest() {
		for (int i = 200; i < 400; i+=3) {
			Secd secd = new Secd(fact.bin, fact.entryPoint, i);
			assertEquals(24, secd.run());
		}
	}
}
