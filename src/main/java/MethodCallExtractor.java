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
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 方法调用提取类
 */
public class MethodCallExtractor {
    public static void main(String[] args) {
        String srcPath = "D:\\git\\jp\\data\\MaintenanceUPL\\src\\main\\java";//扫描的工程目录，从包含com.huawei.it的目录开始
        String libPath = "D:\\git\\jp\\data\\lib"; //工程依赖的包， 可以不配
        MethodCallExtractor extractor = new MethodCallExtractor();
        Map<String, List<String>> methodCallRelation = extractor.getMethodCallRelation(srcPath, libPath);
        extractor.printMap(methodCallRelation);
    }

    //获取符号推理器
    private TypeSolver getCombinedSolver(String javaSrcPath, String libPath) {
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver(); // jdk推理
        reflectionTypeSolver.setParent(reflectionTypeSolver);
        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(javaSrcPath)); //工程内代码推理
        List<String> jarPaths = getFilesBySuffixIgnoringCase(libPath, "jar");
        List<JarTypeSolver> jarTypeSolvers = new ArrayList<>(jarPaths.size()); //jar包依赖
        try {
            for (String jarPath : jarPaths) {
                jarTypeSolvers.add(new JarTypeSolver(jarPath));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(reflectionTypeSolver);
        combinedTypeSolver.add(javaParserTypeSolver);
        jarTypeSolvers.stream().forEach(t -> combinedTypeSolver.add(t));
        return combinedTypeSolver;
    }

    /**
     * 获取方法调用关系
     * @param srcPath
     * @param libPath
     * @return 定义的方法->依赖的方法列表
     */
    public Map<String, List<String>> getMethodCallRelation(String srcPath, String libPath) {
        TypeSolver combinedTypeSolver = getCombinedSolver(srcPath, libPath);
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        JavaParser.getStaticConfiguration().setSymbolResolver(symbolSolver);

        Map<String, List<String>> callerCallees = new HashMap<>();
        List<String> javaFiles = getFilesBySuffixIgnoringCase(srcPath, "java");
        int cnt = 2;
        for (String javaFile : javaFiles) {
            //TODO:删掉这个， 加入为了方便调试
//            if (cnt-- < 0) {
//                break;
//            }
            System.out.println("processing: " + javaFile);
            extract(javaFile, callerCallees);
        }
        return callerCallees;
    }


    private void printMap(Map<String, List<String>> callerCallees) {
        callerCallees.entrySet().stream().filter(t -> !t.getValue().isEmpty())
                .forEach(t -> System.out.println(t.getKey() + ": " + t.getValue()));
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
                System.out.println(caller + "-->" +e.getMessage());
            }
            assert caller != null;
            if (!callerCallees.containsKey(caller)) {
                callerCallees.put(caller, new ArrayList<>());
            }
            callerCallees.get(caller).addAll(curCallees);
        }
    }

    //辅助函数， 根据后缀名筛选文件
    private List<String> getFilesBySuffixIgnoringCase(String srcPath, String suffix) {
        List<String> filePaths = null;
        try {
            filePaths = Files.find(Paths.get(srcPath), Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile())
                    .filter(f -> f.toString().toLowerCase().endsWith(suffix))
                    .map(f -> f.toString()).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filePaths;
    }

    //遍历代码文件时， 关注方法调用的表达式， 然后提取到第二个参数中。正式工作时应该删去打印语句
    private static class MethodCallVisitor extends VoidVisitorAdapter<List<String>> {
        @Override
        public void visit(MethodCallExpr n, List<String> collector) {
            // Found a method call
            try {
                ResolvedMethodDeclaration resolvedMethodDeclaration = n.resolve();
                if (resolvedMethodDeclaration instanceof JavaParserMethodDeclaration) {
                    collector.add(n.resolve().getQualifiedSignature());
                    System.out.println("依赖src==> " + n.resolve().getQualifiedSignature());
                } else if (resolvedMethodDeclaration instanceof ReflectionMethodDeclaration) {
                    System.out.println("依赖jdk==> " + n.resolve().getQualifiedSignature());
                } else if (resolvedMethodDeclaration instanceof JavassistMethodDeclaration){
                    System.out.println("依赖jar-->" +  n.resolve().getQualifiedSignature());
                } else if (resolvedMethodDeclaration instanceof JavaParserEnumDeclaration.ValuesMethod) {
                    System.out.println("依赖临时新增在内存的方法, 不应该出现" + n.resolve().getQualifiedSignature());
                }else{
                    System.out.println("能符号解析, 但未知类型. 不应该出现" + n.resolve().getQualifiedSignature());
                }
            } catch (Exception e) {
                System.out.println("Error符号解析异常--->" + n.getNameAsString()+"." + n.getArguments());
            }
            // Don't forget to call super, it may find more method calls inside the arguments of this method call, for example.
            super.visit(n, collector);
        }
    }

}
