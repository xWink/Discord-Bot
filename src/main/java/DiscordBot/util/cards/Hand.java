package DiscordBot.util.cards;

import java.util.ArrayList;
import java.util.List;

public class Hand {

    private ArrayList<Card> hand;

    public Hand(){
        reset();
    }

    public void reset(){
        hand = new ArrayList<>();
    }

    public void add(Card card){
        hand.add(card);
    }

    public boolean remove(Card card){
        return hand.remove(card);
    }

    public List<Card> getHand(){
        return hand;
    }

    public int getValue(){
        int value = 0;
        int numAces = 0;

        for (Card card : hand){
            if (card.getRank().equals(CardRank.ACE)){
                numAces++;
            }
            value += card.getRank().getValue();
        }
        while (numAces > 0 && value > 21){
            numAces--;
            value -= 10;
        }
        return value;
    }

    public String showHand(){
        StringBuilder handString = new StringBuilder();

        for (Card card : hand){
            handString.append(card.toEmote()).append(" ");
        }
        return handString.toString();
    }
}
