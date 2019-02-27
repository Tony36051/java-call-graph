import java.util.*;
import java.util.stream.Collectors;

/**
 * 调用图分析
 */
public class CallGraph {
    public static void main(String[] args) {
        MethodCallExtractor extractor = new MethodCallExtractor();
        Map<String, List<String>> methodCallRelation = extractor.getMethodCallRelationByDefault();

//        CallGraph callGraph = new CallGraph();
        int methodNum = methodCallRelation.keySet().size();
        String[] mes = new String[methodNum];
        Map<String, Integer> me2id = new HashMap<>(methodNum);
        methodCallRelation.keySet().toArray(mes);
//        System.out.println("total method declarations: " + methodNum);
        for (int i = 0; i < methodNum; i++) {
            System.out.println(mes[i]);
            me2id.put(mes[i], i);
        }

        Digraph digraph = new Digraph(methodNum);
        System.out.println("callee: ");
        for (Map.Entry<String, List<String>> entry : methodCallRelation.entrySet()) {
            String caller = entry.getKey();
            Integer from = me2id.get(caller);
            for (String callee : entry.getValue()) {
                Integer to = me2id.get(callee);
                digraph.addEdge(from, to);
                System.out.println(callee);
            }
        }
        Digraph reversedGraph = digraph.reverse();
        while(true){
            Scanner scanner = new Scanner(System.in);
            System.out.print( "Please enter a string : " );
            String inputStr = scanner.next();
            if (inputStr.equalsIgnoreCase("q")) {
                break;
            }
            System.out.print("Your input is :" + inputStr);
            Integer calleeId = me2id.get(inputStr);
            DirectedDFS directedDFS = new DirectedDFS(reversedGraph, calleeId);
            for (int i = 0; i < reversedGraph.V(); i++) {
                if (directedDFS.marked(i)) {
                    System.out.println(i + "-->" + mes[i]);
                }
            }
        }

    }




}
