package cc.game.soc.core;

import cc.game.soc.android.R;

/**
 * Event cards are a variation that replace the dice.
 * 
 * @author chriscaron
 *
 */
public enum EventCardType implements ICardType<Void> {
	
	RobberAttack	(R.string.event_card_robber_attack, R.string.event_card_robber_attack_help, 7, 7, 7, 7, 7),
	Epidemic		(R.string.event_card_epidemic, R.string.event_card_epidemic_help, 6, 8),
	Earthquake		(R.string.event_card_earthquake, R.string.event_card_earthquake_help, 6),
	GoodNeighbor	(R.string.event_card_good_neighbor, R.string.event_card_good_neighbor_help, 6),
	Tournament		(R.string.event_card_tournament, R.string.event_card_tournament_help, 5),
	TradeAdvantage	(R.string.event_card_trade_advantage, R.string.event_card_trade_advantage_help, 5),
	CalmSea			(R.string.event_card_calm_sea, R.string.event_card_calm_sea_help, 9, 12),
	RobberFlees		(R.string.event_card_robber_flees, R.string.event_card_robber_flees_help, 4, 4),
	NeighborlyAssistance(R.string.event_card_neighborly_assistance, R.string.event_card_neighborly_assistance_help, 10, 11),
	Conflict		(R.string.event_card_conflict, R.string.event_card_conflict_help, 3),
	PlentifulYear	(R.string.event_card_plentiful_year, R.string.event_card_plentiful_year_help, 2),
	NoEvent			(R.string.event_card_no_event, R.string.event_card_no_event_help, 3, 4, 5, 5, 6, 6, 6, 8, 8, 8, 8, 9, 9, 9, 10, 10, 11),
	;

	final int nameId;
	final int helpTextId;
	final int [] production;
	
	EventCardType(int nameId, int helpTextId, int cakEventDie, int ... productionDie) {
		this.nameId = nameId;
		this.helpTextId = helpTextId;
		this.production = productionDie;
	}
	
	@Override
	public CardType getCardType() {
		return CardType.Event;
	}

	@Override
	public String getHelpText(Rules rules, StringResource sr) {
		return sr.getString(helpTextId);
	}

	@Override
	public Void getData() {
		return null;
	}

	@Override
	public CardStatus defaultStatus() {
		return CardStatus.USED;
	}

    @Override
    public String getName(StringResource sr) {
        return sr.getString(nameId);
    }
}
