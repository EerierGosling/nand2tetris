import java.io.File;
import java.util.Scanner;

class FileReader {

    public static Scanner sc;

    public FileReader() throws Exception {
        sc = new Scanner(new File("input.txt"));
    }

    public static boolean hasNextLine() {
        return sc.hasNextLine();
    }

    public static String getNextLine() {
        return sc.nextLine();
    }

    public static String getFileContents() {
        String contents = "";
        while (sc.hasNextLine()) {
            contents += sc.nextLine() + "\n";
        }
        return contents;
    }
}