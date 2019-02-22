public class GraphMain {
    public static void main(String[] args) {
        Digraph G = new Digraph(7);
        G.addEdge(1, 2);
        G.addEdge(1, 4);
        G.addEdge(2, 5);
        G.addEdge(3, 5);
        G.addEdge(3, 6);
        G.addEdge(4, 2);
        G.addEdge(5, 4);
        G.addEdge(6, 6);
        DirectedDFS reachable = new DirectedDFS(G, 3);
        for (int i = 0; i < G.V(); i++) {
            if (reachable.marked(i)) {
                System.out.println(i);
            }
        }
    }
}
