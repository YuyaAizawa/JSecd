package com.lethe_river.secd;

import java.util.Arrays;
import java.util.function.IntBinaryOperator;
import java.util.stream.Collectors;

import static com.lethe_river.secd.Opcode.*;

public class Secd {

	// タグ
	static final int INT  = 1;
	static final int CELL = 2;
	static final int FUNC = 3;

	// 仮想機械の全てのメモリ
	int[] memory = new int[1000];

	Stack s = new Stack();
	Stack e = new Stack();
	Stack c = new Stack();
	Stack d = new Stack();
	int f = 1;

	class Stack {
		int top;

		int push(int i) {
			return top = makeCons(i, top);
		}

		int pop() {
			if(memory[top] != CELL) {
				throw new RuntimeException();
			}
			int ret = memory[top+1];
			top = memory[top+2];
			return ret;
		}
	}

	int makeInt(int i) {
		memory[f++] = INT;
		memory[f++] = i;
		return f - 2;
	}

	int makeCons(int i, int j) {
		memory[f++] = CELL;
		memory[f++] = i;
		memory[f++] = j;
		return f - 3;
	}

	int getN(int i) {
		if(memory[i] != INT) {
			throw new RuntimeException(""+i);
		}
		return memory[i+1];
	}

	int getF(int i) {
		if(memory[i] != FUNC) {
			throw new RuntimeException(""+i);
		}
		return memory[i+1];
	}

	int car(int i) {
		if(memory[i] != CELL) {
			throw new RuntimeException();
		}
		return memory[i+1];
	}

	int cdr(int i) {
		if(memory[i] != CELL) {
			throw new RuntimeException();
		}
		return memory[i+2];
	}

	int locate(int i, int j) {
		System.out.println(i+", "+j+", "+e.top);
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
		if(memory[x] != CELL) {
			throw new RuntimeException();
		}
		memory[x] = CELL;
		memory[x+1] = y;
		memory[x+2] = cdr(x);
		return x;
	}

	void step() {
		Opcode inst = Opcode.of(c.pop());
		switch (inst) {
		case STOP: {
			c.top = 0;
			return;
		}

		case NIL: {
			s.push(0);
			return;
		}

		case LDC: {
			s.push(c.pop());
			return;
		}

		case LD: {
//			int work = e.top;
//			int envindex = getN(car(car(cdr(c.top))));
//			for (int i = 0; i < envindex; i++) {
//				work = cdr(work);
//			}
//			work = car(work);
//			envindex = getN(cdr(car(cdr(c.top))));
//			for (int i = 0; i < envindex; i++) {
//				work = cdr(work);
//			}
//			work = car(work);
//			s.push(work);
//			c.pop();
//			c.pop();
//			c.pop();
			int ij = c.pop();
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
			s.push(result == INT ? 1 : 0);
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
			int trueBranch = c.pop();
			int falseBranch = c.pop();
			d.push(c.top);
			c.top = cond ? trueBranch : falseBranch;
			return;
		}

		case JOIN: {
			c.top =d.pop();
			return;
		}

		case LDF: {
			int fn = c.pop();
			s.push(makeCons(fn, e.top));
			return;
		}

		case AP: {
			int fe  = s.pop();
			int env = cdr(fe);
			int fn = getF(car(fe));
			int arg = s.pop();
			d.push(c.top);
			c.top = fn;
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
			c.top = d.pop();
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
			d.push(c.top);
			c.top = func;
			d.push(cdr(e.top));
			e.top = rplaca(nenv, arg);
			d.push(s.top);
			s.top = 0;
			return;
		}

		case DUP: {
			int val = s.pop();
			s.push(val);
			s.push(val);
			return;
		}

		default:
			throw new RuntimeException("inst: "+inst);
		}
	}

	void run() {
		while(c.top != 0) {
			step();
		}
	}

	String sToString() {
		StringBuilder sb = new StringBuilder();
		printData(s.top, sb);
		return sb.toString();
	}

	String eToString() {
		StringBuilder sb = new StringBuilder();
		printData(e.top, sb);
		return sb.toString();
	}

	String cToString() {
		StringBuilder sb = new StringBuilder();
		printProgram(c.top, sb);
		return sb.toString();
	}

