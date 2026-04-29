import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.HashMap;

public class VM_Translator {

    public static int jumpNumber = 0;

    public static void incrementJumpNumber() {
        jumpNumber++;
    }

    public static Scanner fileReader;
    public static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) throws Exception {

        System.out.println("");
        System.out.println("what file would you like to translate?");
        System.out.println("\u001B[38;2;140;140;140m(write 'all' to translate all .vm files in all subdirectories or leave the line blank to use MemoryAccess/BasicTest/BasicTest.vm)\u001B[0m"); // \u001B[38;2;140;140;140m is the ansi escape sequence for a shade of grey (140, 140, 140), \u001B[0m resets color to default (found it on stack overflow)
        String inputFile = sc.nextLine();

        if (inputFile.length() == 0) { // default case
            inputFile = "MemoryAccess/BasicTest/BasicTest.vm";
        }
        else if (inputFile.equals("all")) { // get all files & translate one by one
            ArrayList<String> allFiles = getFiles("./");

            for (String fileName : allFiles) {
                translateFile(fileName);
            }
        }
        else { // user gave file, translate it
            translateFile(inputFile);
        }
    }

    public static ArrayList<String> getFiles(String folderPath) { // gets all .vm files in folder and subfolders (recursively)
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();
        ArrayList<String> fileNames = new ArrayList<String>();

        String folderPathAppend = folderPath;

        // special case for current directory to avoid filenames starting with .//
        if (folderPath.equals("./")) {
            folderPathAppend = ".";
        }

        // if file add it, if directory call recursively and get subfiles
        for (File file : listOfFiles) {
            if (file.isFile() && file.getName().endsWith(".vm")) { // only add .vm files
                fileNames.add(folderPathAppend + "/" + file.getName());
            }
            else if (file.isDirectory()) { // add files from subfolder recursively
                ArrayList<String> subfolderFiles = getFiles(folderPathAppend + "/" + file.getName());
                fileNames.addAll(subfolderFiles);
            }
        }

        return fileNames;
    }

    public static void translateFile(String inputFile) throws Exception { // translates a single file

        FileWriter fileWriter = new FileWriter(inputFile.replace(".vm", ".asm")); // output file with same name but .asm extension in same directory

        fileReader = new Scanner(new File(inputFile));

        while (fileReader.hasNextLine()) {
            Line nextLine = Parser.parseLine(fileReader.nextLine()); // gets the next parsed line as an Line object

            if (nextLine == null) { // if the line was empty or a comment
                continue;
            }

            fileWriter.write(nextLine.toASM() + "\n"); // toASM is defined on Line subclasses

        }

        fileWriter.close();
    }
}

class Parser {

    public static Line parseLine(String newLine) throws Exception {

        newLine = removeWhitespace(newLine);

        if (newLine.length() == 0 || newLine.replace(" ", "").length() == 0) { // if the line without whitespace is empty, return null (had no line)
            return null;
        }

        Line line = null;

        String[] splitLine = newLine.split(" ");
        if (splitLine[0].equals("push") || splitLine[0].equals("pop")) {
            line = new MemoryLine(splitLine[0], splitLine[1], splitLine[2]);
        } else {
            line = new ArithmeticLine(splitLine[0]);
        }

        return line;
    }

    public static String removeWhitespace(String line) {
        line = line.replace("\t", "");
        line = line.split("//")[0]; // removes all comments - anything after a // must be a comment
        return line;
    }

}

class Line { // parent class for line types
    public String toASM() { // defining the function here so it can be called on Line objects
        return "";
    }
}

class ArithmeticLine extends Line {
    String command;

