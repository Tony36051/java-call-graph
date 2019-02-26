import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils {
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

    public static List<String> getLinesFrom(String file) {
        String line = null;
        List<String> lines = new ArrayList<>();
        if (file == null || "".equals(file)) return lines;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            while (null != (line = bufferedReader.readLine())) {
                lines.add(line.trim());
            }
        } catch (FileNotFoundException e) {
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
