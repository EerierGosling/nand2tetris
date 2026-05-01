import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

public class JackParser {

    public static String[] keywords = {"class", "constructor", "function", "method", "field", "static", "var", "int", "char", "boolean", "void", "true", "false", "null", "this", "let", "do", "if", "else", "while", "return"};
    public static String[] symbols = {"{", "}", "(", ")", "[", "]", ".", ",", ";", "+", "-", "*", "/", "&", "|", "<", ">", "=", "~"};
    public static String[] opSymbols = {"+", "-", "*", "/", "&", "|", "<", ">", "="};
    public static String[] unaryOpSymbols = {"-", "~"};

    public static ArrayList<String> fileStrings; // hold all the strings in the file being currently parsed. a static var so that all functions can access it without it having to get passed down

    public static ArrayList<String> tokens = new ArrayList<String>(); // all tokens in file currently being parsed, static var for the same reason

    public static String parse(String filename) throws Exception { // parse a jack file
        return treeToXML(parseToTree(filename), 0); // convert the tree to an xml string and return it
    }

    public static JackTree parseToTree(String fullFile) throws Exception {
        JackTree fullTree;
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

        if (tokens.size() == 0) {
            throw new Exception("empty file");
        } else if (!tokens.get(0).equals("class")) {
            throw new Exception("file must start with class");
        } else {
            fullTree = parseClass(tokens);
        }

        return fullTree;
    }

    public static JackTree parseClass(ArrayList<String> tokens) throws Exception { // parse a class
        JackTree fullTree = new JackTree("class"); // root of tree

        fullTree.children = addNextToken(fullTree.children, "keyword", "class"); // class keyword
        fullTree.children = addNextToken(fullTree.children, "identifier", tokens.get(0)); // name of class
        fullTree.children = addNextToken(fullTree.children, "symbol", tokens.get(0)); // {

        while (tokens.get(0).equals("field") || tokens.get(0).equals("static")) { // if has class var decs, add them to tree
            fullTree.children.add(new JackTree("classVarDec", parseVarDec(tokens)));
        }
        while (tokens.get(0).equals("constructor") || tokens.get(0).equals("function") || tokens.get(0).equals("method")) { // if has subroutine decs, add them to tree
            fullTree.children.add(new JackTree("subroutineDec", parseSubroutineDec(tokens)));
        }
        fullTree.children.add(new JackTree("symbol", "}")); // add closing } to tree and remove from tokens
        return fullTree;
    }

    public static ArrayList<JackTree> parseVarDec(ArrayList<String> tokens) {
        ArrayList<JackTree> children = new ArrayList<JackTree>();

        addNextToken(children, "keyword", tokens.get(0)); // field/static/var keyword

        if (tokens.get(0).equals("int") || tokens.get(0).equals("char") || tokens.get(0).equals("boolean")) { // add type keyword/identifier
            addNextToken(children, "keyword", tokens.get(0));
        } else {
            addNextToken(children, "identifier", tokens.get(0));
        }

        addNextToken(children, "identifier", tokens.get(0)); // var name

        while (tokens.get(0).equals(",")) {
            addNextToken(children, "symbol", tokens.get(0)); // ,
            addNextToken(children, "identifier", tokens.get(0)); // var name
        }

        addNextToken(children, "symbol", tokens.get(0)); // ;

        return children;
    }

    public static ArrayList<JackTree> parseStatements(ArrayList<String> tokens) throws Exception {
        ArrayList<JackTree> children = new ArrayList<JackTree>();

        if (tokens.size() == 0) {
            return children;
        }

        while (tokens.size() > 0) {
            String firstToken = tokens.get(0);
            if (firstToken.equals("let")) {
                children.add(new JackTree("letStatement", parseLetStatement(tokens)));
            } else if (firstToken.equals("do")) {
                children.add(new JackTree("doStatement", parseDoStatement(tokens)));
            } else if (firstToken.equals("if")) {
                children.add(new JackTree("ifStatement", parseIfOrWhileStatement(tokens)));
            } else if (firstToken.equals("while")) {
                children.add(new JackTree("whileStatement", parseIfOrWhileStatement(tokens)));
            } else if (firstToken.equals("return")) {
                children.add(new JackTree("returnStatement", parseReturnStatement(tokens)));
            } else {
                break;
            }
        }

        return children;
    }

