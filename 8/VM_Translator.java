import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.HashMap;

public class VM_Translator {

    public static int jumpNumber = 0;
    public static int functionCallNumber = 0;
    public static String currentFileName = "";
    public static String currentFunction = "";

    public static void incrementJumpNumber() {
        jumpNumber++;
    }

    public static void incrementFunctionCallNumber() {
        functionCallNumber++;
    }

    public static Scanner fileReader;
    public static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) throws Exception {

        System.out.println("");
        System.out.println("what file or directory would you like to translate?");
        System.out.println("\u001B[38;2;140;140;140m(write 'all' to translate all .vm files in all subdirectories (won't work for folders with multiple vm files!) or leave the line blank to use FunctionCalls/SimpleFunction/SimpleFunction.vm)\u001B[0m"); // \u001B[38;2;140;140;140m is the ansi escape sequence for a shade of grey (140, 140, 140), \u001B[0m resets color to default (found it on stack overflow)


        String inputFile = sc.nextLine();

        if (inputFile.length() == 0) { // default case
            inputFile = "FunctionCalls/SimpleFunction/SimpleFunction.vm";
        } else if (inputFile.equals("all")) { // get all files & translate one by one
            ArrayList<String> allFiles = getFiles("./");

            for (String fileName : allFiles) {
                translateFile(fileName);
            }
        } else { // user gave file, translate it
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
            } else if (file.isDirectory()) { // add files from subfolder recursively
                ArrayList<String> subfolderFiles = getFiles(folderPathAppend + "/" + file.getName());
                fileNames.addAll(subfolderFiles);
            }
        }

        return fileNames;
    }

    public static void translateFile(String inputFile) throws Exception { // translates a single file or directory

        File input = new File(inputFile);
        FileWriter fileWriter;
        ArrayList<File> vmFiles = new ArrayList<File>();
        boolean needsBootstrap = false;

        if (input.isDirectory()) {
            // translate all .vm files in directory
            needsBootstrap = true;
            File[] files = input.listFiles();
            for (File file : files) {
                if (file.getName().endsWith(".vm")) {
                    vmFiles.add(file);
                }
            }
            // output file is directory name + .asm
            String dirName = input.getName();
            fileWriter = new FileWriter(new File(input, dirName + ".asm"));
        } else { // translate single file
            vmFiles.add(input);
            fileWriter = new FileWriter(inputFile.replace(".vm", ".asm"));
        }

        if (needsBootstrap) { // only need bootstrap code if there are multiple files, otherwise the single file just needs RETURN_INTERNAL routine if it has any returns
            fileWriter.write(Bootstrapping.fullBootstrap + "\n");
        } else {
            fileWriter.write(Bootstrapping.returnRoutine + "\n");
        }

        // translate each file
        for (File vmFile : vmFiles) {
            currentFileName = vmFile.getName().replace(".vm", "");
            fileReader = new Scanner(vmFile);

            while (fileReader.hasNextLine()) {
                Line nextLine = Parser.parseLine(fileReader.nextLine());

                if (nextLine == null) {
                    continue;
                }

                fileWriter.write(nextLine.toASM() + "\n");
            }
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
        String command = splitLine[0];

        // create the right type of line

        if (command.equals("push") || command.equals("pop")) {
            line = new MemoryLine(splitLine[0], splitLine[1], splitLine[2]);
        } else if (command.equals("label")) {
            line = new ProgramFlow(ProgramFlow.CommandType.LABEL, splitLine[1]);
        } else if (command.equals("goto")) {
            line = new ProgramFlow(ProgramFlow.CommandType.GOTO, splitLine[1]);
        } else if (command.equals("if-goto")) {
            line = new ProgramFlow(ProgramFlow.CommandType.IF_GOTO, splitLine[1]);
        } else if (command.equals("function")) {
            VM_Translator.currentFunction = splitLine[1];
            line = new FunctionDef(splitLine[1], Integer.parseInt(splitLine[2]));
        } else if (command.equals("call")) {
            line = new FunctionCall(splitLine[1], Integer.parseInt(splitLine[2]));
        } else if (command.equals("return")) {
            line = new Return();
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

class Bootstrapping {

    public static String returnRoutine = "@AFTER_RETURN_INTERNAL\n0;JMP\n(RETURN_INTERNAL)\n" +
            "@LCL\nD=M\n@R13\nM=D\n" + // store LCL in R13 to use as endFrame
            "@5\nA=D-A\nD=M\n@R14\nM=D\n" + // store return address in R14
            "@SP\nAM=M-1\nD=M\n@ARG\nA=M\nM=D\n" + // store returned value from function call in ARG[0] (now the top of the stack)
            "@ARG\nD=M+1\n@SP\nM=D\n" + // reset SP to ARG + 1 (next address after return value)
            "@R13\nAM=M-1\nD=M\n@THAT\nM=D\n" + // reset THAT to saved
            "@R13\nAM=M-1\nD=M\n@THIS\nM=D\n" + // reset THIS to saved
            "@R13\nAM=M-1\nD=M\n@ARG\nM=D\n" + // reset ARG to saved
            "@R13\nAM=M-1\nD=M\n@LCL\nM=D\n" + // reset LCL to saved
            "@R14\nA=M\n0;JMP\n" + // jump to return address
            "(AFTER_RETURN_INTERNAL)";

    public static String fullBootstrap = "@256\nD=A\n@SP\nM=D\n" + // SP = 256
            new FunctionCall("Sys.init", 0).toASM() + "\n" + // call Sys.init
            Bootstrapping.returnRoutine + "\n";
}

class Line { // parent class for line types

    public Line() {
    }

    public String toASM() { // defining the function here so it can be called on Line objects
        return "";
    }
}

class ArithmeticLine extends Line {
    String command;

    public static HashMap<String, String> commandTable = new HashMap<String, String>() {
        {
            put(
                "add",
                "@SP\nAM=M-1\nD=M\nA=A-1\nM=M+D");
            put(
                "sub",
                "@SP\nAM=M-1\nD=M\nA=A-1\nM=M-D");
            put(
                "neg",
                "@SP\nA=M-1\nM=-M");
            put( // using JUMPTEMP[num] as placeholder - replaced later
                "eq",
                "@SP\nAM=M-1\nD=M\nA=A-1\nD=M-D\n@JUMPTEMP1\nD;JEQ\n@SP\nA=M-1\nM=0\n@JUMPTEMP2\n0;JMP\n(JUMPTEMP1)\n@SP\nA=M-1\nM=-1\n(JUMPTEMP2)");
            put(
                "gt",
                "@SP\nAM=M-1\nD=M\nA=A-1\nD=M-D\n@JUMPTEMP1\nD;JGT\n@SP\nA=M-1\nM=0\n@JUMPTEMP2\n0;JMP\n(JUMPTEMP1)\n@SP\nA=M-1\nM=-1\n(JUMPTEMP2)");
            put(
                "lt",
                "@SP\nAM=M-1\nD=M\nA=A-1\nD=M-D\n@JUMPTEMP1\nD;JLT\n@SP\nA=M-1\nM=0\n@JUMPTEMP2\n0;JMP\n(JUMPTEMP1)\n@SP\nA=M-1\nM=-1\n(JUMPTEMP2)");
            put(
                "and",
                "@SP\nAM=M-1\nD=M\nA=A-1\nM=M&D");
            put(
                "or",
                "@SP\nAM=M-1\nD=M\nA=A-1\nM=M|D");
            put(
                "not",
                "@SP\nA=M-1\nM=!M");
        }
    };

    public ArithmeticLine(String command) {
        this.command = command;
    }

    public String toASM() {

        String commandASM = commandTable.get(command);

        // replacing JUMPTEMP[num] with unique JUMP[num] labels that don't conflict with other lines (using VM_Translator.jumpNumber which holds the current number the entire program is on)
        int jumpNum = 1;
        boolean foundJump = commandASM.contains("JUMPTEMP" + jumpNum);

        while (foundJump) {
            commandASM = commandASM.replace("JUMPTEMP" + jumpNum, "JUMP_INTERNAL_ASM" + VM_Translator.jumpNumber);
            VM_Translator.incrementJumpNumber();
            jumpNum++;
            foundJump = commandASM.contains("JUMPTEMP" + jumpNum);
        }

        return commandASM;
    }
}

class MemoryLine extends Line {
    String commandType;
    String memorySegment;
    String commandNum;

    public static HashMap<String, String> memorySegmentsTable = new HashMap<String, String>() {
        {
            put("local", "LCL");
            put("argument", "ARG");
            put("this", "THIS");
            put("that", "THAT");
            put("pointer", "3");
            put("temp", "5");
            put("static", "16");
            put("constant", "0");
        }
    };

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
            } else if (memorySegment.equals("static")) {
                asmString = "@" + VM_Translator.currentFileName + "." + commandNum + "\nD=M";
            } else if (memorySegment.equals("pointer") || memorySegment.equals("temp")) {
                asmString = "@" + (Integer.parseInt(commandNum) + Integer.parseInt(memorySegmentsTable.get(memorySegment))) + "\nD=M";
            } else {
                asmString = "@" + memorySegmentsTable.get(memorySegment) + "\nD=M\n@" + (commandNum) + "\nA=D+A\nD=M";
            }

            asmString += "\n@SP\nM=M+1\nA=M-1\nM=D";

        } else if (commandType.equals("pop")) {
            // special memory segments - don't have a label (add segment start address to commandNum) & don't need to hold address in R13
            if (memorySegment.equals("static")) {
                asmString = "@SP\nAM=M-1\nD=M\n@" + VM_Translator.currentFileName + "." + commandNum + "\nM=D";
                return asmString;
            } else if (memorySegment.equals("pointer") || memorySegment.equals("temp")) {
                asmString = "@SP\nAM=M-1\nD=M\n@" + (Integer.parseInt(commandNum) + Integer.parseInt(memorySegmentsTable.get(memorySegment))) + "\nM=D";
                return asmString;
            }

            asmString = "@" + memorySegmentsTable.get(memorySegment) + "\nD=M\n@" + commandNum + "\nD=D+A\n@R13\nM=D\n@SP\nAM=M-1\nD=M\n@R13\nA=M\nM=D";
        }

        return asmString;
    }

}

