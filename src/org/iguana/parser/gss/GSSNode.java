/*
 * Copyright (c) 2015, Ali Afroozeh and Anastasia Izmaylova, Centrum Wiskunde & Informatica (CWI)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this 
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this 
 *    list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY 
 * OF SUCH DAMAGE.
 *
 */

package org.iguana.parser.gss;

import iguana.utils.collections.hash.MurmurHash3;
import iguana.utils.input.Input;
import org.iguana.datadependent.env.Environment;
import org.iguana.grammar.slot.BodyGrammarSlot;
import org.iguana.grammar.slot.EndGrammarSlot;
import org.iguana.grammar.slot.NonterminalGrammarSlot;
import org.iguana.parser.descriptor.ResultOps;
import org.iguana.util.ParserLogger;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Ali Afroozeh
 * @author Anastasia Izmaylova
 * 
 */
public class GSSNode<T> {
	
	protected final NonterminalGrammarSlot<T> slot;

	private final int inputIndex;
	
	private final PoppedElements<T> poppedElements;

	private GSSEdge<T> firstGSSEdge;
	
	private List<GSSEdge<T>> gssEdges;

	public GSSNode(NonterminalGrammarSlot<T> slot, int inputIndex, ResultOps<T> ops) {
		this.slot = slot;
		this.inputIndex = inputIndex;
		this.poppedElements = new PoppedElements<>(ops);
	}
	
	public void createGSSEdge(Input input, BodyGrammarSlot<T> returnSlot, GSSNode<T> destination, T w, ResultOps<T> ops) {
		GSSEdge<T> edge = new NewGSSEdgeImpl<>(returnSlot, w, destination);
		ParserLogger.getInstance().gssEdgeAdded(edge);

		if (firstGSSEdge == null) {
			firstGSSEdge = edge;
		} else {
			if (gssEdges == null) gssEdges = new ArrayList<>(4);
			gssEdges.add(edge);
		}

		for (T z : poppedElements) {
            if (edge.getReturnSlot().testFollow(input.charAt(ops.getRightIndex(z)))) {
				T result = edge.addDescriptor(input, this, z, ops);
                if (result != null) {
                    slot.getRuntime().scheduleDescriptor(returnSlot, destination, result);
                }
            }
        }
	}

    public void pop(Input input, EndGrammarSlot slot, T child, ResultOps<T> ops) {
		ParserLogger.getInstance().log("Pop %s, %d, %s", this, inputIndex, child);
        T node = poppedElements.add(slot, child);
        if (node != null) iterateOverEdges(input, node, ops);
    }

    public void pop(Input input, EndGrammarSlot slot, T child, Object value, ResultOps<T> ops) {
        T node = poppedElements.add(slot, child, value);
        if (node != null) iterateOverEdges(input, node, ops);
    }

    private void iterateOverEdges(Input input, T node, ResultOps<T> ops) {
		if (firstGSSEdge != null)
			processEdge(input, node, ops, firstGSSEdge);

		if (gssEdges != null)
			for (int i = 0; i < gssEdges.size(); i++) {
				GSSEdge<T> edge = gssEdges.get(i);
				processEdge(input, node, ops, edge);
			}
    }

	private void processEdge(Input input, T node, ResultOps<T> ops, GSSEdge<T> edge) {
		if (!edge.getReturnSlot().testFollow(input.charAt(ops.getRightIndex(node)))) return;

		T result = edge.addDescriptor(input, this, node, ops);
		if (result != null) {
            slot.getRuntime().scheduleDescriptor(edge.getReturnSlot(), edge.getDestination(), result);
        }
	}

	public T getResult(int j) {
		return poppedElements.getResult(j);
	}

	public NonterminalGrammarSlot getGrammarSlot() {
		return slot;
	}

	public int getInputIndex() {
		return inputIndex;
	}
	
	public int countGSSEdges() {
	    int count = 0;
	    count += firstGSSEdge == null ? 0 : 1;
	    count += gssEdges == null ? 0 : gssEdges.size();
		return count;
	}
	
	public int countPoppedElements() {
		return poppedElements.size();
	}
		
	public Iterable<GSSEdge<T>> getGSSEdges() {
		return gssEdges;
	}
	
	public boolean equals(Object obj) {
		
		if(this == obj)
			return true;

		if (!(obj instanceof GSSNode))
			return false;
		
		GSSNode other = (GSSNode) obj;

		return  slot == other.getGrammarSlot() &&
				inputIndex == other.getInputIndex();
	}

	public int hashCode() {
		return MurmurHash3.f2().apply(slot.hashCode(), inputIndex);
	}
	
	public String toString() {
		return String.format("(%s, %d)", slot, inputIndex);
	}

	public int getCountGSSEdges() {
		return gssEdges.size();
	}
	
	/*
	 * 
	 * Data-dependent GLL parsing
	 * 
	 */
	
	public void createGSSEdge(Input input, BodyGrammarSlot<T> returnSlot, GSSNode<T> destination, T w, Environment env, ResultOps<T> ops) {
		GSSEdge<T> edge = new org.iguana.datadependent.gss.NewGSSEdgeImpl<>(returnSlot, w, destination, env);
		
		gssEdges.add(edge);
		ParserLogger.getInstance().gssEdgeAdded(edge);

		for (T z : poppedElements) {
			if (edge.getReturnSlot().testFollow(input.charAt(ops.getRightIndex(z)))) {
				T result = edge.addDescriptor(input, this, z, ops);
				if (result != null) {
                    returnSlot.getRuntime().scheduleDescriptor(returnSlot, destination, result);
				}
			}
		}
	}
	
}
