package benchmark;

import iguana.utils.input.GraphInput;
import org.iguana.parser.IguanaParser;
import org.iguana.parser.Pair;
import org.iguana.sppf.DefaultTerminalNode;
import org.iguana.sppf.NonterminalNode;
import org.iguana.sppf.SPPFNode;
import org.iguana.sppf.TerminalNode;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BenchmarkProblemAllPaths extends BenchmarkProblem {
    private Stream<String> getPath(SPPFNode node) {
        if (node instanceof TerminalNode) {
            return Stream.of("(" + node.getLeftExtent() +
                    ", " + ((TerminalNode) node).getGrammarSlot().getTerminal().getName() +
                    ", " + node.getRightExtent()
                    + ")");
        } else {
            Stream<String> output = Stream.empty();
            for (int i = 0; i < node.childrenCount(); i++) {
                output = Stream.concat(output, getPath(node.getChildAt(i)));
            }
            return output;
        }
    }
    public void printAllPaths(String path) throws FileNotFoundException {
        try (PrintWriter pathsWriter = new PrintWriter(new FileOutputStream(path), true)) {
            parseResults.forEach((pair, node) -> {
                pathsWriter.println(getPath(node.getChildAt(0)).collect(Collectors.joining( " " )));
            });
        }
    }

    private Map<Pair, NonterminalNode> parseResults = null;
    @Override
    public void runAlgo(IguanaParser parser, GraphInput input) {
        parseResults = parser.getSPPF(input);
    }

    @Override
    public long getResult() {
        try {
            printAllPaths("AllPaths.txt");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return parseResults.size();
    }

    @Override
    public String toString() {
        return "SPPF";
    }

}
