package org.jgll.grammar.symbol;

public class PrecedenceGroup extends Group {
	
	public PrecedenceGroup(int lhs, int rhs) {
		super(lhs, rhs);
	}

	public PrecedenceGroup(int lhs) {
		super(lhs, -1);
	}
}
