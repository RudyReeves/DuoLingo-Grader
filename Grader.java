import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

// This will be used for the return value of grade(),
// since Java doesn't have Tuples like Python.
class GradeResult {
    boolean isCorrect;
    String blameWord;
    List<List<List<Integer>>> highlights;

    public GradeResult(boolean isCorrect, String blameWord,
            List<List<List<Integer>>> highlights) {
        this.isCorrect = isCorrect;
        this.blameWord = blameWord;
        this.highlights = highlights;
    }

    public String toString() {
        String highlightsString = null;
        if (highlights == null || highlights.size() == 0) {
            highlightsString = "[]";
        } else {
            highlightsString = highlights.toString();
        }
        return "[" + isCorrect + ", " + blameWord + ", " + highlightsString + "]";
    }
}

/*
 * The primary Grader class. The command line arguments are simply
 * forwarded to grade(). Its command line arguments are:
 * 1. The correct answer
 * 2. The student answer
 */
public class Grader {
    public static void main(String[] args) {
        String correctAnswerRaw = args[0];
        String studentAnswerRaw = args[1];
        // (The task spec required grade() to take two arguments.)
        System.out.println(new Grader(correctAnswerRaw, studentAnswerRaw)
            .grade(correctAnswerRaw, studentAnswerRaw));
    }
    
    static final public String TYPO = "typo";
    static final public String MISSING = "missing";
    static final public String WRONG_WORD = "wrong_word";
    
    // The list of words from each raw, *unformatted* answer
    private String[] correctWordsRaw;
    private String[] studentWordsRaw;
    
    // The list of words from each *formatted* answer
    private String[] correctWords;
    private String[] studentWords;
    
    /*
     *  The list of (potential) highlights. It could remain empty.
     *  It is a list of highlight occurrences, in which each
     *  occurrence is a list of two pairs of indices, in which
     *  each pair of indices is a list of two Integers (start and end).
     */
    private List<List<List<Integer>>> highlights = new ArrayList<List<List<Integer>>>();
    
    // Flags for the possible mistakes and correctness of the student answer
    boolean isTypo = false, isMissingWord = false, isWrongWord = false, isCorrect = true;
    
    /*
     * Flag for if the student types an extra word. This is a mistake,
     * but not technically a "blame" as the task specified.
     */
    boolean isExtraWord = false;

    // The English dictionary of words to check for typos
    private String[] dictionary;
    
    public Grader(String correctAnswer, String studentAnswer) {
        try {
            readDictionary();
        } catch (IOException e) {
            printDictionaryReadError();
            e.printStackTrace();
        }
        
        // Split the *raw* answers into arrays of their words
        studentWordsRaw = studentAnswer.split("[\\s]");
        correctWordsRaw = correctAnswer.split("[\\s]");
                
        // Split the *formatted* answers into arrays of their words
        studentWords = format(studentAnswer).split("[\\s]");
        correctWords = format(correctAnswer).split("[\\s]");
    }
    
