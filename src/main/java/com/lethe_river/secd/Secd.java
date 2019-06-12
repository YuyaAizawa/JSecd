package com.lethe_river.secd;

import java.util.Arrays;
import java.util.Map;
import java.util.function.IntBinaryOperator;
import java.util.stream.Collectors;

import static com.lethe_river.secd.Opcode.*;
import static com.lethe_river.secd.DataHeader.*;
import static com.lethe_river.secd.Assembler.*;

public class Secd {

	// 仮想機械の全てのメモリ
	final int[] memory;

	Stack s = new Stack();
	Stack e = new Stack();
	int c;
	Stack d = new Stack();
	final MemoryManager gc;

	Secd(int[] program, int entryPoint, int memorySize) {
		memory = Arrays.copyOf(program, memorySize);
		c = entryPoint;
		gc = new MemoryManager(memory, program.length+1, s, e, d);
		gc.init();
	}

	class Stack {
		int top;

		int push(int i) {
			return top = gc.allocCons(i, top);
		}
	}

	int getN(int i) {
		assert getHeader(i) == INT;
		return memory[i];
	}

	int getF(int i) {
		assert getHeader(i) == FNP;
		return memory[i];
	}

	int cons(int car, int cdr) {
		return gc.allocCons(car, cdr);
	}

	int car(int i) {
		assert getHeader(i) == CELL;
		return memory[i];
	}

	int cdr(int i) {
		assert getHeader(i) == CELL;
		return memory[i+1];
	}

	int locate(int i, int j) {
		return locatei(i, j, e.top);
	}
	int locatei(int i, int j, int env) {
		return i == 0 ? locatej(j, car(env)) : locatei(i - 1, j, cdr(env));
	}
	int locatej(int j, int env) {
		return j == 0 ? car(env) : locatej(j - 1, cdr(env));
	}

	int binOp(IntBinaryOperator op) {
		int a = car(s.top);
		int b = car(cdr(s.top));
		int result = cons(op.applyAsInt(a, b), cdr(cdr(s.top)));
		return s.top = result;
	}

	// メモリを破壊的に代入するので注意
	int rplaca(int x, int y) {
		if(memory[x] != CELL.bin) {
			throw new RuntimeException();
		}
		memory[x] = CELL.bin;
		memory[x+1] = y;
		memory[x+2] = cdr(x);
		return x;
	}

	void step() {
		Opcode inst = Opcode.fromBin(memory[c++]);
		switch (inst) {
		case STOP: {
			c = -1;
			return;
		}

		case NIL: {
			s.push(0);
			return;
		}

		case LDC: {
			s.push(memory[c++]);
			return;
		}

		case LD: {
			int ij = memory[c++];
			int i = getN(car(ij));
			int j = getN(cdr(ij));
			s.push(locate(i, j));
			return;
		}

		case CAR: {
			s.top = gc.allocCons(car(car(s.top)), cdr(s.top));
			return;
		}

		case CDR: {
			s.top = gc.allocCons(cdr(car(s.top)), cdr(s.top));
			return;
		}

		case ATOM: {
			int target = car(s.top);
			DataHeader type = getHeader(target);
			s.top = gc.allocCons(type == INT || type == FNP ? 1 : 0, cdr(s.top));
			return;
		}

		case CONS: {
			binOp((a, b) -> gc.allocCons(a, b));
			return;
		}

		case EQ: {
			binOp((a, b) -> gc.allocInt(getN(a) == getN(b) ? 1 : 0));
			return;
		}

		case LEQ: {
			binOp((a, b) -> gc.allocInt(getN(a) <= getN(b) ? 1 : 0));
			return;
		}

		case ADD: {
			binOp((a, b) -> gc.allocInt(getN(a) + getN(b)));
			return;
		}

		case SUB: {
			binOp((a, b) -> gc.allocInt(getN(a) - getN(b)));
			return;
		}

		case MUL: {
			binOp((a, b) -> gc.allocInt(getN(a) * getN(b)));
			return;
		}

		case DIV: {
			binOp((a, b) -> gc.allocInt(getN(a) / getN(b)));
			return;
		}

		case REM: {
			binOp((a, b) -> gc.allocInt(getN(a) % getN(b)));
			return;
		}

		case SEL: {
			boolean cond = getN(car(s.top)) != 0;
			int trueBranch = memory[c++];
			int falseBranch = memory[c++];
			d.push(c);
			c = cond ? trueBranch : falseBranch;
			s.top = cdr(s.top);
			return;
		}

		case JOIN: {
			c     = car(d.top);
			d.top = cdr(d.top);
			return;
		}

		case LDF: {
			int fn = memory[c++];
			s.push(gc.allocCons(fn, e.top));
			return;
		}

		case AP: {
			d.top = cons(cdr(cdr(s.top)), cons(e.top, cons(c, d.top)));
			e.top = cons(car(cdr(s.top)), cdr(car(s.top)));
			c     = getF(car(car(s.top)));
			s.top = 0;
			return;
		}

		case RTN: {
			s.top = cons(car(s.top), car(d.top));
			e.top = car(cdr(d.top));
			c     = car(cdr(cdr(d.top)));
			d.top = cdr(cdr(cdr(d.top)));
			return;
		}

		case DUM: {
			e.push(0);
			return;
		}

		case RAP: {
			int fe = car(s.top);
			int func = car(fe);
			int nenv = cdr(fe);
			int arg = car(cdr(s.top));
			d.push(c);
			c = func;
			d.push(cdr(e.top));
			e.top = rplaca(nenv, arg);
			d.push(car(cdr(cdr(s.top))));
			s.top = 0;
			return;
		}

		default:
			throw new RuntimeException("inst: "+inst);
		}
	}

