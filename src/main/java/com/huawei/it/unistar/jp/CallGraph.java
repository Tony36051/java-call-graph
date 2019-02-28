package com.huawei.it.unistar.jp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 调用图分析
 */
public class CallGraph {
    private static Logger logger = LoggerFactory.getLogger(CallGraph.class);

    public static void main(String[] args) {
        CallGraph callGraph = new CallGraph();
        callGraph.report();
    }

    public void report() {
        MethodCallExtractor extractor = new MethodCallExtractor();
        Map<String, List<String>> methodCallRelation = extractor.getMethodCallRelationByDefault();

        //给所有方法编序号，id->method, method->id
        int methodNum = methodCallRelation.keySet().size();
        String[] methods = new String[methodNum];
        Map<String, Integer> method2id = new HashMap<>(methodNum);
        methodCallRelation.keySet().toArray(methods);
        logger.info("Total method declarations: {}", methodNum);
        for (int i = 0; i < methodNum; i++) {
            method2id.put(methods[i], i);
        }
        // 构造有向图，图只存方法的序号id，节点是方法，边就是依赖关系
        Digraph digraph = new Digraph(methodNum);
        for (Map.Entry<String, List<String>> entry : methodCallRelation.entrySet()) {
            String caller = entry.getKey();
            Integer from = method2id.get(caller);
            for (String callee : entry.getValue()) {
                Integer to = method2id.get(callee);
                digraph.addEdge(from, to);
            }
        }
        final List<Pattern> patternsToSkip = Utils.getLinesFrom(Utils.SKIP_CFG).stream().map(t -> Pattern.compile(t)).collect(Collectors.toList());
        // 获取反向有向图，准备求某方法被谁依赖
        Digraph reversedGraph = digraph.reverse();
        for (int i = 0; i < methodNum; i++) {
            if (shouldSkip(methods[i], patternsToSkip)) {
                logger.warn("skip output: " + methods[i]);
                continue;
            }
            ;
            writeOneMethod(i, reversedGraph, methods);
        }
    }

    private boolean shouldSkip(String qualifiedSignature, List<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(qualifiedSignature).matches()) {
                return true;
            }
        }
        return false;
    }

    private void writeOneMethod(int methodId, Digraph reversedGraph, String[] methods) {
        DirectedDFS directedDFS = new DirectedDFS(reversedGraph, methodId);
        final List<Integer> mayBeAffectedMothodId = directedDFS.getMarkedV();
        if (mayBeAffectedMothodId.size() <= 1) return;//有向图只有1个点时，改点是方法定义本身，所以只有自身递归也不会输出
        String callee = methods[methodId];
        final List<String> mayBeAffectedMethods = mayBeAffectedMothodId.stream().map(t -> methods[t]).collect(Collectors.toList());
        List<String> linesToWrite = new ArrayList<>(1 + mayBeAffectedMethods.size());
        linesToWrite.add(callee);
        linesToWrite.addAll(mayBeAffectedMethods);
        Utils.writeLinesTo(linesToWrite, mayBeAffectedMethods.size() + "_" + methodId + ".txt");
    }


}
