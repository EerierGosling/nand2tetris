import java.util.Hashtable;

class CensoredTable {

    public static Integer numInOrder = 0;

    public static Hashtable<String, Integer[]> censoredTable = new Hashtable<>();

    public CensoredTable() {
        for (String word : BannedWords.bannedWords) {
            censoredTable.put(word, new Integer[]{0,0});
        }
    }

    public static String getReplacement(String word) {

        censoredTable.get(word)[0] += 1;
        if (censoredTable.get(word)[1] == 0) {
            numInOrder += 1;
            censoredTable.get(word)[1] = numInOrder;
        }


        return "#$@!"+(String.valueOf(censoredTable.get(word)[1]));
    }

    public static String getSummary() {
        String summary = "";
        for (String word : BannedWords.bannedWords) {
            if (censoredTable.get(word)[0] != 0) {
                Integer[] data = censoredTable.get(word);
                summary += word + " - #$@!" + data[1] + " - " + data[0] + " time" + (data[0]==1 ? "" : "s") + "\n";
            }
        }
        return summary;
    }
 

}