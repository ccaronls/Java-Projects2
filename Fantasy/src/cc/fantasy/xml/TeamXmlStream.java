package cc.fantasy.xml;

import java.util.List;

import cc.fantasy.model.Team;
import cc.fantasy.model.TeamPlayer;

import com.thoughtworks.xstream.XStream;

public class TeamXmlStream extends XStream {

    public TeamXmlStream() {
        
        alias("Team", Team.class);
        alias("TeamPlayer", TeamPlayer.class);
        alias("teams", List.class);

        omitField(TeamPlayer.class, "team");

        setMode(XStream.NO_REFERENCES);

    }
    
}
