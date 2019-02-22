import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
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

public class MethodCallExtractor {
    public static void main(String[] args) {
        String srcPath = "D:\\git\\testservices-common\\src\\main\\java";
        MethodCallExtractor extractor = new MethodCallExtractor();
        Map<String, List<String>> methodCallRelation = extractor.getMethodCallRelation(srcPath);
        extractor.printMap(methodCallRelation);
    }

    private TypeSolver getCombinedSolver(String javaSrcPath) {
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        reflectionTypeSolver.setParent(reflectionTypeSolver);
        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(javaSrcPath));
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(reflectionTypeSolver);
        combinedTypeSolver.add(javaParserTypeSolver);

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        return combinedTypeSolver;
    }

    public Map<String, List<String>> getMethodCallRelation(String srcPath) {
        TypeSolver combinedTypeSolver = getCombinedSolver(srcPath);
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        JavaParser.getStaticConfiguration().setSymbolResolver(symbolSolver);

        Map<String, List<String>> callerCallees = new HashMap<>();
        List<String> javaFiles = getFilesBySuffixIgnoringCase(srcPath, "java");
        for (String javaFile : javaFiles) {
            if ("D:\\git\\testservices-common\\src\\main\\java\\com\\huawei\\tmss\\services\\TestManagementService\\TestManagementServiceSoapBindingStub.java".equals(javaFile)) {
                continue;
            }
            System.out.println("processing: " + javaFile);
            extract(javaFile, callerCallees);
        }
        return callerCallees;
    }

    private void printMap(Map<String, List<String>> callerCallees) {
        callerCallees.entrySet().stream().filter(t -> !t.getValue().isEmpty())
                .forEach(t -> System.out.println(t.getKey() + ": " + t.getValue()));
    }

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
                System.out.println(e.getMessage());
            }
            assert caller != null;
            if (!callerCallees.containsKey(caller)) {
                callerCallees.put(caller, new ArrayList<>());
            }
            callerCallees.get(caller).addAll(curCallees);
        }
    }

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

    private static class MethodCallVisitor extends VoidVisitorAdapter<List<String>> {
        @Override
        public void visit(MethodCallExpr n, List<String> collector) {
            // Found a method call
            try {
                ResolvedMethodDeclaration resolvedMethodDeclaration = n.resolve();
                if (resolvedMethodDeclaration instanceof JavaParserMethodDeclaration) {
                    collector.add(n.resolve().getQualifiedSignature());
//                    System.out.println(n.resolve().getQualifiedSignature());
                } else {
//                    System.out.println("不是工程下的方法调用");
                }
            } catch (Exception e) {
//                System.out.println("第三方调用--->" + n.getName()+"." + n.getArguments());
            }
            // Don't forget to call super, it may find more method calls inside the arguments of this method call, for example.
            super.visit(n, collector);
        }
    }

}
