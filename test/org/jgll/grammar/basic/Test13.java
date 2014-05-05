package org.jgll.grammar.basic;

import static org.jgll.util.CollectionsUtil.*;
import static org.junit.Assert.*;

import org.jgll.grammar.Grammar;
import org.jgll.grammar.GrammarBuilder;
import org.jgll.grammar.slot.factory.GrammarSlotFactory;
import org.jgll.grammar.slot.factory.GrammarSlotFactoryImpl;
import org.jgll.grammar.symbol.Character;
import org.jgll.grammar.symbol.Nonterminal;
import org.jgll.grammar.symbol.Rule;
import org.jgll.parser.GLLParser;
import org.jgll.parser.ParseError;
import org.jgll.parser.ParserFactory;
import org.jgll.sppf.NonterminalSymbolNode;
import org.jgll.sppf.SPPFNode;
import org.jgll.sppf.TokenSymbolNode;
import org.jgll.util.Input;
import org.jgll.util.Visualization;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * S ::= A B C D
 * A ::= 'a' | epsilon
 * B ::= 'a' | epsilon
 * C ::= 'a' | epsilon
 * D ::= 'a' | epsilon
 * 
 * @author Ali Afroozeh
 */
public class Test13 {

	private Grammar grammar;

	private Nonterminal S = new Nonterminal("S");
	private Nonterminal A = new Nonterminal("A");
	private Nonterminal B = new Nonterminal("B");
	private Nonterminal C = new Nonterminal("C");
	private Nonterminal D = new Nonterminal("D");
	private Character a = new Character('a');
	
	@Before
	public void init() {
		Rule r1 = new Rule(S, list(A, B, C, D));
		Rule r2 = new Rule(A, list(a));
		Rule r3 = new Rule(A);
		Rule r4 = new Rule(B, list(a));
		Rule r5 = new Rule(B);
		Rule r6 = new Rule(C, list(a));
		Rule r7 = new Rule(C);
		Rule r8 = new Rule(D, list(a));
		Rule r9 = new Rule(D);

		GrammarSlotFactory factory = new GrammarSlotFactoryImpl();
		grammar = new GrammarBuilder("Test3", factory).addRule(r1).addRule(r2).addRule(r3).
													   addRule(r4).addRule(r5).addRule(r6).
													   addRule(r7).addRule(r8).addRule(r9).build();
	}
	
	@Test
	public void testNullable() {
		assertTrue(grammar.getHeadGrammarSlot("S").isNullable());
		assertTrue(grammar.getHeadGrammarSlot("A").isNullable());
		assertTrue(grammar.getHeadGrammarSlot("B").isNullable());
		assertTrue(grammar.getHeadGrammarSlot("C").isNullable());
		assertTrue(grammar.getHeadGrammarSlot("D").isNullable());
	}
	
	@Test
	public void testParser() throws ParseError {
		Input input = Input.fromString("a");
		GLLParser parser = ParserFactory.newParser(grammar, input);
		NonterminalSymbolNode sppf = parser.parse(input, grammar, "S");
		Visualization.generateSPPFGraphWithoutIntermeiateNodes("/Users/ali/output", sppf, grammar, input);
//		assertEquals(true, sppf.deepEquals(expectedSPPF()));
	}
	
	private SPPFNode expectedSPPF() {
		NonterminalSymbolNode node1 = new NonterminalSymbolNode(grammar.getNonterminalId(A), 1, 0, 2);
		NonterminalSymbolNode node2 = new NonterminalSymbolNode(grammar.getNonterminalId(B), 1, 0, 1);
		TokenSymbolNode node3 = new TokenSymbolNode(grammar.getRegularExpressionId(a), 0, 1);
		node2.addChild(node3);
		TokenSymbolNode node4 = new TokenSymbolNode(grammar.getRegularExpressionId(a), 1, 1);
		node1.addChild(node2);
		node1.addChild(node4);
		return node1;
	}
}
