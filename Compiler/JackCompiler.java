import java.util.ArrayList;
import java.util.HashMap;

public class JackCompiler {

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
    
}

class JackVariable {
    public String name;
    public String type;
    public Kind kind; 

    public JackVariable(String name, String type, Kind kind) {
        this.name = name;
        this.type = type;
        this.kind = kind;
    }
}