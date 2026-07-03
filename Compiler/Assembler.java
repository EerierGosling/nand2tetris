import java.util.ArrayList;
import java.util.HashMap;

public class Assembler { // todo: make it work if you flip the order of A and M

    public static HashMap<String, Integer> symbolTable = new HashMap<String, Integer>(){{ // initialized with predefined symbols and defined symbols are added later
        for (int i = 0; i <= 15; i++) {
            put("R" + i, i);
        }
        put("SCREEN", 16384);
        put("KBD", 24576);
        put("SP", 0);
        put("LCL", 1);
        put("ARG", 2);
        put("THIS", 3);
        put("THAT", 4);
    }};
    
    public static ArrayList<Instruction> instructions = new ArrayList<>(); // holds a list of all the instructions in order as Instruction objects (created in first loop)
    public static ArrayList<String> instructionsBinary = new ArrayList<>(); // holds a list of all the instructions in order as binary strings (created in second loop) - what is printed to the file

    public static String assemble(String input) throws Exception {

        String[] inputLines = input.split("\n"); // splits the input string into lines

        for (String line : inputLines) {
            Instruction nextInstruction = AsmParser.parseLine(line); // gets the next parsed line as an Instruction object - could be A, C, or Label

            if (nextInstruction == null) { // if the line was empty or a comment
                continue;
            }

            if (nextInstruction instanceof A_Instruction) { // if it's an A_Instruction, put the symbol in the symbol table if it doesn't already exist and add it to the instructions list
                A_Instruction aInstruction = (A_Instruction) nextInstruction;
                if (aInstruction.symbol != null && !symbolTable.containsKey(aInstruction.symbol)) {
                    symbolTable.put(aInstruction.symbol, null);
                }
                instructions.add(nextInstruction);

            } else if (nextInstruction instanceof Label) { // if it's a Label, add the label to the symbol table with the current instruction address and do not add it to the instructions list
                symbolTable.put(((Label) nextInstruction).label, instructions.size());
            } else { // if C_Instruction, add it to the instructions list
                instructions.add(nextInstruction);
            }
        }

        int currentAddress = 16; // first available RAM address for symbols

        for (Instruction instruction : instructions) {
            if (instruction instanceof A_Instruction) { // if it's an A_Instruction, check if the symbol needs to be assigned an address
                A_Instruction aInstruction = (A_Instruction) instruction;
                if (aInstruction.symbol != null) { // check if it is a symbol (not a number)
                    if (symbolTable.get(aInstruction.symbol) == null) { // if the symbol does not have an assigned address yet, assign the next available one
                        symbolTable.put(aInstruction.symbol, currentAddress);
                        aInstruction.address = currentAddress;
                        currentAddress += 1;
                    } else {
                        aInstruction.address = symbolTable.get(aInstruction.symbol); // if it already has an address, just set the address in the instruction to that
                    }
                }
            }
            instructionsBinary.add(instruction.toBinary()); // convert the a or c instruction to binary and add it to the binary instructions list
        }

        return String.join("\n", instructionsBinary); // return the binary instructions as the full file
    }
}

class AsmParser {