    public static ArrayList<JackTree> parseLetStatement(ArrayList<String> tokens) throws Exception {
        ArrayList<JackTree> children = new ArrayList<JackTree>();

        addNextToken(children, "keyword", tokens.get(0)); // let
        addNextToken(children, "identifier", tokens.get(0)); // var name
        if (tokens.get(0).equals("[")) {
            addNextToken(children, "symbol", tokens.get(0)); // [
            children.add(new JackTree("expression", parseExpression(tokens)));
            addNextToken(children, "symbol", tokens.get(0)); // ]
        }
        addNextToken(children, "symbol", tokens.get(0)); // =
        children.add(new JackTree("expression", parseExpression(tokens)));
        addNextToken(children, "symbol", tokens.get(0)); // ;

        return children;
    }

    public static ArrayList<JackTree> parseDoStatement(ArrayList<String> tokens) throws Exception {
        ArrayList<JackTree> children = new ArrayList<JackTree>();

        addNextToken(children, "keyword", tokens.get(0)); // do
        addNextToken(children, "identifier", tokens.get(0)); // subroutine name or class/var name
        if (tokens.get(0).equals(".")) {
            addNextToken(children, "symbol", tokens.get(0)); // .
            addNextToken(children, "identifier", tokens.get(0)); // subroutine name
        }
        addNextToken(children, "symbol", tokens.get(0)); // (
        children.add(new JackTree("expressionList", parseExpressionList(tokens)));
        addNextToken(children, "symbol", tokens.get(0)); // )
        addNextToken(children, "symbol", tokens.get(0)); // ;

        return children;
    }

    public static ArrayList<JackTree> parseIfOrWhileStatement(ArrayList<String> tokens) throws Exception { // both are similar enough it makes sense to combine
        ArrayList<JackTree> children = new ArrayList<JackTree>();

        String statementType = tokens.get(0);
        addNextToken(children, "keyword", tokens.get(0)); // if/while
        addNextToken(children, "symbol", tokens.get(0)); // (
        children.add(new JackTree("expression", parseExpression(tokens))); // expression in the condition
        addNextToken(children, "symbol", tokens.get(0)); // )
        addNextToken(children, "symbol", tokens.get(0)); // {
        children.add(new JackTree("statements", parseStatements(tokens))); // statements content of loop
        addNextToken(children, "symbol", tokens.get(0)); // }

        if (statementType.equals("if") && tokens.size() > 0 && tokens.get(0).equals("else")) {
            addNextToken(children, "keyword", tokens.get(0)); // else
            addNextToken(children, "symbol", tokens.get(0)); // {
            children.add(new JackTree("statements", parseStatements(tokens))); // statements content of else block
            addNextToken(children, "symbol", tokens.get(0)); // }
        }

        return children;
    }

    public static ArrayList<JackTree> parseReturnStatement(ArrayList<String> tokens) throws Exception {
        ArrayList<JackTree> children = new ArrayList<JackTree>();

        addNextToken(children, "keyword", tokens.get(0)); // return
        if (!tokens.get(0).equals(";")) {
            children.add(new JackTree("expression", parseExpression(tokens))); // expression after return
        }
        addNextToken(children, "symbol", tokens.get(0)); // ;

        return children;
    }

    public static ArrayList<JackTree> parseSubroutineDec(ArrayList<String> tokens) throws Exception {
        ArrayList<JackTree> children = new ArrayList<JackTree>();

        addNextToken(children, "keyword", tokens.get(0)); // constructor/function/method
        addNextToken(children, tokens.get(0).equals("void") ? "keyword" : "identifier", tokens.get(0)); // return type
        addNextToken(children, "identifier", tokens.get(0)); // name

        addNextToken(children, "symbol", tokens.get(0)); // (
        children.add(new JackTree("parameterList", parseParameterList(tokens))); // parameter list
        addNextToken(children, "symbol", tokens.get(0)); // )

        // subroutine body parsing - not seperate function bc single use and not too long
        ArrayList<JackTree> bodyChildren = new ArrayList<JackTree>();
        addNextToken(bodyChildren, "symbol", tokens.get(0)); // {
        while (tokens.get(0).equals("var")) {
            bodyChildren.add(new JackTree("varDec", parseVarDec(tokens)));
        }
        bodyChildren.add(new JackTree("statements", parseStatements(tokens)));
        addNextToken(bodyChildren, "symbol", tokens.get(0)); // }
        children.add(new JackTree("subroutineBody", bodyChildren));

        return children;
    }

