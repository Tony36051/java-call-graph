import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparser.Navigator;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

public class Demo {


    public static void main(String[] args) throws FileNotFoundException {
        String path = "D:\\git\\testservices-common\\src\\main\\java\\com\\huawei\\it\\unistar\\test\\common\\" +
                "service\\impl\\TFSTestManagementServiceImpl.java";
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        reflectionTypeSolver.setParent(reflectionTypeSolver);
        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File("D:\\git\\testservices-common\\src\\main\\java"));
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(reflectionTypeSolver);
        combinedTypeSolver.add(javaParserTypeSolver);

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        JavaParser.getStaticConfiguration().setSymbolResolver(symbolSolver);

        FileInputStream in = new FileInputStream(path);
        CompilationUnit cu = JavaParser.parse(in);

        List<MethodDeclaration> all = cu.findAll(MethodDeclaration.class);
        for (MethodDeclaration methodDeclaration : all) {
            System.out.println("=======" + methodDeclaration.resolve().getQualifiedSignature() + "==========");
            methodDeclaration.accept(new MethodCallVisitor(), null);

        }
    }

    private static class MethodVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(MethodDeclaration n, Void arg) {
//            System.out.println(n.getBody().get().getStatements().get(0).getChildNodes().get(0). + "======================");
//            System.out.println(n.getRange().get().begin.line + n.getSignature().toString());
            super.visit(n, arg);
        }
    }

    private static class MethodCallVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(MethodCallExpr n, Void arg) {
            // Found a method call
            try {
                ResolvedMethodDeclaration resolvedMethodDeclaration = n.resolve();
                if (resolvedMethodDeclaration instanceof JavaParserMethodDeclaration) {
                    System.out.println(n.resolve().getQualifiedSignature());
                }else{
                    System.out.println("不是工程下的方法调用");
                }
            } catch (Exception e) {
                System.out.println("第三方调用--->" + n.getName()+"." + n.getArguments());
            }
            // Don't forget to call super, it may find more method calls inside the arguments of this method call, for example.
            super.visit(n, arg);
        }
    }

}
