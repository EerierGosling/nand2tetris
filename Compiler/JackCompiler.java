import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.HashMap;

public class JackCompiler {

    public static String[] keywords = {"class", "constructor", "function", "method", "field", "static", "var", "int", "char", "boolean", "void", "true", "false", "null", "this", "let", "do", "if", "else", "while", "return"};
    public static String[] symbols = {"{", "}", "(", ")", "[", "]", ".", ",", ";", "+", "-", "*", "/", "&", "|", "<", ">", "=", "~"};
    public static String[] opSymbols = {"+", "-", "*", "/", "&", "|", "<", ">", "="};
    public static String[] unaryOpSymbols = {"-", "~"};

    public static HashMap<String, Command> opSymbolToCommand = new HashMap<String, Command>() {{
        put("+", Command.ADD);
        put("-", Command.SUB);
        put("&", Command.AND);
        put("|", Command.OR);
        put("<", Command.LT);
        put(">", Command.GT);
        put("=", Command.EQ);
    }};

    public static ArrayList<String> fileStrings; // hold all the strings in the file being currently compiled. a static var so that all functions can access it without it having to get passed down

    public static ArrayList<String> tokens = new ArrayList<String>(); // all tokens in file currently being compiled, static var for the same reason

    public static String className;
    public static int jumpCounter = 0; // for generating unique labels in if and while statements

    public static String compile(String fullFile) throws Exception {
        fileStrings = new ArrayList<String>(); // initialize to empty array

        String symbolsString = String.join("", symbols).replace("[", "\\[").replace("]", "\\]").replace("-", "\\-"); // string of all symbols for the regex expressions
        
        // find all strings in the file, put them in an array, and replace them in the token list with a placeholder corresponding to the string in the array
        Matcher stringMatcher = Pattern.compile("\"[^\"]*\"").matcher(fullFile);
        int strCounter = 0;
        while (stringMatcher.find()) {
            String nextString = stringMatcher.group();
            fileStrings.add(nextString.substring(1, nextString.length() - 1)); // add string to array without quotes
            fullFile = fullFile.replace(nextString, "__STRING" + strCounter + "__");
            strCounter++;
        }

        fullFile = fullFile.replaceAll("/\\*[\\s\\S]*?\\*/", "").replaceAll("//.*", ""); // delete all multiline comments and then all single line comments

        String[] origTokens = fullFile.split("\\s+|(?=["+symbolsString+"])|(?<=["+symbolsString+"])"); // split tokens by whitespace or before or after any symbols

        // remove all empty tokens or whitespace only
        tokens = new ArrayList<String>();
        for (String token : origTokens) {
            if (token.length() > 0) {
                tokens.add(token);
            }
        }

        String vmCode;

        if (tokens.size() == 0) {
            throw new Exception("empty file");
        } else if (!tokens.get(0).equals("class")) {
            throw new Exception("file must start with class");
        } else {
            vmCode = compileClass();
        }

        return vmCode;
    }

    public static String compileClass() throws Exception { // compile a class

        tokens.remove(0); // remove class keyword
        className = tokens.remove(0);
        tokens.remove(0); // remove {

        while (tokens.get(0).equals("field") || tokens.get(0).equals("static")) { // if has class var decs, add them to tree
            compileVarDec();
        }
        while (tokens.get(0).equals("constructor") || tokens.get(0).equals("function") || tokens.get(0).equals("method")) { // if has subroutine decs, add them to tree
            compileSubroutineDec();
        }

        String vmCode = VMWriter.vmCode;

        VMWriter.reset();

        return vmCode;
    }

    public static void compileVarDec() {

        Kind varCategory = JackVariable.toKind(tokens.remove(0)); // field/static/var keyword
        String varType = tokens.remove(0); // variable type

        Symbols.add(varCategory, varType, tokens.remove(0)); // add var to subroutine symbol table

        while (tokens.get(0).equals(",")) {
            tokens.remove(0); // remove ,
            Symbols.add(varCategory, varType, tokens.remove(0)); // add next var to subroutine symbol table
        }

        tokens.remove(0); // remove ;
    }

    public static void compileStatements() throws Exception {

        if (tokens.size() == 0) {
            return;
        }

        while (tokens.size() > 0) {
            String firstToken = tokens.get(0);
            if (firstToken.equals("let")) {
                compileLetStatement();
            } else if (firstToken.equals("do")) {
                compileDoStatement();
            } else if (firstToken.equals("if")) {
                compileIfStatement();
            } else if (firstToken.equals("while")) {
                compileWhileStatement();
            } else if (firstToken.equals("return")) {
                compileReturnStatement();
            } else {
                break;
            }
        }
    }

