package com.huawei.it.unistar.jp;


import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 有向图的深度优先遍历，用于代码影响分析，逆向图后可以往上分析
 */
public class DirectedDFS {
    private boolean[] marked;
    public DirectedDFS(Digraph G, int s){
        marked = new boolean[G.V()];
        dfs(G, s);
    }
    private void dfs(Digraph G, int v){
        marked[v] = true;
        for (int w : G.adj(v)) {
            if (!marked[w]) {
                dfs(G, w);
            }
        }
    }
    public boolean marked(int v){
        return marked[v];
    }

    public List<Integer> getMarkedV(){
        List<Integer> markedIndexList = new ArrayList<>();
        for (int i = 0; i < marked.length; i++) {
            if (marked[i]) {
                markedIndexList.add(i);
            }
        }
        return markedIndexList;
    }

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
        List<Integer> markedV = reachable.getMarkedV();
        LoggerFactory.getLogger(reachable.getClass()).info(markedV.toString());
    }
}
