package cc.game.soc.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class EventCard extends Card {

	private int production;
	private int cakEvent;

	@Override
	public void serialize(PrintWriter out) throws IOException {
		super.serialize(out);
		out.println("production="+production);
		out.println("cakEvent="+cakEvent);
	}

	private String afterEquals(String s) {
		return s.substring(s.indexOf('=')+1);
	}
	
	@Override
	protected void deserialize(BufferedReader in) throws Exception {
		super.deserialize(in);
		production = Integer.parseInt(afterEquals(in.readLine()));
		cakEvent = Integer.parseInt(afterEquals(in.readLine()));
	}

	public EventCard() {
		super();
		production = cakEvent = -1;
	}
	
	public EventCard(EventCardType t, int production, int cakEvent) {
		super(t);
		this.production = production;
		this.cakEvent = cakEvent;
	}

	public final int getProduction() {
		return production;
	}

	public final int getCakEvent() {
		return cakEvent;
	}
	
	public EventCardType getType() {
		return EventCardType.values()[getTypeOrdinal()];
	}
	
}
