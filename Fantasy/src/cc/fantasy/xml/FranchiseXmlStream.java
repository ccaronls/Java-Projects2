package cc.fantasy.xml;

import java.util.List;

import cc.fantasy.model.Franchise;
import cc.fantasy.model.League;
import cc.fantasy.model.Player;
import cc.fantasy.model.Position;
import cc.fantasy.model.Stat;

import com.thoughtworks.xstream.XStream;

public class FranchiseXmlStream extends XStream {
    
    public FranchiseXmlStream() {
        alias("franchises", List.class);
        alias("Franchise", Franchise.class);
        alias("Player", Player.class);
        alias("Position", Position.class);
        alias("Stat", Stat.class);
        
        // omit circular references
        omitField(League.class, "franchise");
        omitField(Player.class, "franchise");
        omitField(Position.class, "franchise");
        omitField(Stat.class, "position");
        omitField(Franchise.class, "numFields");

        setMode(XStream.NO_REFERENCES);     
        
    }
    
}
