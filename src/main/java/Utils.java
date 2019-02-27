import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils {

    public static String SRC_CFG = "src.cfg";
    public static String LIB_CFG = "lib.cfg";

    public static void printMap(Map<String, List<String>> callerCallees) {
        callerCallees.entrySet().stream().filter(t -> !t.getValue().isEmpty())
                .forEach(t -> System.out.println(t.getKey() + ": " + t.getValue()));
    }

    //辅助函数， 根据后缀名筛选文件
    public static List<String> getFilesBySuffixInPath(String suffix, String path) {
        List<String> filePaths = null;
        try {
            filePaths = Files.find(Paths.get(path), Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile())
                    .filter(f -> f.toString().toLowerCase().endsWith(suffix))
                    .map(f -> f.toString()).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filePaths;
    }

    public static List<String> getFilesBySuffixInPaths(String suffix, List<String> paths) {
        List<String> files = new ArrayList<>();
        for (String path : paths) {
            files.addAll(getFilesBySuffixInPath(suffix, path));
        }
        return files;
    }

    private static File getFileInResources(String fileName){
        ClassLoader classLoader = Utils.class.getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        return file;
    }

    public static List<String> getLinesFrom(String fileName) {
        String line = null;
        List<String> lines = new ArrayList<>();
        if (fileName == null || "".equals(fileName)) return lines;
        try {
            File file = getFileInResources(fileName);
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            while (null != (line = bufferedReader.readLine())) {
                lines.add(line.trim());
            }
        } catch (FileNotFoundException e) {
            System.out.println("cannot find from " + new File(".").getAbsoluteFile());
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    public static <T> List<T> makeListFromOneElement(T object) {
        ArrayList<T> list = new ArrayList<>();
        if (object != null) {
            list.add(object);
        }
        return list;
    }
}