    public static Instruction parseLine(String newLine) throws Exception {
        // turn the String line into an Instruction object (A_Instruction, C_Instruction, or Label)

        newLine = removeWhitespace(newLine);

        if (newLine.length() == 0) { // if the line without whitespace is empty, return null (had no instruction)
            return null;
        }

        Instruction instruction = new Instruction();

        if (newLine.charAt(0)=='@') { // if it starts with @, it's an A_Instruction
            String withoutAt = newLine.replace("@", "");
            try { // try to parse it as an integer address - if it fails, it's a symbol
                int address = Integer.parseInt(withoutAt);
                instruction = new A_Instruction(address);
            } catch (Exception e) {
                instruction = new A_Instruction(withoutAt);
            }
        } else if (newLine.charAt(0)=='(') { // if it starts with (, it's a Label
            String label = newLine.replace("(", "");
            label = label.replace(")", "");
            instruction = new Label(label); // create a Label instruction
        } else { // otherwise, it's a C_Instruction
            // split the line into dest, comp, and jump parts
            String[] splitLine = newLine.split("[=;]"); // regex for = or ;
            String dest = newLine.contains("=") ? splitLine[0] : null; // if there's an =, dest is the part before it, otherwise dest doesn't exist
            String comp = newLine.contains("=") ? splitLine[1] : splitLine[0]; // if there's an =, comp is after it, otherwise it is the first element in the line
            String jump = newLine.contains(";") ? (newLine.contains("=") ? splitLine[2] : splitLine[1]) : null; // if there's a ;, jump is after it (which is element 3 if there's an =, otherwise element 2), otherwise jump doesn't exist

            instruction = new C_Instruction(dest, comp, jump); // create a C_Instruction instruction from those parts
        }

        return instruction;
    }

    public static String removeWhitespace(String line) {
        line = line.replace("\t", "");
        line = line.replace(" ", "");
        line = line.split("//")[0]; // removes all comments - anything after a // must be a comment
        return line;
    }
    
}

class Instruction { // parent class for A_Instruction, C_Instruction, and Label
    public String toBinary() { // defining the function here so it can be called on Instruction objects - actually defined in A_Instruction and C_Instruction
        return "";
    }
}

class A_Instruction extends Instruction {
    String symbol;
    int address;

    // two constructors - one for symbol input, one for address input

    public A_Instruction(String symbolInput) {
        symbol = symbolInput;
    }

    public A_Instruction(int addressInput) {
        address = addressInput;
    }

    public String toBinary() {
        String binary = Integer.toBinaryString(address);
    
        while (binary.length() < 16) { // makes the binary 16 bits long
            binary = "0" + binary;
        }

        return binary;
    }
}

class C_Instruction extends Instruction {
    String dest;
    String comp;
    String jump;

    HashMap<String, String> compReplacements = new HashMap<String, String>() {{ // X is here in the place of A or M
        put("0", "101010");
        put("1", "111111");
        put("-1", "111010");
        put("D", "001100");
        put("X", "110000");
        put("!D", "001101");
        put("!X", "110001");
        put("-D", "001111");
        put("-X", "110011");
        put("D+1", "011111");
        put("X+1", "110111");
        put("D-1", "001110");
        put("X-1", "110010");
        put("D+X", "000010");
        put("D-X", "010011");
        put("X-D", "000111");
        put("D&X", "000000");
        put("D|X", "010101");
    }};

    HashMap<String, String> jumpReplacements = new HashMap<String, String>() {{ // null is no jump
        put(null, "000");
        put("JGT", "001");
        put("JEQ", "010");
        put("JGE", "011");
        put("JLT", "100");
        put("JNE", "101");
        put("JLE", "110");
        put("JMP", "111");
    }};


    public C_Instruction(String destInput, String compInput, String jumpInput) {
        dest = destInput;
        comp = compInput;
        jump = jumpInput;
    }

    private String getCompBits() {
        return compReplacements.get(comp.replace("M", "X").replace("A", "X")); // replaces A or M with X to get the correct bits from the map
    }

    private String getDestBits() {
        String destBits;
        if (dest == null) {
            destBits = "000";
        } else {
            destBits = (dest.contains("A") ? "1" : "0") + (dest.contains("D") ? "1" : "0") + (dest.contains("M") ? "1" : "0"); // first bit is whether or not A is contained, second is D, third is M
        }
        return destBits;
    }

    private String getJumpBits() {
        return jumpReplacements.get(jump); // gets the jump bits from the map
    }

    public String toBinary() {
        int aBit = comp.contains("M") ? 1 : 0;

        return "111" + aBit + getCompBits() + getDestBits() + getJumpBits(); // puts al the parts together for the C instruction binary
    }
}

class Label extends Instruction {
    String label;

    public Label(String labelInput) { // just holds the label string
        label = labelInput;
    }
}