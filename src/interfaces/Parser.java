package interfaces;

public interface Parser {

    /**
     * Returns the array of strings of every
     * word separated by space in a string.
     *
     * @param input the string being parsed
     * @return the array of strings being parsed
     */
    static String[] splitBySpaces(String input) {
        return input.split(" ");
    }
}
