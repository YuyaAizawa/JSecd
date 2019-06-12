package com.lethe_river.secd;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

public class AssemblerTest {
	@Test
	public void factTset() throws IOException {

		String src =
				"INT 1 label ONE \r\n" +
				"				INT 4 label FOUR \r\n" +
				"				FNP REF FACT label FACT_P \r\n" +
				"				INT 0 label ZERO \r\n" +
				"				CELL REF ZERO label ZZ REF ZERO \r\n" +
				"\r\n" +
				"				NIL label ENTRY \r\n" +
				"				LDC REF FOUR \r\n" +
				"				CONS\r\n" +
				"				LDF REF FACT_P\r\n" +
				"				AP\r\n" +
				"				STOP\r\n" +
				"\r\n" +
				"				NIL label FACT\r\n" +
				"				LD  REF ZZ \r\n" +
				"				LDC REF ZERO\r\n" +
				"				EQ\r\n" +
				"				SEL REF FACT_T REF FACT_F\r\n" +
				"				RTN\r\n" +
				"\r\n" +
				"				LDC label FACT_T REF ONE\r\n" +
				"				JOIN\r\n" +
				"\r\n" +
				"				LDC label FACT_F REF ONE\r\n" +
				"				LD REF ZZ\r\n" +
				"				SUB\r\n" +
				"				CONS\r\n" +
				"				LDF REF FACT_P\r\n" +
				"				AP\r\n" +
				"				LD  REF ZZ\r\n" +
				"				MUL\r\n" +
				"				JOIN";

		InputStream is = new ByteArrayInputStream(src.getBytes("utf-8"));
		Program program = Assembler.assemble(is);

		Secd secd = new Secd(program.bin, program.entryPoint, 500);

		assertEquals(24, secd.run());
	}
}