    public static HashMap<String, String> commandTable = new HashMap<String, String>() {{
        put(
            "add",
            "@SP\nAM=M-1\nD=M\nA=A-1\nM=M+D"
        );
        put(
            "sub",
            "@SP\nAM=M-1\nD=M\nA=A-1\nM=M-D"
        );
        put(
            "neg",
            "@SP\nA=M-1\nM=-M"
        );
        put( // using JUMPTEMP[num] as placeholder - replaced later
            "eq",
            "@SP\nAM=M-1\nD=M\nA=A-1\nD=M-D\n@JUMPTEMP1\nD;JEQ\n@SP\nA=M-1\nM=0\n@JUMPTEMP2\n0;JMP\n(JUMPTEMP1)\n@SP\nA=M-1\nM=-1\n(JUMPTEMP2)"
        );
        put(
            "gt",
            "@SP\nAM=M-1\nD=M\nA=A-1\nD=M-D\n@JUMPTEMP1\nD;JGT\n@SP\nA=M-1\nM=0\n@JUMPTEMP2\n0;JMP\n(JUMPTEMP1)\n@SP\nA=M-1\nM=-1\n(JUMPTEMP2)"
        );
        put(
            "lt",
            "@SP\nAM=M-1\nD=M\nA=A-1\nD=M-D\n@JUMPTEMP1\nD;JLT\n@SP\nA=M-1\nM=0\n@JUMPTEMP2\n0;JMP\n(JUMPTEMP1)\n@SP\nA=M-1\nM=-1\n(JUMPTEMP2)"
        );
        put(
            "and",
            "@SP\nAM=M-1\nD=M\nA=A-1\nM=M&D"
        );
        put(
            "or",
            "@SP\nAM=M-1\nD=M\nA=A-1\nM=M|D"
        );
        put(
            "not",
            "@SP\nA=M-1\nM=!M"
        );
    }};

    public ArithmeticLine(String command) {
        this.command = command;
    }

    public String toASM() {

        String commandASM = commandTable.get(command);

        // replacing JUMPTEMP[num] with unique JUMP[num] labels that don't conflict with other lines (using VM_Translator.jumpNumber which holds the current number the entire program is on)
        int jumpNum = 1;
        boolean foundJump = commandASM.contains("JUMPTEMP"+jumpNum);

        while (foundJump) {
            commandASM = commandASM.replace("JUMPTEMP"+jumpNum, "JUMP"+VM_Translator.jumpNumber);
            VM_Translator.incrementJumpNumber();
            jumpNum++;
            foundJump = commandASM.contains("JUMPTEMP"+jumpNum);
        }
        
        return commandASM;
    }
}

class MemoryLine extends Line {
    String commandType;
    String memorySegment;
    String commandNum;

    public static HashMap<String, String> memorySegmentsTable = new HashMap<String, String>() {{
        put("local", "LCL");
        put("argument", "ARG");
        put("this", "THIS");
        put("that", "THAT");
        put("pointer", "3");
        put("temp", "5");
        put("static", "16");
        put("constant", "0");
    }};

    public MemoryLine(String commandType, String memorySegment, String commandNum) {
        this.commandType = commandType;
        this.memorySegment = memorySegment;
        this.commandNum = commandNum;
    }

    public String toASM() {

        String asmString = "";

        if (commandType.equals("push")) {

            // handles special (don't have a label) memory segments differently
            if (memorySegment.equals("constant")) {
                asmString = "@" + commandNum + "\nD=A";
            } else if (memorySegment.equals("pointer") || memorySegment.equals("temp") || memorySegment.equals("static")) { // these ones have their start address in the table, so you can just add it to the commandNum
                asmString = "@" + (Integer.parseInt(commandNum) + Integer.parseInt(memorySegmentsTable.get(memorySegment))) +"\nD=M";
            } else {
                asmString = "@" + memorySegmentsTable.get(memorySegment) + "\nD=M\n@" + (commandNum) +"\nA=D+A\nD=M";
            }
            
            asmString += "\n@SP\nM=M+1\nA=M-1\nM=D";

        } else if (commandType.equals("pop")) {
            // special memory segments - don't have a label (add segment start address to commandNum) & don't need to hold address in R13
            if (memorySegment.equals("pointer") || memorySegment.equals("temp") || memorySegment.equals("static")) {
                asmString = "@SP\nAM=M-1\nD=M\n@" + (Integer.parseInt(commandNum) + Integer.parseInt(memorySegmentsTable.get(memorySegment))) + "\nM=D";
                return asmString;
            }
            
            asmString = "@" + memorySegmentsTable.get(memorySegment) + "\nD=M\n@" + commandNum + "\nD=D+A\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D";
        }
        
        return asmString;
    }

}