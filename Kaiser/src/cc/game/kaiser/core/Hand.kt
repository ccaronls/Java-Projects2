package cc.game.kaiser.core;

import java.util.Arrays;
import java.util.Comparator;

import cc.lib.utils.Reflector;

public final class Hand extends Reflector<Hand> {
    
    static {
        addAllFields(Hand.class);
    }
    
    public static final int MAX_HAND_CARDS = 8;

    final Card[] mCards;
    int mNumCards = 0;

    public Hand() {
        mCards = new Card[MAX_HAND_CARDS];
    }
    
    public void addCard(Card card) {
        mCards[mNumCards++] = card;
    }

    public void setCard(int index, Card card) {
        mCards[index] = card;
        if (mNumCards <= index)
            mNumCards = index+1;
    }
    
    public int getNumCards() {
        return mNumCards;
    }

    public Card getCard(int index) {
        return mCards[index];
    }

    private static Comparator<Card> defaultCompare =  new Comparator<Card>() {

        @Override
        public int compare(Card c0, Card c1) {
            return c1.rank.ordinal() - c0.rank.ordinal();
        }
        
    };
    
    public void sort() {
        Arrays.sort(mCards, 0, mNumCards, defaultCompare);
    }

    public void sort(Comparator<Card> comp) {
        Arrays.sort(mCards, 0, mNumCards, comp);
    }

    public void clear() {
        mNumCards = 0;
    }

    public void remove(int index) {
        mCards[index] = mCards[--mNumCards];
    }

    @Override
    public String toString() {
        return Arrays.toString(mCards);
    }

    @Override
    public boolean equals(Object o) {
        Hand hand = (Hand) o;
        return Arrays.equals(mCards, hand.mCards);
    }

    public void remove(Card card) {
        for (int i=0; i<mNumCards; i++)
            if (mCards[i].equals(card)) {
                mCards[i] = mCards[--mNumCards];
                break;
            }
    }
    
    public Card [] getCards() {
        Card [] cards = new Card[mNumCards];
        for (int i=0; i<mNumCards; i++)
            cards[i] = mCards[i];
        return cards;
    }
};