    /*
     *  Reads in the dictionary file, located at ./dictionary.txt
     *  If that's not found, it tries ./src/dictionary.txt
     *  If that's not found, it tries /usr/share/dict/words
     *  If that's not found, an error message is emitted
     */
    private void readDictionary() throws IOException {
        String filename = "dictionary.txt";
        String absolutePath = new File("").getAbsolutePath();
        FileReader fileReader = null;
        
        // Try ./dictionary.txt
        try {
            fileReader = new FileReader(absolutePath.concat("/" + filename));
        } catch (Exception e1) {
            // Try ./src/dictionary.txt
            try {
                fileReader = new FileReader(
                        absolutePath.concat("/src/" + filename));
            } catch (Exception e2) {
                // Try /usr/share/dict/words
                try {
                    fileReader = new FileReader("/usr/share/dict/words");
                } catch (Exception e3) {
                    // Couldn't read dictionary; print an error message
                    printDictionaryReadError();
                }
            }
        }
        
        // We have a fileReader, so let's read it into the dictionary
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        List<String> lines = new ArrayList<String>();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            lines.add(line);
        }
        bufferedReader.close();
        dictionary = lines.toArray(new String[lines.size()]);
    }
    
    private void printDictionaryReadError() {
        System.out.println("Couldn't read the dictionary file. "
                + "Please place 'dictionary.txt' in the "
                + "same directory as 'Grader.java'.");
    }

    // Lowercases str, eliminates punctuation, etc
    private String format(String str) {
        return str.toLowerCase().replaceAll("[^\\w\\s]", "") .replaceAll("\\s+", " ").trim();
    }

    /*
     * Returns how many letters appear in one of the raw answers,
     * up until the 'n'th word, where 'n' is given as wordIndex.
     * If inCorrectAnswerRaw is true, it will do this for the
     * correctAnswerRaw String, otherwise it will use
     * the studentAnswerRaw String. 
     */
    private int numCharsUpToWord(boolean inCorrectAnswerRaw, int wordIndex) {
        String[] words = (inCorrectAnswerRaw ? correctWordsRaw : studentWordsRaw);
        
        int sum = 0;
        for (int i = 0; i < wordIndex; i++) {
            sum += words[i].length()
                    + (wordIndex == 0 ? 0 : 1); // To account for the space
        }
        return sum;
    }
    
    /*
     * Takes highlight endpoints and inserts them into the highlights array.
     * If correctStart or studentStart is null, empty arrays will be inserted
     * into the respective highlight position.
     */
    private void insertHighlight(Integer correctStart, Integer correctEnd,
            Integer studentStart, Integer studentEnd) {
        // Pair the correct answer's highlight endpoints into an ArrayList
        List<Integer> correctPair = new ArrayList<Integer>();
        if (correctStart != null) {
            correctPair.add(correctStart);
            correctPair.add(correctEnd);
        }
        
        // Pair the student's answer's highlight endpoints into an ArrayList
        List<Integer> studentPair = new ArrayList<Integer>();
        if (studentStart != null) {
            studentPair.add(studentStart);
            studentPair.add(studentEnd);
        }
        
        // Pair the correct and student highlight endpoint arrays
        List<List<Integer>> pair = new ArrayList<List<Integer>>();
        pair.add(correctPair);
        pair.add(studentPair);
        
        // Add the pair to the highlights array
        highlights.add(pair);
    }

    // Returns true if and only if the word is in the dictionary
    private boolean isValidWord(String word) {
        for (String dictWord : dictionary) {
            if (word.equals(dictWord)) {
                return true;
            }
        }
        return false;
    }

    // The primary grade() function requested in the task
    GradeResult grade(String correctAnswerRaw, String studentAnswerRaw) {
        // Check for empty-string cases
        if (correctAnswerRaw.trim().length() == 0) {
            if (studentAnswerRaw.trim().length() == 0) {
                return new GradeResult(true, null, highlights);
            } else {
                insertHighlight(null, null, 0, studentWords[0].length());
                return new GradeResult(false, null, highlights);
            }
        } else if (studentAnswerRaw.length() == 0) {
            /*
             * The student answer was (incorrectly) blank, but
             * they may have missed multiple words. Let's check
             */
            String blameString = null;
            if (correctWords.length == 1) {
                blameString =  MISSING;
                insertHighlight(0, correctWords[0].length(), 0, 0);
            }
            return new GradeResult(false, blameString, highlights);
        }
        
        // Check for a "missing" (or an extra) word
        int wordCountDifference = correctWords.length - studentWords.length;
        
        if (wordCountDifference < 0) {
            // There is an extra word; let's find it
            isCorrect = false;
            isExtraWord = true;
            
            int extraWordIndex = 0;
            while (extraWordIndex < correctWords.length &&
                    studentWords[extraWordIndex]
                    .equals(correctWords[extraWordIndex])) {
                extraWordIndex++;
            }
            
            // The extra word
            String extraWordString = studentWords[extraWordIndex];
            
            // Add highlight endpoints
            int extraWordStart = numCharsUpToWord(false, extraWordIndex);
            int extraWordEnd = extraWordStart + extraWordString.length();
            insertHighlight(null, null, extraWordStart, extraWordEnd);
        } else if (wordCountDifference != 0) {
            // There is a "missing" word
            isCorrect = false;
            
            if (wordCountDifference == 1) {
                // There's only one "missing" word, so we need to
                // report a "missing" word and highlight endpoints
                isMissingWord = true;

                // The index of the "missing" word in the *correct* answer
                Integer correctMissingWordIndex = null;
                
                /*
                 * To check for a "missing" word, we'll check
                 * the following cases, in order:
                 * 1. If it was the first word
                 * 2. If it was neither the first or last word
                 * 3. If it was the last word
                 */
                
                // Check if the student missed the first word
                if (studentWords[0].equals(correctWords[1])) {
                    correctMissingWordIndex = 0;
                } else {
                    // They didn't miss the first word
                    for (int i = 0; i < correctWords.length && i < studentWords.length - 1; i++) {
                        if (!studentWords[i + 1].equals(correctWords[i + 1])) {
                            // 'i + 1' is the "missing" word's index
                            correctMissingWordIndex = i + 1;
                            break;
                        }
                    }
                }
                
                if (correctMissingWordIndex == null) {
                    // The "missing" word was the last word
                     correctMissingWordIndex = correctWords.length - 1;
                }

                // Find the start of the correct answer's "missing" word
                String correctMissingWordString = correctWords[correctMissingWordIndex];
                int correctStart = numCharsUpToWord(true, correctMissingWordIndex);
                
                // Find the start of the student's answer's "missing" word
                int studentStart = numCharsUpToWord(false, correctMissingWordIndex);
                
                // Add highlight endpoints
                insertHighlight(correctStart, correctMissingWordString.length() + correctStart,
                        studentStart, studentStart);
            }
        } else {
            // Check for a "wrong_word" or a "typo"

            /* If this variable is true, the for loop 'break's
             * after adding highlight endpoints.
             * (This is for typos with edit distance > 1).
             */
            boolean breakAfterAddingHilight = false;
            
            for (int i = 0; i < correctWords.length; i++) {
                if (!studentWords[i].equals(correctWords[i])) {
                    // This is an incorrect word, but is it a valid English word?
                    if (!isValidWord(studentWords[i])) {
                        // It's not an English word, so it's a "typo"
                        // But it's "correct" if the edit distance equals 1, so let's check
                        if (editDistance(studentWords[i], correctWords[i]) > 1) {
                            isWrongWord = true;
                            isTypo = false;
                            isCorrect = false;
                            highlights.clear();
                            breakAfterAddingHilight = true;
                        } else {
                            isTypo = true;
                        }
                    } else {
                        // It's a valid English word, so it's a "wrong_word"
                        if (isWrongWord) {
                            // This is not the first "wrong_word", so
                            // the blameString should be null (aka 'None').
                            isWrongWord = false;
                            isCorrect = false;
                            break;
                        } else {
                            isWrongWord = true;
                            isCorrect = false;
                        }
                    }
                    
                    // Find the start of the correct answer's blamed word
                    int correctStart = numCharsUpToWord(true, i);
                    
                    // Find the start of the student answer's blamed word
                    int studentStart = numCharsUpToWord(false, i);
                    
                    // Add highlight endpoints
                    insertHighlight(correctStart, correctWords[i].length() + correctStart,
                            studentStart, studentWords[i].length() + studentStart);
                    
                    // (Again, this is for "typo"s with edit distance > 1).
                    if (breakAfterAddingHilight) {
                        break;
                    }
                }
            }
        }

        // Construct the String value for the blame result
        String blameString = null;
        if (isTypo) {
            blameString = TYPO;
        } else if (isMissingWord) {
            blameString = MISSING;
        } else if (isWrongWord) {
            blameString = WRONG_WORD;
        }
        
        /* If blameString wasn't assigned a value, it's null (aka 'None').
         * In that case we don't want to output any highlight endpoints.
         * (Unless the student's answer contained an extra word.)
         */
        if (blameString == null && !isExtraWord) {
            highlights.clear();
        }

        return new GradeResult(isCorrect, blameString, highlights);
    }

    /*
     * Damerau-Levenshtein minimum edit distance.
     * This implementation uses dynamic programming
     * techniques, authored by Kevin Stern,
     * available publicly on GitHub.
     */
    private int editDistance(String source, String target) {
        int insertCost = 1, deleteCost = 1, replaceCost = 1, swapCost = 1;

        if (source.length() == 0) {
            return target.length() * insertCost;
        }
        if (target.length() == 0) {
            return source.length() * deleteCost;
        }
        int[][] table = new int[source.length()][target.length()];
        Map<Character, Integer> sourceIndexByCharacter = new HashMap<Character, Integer>();
        if (source.charAt(0) != target.charAt(0)) {
            table[0][0] = Math.min(replaceCost, deleteCost + insertCost);
        }
        sourceIndexByCharacter.put(source.charAt(0), 0);
        for (int i = 1; i < source.length(); i++) {
            int deleteDistance = table[i - 1][0] + deleteCost;
            int insertDistance = (i + 1) * deleteCost + insertCost;
            int matchDistance = i * deleteCost
                    + (source.charAt(i) == target.charAt(0) ? 0 : replaceCost);
            table[i][0] = Math.min(Math.min(deleteDistance, insertDistance),
                    matchDistance);
        }
        for (int j = 1; j < target.length(); j++) {
            int deleteDistance = (j + 1) * insertCost + deleteCost;
            int insertDistance = table[0][j - 1] + insertCost;
            int matchDistance = j * insertCost
                    + (source.charAt(0) == target.charAt(j) ? 0 : replaceCost);
            table[0][j] = Math.min(Math.min(deleteDistance, insertDistance),
                    matchDistance);
        }
        for (int i = 1; i < source.length(); i++) {
            int maxSourceLetterMatchIndex = source.charAt(i) == target
                    .charAt(0) ? 0 : -1;
            for (int j = 1; j < target.length(); j++) {
                Integer candidateSwapIndex = sourceIndexByCharacter.get(target
                        .charAt(j));
                int jSwap = maxSourceLetterMatchIndex;
                int deleteDistance = table[i - 1][j] + deleteCost;
                int insertDistance = table[i][j - 1] + insertCost;
                int matchDistance = table[i - 1][j - 1];
                if (source.charAt(i) != target.charAt(j)) {
                    matchDistance += replaceCost;
                } else {
                    maxSourceLetterMatchIndex = j;
                }
                int swapDistance;
                if (candidateSwapIndex != null && jSwap != -1) {
                    int iSwap = candidateSwapIndex;
                    int preSwapCost;
                    if (iSwap == 0 && jSwap == 0) {
                        preSwapCost = 0;
                    } else {
                        preSwapCost = table[Math.max(0, iSwap - 1)][Math.max(0,
                                jSwap - 1)];
                    }
                    swapDistance = preSwapCost + (i - iSwap - 1) * deleteCost
                            + (j - jSwap - 1) * insertCost + swapCost;
                } else {
                    swapDistance = Integer.MAX_VALUE;
                }
                table[i][j] = Math.min(
                        Math.min(Math.min(deleteDistance, insertDistance),
                                matchDistance), swapDistance);
            }
            sourceIndexByCharacter.put(source.charAt(i), i);
        }
        return table[source.length() - 1][target.length() - 1];
    }
}