	String dToString() {
		StringBuilder sb = new StringBuilder();
		printProgram(d.top, sb);
		return sb.toString();
	}

	void printData(int i, StringBuilder sb) {
		int kind = memory[i];
		switch (kind) {
		case 0:
			sb.append("()");
			return;
		case INT:
			sb.append(Integer.toString(memory[i+1]));
			return;
		case CELL:
			sb.append("(");
			printData(car(i), sb);
			sb.append(" ");
			printData(cdr(i), sb);
			sb.append(")");
			return;
		case FUNC:
			sb.append("*");
			sb.append(Integer.toString(memory[i+1]));
			return;
		default:
			throw new RuntimeException();
		}
	}

	void printProgram(int i, StringBuilder sb) {
		int next;
		try {
			if(memory[i] == 0) {
				sb.append("()");
				return;
			}
			if(memory[i] != CELL) {
				throw new RuntimeException(""+i);
			}
			Opcode opcode = Opcode.of(car(i));

			sb.append("(");
			sb.append(opcode);
			sb.append(" ");

			next = cdr(i);
			for(int j = 0;j < opcode.operands;j++) {
				int car = car(next);
				int cdr = cdr(next);
				if(opcode == LDC) {
					sb.append(getN(car));
				} else if(opcode == LDF){
					sb.append(getF(car));
				} else if(opcode == LD){
					printData(car, sb);
				}
				sb.append(" ");
				next = cdr;
			}
		} catch (RuntimeException e) {
			throw new RuntimeException(""+i, e);
		}
		printProgram(next, sb);
		sb.append(")");
	}

	public String memoryDump() {
		return Arrays.stream(memory)
				.boxed()
				.map(i -> String.format(" %2d", i))
				.collect(Collectors.joining(","));
	}

	@Override
	public String toString() {
		return String.format("s: %10s, e: %10s, c: %30s, d: %10s",
				sToString(),
				eToString(),
				cToString(),
				"");
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

		Secd m = new Secd();
		m.memory[ 1] = INT;
		m.memory[ 2] = 1;
		m.memory[ 3] = INT;
		m.memory[ 4] = 4;
		m.memory[ 5] = INT;
		m.memory[ 6] = -1;
		m.memory[ 7] = FUNC;
		m.memory[ 8] = 0;
		m.memory[ 9] = INT;
		m.memory[10] = 0;
		m.memory[11] = CELL;
		m.memory[12] = 9;
		m.memory[13] = 9;

		m.f = 14;

		int ONE   =  1;
		int FOUR  =  3;
		int M_ONE =  5;
		int ZERO  =  9;
		int ZZ    = 11;

		int FRAC  =  7;

		m.c.push(JOIN.bin);
		m.c.push(MUL.bin);
		m.c.push(ZZ);
		m.c.push(LD.bin);
		m.c.push(AP.bin);
		m.c.push(FRAC);
		m.c.push(LDF.bin);
		m.c.push(CONS.bin);
		m.c.push(ADD.bin);
		m.c.push(M_ONE);
		m.c.push(LDC.bin);
		int fb = m.c.top;

		m.c.push(JOIN.bin);
		m.c.push(ONE);
		m.c.push(LDC.bin);
		int tb = m.c.top;

		m.c.push(RTN.bin);
		m.c.push(fb);
		m.c.push(tb);
		m.c.push(SEL.bin);
		m.c.push(EQ.bin);
		m.c.push(ZERO);
		m.c.push(LDC.bin);
		m.c.push(DUP.bin);
		m.c.push(ZZ);
		m.c.push(LD.bin);
		m.c.push(NIL.bin);
		m.memory[FRAC+1] = m.c.top;

		m.c.push(STOP.bin);
		m.c.push(AP.bin);
		m.c.push(FRAC);
		m.c.push(LDF.bin);
		m.c.push(CONS.bin);
		m.c.push(FOUR);
		m.c.push(LDC.bin);
		m.c.push(NIL.bin);

		System.out.println(m.memoryDump());
		System.out.println(m.c.top);
		System.out.println(m);
		while(m.c.top != 0) {
			m.step();
//			System.out.println(m.memoryDump());
//			System.out.println(m.c.top);
//			System.out.println(m.s.top);
			System.out.println(m);
		}
	}
}