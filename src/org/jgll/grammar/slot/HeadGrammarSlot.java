package org.jgll.grammar.slot;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Set;

import org.jgll.grammar.slot.test.FollowTest;
import org.jgll.grammar.slot.test.PredictionTest;
import org.jgll.grammar.symbol.Nonterminal;
import org.jgll.grammar.symbol.Symbol;
import org.jgll.lexer.GLLLexer;
import org.jgll.parser.GLLParser;
import org.jgll.sppf.ListSymbolNode;
import org.jgll.sppf.NonterminalSymbolNode;

/**
 * 
 * The grammar slot corresponding to the head of a rule.
 *
 * 
 * @author Ali Afroozeh
 * 
 */
public class HeadGrammarSlot implements GrammarSlot {
	
	private static final long serialVersionUID = 1L;

	protected final Nonterminal nonterminal;
	
	private boolean nullable;
	
	protected BodyGrammarSlot firstSlots[];

	private final int nonterminalId;
	
	private final int id;
	
	private final PredictionTest predictionTest;
	
	private final FollowTest followTest;
	
	public HeadGrammarSlot(int id, Nonterminal nonterminal, 
						   int nonterminalId, List<List<Symbol>> alts, boolean nullable, 
						   PredictionTest predictionTest, FollowTest followTest) {
		this.id = id;
		this.nonterminal = nonterminal;
		this.nonterminalId = nonterminalId;
		this.firstSlots = new BodyGrammarSlot[alts.size()];
		this.nullable = nullable;
		this.followTest = followTest;
		this.predictionTest = predictionTest;
	}
		
	public boolean isNullable() {
		return nullable;
	}
		
	public boolean test(int v) {
		return predictionTest.test(v);
	}
	
	public boolean testFollowSet(int v) {
		return followTest.test(v);
	}
	
	public void setFirstGrammarSlotForAlternate(BodyGrammarSlot slot, int index) {
		firstSlots[index] = slot;
	}
	
	public BodyGrammarSlot[] getFirstSlots() {
		return firstSlots;
	}
	
	@Override
	public GrammarSlot parse(GLLParser parser, GLLLexer lexer) {
		int ci = parser.getCurrentInputIndex();
		
		Set<Integer> set = predictionTest.get(lexer.getInput().charAt(ci));
		
		if(set == null) return null;
		
		for(int alternateIndex : set) {
			parser.addDescriptor(firstSlots[alternateIndex]);
		}
		
		return null;
	}
	
	@Override
	public void codeParser(Writer writer) throws IOException {
		writer.append("// " + nonterminal.getName() + "\n");
		writer.append("private void parse_" + id + "() {\n");
		for(BodyGrammarSlot slot : firstSlots) {
			writer.append("   //" + slot + "\n");
			slot.codeIfTestSetCheck(writer);			
			writer.append("   add(grammar.getGrammarSlot(" + slot.getId() + "), cu, ci, DummyNode.getInstance());\n");
			writer.append("}\n");
		}
		writer.append("   label = L0;\n");
		writer.append("}\n");

		for(BodyGrammarSlot slot : firstSlots) {
			writer.append("// " + slot + "\n");
			writer.append("private void parse_" + slot.getId() + "() {\n");
			slot.codeParser(writer);
		}
	}
	
	public Nonterminal getNonterminal() {
		return nonterminal;
	}
		
	@Override
	public String toString() {
		return nonterminal.toString();
	}
	
	public NonterminalSymbolNode createSPPFNode(int nonterminalId, int numberOfAlternatives, int leftExtent, int rightExtent) {
		if(nonterminal.isEbnfList()) {
			return new ListSymbolNode(nonterminalId, numberOfAlternatives, leftExtent, rightExtent);
		} else {
			return new NonterminalSymbolNode(nonterminalId, numberOfAlternatives, leftExtent, rightExtent);
		}
	}

	@Override
	public int getNodeId() {
		return nonterminalId;
	}
	
	@Override
	public int getId() {
		return id;
	}
	
}
