import java.util.HashSet;
import java.util.Set;

public class Digraph {
    private final int V;
    private int E;
    private Set<Integer>[] adj;

    public Digraph(int v) {
        V = v;
        this.E = 0;
        adj = new Set[V];
        for (int i = 0; i < V; i++) {
            adj[i] = new HashSet<>();
        }
    }

    public int V() {
        return V;
    }

    public int E() {
        return E;
    }

    public void addEdge(int v, int w) {
        adj[v].add(w);
        E++;
    }

    public Iterable<Integer> adj(int v) {
        return adj[v];
    }
    public Digraph reverse(){
        Digraph R = new Digraph(V);
        for (int i = 0; i <V; i++) {
            for (Integer w : adj(i)) {
                R.addEdge(w, i);
            }
        }
        return R;
    }

}
