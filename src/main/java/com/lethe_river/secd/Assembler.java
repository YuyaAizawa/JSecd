package com.lethe_river.secd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Assembler {
	private final Map<String, Integer> labelMap = new HashMap<>();
	private final AsmElement[] asms;

	public Assembler(AsmElement... asms) {
		this.asms = asms;
	}

	public Program assembl(String entryPoint) {
		for (int i = 0; i < asms.length; i++) {
			AsmElement e = asms[i];
			String label = e.getLabel();
			if(label != null) {
				labelMap.put(label, i);
			}
		}

		int[] bin = new int[asms.length];
		for (int i = 0; i < asms.length; i++) {
			bin[i] = asms[i].bin(labelMap);
		}
		return new Program(
				bin,
				labelMap.get(entryPoint),
				labelMap.entrySet()
				.stream()
				.collect(Collectors.toMap(
						e -> e.getValue(),
						e -> e.getKey())));
	}

	public static AsmElement REF(String label) {
		return new AsmElement() {
			@Override
			public int bin(Map<String, Integer> map) {
				return map.get(label);
			}
		};
	}

	public static AsmElement VALUE(int value) {
		return new AsmElement() {
			@Override
			public int bin(Map<String, Integer> map) {
				return value;
			}
		};
	}

	public static Program assemble(InputStream is) throws IOException {

		StreamTokenizer tokenizer = new StreamTokenizer(new BufferedReader(new InputStreamReader(is)));
		tokenizer.resetSyntax();
		tokenizer.wordChars('0', '9');
		tokenizer.wordChars('a', 'z');
		tokenizer.wordChars('A', 'Z');
		tokenizer.wordChars('_', '_');
		tokenizer.whitespaceChars(' ', ' ');
		tokenizer.whitespaceChars('\t', '\t');
		tokenizer.whitespaceChars('\n', '\n');
		tokenizer.whitespaceChars('\r', '\r');
		tokenizer.eolIsSignificant(false);
		tokenizer.parseNumbers();
		tokenizer.lowerCaseMode(true);

		List<Integer> asms = new ArrayList<>();
		Map<String, Integer> labels = new HashMap<>();
		Map<Integer, String> refs = new HashMap<>();

		int kind;
		while((kind = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
			switch(kind) {
			case StreamTokenizer.TT_WORD:
				int b = 0;
				switch(tokenizer.sval) {
				case "label":
					tokenizer.nextToken();
					labels.put(tokenizer.sval, asms.size()-1);
					b = -2;
					break;
				case "ref"  :
					tokenizer.nextToken();
					refs.put(asms.size(), tokenizer.sval);
					b = -1;
					break;

				case "int":  b = 1; break;
				case "cell": b = 2; break;
				case "fnp":  b = 3; break;

				case "stop": b = 0; break;
				case "nil" : b = 1; break;
				case "ldc" : b = 2; break;
				case "ld"  : b = 3; break;
				case "car" : b = 4; break;
				case "cdr" : b = 5; break;
				case "atom": b = 6; break;
				case "cons": b = 7; break;
				case "eq"  : b = 8; break;
				case "leq" : b = 9; break;
				case "add" : b =10; break;
				case "sub" : b =11; break;
				case "mul" : b =12; break;
				case "div" : b =13; break;
				case "rem" : b =14; break;
				case "sel" : b =15; break;
				case "join": b =16; break;
				case "ldf" : b =17; break;
				case "ap"  : b =18; break;
				case "rtn" : b =19; break;
				case "dum" : b =20; break;
				case "rap" : b =21; break;
				}

				if (b != -2) {
					asms.add(b);
				}
				break;
			case StreamTokenizer.TT_NUMBER:
				asms.add((int)tokenizer.nval);
				break;
			default:
				throw new RuntimeException();
			}
		}
		refs.entrySet().forEach(e -> {
			asms.set(e.getKey(), labels.get(e.getValue()));
		});

		return new Program(
				asms.stream().mapToInt(i -> i).toArray(),
				labels.get("entry"),
				labels.entrySet()
				.stream()
				.collect(Collectors.toMap(
						e -> e.getValue(),
						e -> e.getKey())));
	}
}