    public static void compileLetStatement() throws Exception {

        tokens.remove(0); // let
        JackVariable var = Symbols.get(tokens.remove(0)); // var name
        if (tokens.get(0).equals("[")) {
            tokens.remove(0); // [
            VMWriter.writePushVar(var); // base address of array
            compileExpression(); // index of array element
            VMWriter.writeArithmetic(Command.ADD); // address of array element now on stack
            tokens.remove(0); // ]
            tokens.remove(0); // =
            compileExpression(); // new value on stack
            tokens.remove(0); // ;
            VMWriter.writePop(Segment.TEMP, 0); // store new value in temp
            VMWriter.writePop(Segment.POINTER, 1); // set THAT pointer to address of array element
            VMWriter.writePush(Segment.TEMP, 0); // push new value back to stack
            VMWriter.writePop(Segment.THAT, 0); // pop new value to array element
        } else {
            tokens.remove(0); // =
            compileExpression(); // new value on stack
            tokens.remove(0); // ;
            VMWriter.writePopVar(var); // pop new value to variable
        }
    }

    public static void compileDoStatement() throws Exception {

        tokens.remove(0); // do
        String baseName = tokens.get(0); // subroutine name or class/var name
        JackVariable var = Symbols.get(baseName);
        int numArgs;

        if (var != null) {
            tokens.remove(0); // .
            baseName = var.type + "." + tokens.remove(0);
            tokens.remove(0); // (
            VMWriter.writePushVar(var); // push object being called on as first argument
            numArgs = compileExpressionList() + 1; // expression list in method call, add 1 for object being passed as arg
        } else {
            if (tokens.get(0).equals(".")) {
                baseName += tokens.remove(0) + tokens.remove(0); // . & subroutine name
            } else {
                baseName = className + "." + baseName; // method call on current class, add class name to beginning
            }
            tokens.remove(0); // (
            numArgs = compileExpressionList(); // expression list in subroutine call
        }
        tokens.remove(0); // )
        tokens.remove(0); // ;
        VMWriter.writeCall(baseName, numArgs);
        VMWriter.writePop(Segment.TEMP, 0); // pop return value off the stack (not used)
    }

    public static void compileIfStatement() throws Exception {

        tokens.remove(0); // if
        tokens.remove(0); // (
        compileExpression(); // expression in the condition
        tokens.remove(0); // )
        tokens.remove(0); // {
        VMWriter.writeArithmetic(Command.NOT); // negate condition for if-goto

        // either else or end depending on if there's an else statement
        // if else, is jump to else
        // if no else, is jump to end of if statement
        String jump1 = "JUMP" + jumpCounter++;
        VMWriter.writeIf(jump1); 
        compileStatements(); // statements content of loop
        tokens.remove(0); // }

        if (tokens.size() > 0 && tokens.get(0).equals("else")) {
            String jump2 = "JUMP" + jumpCounter++; // jump to end of if/else statement after if block
            VMWriter.writeGoto(jump2);
            VMWriter.writeLabel(jump1); // label for start of else block
            tokens.remove(0); // else
            tokens.remove(0); // {
            compileStatements(); // contents of else block
            tokens.remove(0); // }
            VMWriter.writeLabel(jump2); // label for end of if/else statement
        } else {
            VMWriter.writeLabel(jump1); // if no else, add label for end of if statement
        }
    }

    public static void compileWhileStatement() throws Exception {

        String startLabel = "JUMP" + jumpCounter++;
        String endLabel = "JUMP" + jumpCounter++;
        VMWriter.writeLabel(startLabel);
        tokens.remove(0); // while
        tokens.remove(0); // (
        compileExpression(); // expression in the condition
        tokens.remove(0); // )
        VMWriter.writeArithmetic(Command.NOT); // negate condition for if-goto
        VMWriter.writeIf(endLabel); // if condition false, jump to end of loop

        tokens.remove(0); // {
        compileStatements(); // statements content of loop
        tokens.remove(0); // }
        VMWriter.writeGoto(startLabel); // go to start to check condition again
        VMWriter.writeLabel(endLabel); // end of while loop
    }

    public static void compileReturnStatement() throws Exception {

        tokens.remove(0); // return
        if (!tokens.get(0).equals(";")) {
            compileExpression(); // expression after return
        } else {
            VMWriter.writePush(Segment.CONST, 0); // push 0 for void return
        }
        tokens.remove(0); // ;
        VMWriter.writeReturn();
    }

    public static void compileSubroutineDec() throws Exception {

        Symbols.resetSubroutine();

        boolean isMethod = tokens.remove(0).equals("method"); // constructor/function/method
        tokens.remove(0); // return type
        String funcName = tokens.remove(0); // name

        int funcLoc = VMWriter.vmCode.size(); // want to put the function def before the param list compilation - need to know where func starts

        tokens.remove(0); // (
        int varCount = compileParameterList(isMethod); // parameter list
        tokens.remove(0); // )

        VMWriter.writeFunction(funcName, varCount, funcLoc); // write function declaration with number of local variables and location in code for function start

        // subroutine body parsing - not seperate function bc single use and not too long
        tokens.remove(0); // {
        while (tokens.get(0).equals("var")) {
            compileVarDec();
        }
        compileStatements();
        tokens.remove(0); // }
    }

