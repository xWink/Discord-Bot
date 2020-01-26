package command.util.cards;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public final class PhotoCombine {

    private PhotoCombine() { }

    /**
     * Generates a PNG image in the res/ directory which contains a 2D representation
     * of the ArrayList of cards passed as an argument.
     *
     * @param cards the ArrayList of cards which will be represented in the image
     * @return true if successful, false if something went wrong
     */
    public static boolean genPhoto(ArrayList<Card> cards) {
        try {
            BufferedImage cardPallet = ImageIO.read(new File("../../res/cards/"
                    + cards.get(0).toFileFormat() + ".png"));

            for (int i = 1; i < cards.size(); i++) {
                BufferedImage tmpCard = ImageIO.read(new File("../../res/cards/"
                        + cards.get(i).toFileFormat() + ".png"));
                cardPallet = cards.size() > 4 ? joinBIBig(cardPallet, tmpCard) : joinBISmall(cardPallet, tmpCard);
            }

            cardPallet = crop(cardPallet);
            ImageIO.write(cardPallet, "png", new File("res/out.png"));
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static BufferedImage crop(BufferedImage base) {
        int offset = 0;
        int width = 260;
        int height = base.getHeight() + offset;
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = newImage.createGraphics();
        g2.drawImage(base, null, 0, 0);
        g2.dispose();
        return newImage;
    }

    private static BufferedImage joinBIBig(BufferedImage base, BufferedImage add) {
        int offset = 0;
        int width = base.getWidth() + add.getWidth();
        int height = Math.max(base.getHeight(), add.getHeight()) + offset;
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = newImage.createGraphics();
        g2.drawImage(base, null, 0, 0);
        g2.drawImage(add, null, base.getWidth() / 4 + offset, 0);
        g2.dispose();
        return newImage;
    }

    private static BufferedImage joinBISmall(BufferedImage base, BufferedImage add) {
        int offset = 0;
        int width = base.getWidth() + add.getWidth();
        int height = Math.max(base.getHeight(), add.getHeight()) + offset;
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = newImage.createGraphics();
        g2.drawImage(base, null, 0, 0);
        g2.drawImage(add, null, base.getWidth() + offset, 0);
        g2.dispose();
        return newImage;
    }

    /*DEBUG AND CHECK TO SEE IF ALL PNGS ARE PRESENT
    public static void printClubs() throws IOException {
        ArrayList<String> cards = new ArrayList<>();
        cards.add("AC");
        cards.add("2C");
        cards.add("3C");
        cards.add("4C");
        cards.add("5C");
        cards.add("6C");
        cards.add("7C");
        cards.add("8C");
        cards.add("9C");
        cards.add("10C");
        cards.add("JC");
        cards.add("QC");
        cards.add("KC");
        genPhoto(cards);
    }
    public static void printSpades() throws IOException {
        ArrayList<String> cards = new ArrayList<>();
        cards.add("AS");
        cards.add("2S");
        cards.add("3S");
        cards.add("4S");
        cards.add("5S");
        cards.add("6S");
        cards.add("7S");
        cards.add("8S");
        cards.add("9S");
        cards.add("10S");
        cards.add("JS");
        cards.add("QS");
        cards.add("KS");
        genPhoto(cards);
    }
    public static void printHearts() throws IOException {
        ArrayList<String> cards = new ArrayList<>();
        cards.add("AH");
        cards.add("2H");
        cards.add("3H");
        cards.add("4H");
        cards.add("5H");
        cards.add("6H");
        cards.add("7H");
        cards.add("8H");
        cards.add("9H");
        cards.add("10H");
        cards.add("JH");
        cards.add("QH");
        cards.add("KH");

        genPhoto(cards);
    }
    public static void printDiamonds() throws IOException {
        ArrayList<String> cards = new ArrayList<>();
        cards.add("AD");
        cards.add("2D");
        cards.add("3D");
        cards.add("4D");
        cards.add("5D");
        cards.add("6D");
        cards.add("7D");
        cards.add("8D");
        cards.add("9D");
        cards.add("10D");
        cards.add("JD");
        cards.add("QD");
        cards.add("KD");
        genPhoto(cards);
    }*/
}