    public static ArrayList<JackTree> parseParameterList(ArrayList<String> tokens) {
        ArrayList<JackTree> children = new ArrayList<JackTree>();

        if (tokens.get(0).equals(")")) {
            return children;
        }

        while (true) {
            if (tokens.get(0).equals("int") || tokens.get(0).equals("char") || tokens.get(0).equals("boolean")) { // add type keyword/identifier
                addNextToken(children, "keyword", tokens.get(0));
            } else {
                addNextToken(children, "identifier", tokens.get(0));
            }
            addNextToken(children, "identifier", tokens.get(0)); // name of parameter
            if (tokens.get(0).equals(",")) { // if there's a comma add it
                addNextToken(children, "symbol", tokens.get(0)); // ,
            } else {
                break;
            }
        }

        return children;
    }

    public static ArrayList<JackTree> parseExpressionList(ArrayList<String> tokens) throws Exception {
        ArrayList<JackTree> children = new ArrayList<JackTree>();

        if (tokens.get(0).equals(")")) { // if empty return empty array
            return children;
        }

        while (true) { // add each expression while there are more
            children.add(new JackTree("expression", parseExpression(tokens)));
            if (tokens.get(0).equals(",")) {
                addNextToken(children, "symbol", tokens.get(0)); // ,
            } else {
                break;
            }
        }

        return children;
    }

    public static ArrayList<JackTree> parseExpression(ArrayList<String> tokens) throws Exception {
        ArrayList<JackTree> children = new ArrayList<JackTree>();

        while (true) { // add term, if there's an operator add it and then the next term until there's no more
            children.add(new JackTree("term", parseTerm(tokens)));
            if (tokens.size() > 0 && Compiler.contains(opSymbols, tokens.get(0))) {
                addNextToken(children, "symbol", tokens.get(0)); // operator
            } else {
                break;
            }
        }

        return children;
    }

    public static ArrayList<JackTree> parseTerm(ArrayList<String> tokens) throws Exception {
        ArrayList<JackTree> children = new ArrayList<JackTree>();

        if (tokens.get(0).equals("(")) {

            addNextToken(children, "symbol", tokens.get(0)); // (
            children.add(new JackTree("expression", parseExpression(tokens))); // expression in parentheses
            addNextToken(children, "symbol", tokens.get(0)); // )

        } else if (Compiler.contains(unaryOpSymbols, tokens.get(0))) {

            addNextToken(children, "symbol", tokens.get(0)); // unary operator
            children.add(new JackTree("term", parseTerm(tokens))); // term after unary operator

        } else {

            if (tokens.get(0).matches("\\d+")) {
                addNextToken(children, "integerConstant", tokens.get(0)); // integer constant

            } else if (tokens.get(0).startsWith("__STRING")) {
                String strValue = fileStrings.get(Integer.parseInt(tokens.get(0).substring(8, tokens.get(0).length() - 2))); // get number out of ___STRING#___ str & get corresponding string from array
                addNextToken(children, "stringConstant", strValue); // add string value

            } else if (Compiler.contains(keywords, tokens.get(0))) {
                addNextToken(children, "keyword", tokens.get(0)); // if keyword, add it

            } else {
                addNextToken(children, "identifier", tokens.get(0)); // else must be identifier
            }

            if (tokens.size() > 0 && tokens.get(0).equals("[")) {
                addNextToken(children, "symbol", tokens.get(0)); // [
                children.add(new JackTree("expression", parseExpression(tokens))); // expression in array index
                addNextToken(children, "symbol", tokens.get(0)); // ]

            } else if (tokens.size() > 0 && tokens.get(0).equals("(")) {
                addNextToken(children, "symbol", tokens.get(0)); // (
                children.add(new JackTree("expressionList", parseExpressionList(tokens)));
                addNextToken(children, "symbol", tokens.get(0)); // )

            } else if (tokens.size() > 1 && tokens.get(0).equals(".")) {
                addNextToken(children, "symbol", tokens.get(0)); // .
                addNextToken(children, "identifier", tokens.get(0)); // subroutine name
                addNextToken(children, "symbol", tokens.get(0)); // (
                children.add(new JackTree("expressionList", parseExpressionList(tokens))); // expression list in subroutine call
                addNextToken(children, "symbol", tokens.get(0)); // )
            }
        }

        return children;
    }

    public static ArrayList<JackTree> addNextToken(ArrayList<JackTree> arr, String tokenType, String tokenValue) { // add next token to the tree and remove it from tokens list - made bc its more consise than two lines of code everywhere
        arr.add(new JackTree(tokenType, tokenValue));
        tokens.remove(0);
        return arr;
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