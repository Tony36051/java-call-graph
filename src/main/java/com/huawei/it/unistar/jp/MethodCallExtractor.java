package com.huawei.it.unistar.jp;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserEnumDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistMethodDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 方法调用提取类
 */
public class MethodCallExtractor {

    private static Logger logger = LoggerFactory.getLogger(MethodCallExtractor.class);

    public static void main(String[] args) {
        MethodCallExtractor extractor = new MethodCallExtractor();
        Map<String, List<String>> methodCallRelation = extractor.getMethodCallRelationByDefault();
//        Utils.printMap(methodCallRelation);
    }

    public Map<String, List<String>> getMethodCallRelationByDefault() {
        logger.info("从resources目录下配置文件定义的扫描目录开始扫描代码，分析方法调用关系...");
        List<String> srcPaths = Utils.getLinesFrom(Utils.SRC_CFG);
        List<String> libPaths = Utils.getLinesFrom(Utils.LIB_CFG);
        return getMethodCallRelation(srcPaths, libPaths);
    }

    /**
     * 获取方法调用关系
     *
     * @param srcPath
     * @param libPath
     * @return 定义的方法->依赖的方法列表
     */
    public Map<String, List<String>> getMethodCallRelation(String srcPath, String libPath) {
        return getMethodCallRelation(Utils.makeListFromOneElement(srcPath), Utils.makeListFromOneElement(libPath));
    }

    public Map<String, List<String>> getMethodCallRelation(List<String> srcPaths, List<String> libPaths) {
        JavaSymbolSolver symbolSolver = SymbolSolverFactory.getJavaSymbolSolver(srcPaths, libPaths);
        JavaParser.getStaticConfiguration().setSymbolResolver(symbolSolver);

        Map<String, List<String>> callerCallees = new HashMap<>();
        List<String> javaFiles = Utils.getFilesBySuffixInPaths("java", srcPaths);//所有srcPaths中的java文件
        int javaFileNum = javaFiles.size();
        for (int i = 0; i < javaFiles.size(); i++) {
            String javaFile = javaFiles.get(i);
            logger.debug("{}/{} processing: {}", i, javaFileNum, javaFile);
            extract(javaFile, callerCallees);
        }
        return callerCallees;
    }


    // 分析单个文件
    private void extract(String javaFile, Map<String, List<String>> callerCallees) {
        CompilationUnit cu = null;
        try {
            cu = JavaParser.parse(new FileInputStream(javaFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        List<MethodDeclaration> all = cu.findAll(MethodDeclaration.class);
        for (MethodDeclaration methodDeclaration : all) {
            List<String> curCallees = new ArrayList<>();
            methodDeclaration.accept(new MethodCallVisitor(), curCallees);
            String caller;
            try {
                caller = methodDeclaration.resolve().getQualifiedSignature();
            } catch (Exception e) {
                caller = methodDeclaration.getSignature().asString();
                logger.error("Use {} instead of  qualified signature, cause: {}", caller, e.getMessage());
            }
            assert caller != null;
            if (!callerCallees.containsKey(caller)) {
                callerCallees.put(caller, new ArrayList<>());
            }
            callerCallees.get(caller).addAll(curCallees);
        }
    }


    //遍历代码文件时， 关注方法调用的表达式， 然后提取到第二个参数中。正式工作时应该删去打印语句
    private static class MethodCallVisitor extends VoidVisitorAdapter<List<String>> {
        @Override
        public void visit(MethodCallExpr n, List<String> collector) {
            // Found a method call
            ResolvedMethodDeclaration resolvedMethodDeclaration = null;
            try {
                resolvedMethodDeclaration = n.resolve();
                // 仅关注提供src目录的工程代码
                if (resolvedMethodDeclaration instanceof JavaParserMethodDeclaration) {
                    collector.add(n.resolve().getQualifiedSignature());
                }
            } catch (Exception e) {
                logger.error("Line {}, {} cannot resolve some symbol, because {}",
                        n.getRange().get().begin.line,
                        n.getNameAsString() + n.getArguments().toString().replace("[", "(").replace("]", ")"),
                        e.getMessage());
            }
            printSymbolType(resolvedMethodDeclaration, n); // 调试用
            // Don't forget to call super, it may find more method calls inside the arguments of this method call, for example.
            super.visit(n, collector);
        }

        private void printSymbolType(ResolvedMethodDeclaration resolvedMethodDeclaration, MethodCallExpr n) {
            if (resolvedMethodDeclaration != null) {
                if (resolvedMethodDeclaration instanceof JavaParserMethodDeclaration) {
                    logger.trace("depend on src: {}", resolvedMethodDeclaration.getQualifiedSignature());
                } else if (resolvedMethodDeclaration instanceof ReflectionMethodDeclaration) {
                    logger.trace("depend on jdk: {}", resolvedMethodDeclaration.getQualifiedSignature());
                } else if (resolvedMethodDeclaration instanceof JavassistMethodDeclaration) {
                    logger.trace("depend on jar: {}", resolvedMethodDeclaration.getQualifiedSignature());
                } else if (resolvedMethodDeclaration instanceof JavaParserEnumDeclaration.ValuesMethod) {
                    logger.error("depend on mem: {}", resolvedMethodDeclaration.getQualifiedSignature());
                } else {
                    logger.error("depend on ???: {}", resolvedMethodDeclaration.getQualifiedSignature());
                }
            }
        }

    }

}
