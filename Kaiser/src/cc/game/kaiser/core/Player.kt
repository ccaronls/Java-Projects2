package cc.game.kaiser.core;

import cc.lib.utils.Reflector;

/**
 * base class for a KaiserPlayer
 * @author ccaron
 *
 */
public abstract class Player extends Reflector<Player> {

    static {
        addAllFields(Player.class);
    }
    
    public static final int HAND_SIZE = 8;
    public static final int MAX_PLAYER_TRICKS = 8;

    String mName;
    int mTeam;
    Hand mHand = new Hand();
    int playerNum = -1;

    Hand[] mTricks = new Hand[MAX_PLAYER_TRICKS];
    int mNumTricks = 0;

    /**
     * 
     * @param name
     */
    protected Player(String name) {
        mName = (name);
    }

    /**
     * Callback when a new game has started. Default impl does nothing.
     * 
     * @param k
     */
    public void onNewGame(Kaiser k) {
    }

    /**
     * handle when a new round is started. kaiser state is NEW_ROUND Default
     * impl does nothing.
     * 
     * @param k
     */
    public void onNewRound(Kaiser k) {
    }

    /**
     * handle when this player wins a trick. kaiser state is PROCESS_TRICK
     * Default impl does nothing.
     * 
     * @param k
     */
    public void onWinsTrick(Kaiser k, Hand trick) {
    }

    /**
     * handle for every card this player is dealt. kaiser state is DEAL Default
     * impl does nothing.
     * 
     * @param c
     */
    public void onDealtCard(Kaiser k, Card c) {
    }

    /**
     * handle for end of trick processing. kaiser state is PROCESS_TRICK Default
     * impl does nothing.
     * 
     * @param kaiser
     * @param trick
     * @param reciever
     * @param pointsInTrick
     */
    public void onProcessTrick(Kaiser kaiser, Hand trick, Player reciever,
            int pointsInTrick) {
    }

    /**
     * handle for end of round processing. kaiser state is PROCESS_ROUND Default
     * impl does nothing.
     * 
     * @param k
     */
    public void onProcessRound(Kaiser k) {
    }

    /**
     * Returns a value from the options array. returning null will result in no
     * advance in state. This is to support integrations that do not want this
     * method to ever block.
     * 
     * @param kaiser
     * @param options
     * @param numOptions
     * @return
     */
    public abstract Card playTrick(Kaiser kaiser, Card [] options);

    /**
     * Returning NULL will result in no advancement of state. This is to support
     * integrations that do not want this method to ever block.
     * 
     * @param kaiser
     * @param numOptions
     * @return
     */
    public abstract Bid makeBid(Kaiser kaiser, Bid [] options);

    /**
     * 
     * @return
     */
    public final int getPlayerNum() {
        return playerNum;
    }

    /**
     * 
     * @return
     */
    public final int getNumCards() {
        return mHand.getNumCards();
    }

    /**
     * 
     * @param index
     * @return
     */
    public final Card getCard(int index) {
        return mHand.getCard(index);
    }

    /**
     * Return a copy if this hand to avoid invalidating the game.
     * @return
     */
    public final Hand getHand() {
        return mHand.deepCopy();
    }

    /**
     * 
     * @return
     */
    public final int getTeam() {
        return mTeam;
    }

    /**
     * 
     * @return
     */
    public final String getName() {
        return mName;
    }

    /**
     * 
     * @param nm
     */
    public final void setName(String nm) {
        mName = nm;
    }

    /**
     * 
     * @return
     */
    public final int getNumTricks() {
        return mNumTricks;
    }

    /**
     * 
     * @param index
     * @return
     */
    public final Hand getTrick(int index) {
        return mTricks[index];
    }

    /**
     * 
     * @return
     *
    public Player getTeammate() {
        if (this.mTeam.getPlayerA() == this)
            return mTeam.getPlayerB();
        return mTeam.getPlayerA();
    }

    /**
     * 
     * @return
     */
    public final Hand [] getTricks() {
        return mTricks;
    }
}
