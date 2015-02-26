package cc.fantasy.xml;

import java.util.List;

import cc.fantasy.model.League;
import cc.fantasy.model.LeaguePosition;
import cc.fantasy.model.LeagueStat;
import cc.fantasy.model.Stat;

import com.thoughtworks.xstream.XStream;

public class LeagueXmlStream extends XStream {

	public LeagueXmlStream() {
		alias("leagues", List.class);
		alias("League", League.class);
		alias("LeaguePosition", LeaguePosition.class);
		alias("LeagueStat", LeagueStat.class);
        
        // omit circular references
		omitField(LeaguePosition.class, "league");
		omitField(LeagueStat.class, "position");
		omitField(Stat.class, "position");

		setMode(XStream.NO_REFERENCES);

	}
}
