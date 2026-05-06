import java.io.File;
import java.util.Scanner;
import java.util.ArrayList;
import java.io.FileWriter;

// Tests/10/ArrayTest/Main.jack

public class Compiler {

    public static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        System.out.println("");
        System.out.println("what file type is your input? \u001B[38;2;140;140;140m(enter the number)\u001B[0m");
        System.out.println("  \u001B[38;2;140;140;140m1.\u001B[0m .asm");
        System.out.println("  \u001B[38;2;140;140;140m2.\u001B[0m .vm");
        System.out.println("  \u001B[38;2;140;140;140m3.\u001B[0m .jack");
        
        int inputNum = sc.nextInt();
        sc.nextLine();

        System.out.println("");
        System.out.println("what would you like to output? \u001B[38;2;140;140;140m(enter the number)\u001B[0m");
        System.out.println("  \u001B[38;2;140;140;140m1.\u001B[0m .hack");
        if (inputNum > 1) System.out.println("  \u001B[38;2;140;140;140m2.\u001B[0m .asm");
        if (inputNum > 2) System.out.println("  \u001B[38;2;140;140;140m3.\u001B[0m .vm");
        
        int outputNum = sc.nextInt();
        sc.nextLine();

        FileType inputType;
        FileType outputType;

        if (inputNum == 1) {
            inputType = FileType.ASM;
        } else if (inputNum == 2) {
            inputType = FileType.VM;
        } else if (inputNum == 3) {
            inputType = FileType.JACK;
        } else {
            inputType = null;
        }

        if (outputNum == 1) {
            outputType = FileType.HACK;
        } else if (outputNum == 2) {
            outputType = FileType.ASM;
        } else if (outputNum == 3) {
            outputType = FileType.VM;
        } else {
            outputType = null;
        }


        System.out.println("");
        System.out.println("what file or directory would you like to compile?");
        System.out.println("\u001B[38;2;140;140;140m(write 'all' to translate all ." + inputType.toString() + " files in all subdirectories)\u001B[0m"); // \u001B[38;2;140;140;140m is the ansi escape sequence for a shade of grey (140, 140, 140), \u001B[0m resets color to default (found it on stack overflow)

        String inputFile = sc.nextLine();

        if (inputFile.equals("all")) { // parse all files
            ArrayList<String> allFiles = getFiles("./", inputType.toString()); // get all files in current directory and subdirectories
            for (String fileName : allFiles) {
                compile(fileName, inputType, outputType);
            }
        } else { // parse user-given file
            if (inputFile.equals("")){
                inputFile = "Tests/10/ArrayTest/Main.jack";
            }
            compile(inputFile, inputType, outputType);
        }
    }

    public static void compile(String file, FileType inputType, FileType outputType) throws Exception {
        Scanner sc = new Scanner(new File(file));
        String fileText = sc.useDelimiter("\\A").next(); // read the whole file into a string
        sc.close();

        if (inputType == FileType.JACK) {
            fileText = JackCompiler.compile(fileText);
        }
        if (inputType.toInt() >= FileType.VM.toInt()  && outputType.toInt() < FileType.VM.toInt()) {
            fileText = VMTranslator.translate(fileText, true); // todo: check if it's really a directory
        }
        if (inputType.toInt() >= FileType.ASM.toInt()  && outputType.toInt() < FileType.ASM.toInt()) {
            fileText = Assembler.assemble(fileText);
        }
        //  + "_ans"
        String outputFile = file.substring(0, file.lastIndexOf(".")) + "." + outputType.toString(); // change the file extension to the output type
        FileWriter writer = new FileWriter(new File(outputFile));
        writer.write(fileText);
        writer.close();
    }

    public static ArrayList<String> getFiles(String folderPath, String fileType) { // gets all files of a specific type in the directory
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();
        ArrayList<String> fileNames = new ArrayList<String>();

        String folderPathAppend = folderPath;

        if (folderPath.equals("./")) {
            folderPathAppend = ".";
        }

        for (File file : listOfFiles) {
            if (file.isFile() && file.getName().endsWith("." + fileType)) {
                fileNames.add(folderPathAppend + "/" + file.getName());
            } else if (file.isDirectory()) {
                ArrayList<String> subfolderFiles = getFiles(folderPathAppend + "/" + file.getName(), fileType);
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

enum FileType {
    HACK, ASM, VM, XML, JACK;

    public String toString() {
        switch (this) {
            case HACK:
                return "hack";
            case ASM:
                return "asm";
            case VM:
                return "vm";
            case XML:
                return "xml";
            case JACK:
                return "jack";
            default:
                return null;
        }
    }

    public int toInt() {
        switch (this) {
            case HACK:
                return 1;
            case ASM:
                return 2;
            case VM:
                return 3;
            case XML:
                return 4;
            case JACK:
                return 5;
            default:
                return -1;
        }
    }
}