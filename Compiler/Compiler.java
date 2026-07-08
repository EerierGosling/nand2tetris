import java.io.File;
import java.util.Scanner;
import java.util.ArrayList;
import java.io.FileWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Compiler {

    public static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            runInteractive();
            return;
        }

        String path = null;
        FileType inputType = null;
        FileType outputType = FileType.HACK;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-i":
                case "--in":
                    inputType = parseFileType(requireValue(args, ++i, arg), true);
                    break;
                case "-o":
                case "--out":
                    outputType = parseFileType(requireValue(args, ++i, arg), false);
                    break;
                case "-h":
                case "--help":
                    printUsage();
                    return;
                default:
                    if (arg.startsWith("-")) {
                        System.err.println("unknown flag: " + arg);
                        printUsage();
                        System.exit(1);
                        return;
                    }
                    if (path != null) {
                        System.err.println("only one input file or folder may be specified");
                        System.exit(1);
                        return;
                    }
                    path = arg;
            }
        }

        if (path == null) {
            System.err.println("missing input file or folder");
            printUsage();
            System.exit(1);
            return;
        }

        File target = new File(path);
        if (inputType == null) {
            if (target.isDirectory()) {
                System.err.println("--in is required when compiling a folder");
                printUsage();
                System.exit(1);
                return;
            }
            inputType = inferInputType(path);
        }

        if (inputType.toInt() < outputType.toInt()) {
            System.err.println("input type ." + inputType + " cannot be compiled to a higher-level ." + outputType);
            System.exit(1);
            return;
        }

        if (target.isDirectory()) {
            compileDirectory(path, inputType, outputType);
        } else {
            compile(path, inputType, outputType);
        }
    }

    private static FileType inferInputType(String path) {
        int dot = path.lastIndexOf('.');
        String ext = dot < 0 ? "" : path.substring(dot + 1).toLowerCase();
        switch (ext) {
            case "asm":
                return FileType.ASM;
            case "vm":
                return FileType.VM;
            case "jack":
                return FileType.JACK;
            default:
                System.err.println("cannot infer input type from ." + ext + "; pass --in explicitly");
                System.exit(1);
                return null;
        }
    }

    private static FileType parseFileType(String value, boolean isInput) {
        switch (value.toLowerCase()) {
            case "asm":
                return FileType.ASM;
            case "vm":
                return FileType.VM;
            case "jack":
                if (!isInput) {
                    System.err.println("jack is not a valid output type");
                    System.exit(1);
                }
                return FileType.JACK;
            case "hack":
                if (isInput) {
                    System.err.println("hack is not a valid input type");
                    System.exit(1);
                }
                return FileType.HACK;
            default:
                System.err.println("unknown file type: " + value);
                System.exit(1);
                return null;
        }
    }

    private static String requireValue(String[] args, int i, String flag) {
        if (i >= args.length) {
            System.err.println(flag + " requires a value");
            System.exit(1);
        }
        return args[i];
    }

    private static void printUsage() {
        System.out.println("usage: jackc [-i <asm|vm|jack>] [-o <hack|asm|vm>] <file-or-folder>");
        System.out.println();
        System.out.println("  -i, --in <type>   input file type (inferred from extension if omitted)");
        System.out.println("  -o, --out <type>  output file type (default: hack)");
        System.out.println("  -h, --help        show this message");
        System.out.println();
        System.out.println("if <file-or-folder> is a folder, all matching files in it and its subfolders are compiled");
        System.out.println();
        System.out.println("emulators (official nand2tetris tools - need java installed):");
        System.out.println("  jackc vm-emulator [file.tst]         run VM programs (built-in OS, no 32K limit)");
        System.out.println("  jackc cpu-emulator [file.tst]        run compiled .hack binaries");
        System.out.println("  jackc hardware-simulator [file.tst]  simulate .hdl chip designs");
        System.out.println("with no .tst file they open interactively; with one they run it in batch mode");
    }

    public static void runInteractive() throws Exception { // i've only tested this on jack to vm! it theoretically works on everything else, but it does what it's supposed to for this lab
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
        System.out.println("what file or folder would you like to compile?");
        System.out.println("\u001B[38;2;140;140;140m(write 'all' to translate all ." + inputType.toString() + " files in all subfolders)\u001B[0m"); // \u001B[38;2;140;140;140m is the ansi escape sequence for a shade of grey (140, 140, 140), \u001B[0m resets color to default (found it on stack overflow)

        String inputFile = sc.nextLine();

        if (inputFile.equals("all") || new File(inputFile).isDirectory()) { // parse all files
            if (inputFile.equals("all")) {
                inputFile = "./";
            }
            compileDirectory(inputFile, inputType, outputType);
        } else { // parse user-given file
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
            String baseName = new File(file).getName();
            VMTranslator.currentFileName = baseName.substring(0, baseName.lastIndexOf("."));
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

    // combines every matching file in a folder into one bootstrapped program instead of compiling each in isolation,
    // since VM/Jack programs share one address space and can only have a single Sys.init bootstrap and RETURN_INTERNAL routine.
    // .asm folders are the exception - each .asm file is already a standalone program, so those stay one-to-one.
    public static void compileDirectory(String folderPath, FileType inputType, FileType outputType) throws Exception {
        boolean merge = inputType.toInt() >= FileType.VM.toInt() && outputType.toInt() < FileType.VM.toInt();

        if (!merge) {
            ArrayList<String> fileNames = getFiles(folderPath, inputType.toString());
            for (String fileName : fileNames) {
                compile(fileName, inputType, outputType);
            }

            if (inputType == FileType.JACK && outputType == FileType.VM) { // link the OS in so the folder is a runnable program (e.g. for VMEmulator's "load,")
                ArrayList<String> baseNames = new ArrayList<String>();
                for (String fileName : fileNames) {
                    String baseName = new File(fileName).getName();
                    baseNames.add(baseName.substring(0, baseName.lastIndexOf(".")));
                }
                linkOsFiles(folderPath, baseNames);
            }

            return;
        }

        ArrayList<String> vmFileNames = getFiles(folderPath, inputType.toString());
        ArrayList<String> baseNames = new ArrayList<String>();
        ArrayList<String> vmTexts = new ArrayList<String>();

        for (String fileName : vmFileNames) {
            Scanner fileScanner = new Scanner(new File(fileName));
            String fileText = fileScanner.useDelimiter("\\A").next();
            fileScanner.close();

            if (inputType == FileType.JACK) {
                fileText = JackCompiler.compile(fileText);
            }

            String baseName = new File(fileName).getName();
            baseNames.add(baseName.substring(0, baseName.lastIndexOf(".")));
            vmTexts.add(fileText);
        }

        String outputText = Bootstrapping.fullBootstrap + "\n";
        outputText += translateOs(baseNames, String.join("\n", vmTexts));

        for (int i = 0; i < vmTexts.size(); i++) {
            VMTranslator.currentFileName = baseNames.get(i);
            outputText += VMTranslator.translateBody(vmTexts.get(i)) + "\n";
        }

        if (outputType.toInt() < FileType.ASM.toInt()) {
            outputText = Assembler.assemble(outputText);
        }

        String folderName = new File(folderPath).getCanonicalFile().getName();
        String outputFile = new File(folderPath, folderName + "." + outputType.toString()).getPath();

        FileWriter writer = new FileWriter(new File(outputFile));
        writer.write(outputText);
        writer.close();
    }

    private static final String[] OS_CLASSES = { "Array", "Keyboard", "Math", "Memory", "Output", "Screen", "String", "Sys" };

    // reads one OS class's VM code - from the classpath resource baked into the jar/native binary if present
    // (how the distributed compiler ships, self-contained), otherwise from the OS/ folder next to the source
    // (dev runs from this repo). returns null if neither exists.
    private static String readOsFile(String osClass) throws Exception {
        java.io.InputStream resource = Compiler.class.getResourceAsStream("/OS/" + osClass + ".vm");

        Scanner osScanner;
        if (resource != null) {
            osScanner = new Scanner(resource);
        } else {
            File osFile = new File("OS", osClass + ".vm");
            if (!osFile.exists()) {
                return null;
            }
            osScanner = new Scanner(osFile);
        }

        String osText = osScanner.useDelimiter("\\A").next();
        osScanner.close();
        return osText;
    }

    // copies the whole bundled OS into a compiled VM folder (skipping any class the program already defines its
    // own version of) so the folder is a complete, self-contained, runnable program - e.g. for VMEmulator's
    // "load," which only looks at literal files in the folder and has no notion of a program needing outside classes
    private static void linkOsFiles(String folderPath, ArrayList<String> programClasses) throws Exception {
        for (String osClass : OS_CLASSES) {
            if (programClasses.contains(osClass)) { // program supplies its own version of this OS class
                continue;
            }

            String osText = readOsFile(osClass);
            if (osText == null) {
                System.err.println("warning: bundled OS class " + osClass + " not found, VM output will be missing it");
                continue;
            }

            FileWriter writer = new FileWriter(new File(folderPath, osClass + ".vm"));
            writer.write(osText);
            writer.close();
        }
    }

    // bundles only the OS classes actually reachable from the program (plus whatever those OS classes call
    // internally, e.g. Screen calling Math) instead of the whole OS every time - the Hack ROM only holds 32K
    // instructions and the untrimmed OS alone gets close to that limit once naively translated, so unused
    // classes like Array/String need to be left out whenever nothing calls them
    private static String translateOs(ArrayList<String> programClasses, String programVmText) throws Exception {
        ArrayList<String> needed = new ArrayList<String>();
        needed.add("Sys"); // the bootstrap always calls Sys.init directly
        needed.addAll(findOsCalls(programVmText));

        ArrayList<String> included = new ArrayList<String>();
        String output = "";

        for (int i = 0; i < needed.size(); i++) {
            String osClass = needed.get(i);

            if (programClasses.contains(osClass) || included.contains(osClass) || !contains(OS_CLASSES, osClass)) {
                continue; // program supplies its own version, already bundled, or not actually an OS class
            }

            String osText = readOsFile(osClass);
            if (osText == null) {
                System.err.println("warning: bundled OS class " + osClass + " not found, calls to it will not resolve");
                continue;
            }

            included.add(osClass);
            VMTranslator.currentFileName = osClass;
            output += VMTranslator.translateBody(osText) + "\n";

            for (String calledClass : findOsCalls(osText)) { // OS classes call each other (e.g. Screen calls Math)
                if (!needed.contains(calledClass)) {
                    needed.add(calledClass);
                }
            }
        }

        return output;
    }

    private static ArrayList<String> findOsCalls(String vmText) {
        ArrayList<String> calls = new ArrayList<String>();
        Matcher matcher = Pattern.compile("(?m)^call ([A-Za-z0-9_]+)\\.").matcher(vmText);

        while (matcher.find()) {
            String className = matcher.group(1);
            if (!calls.contains(className)) {
                calls.add(className);
            }
        }

        return calls;
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