class FunctionDef extends Line {
    String functionName;
    int numLocals;

    public FunctionDef(String functionName, int numLocals) {
        this.functionName = functionName;
        this.numLocals = numLocals;
    }

    public String toASM() {

        String asmString = "(" + functionName + ")\n";
        for (int i = 0; i < numLocals; i++) { // push 0 onto stack numLocals times to initialize local variables
            asmString += "@SP\nM=M+1\nA=M-1\nM=0\n";
        }

        return asmString;
    }
}

class FunctionCall extends Line {
    String functionName;
    int numArgs;

    public FunctionCall(String functionName, int numArgs) {
        this.functionName = functionName;
        this.numArgs = numArgs;
    }

    public String toASM() {
        String returnLabel = "RETURN_" + VM_Translator.functionCallNumber;
        VM_Translator.incrementFunctionCallNumber();

        return "@" + returnLabel + "\nD=A\n@SP\nM=M+1\nA=M-1\nM=D\n" + // push return address onto stack
                "@LCL\nD=M\n@SP\nM=M+1\nA=M-1\nM=D\n" + // push LCL, ARG, THIS, THAT onto stack to save them for when it returns
                "@ARG\nD=M\n@SP\nM=M+1\nA=M-1\nM=D\n" +
                "@THIS\nD=M\n@SP\nM=M+1\nA=M-1\nM=D\n" +
                "@THAT\nD=M\n@SP\nM=M+1\nA=M-1\nM=D\n" +
                "@SP\nD=M\n@5\nD=D-A\n@" + numArgs + "\nD=D-A\n@ARG\nM=D\n" + // set arg to SP - numArgs - 5
                "@SP\nD=M\n@LCL\nM=D\n" + // LCL = SP (sp is now address right after saved addresses)
                "@" + functionName + "\n0;JMP\n" + // go to function
                "(" + returnLabel + ")";
    }

}

class Return extends Line {
    public String toASM() {
        return "@RETURN_INTERNAL\n0;JMP";
    }
}

class ProgramFlow extends Line {
    public enum CommandType {
        LABEL,
        GOTO,
        IF_GOTO
    }

    CommandType commandType;
    String label; // labels cannot be of form JUMP_INTERNAL_ASM[number]

    public ProgramFlow(CommandType commandType, String label) {
        this.commandType = commandType;
        this.label = label;
    }

    public String toASM() {
        String fullLabel = VM_Translator.currentFunction + "." + label;

        if (commandType == CommandType.LABEL) { // return the assembly for the right command type
            return "(" + fullLabel + ")";
        } else if (commandType == CommandType.GOTO) {
            return "@" + fullLabel + "\n0;JMP";
        } else {
            return "@SP\nAM=M-1\nD=M\n@" + fullLabel + "\nD;JNE";
        }
    }
}