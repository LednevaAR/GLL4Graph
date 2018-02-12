package org.iguana.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.iguana.grammar.GrammarGraph;
import org.iguana.grammar.slot.NonterminalGrammarSlot;
import org.iguana.grammar.slot.TerminalGrammarSlot;
import org.iguana.sppf.*;
import org.iguana.traversal.SPPFVisitor;

import java.io.IOException;
import java.util.*;

public class SPPFJsonSerializer {

    public static String serialize(NonterminalNode node) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(NonterminalNode.class, new NonterminalNodeSerializer(NonterminalNode.class));
        mapper.registerModule(module);
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
        pp.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        try {
            return mapper.writer(pp).writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static NonterminalNode deserialize(String json, GrammarGraph graph) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(NonterminalNode.class, new NonterminalDeserializer(graph, NonterminalNode.class));
        mapper.registerModule(module);
        try {
            return mapper.readValue(json, NonterminalNode.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class NonterminalNodeSerializer extends StdSerializer<NonterminalNode> {

        NonterminalNodeSerializer(Class<NonterminalNode> t) {
            super(t);
        }

        @Override
        public void serialize(NonterminalNode node, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
            gen.writeStartArray();
            ToJsonSPPFVisitor visitor = new ToJsonSPPFVisitor(gen);
            node.accept(visitor);
            gen.writeEndArray();
        }
    }

    private static class NonterminalDeserializer extends StdDeserializer<NonterminalNode> {

        private final GrammarGraph grammarGraph;

        protected NonterminalDeserializer(GrammarGraph grammarGraph, Class<?> vc) {
            super(vc);
            this.grammarGraph = grammarGraph;
        }

        @Override
        public NonterminalNode deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
            ObjectCodec codec = parser.getCodec();
            JsonNode node = codec.readTree(parser);

            Map<Integer, SPPFNode<?,?>> idToNodeMap = new HashMap<>();
            Map<Integer, List<Integer>> children = new HashMap<>();

            for (JsonNode child : node) {
                String kind = child.get("kind").asText();
                int id = child.get("id").asInt();
                int leftExtent = child.get("leftExtent").asInt();
                int rightExtent = child.get("leftExtent").asInt();
                SPPFNode<?,?> sppfNode;
                switch (kind) {
                    case "TerminalNode":
                        sppfNode = new TerminalNode(getGrammarSlot(child), leftExtent, rightExtent);
                        idToNodeMap.put(id, sppfNode);
                        break;

                    case "NonterminalNode":
                        sppfNode = new NonterminalNode(getGrammarSlot(child));
                        idToNodeMap.put(id, sppfNode);
                        addChildren(child, children);
                        break;

                    case "PackedNode":
                        sppfNode = new PackedNode(getGrammarSlot(child));
                        idToNodeMap.put(id, sppfNode);
                        addChildren(child, children);
                        break;
                }
            }

            for (Map.Entry<Integer, SPPFNode<?,?>> entry : idToNodeMap.entrySet()) {
                int id = entry.getKey();
                SPPFNode<?,?> sppfNode = entry.getValue();
                if (sppfNode instanceof NonterminalOrIntermediateNode) {
                    for (int childId : children.get(id)) {
                        ((NonterminalOrIntermediateNode) sppfNode).addPackedNode((PackedNode) idToNodeMap.get(childId));
                    }
                } else if (sppfNode instanceof PackedNode) {
                    NonPackedNode leftChild = (NonPackedNode) idToNodeMap.get(children.get(id).get(0));
                    NonPackedNode rightChild = null;
                    if (children.get(id).size() > 1) {
                        rightChild = (NonPackedNode) idToNodeMap.get(children.get(id).get(0));
                    }
                    ((PackedNode) sppfNode).setLeftChild(leftChild);
                    ((PackedNode) sppfNode).setRightChild(rightChild);
                }
            }

            return (NonterminalNode) idToNodeMap.get(0);
        }

        @SuppressWarnings("unchecked")
        private <T> T getGrammarSlot(JsonNode node) {
            return (T) grammarGraph.getSlot(node.get("label").asText());
        }

        private void addChildren(JsonNode node, Map<Integer, List<Integer>> children) {
            int id = node.get("id").asInt();
            for (JsonNode child : node.get("children")) {
                children.computeIfAbsent(id, key -> new ArrayList<>()).add(child.asInt());
            }
        }
    }

    private static class ToJsonSPPFVisitor implements SPPFVisitor<Void> {

        private JsonGenerator gen;
        private Set<SPPFNode<?,?>> visitedNodes;
        private Map<SPPFNode<?,?>, Integer> ids;

        ToJsonSPPFVisitor(JsonGenerator gen) {
            this.gen = gen;
            visitedNodes = new HashSet<>();
            ids = new HashMap<>();
        }

        private int getId(SPPFNode<?,?> node) {
            return ids.computeIfAbsent(node, key -> ids.size());
        }

        @Override
        public Void visit(TerminalNode node) {
            try {
                generateNode(node, "TerminalNode");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public Void visit(NonterminalNode node) {
            try {
                generateNode(node, "NonterminalNode");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public Void visit(IntermediateNode node) {
            try {
                generateNode(node, "IntermediateNode");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public Void visit(PackedNode node) {
            try {
                generateNode(node, "PackedNode");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private void generateNode(SPPFNode<?,?> node, String kind) throws IOException {
            if (visitedNodes.contains(node)) return;

            visitedNodes.add(node);

            gen.writeStartObject();
            gen.writeNumberField("id", getId(node));
            gen.writeStringField("kind", kind);
            gen.writeStringField("label", node.getGrammarSlot().toString());
            gen.writeNumberField("leftExtent", node.getLeftExtent());
            gen.writeNumberField("rightExtent", node.getRightExtent());

            if (node.getChildren().size() > 0) {
                gen.writeArrayFieldStart("children");
                for (SPPFNode<?,?> child : node.getChildren())
                    gen.writeNumber(getId(child));
                gen.writeEndArray();
            }

            gen.writeEndObject();

            for (SPPFNode<?,?> child : node.getChildren())
                child.accept(this);
        }
    }
}
