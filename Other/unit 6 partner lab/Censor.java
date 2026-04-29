import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

class Censor {

    public static FileWriter fileWriterCensoredText;
    public static FileWriter fileWriterSummary;

    public static void main(String a[]) throws Exception {

        fileWriterCensoredText = new FileWriter("censored_text.txt");
        fileWriterSummary = new FileWriter("summary.txt");

        FileReader fileReader = new FileReader();
        CensoredTable censoredTable = new CensoredTable();

        String fileContents = FileReader.getFileContents();
        System.out.println(fileContents);
        String[] words = splitLine(fileContents);
        System.out.println(Arrays.toString(words));
        String censoredText = "";
        for (String word : words) {
            // System.out.println(word);
            // System.out.println(BannedWords.isBanned(word));

            if (BannedWords.isBanned(word)) {
                censoredText += CensoredTable.getReplacement(word);
            } else {
                censoredText += word;
            }
            System.out.println(censoredText);
        }
        fileWriterCensoredText.write(censoredText);

        fileWriterSummary.write(CensoredTable.getSummary());

        fileWriterCensoredText.close();
        fileWriterSummary.close();
    }

    public static ArrayList<Character> punctuation = new ArrayList<>(Arrays.asList(new Character[]{' ', '.', ',', '!', '?', ';', ':', '\n'}));

    public static String[] splitLine(String line) {
        ArrayList<String> words = new ArrayList<>();
        words.add("");
        for (char c : line.toCharArray()) {
            if (punctuation.contains(c)) {
                words.add(String.valueOf(c));
                words.add("");
            } else {
                words.set(words.size() - 1, words.get(words.size() - 1) + c);
            }
        }
        return words.toArray(new String[0]);
    }
}