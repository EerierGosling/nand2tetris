class BannedWords {
    public static final String[] bannedWords = new String[]{
        "crap",
        "gosh",
        "banana",
        "bananas",
        "dumb",
        "boring",
        "ugly",
        "pen",
        "gross",
        "mistake",
        "mistakes",
        "fuzzy"
    };

    public static boolean isBanned(String word) {
        for (String banned : bannedWords) {
            if (banned.equalsIgnoreCase(word)) {
                return true;
            }
        }
        return false;
    }
}