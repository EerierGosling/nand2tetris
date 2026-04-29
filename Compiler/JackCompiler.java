import java.io.File;
import java.util.Scanner;
import java.util.ArrayList;

import java.io.FileWriter;

public class JackCompiler {

    public static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        System.out.println("");
        System.out.println("what file type is your input? \u001B[38;2;140;140;140m(enter the number)\u001B[0m");
        System.out.println("  \u001B[38;2;140;140;140m1.\u001B[0m .asm");
        System.out.println("  \u001B[38;2;140;140;140m2.\u001B[0m .vm");
        System.out.println("  \u001B[38;2;140;140;140m3.\u001B[0m .xml");
        System.out.println("  \u001B[38;2;140;140;140m4.\u001B[0m .jack");
        
        int inputType = sc.nextInt();
        sc.nextLine();

        System.out.println("");
        System.out.println("what file type is your input? \u001B[38;2;140;140;140m(enter the number)\u001B[0m");
        System.out.println("  \u001B[38;2;140;140;140m1.\u001B[0m .jack");
        System.out.println("  \u001B[38;2;140;140;140m2.\u001B[0m .xml");
        System.out.println("  \u001B[38;2;140;140;140m3.\u001B[0m .vm");
        System.out.println("  \u001B[38;2;140;140;140m4.\u001B[0m .asm");
        
        int outputType = sc.nextInt();
        sc.nextLine();

        System.out.println("");
        System.out.println("what file or directory would you like to compile?");
        System.out.println("\u001B[38;2;140;140;140m(write 'all' to translate all .jack files in all subdirectories or leave the line blank to use ArrayTest/Main.jack)\u001B[0m"); // \u001B[38;2;140;140;140m is the ansi escape sequence for a shade of grey (140, 140, 140), \u001B[0m resets color to default (found it on stack overflow)

        String inputFile = sc.nextLine();

        if (inputFile.length() == 0) { // use default
            inputFile = "ArrayTest/Main.jack";
            JackParser.parseJack(inputFile);
        } else if (inputFile.equals("all")) { // parse all files
            ArrayList<String> allFiles = getFiles("./");
            for (String fileName : allFiles) {
                JackParser.parseJack(fileName);
            }
        } else { // parse user-given file
            JackParser.parseJack(inputFile);
        }
    }

    public static ArrayList<String> getFiles(String folderPath) { // gets all jack files in the directory
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();
        ArrayList<String> fileNames = new ArrayList<String>();

        String folderPathAppend = folderPath;

        if (folderPath.equals("./")) {
            folderPathAppend = ".";
        }

        for (File file : listOfFiles) {
            if (file.isFile() && file.getName().endsWith(".jack")) {
                fileNames.add(folderPathAppend + "/" + file.getName());
            } else if (file.isDirectory()) {
                ArrayList<String> subfolderFiles = getFiles(folderPathAppend + "/" + file.getName());
                fileNames.addAll(subfolderFiles);
            }
        }

        return fileNames;
    }

    public static boolean contains(String[] arr, String str) { // check if array contains a string
        for (String s : arr) {
            if (s.equals(str)) {
                return true;
            }
        }
        return false;
    }

}

class XMLWriter {

    public static void writeToFile(String filename, JackTree tree) throws Exception {
        FileWriter fileWriter = new FileWriter(filename.replace(".jack", "Ans.xml")); // output file with same name (+ Ans so it's different from the file w the right answer) but .xml extension in same directory
        
        fileWriter.write(treeToXML(tree, 0)); // write the xml string to the file
        fileWriter.close();
    }

    public static String treeToXML(JackTree tree, int indentLevel) { // converts the jacktree to an xml string recursively
        String xml = "";
        String indent = "";
        for (int i = 0; i < indentLevel; i++) {
            indent += "  ";
        }

        if (tree.isToken) { // if its a token, add the token type with value
            xml += indent + "<" + tree.tokenType + "> " + tree.tokenValue.replace("<", "&lt;").replace(">", "&gt;") + " </" + tree.tokenType + ">\n";
        } else { // if enclosing more xml, sandwich the children with the opening and closing tags
            xml += indent + "<" + tree.tokenType + ">\n";
            for (JackTree child : tree.children) {
                xml += treeToXML(child, indentLevel + 1);
            }
            xml += indent + "</" + tree.tokenType + ">\n";
        }
        return xml;
    }
}