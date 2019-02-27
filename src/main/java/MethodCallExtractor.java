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

import java.io.File;
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
    public static void main(String[] args) {

        String srcPath = "D:\\git\\jp\\data\\MaintenanceUPL\\src\\main\\java";//扫描的工程目录，从包含com.huawei.it的目录开始
        String libPath = "D:\\git\\jp\\data\\lib"; //工程依赖的包， 可以不配
        MethodCallExtractor extractor = new MethodCallExtractor();
        Map<String, List<String>> methodCallRelation = extractor.getMethodCallRelation(srcPath, libPath);
        Utils.printMap(methodCallRelation);
    }

    public Map<String, List<String>> getMethodCallRelationByDefault() {

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
            System.out.println(i + "/" + javaFileNum + " processing: " + javaFile);
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
                System.out.println("ERROR无法获取全限定方法名：" + caller + " 因为：" + e.getMessage());
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
                System.out.println("Error引用解析异常：" + n.getNameAsString() + "." +
                        n.getArguments().toString().replace("[", "(").replace("]", ")")
                        + " 因为：" + e.getMessage() + " 发生在-->" + n.getRange().get().begin + ":" + n.toString());
//                e.printStackTrace();
            }
//            printSymbolType(resolvedMethodDeclaration, n); // 调试用
            // Don't forget to call super, it may find more method calls inside the arguments of this method call, for example.
            super.visit(n, collector);
        }

        private void printSymbolType(ResolvedMethodDeclaration resolvedMethodDeclaration, MethodCallExpr n) {
            if (resolvedMethodDeclaration != null) {
                if (resolvedMethodDeclaration instanceof JavaParserMethodDeclaration) {
                    System.out.println("依赖src==> " + n.resolve().getQualifiedSignature());
                } else if (resolvedMethodDeclaration instanceof ReflectionMethodDeclaration) {
                    System.out.println("依赖jdk==> " + n.resolve().getQualifiedSignature());
                } else if (resolvedMethodDeclaration instanceof JavassistMethodDeclaration) {
                    System.out.println("依赖jar==>" + n.resolve().getQualifiedSignature());
                } else if (resolvedMethodDeclaration instanceof JavaParserEnumDeclaration.ValuesMethod) {
                    System.out.println("依赖临时新增在内存的方法, 不应该出现" + n.resolve().getQualifiedSignature());
                } else {
                    System.out.println("能符号解析, 但未知类型. 不应该出现" + n.resolve().getQualifiedSignature());
                }
            }
        }

    }

}
