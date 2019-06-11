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
	int[] memory = new int[1000];

	Stack s = new Stack();
	Stack e = new Stack();
	int c;
	Stack d = new Stack();
	int f;

	final int programArea;

	Secd(int[] program, int entryPoint) {
		memory = Arrays.copyOf(program, 1000);
		c = entryPoint;
		f = program.length+1;
		programArea = program.length;
	}

	class Stack {
		int top;

		int push(int i) {
			return top = makeCons(i, top);
		}

		int pop() {
			int ret = car(top);
			top = cdr(top);
			return ret;
		}
	}

	int makeInt(int i) {
		memory[f++] = INT.bin;
		memory[f++] = i;
		return f - 1;
	}

	int makeCons(int i, int j) {
		memory[f++] = CELL.bin;
		memory[f++] = i;
		memory[f++] = j;
		return f - 2;
	}

	int getN(int i) {
		assert getHeader(i) == INT;
		return memory[i];
	}

	int getF(int i) {
		assert getHeader(i) == FNP;
		return memory[i];
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
		int a = s.pop();
		int b = s.pop();
		return s.push(op.applyAsInt(a, b));
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
			s.push(car(s.pop()));
			return;
		}

		case CDR: {
			s.push(cdr(s.pop()));
			return;
		}

		case ATOM: {
			int result = memory[s.pop()];
			s.push(result == INT.bin ? 1 : 0);
			return;
		}

		case CONS: {
			binOp((a, b) -> makeCons(a, b));
			return;
		}

		case EQ: {
			binOp((a, b) -> makeInt(getN(a) == getN(b) ? 1 : 0));
			return;
		}

		case LEQ: {
			binOp((a, b) -> makeInt(getN(a) <= getN(b) ? 1 : 0));
			return;
		}

		case ADD: {
			binOp((a, b) -> makeInt(getN(a) + getN(b)));
			return;
		}

		case SUB: {
			binOp((a, b) -> makeInt(getN(a) - getN(b)));
			return;
		}

		case MUL: {
			binOp((a, b) -> makeInt(getN(a) * getN(b)));
			return;
		}

		case DIV: {
			binOp((a, b) -> makeInt(getN(a) / getN(b)));
			return;
		}

		case REM: {
			binOp((a, b) -> makeInt(getN(a) % getN(b)));
			return;
		}

		case SEL: {
			boolean cond = getN(s.pop()) != 0;
			int trueBranch = memory[c++];
			int falseBranch = memory[c++];
			d.push(c);
			c = cond ? trueBranch : falseBranch;
			return;
		}

		case JOIN: {
			c = d.pop();
			return;
		}

		case LDF: {
			int fn = memory[c++];
			s.push(makeCons(fn, e.top));
			return;
		}

		case AP: {
			int fe  = s.pop();
			int env = cdr(fe);
			int fn = getF(car(fe));
			int arg = s.pop();
			d.push(c);
			c = fn;
			d.push(e.top);
			e.top = makeCons(arg, env);
			d.push(s.top);
			s.top = 0;
			return;
		}

		case RTN: {
			int ret = s.pop();
			s.top = makeCons(ret, d.pop());
			e.top = d.pop();
			c = d.pop();
			return;
		}

		case DUM: {
			e.push(0);
			return;
		}

		case RAP: {
			int fe = s.pop();
			int func = car(fe);
			int nenv = cdr(fe);
			int arg = s.pop();
			d.push(c);
			c = func;
			d.push(cdr(e.top));
			e.top = rplaca(nenv, arg);
			d.push(s.top);
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

	void run() {
		while(c != -1) {
			step();
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
			sb.append(Integer.toString(memory[i]));
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
			int address = memory[i];
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

		Secd m = new Secd(program.bin, program.entryPoint);

		System.out.println(Arrays.toString(program.bin));
		while(m.c != -1) {
			System.out.println(m.toString(program.labelName));
			m.step();
		}
		System.out.println(Arrays.toString(m.memory));
	}
}