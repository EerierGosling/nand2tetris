import java.util.ArrayList;

public class JackTree { // class for the tree structure of the parsed code
    public boolean isToken; // if true, has a tokenValue and doesn't have children
    public ArrayList<JackTree> children; // children of the node
    public String tokenType; // type of the node (keyword, identifier, etc)
    public String tokenValue; // value if it has no children

    public JackTree(String tokenType, String tokenValue) {
        this.isToken = true;
        this.tokenType = tokenType;
        this.tokenValue = tokenValue;
    }

    public JackTree(String tokenType) {
        this.isToken = false;
        this.tokenType = tokenType;
        this.children = new ArrayList<JackTree>();
    }

    public JackTree(String tokenType, ArrayList<JackTree> children) {
        this.isToken = false;
        this.tokenType = tokenType;
        this.children = children;
    }
}