    public static int compileParameterList(boolean isMethod) {

        if (isMethod) { // if method, add this as first argument
            Symbols.add(Kind.ARG, className, "this");
        }

        if (tokens.get(0).equals(")")) {
            return 0;
        }

        int paramCount = 0;
        while (true) {
            paramCount++;
            Symbols.add(Kind.ARG, tokens.remove(0), tokens.remove(0)); // type, name of parameter
            if (tokens.get(0).equals(",")) { // if there's a comma add it
                tokens.remove(0); // ,
            } else {
                break;
            }
        }
        return paramCount;
    }

    public static int compileExpressionList() throws Exception {

        int argCount = 0;

        if (tokens.get(0).equals(")")) { // if empty return empty array
            return argCount;
        }

        while (true) { // add each expression while there are more
            compileExpression();
            argCount++;
            if (tokens.get(0).equals(",")) {
                tokens.remove(0); // ,
            } else {
                break;
            }
        }

        return argCount;
    }

    public static void compileExpression() throws Exception {
        
        while (true) { // add term, if there's an operator add it and then the next term until there's no more
            compileTerm();
            if (tokens.size() > 0 && Compiler.contains(opSymbols, tokens.get(0))) {
                if (tokens.get(0).equals("*")) {
                    VMWriter.writeCall("Math.multiply", 2);
                } else if (tokens.get(0).equals("/")) {
                    VMWriter.writeCall("Math.divide", 2);
                } else {
                    Command command = opSymbolToCommand.get(tokens.get(0));
                    VMWriter.writeArithmetic(command);
                }
                tokens.remove(0); // operator
            } else {
                break;
            }
        }
    }

    public static void compileTerm() throws Exception {
        
        if (tokens.get(0).equals("(")) {
            tokens.remove(0); // (
            compileExpression(); // expression in parentheses
            tokens.remove(0); // )

        } else if (Compiler.contains(unaryOpSymbols, tokens.get(0))) {
            tokens.remove(0); // unary operator
            compileTerm(); // term after unary operator
            VMWriter.writeArithmetic(tokens.get(0).equals("-") ? Command.NEG : Command.NOT); // write unary command

        } else {
            String identifierName = "";

            if (tokens.get(0).matches("\\d+")) { // int
                VMWriter.writePush(Segment.CONST, Integer.parseInt(tokens.remove(0))); // push int to stack
                tokens.remove(0);

            } else if (tokens.get(0).startsWith("__STRING")) {
                String strValue = fileStrings.get(Integer.parseInt(tokens.get(0).substring(8, tokens.get(0).length() - 2))); // get number out of ___STRING#___ str & get corresponding string from array
                VMWriter.writePush(Segment.CONST, strValue.length()); // push length of string to stack
                VMWriter.writeCall("String.new", 1); // call String.new to create new string of that length
                for (char c : strValue.toCharArray()) { // for each char in the string, push ascii value and call String.appendChar to add it to the end of the string
                    VMWriter.writePush(Segment.CONST, (int) c); 
                    VMWriter.writeCall("String.appendChar", 2);
                }
                tokens.remove(0);

            } else if (tokens.get(0).equals("true")) {
                VMWriter.writePush(Segment.CONST, 0); // push 0 for false
                VMWriter.writeArithmetic(Command.NOT); // negate to get true
                tokens.remove(0);

            } else if (tokens.get(0).equals("false") || tokens.get(0).equals("null")) {
                VMWriter.writePush(Segment.CONST, 0); // push 0 for false or null
                tokens.remove(0);

            } else if (tokens.get(0).equals("this")) {
                VMWriter.writePush(Segment.POINTER, 0); // push this
                tokens.remove(0);
                
            } else {
                identifierName = tokens.get(0); // var name or subroutine name
                JackVariable var = Symbols.get(identifierName);
                if (var != null) {
                    VMWriter.writePushVar(var); // push variable value to stack
                    tokens.remove(0);
                } 
            }

            if (tokens.size() > 0 && tokens.get(0).equals("[")) {
                tokens.remove(0); // [
                compileExpression(); // expression in array index
                tokens.remove(0); // ]
                VMWriter.writeArithmetic(Command.ADD); // add base address and index to get address of array element
                VMWriter.writePop(Segment.POINTER, 1); // set THAT pointer to address of array element
                VMWriter.writePush(Segment.THAT, 0); // push array element value to stack

            } else if (tokens.size() > 0 && tokens.get(0).equals("(")) {
                tokens.remove(0); // (
                compileExpressionList();
                tokens.remove(0); // )
                VMWriter.writeCall(className + "." + identifierName, 0); // method call on current class

            } else if (tokens.size() > 1 && tokens.get(0).equals(".")) {
                tokens.remove(0); // .
                tokens.remove(0); // subroutine name
                tokens.remove(0); // (
                compileExpressionList(); // expression list in subroutine call
                tokens.remove(0); // )
                VMWriter.writeCall(identifierName, 0); // subroutine call
            }
        }
    }
}