	DataHeader getHeader(int adress) {
		return DataHeader.fromBin(memory[adress - 1]);
	}

	public int run() {
		while(c != -1) {
			step();
		}
		if(getHeader(car(s.top)) == INT) {
			return getN(car(s.top));
		} else {
			return memory[car(s.top)];
		}
	}

	String sToString(Map<Integer, String> labelName) {
		StringBuilder sb = new StringBuilder();
		printData(s.top, labelName, false, sb);
		return sb.toString();
	}

	String eToString(Map<Integer, String> labelName) {
		StringBuilder sb = new StringBuilder();
		printData(e.top, labelName, false, sb);
		return sb.toString();
	}

	private void printData(int i, Map<Integer, String> labelName, boolean inCdr, StringBuilder sb) {
		if(i == 0) {
			sb.append("()");
			return;
		}
		switch (getHeader(i)) {
		case INT:
			sb.append(Integer.toString(getN(i)));
			return;
		case CELL:
			if(!inCdr) {
				sb.append("(");
			}
			printData(car(i), labelName, false, sb);
			sb.append(" ");
			printData(cdr(i), labelName, true, sb);
			if(!inCdr) {
				sb.append(")");
			}
			return;
		case FNP:
			int address = getF(i);
			String label = labelName.get(address);
			if(label == null) {
				sb.append("*");
				sb.append(Integer.toString(address));
			} else {
				sb.append(label);
			}
			return;
		default:
			throw new RuntimeException();
		}
	}

	public String memoryDump() {
		return Arrays.stream(memory)
				.boxed()
				.map(i -> String.format(" %2d", i))
				.collect(Collectors.joining(","));
	}

	public String toString(Map<Integer, String> labelName) {
		return String.format("s: %50s, e: %40s, nextOp: %4s",
				sToString(labelName),
				eToString(labelName),
				Opcode.fromBin(memory[c]));
	}

	public static void main(String[] args) {
		/*
		 * let fun fact n =
		 *   if n == 0
		 *   then 1
		 *   else n * fact(n-1)
		 * in
		 *   fact(4)
		 */

		Program program = new Assembler(
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

		Secd m = new Secd(program.bin, program.entryPoint, 200);

		System.out.println(Arrays.toString(program.bin));
		while(m.c != -1) {
			System.out.println(m.toString(program.labelName));
			m.step();
		}
		System.out.println(Arrays.toString(m.memory));
	}
}