import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SymbolSolverFactory {
    public static JavaSymbolSolver getJavaSymbolSolverFromConfig(String srcCfg, String libCfg){
        List<String> srcPaths = Utils.getLinesFrom(srcCfg);
        List<String> libPaths = Utils.getLinesFrom(libCfg);
        return  getJavaSymbolSolver(srcPaths, libPaths);
    }

    public static JavaSymbolSolver getJavaSymbolSolverByDefault(){
        return getJavaSymbolSolverFromConfig(Utils.SRC_CFG, Utils.LIB_CFG);
    }



    //获取符号推理器
    public JavaSymbolSolver getJavaSymbolSolver(String srcPath, String libPath) {
        return getJavaSymbolSolver(Utils.makeListFromOneElement(srcPath), Utils.makeListFromOneElement(libPath));
    }

    //获取符号推理器
    public static JavaSymbolSolver getJavaSymbolSolver(List<String> srcPaths, List<String> libPaths) {
        ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver(); // jdk推理
        reflectionTypeSolver.setParent(reflectionTypeSolver);
        List<JavaParserTypeSolver> javaParserTypeSolvers = makeJavaParserTypeSolvers(srcPaths); //工程内代码推理
        List<JarTypeSolver> jarTypeSolvers = makeJarTypeSolvers(libPaths);//jar包推理
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(reflectionTypeSolver);
        javaParserTypeSolvers.stream().forEach(t -> combinedTypeSolver.add(t));
        jarTypeSolvers.stream().forEach(t -> combinedTypeSolver.add(t));
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        return symbolSolver;
    }

    // 获取jar包的符号推理器
    private static List<JarTypeSolver> makeJarTypeSolvers(List<String> libPaths) {
        List<String> jarPaths = Utils.getFilesBySuffixInPaths("jar", libPaths);
        List<JarTypeSolver> jarTypeSolvers = new ArrayList<>(jarPaths.size());
        try {
            for (String jarPath : jarPaths) {
                jarTypeSolvers.add(new JarTypeSolver(jarPath));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jarTypeSolvers;
    }

    //获取工程源代码src的符号推理器
    private static List<JavaParserTypeSolver> makeJavaParserTypeSolvers(List<String> srcPaths) {
        List<JavaParserTypeSolver> javaParserTypeSolvers = srcPaths.stream()
                .map(t -> new JavaParserTypeSolver(new File(t))).collect(Collectors.toList());
        return javaParserTypeSolvers;
    }
}