class Symbols {
    public static HashMap<String, JackVariable> classSymbols = new HashMap<String, JackVariable>();
    public static int staticCount = 0;
    public static int fieldCount = 0;

    public static HashMap<String, JackVariable> subroutineSymbols = new HashMap<String, JackVariable>();
    public static int argCount = 0;
    public static int varCount = 0;

    public static void add(Kind kind, String type, String name) {
        JackVariable variable = new JackVariable(type, kind, incrementCount(kind));
        if (isClass(kind)) {
            classSymbols.put(name, variable);
        } else {
            subroutineSymbols.put(name, variable);
        }
    }

    public static boolean isClass(Kind kind) {
        return kind == Kind.STATIC || kind == Kind.FIELD;
    }

    public static int incrementCount(Kind kind) {
        if (kind == Kind.STATIC) {
            return staticCount++;
        } else if (kind == Kind.FIELD) {
            return fieldCount++;
        } else if (kind == Kind.ARG) {
            return argCount++;
        } else if (kind == Kind.VAR) {
            return varCount++;
        } else {
            return -1;
        }
    }

    public static JackVariable get(String name) {
        if (subroutineSymbols.containsKey(name)) {
            return subroutineSymbols.get(name);
        } else if (classSymbols.containsKey(name)) {
            return classSymbols.get(name);
        } else {
            return null; // not found
        }
    }

    public static void resetSubroutine() {
        subroutineSymbols = new HashMap<String, JackVariable>();
        argCount = 0;
        varCount = 0;
    }
}

class VMWriter {
    public static ArrayList<String> vmCode = new ArrayList<String>();

    public static void writePush(Segment segment, int index) {
        vmCode.add("push " + segment.toString().toLowerCase() + " " + index);
    }
    public static void writePushVar(JackVariable var) {
        vmCode.add("push " + var.getSegment().toString().toLowerCase() + " " + var.index);
    }
    public static void writePop(Segment segment, int index) {
        vmCode.add("pop " + segment.toString().toLowerCase() + " " + index);
    }
    public static void writePopVar(JackVariable var) {
        vmCode.add("pop " + var.getSegment().toString().toLowerCase() + " " + var.index);
    }
    public static void writeArithmetic(Command command) {
        vmCode.add(command.toString().toLowerCase());
    }
    public static void writeLabel(String label) {
        vmCode.add("label " + label);
    }
    public static void writeGoto(String label) {
        vmCode.add("goto " + label);
    }
    public static void writeIf(String label) {
        vmCode.add("if-goto " + label);
    }
    public static void writeCall(String funcName, int argCount) {
        vmCode.add("call " + funcName + " " + argCount);
    }
    public static void writeFunction(String funcName, int varCount, int codeLocation) {
        vmCode.add("function " + funcName + " " + varCount);
    }
    public static void writeReturn() {
        vmCode.add("return");
    }

    public static void reset() {
        vmCode = new ArrayList<String>();
    }
}

enum Kind { STATIC, FIELD, ARG, VAR }

enum Segment { CONST, ARG, LOCAL, STATIC, THIS, THAT, POINTER, TEMP }

enum Command { ADD, SUB, NEG, EQ, GT, LT, AND, OR, NOT }

class JackVariable {
    public String type;
    public Kind kind;
    public int index;

    public JackVariable(String type, Kind kind, int index) {
        this.type = type;
        this.kind = kind;
        this.index = index;
    }

    public static Kind toKind(String kindStr) {
        if (kindStr.equals("static")) {
            return Kind.STATIC;
        } else if (kindStr.equals("field")) {
            return Kind.FIELD;
        } else if (kindStr.equals("arg")) {
            return Kind.ARG;
        } else if (kindStr.equals("var")) {
            return Kind.VAR;
        } else {
            return null;
        }
    }

    public Segment getSegment() {
        if (kind == Kind.STATIC) {
            return Segment.STATIC;
        } else if (kind == Kind.FIELD) {
            return Segment.THIS;
        } else if (kind == Kind.ARG) {
            return Segment.ARG;
        } else if (kind == Kind.VAR) {
            return Segment.LOCAL;
        } else {
            return null;
        }
    }
}