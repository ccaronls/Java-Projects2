package cc.fantasy.xml;

import java.util.List;

import cc.fantasy.model.User;

import com.thoughtworks.xstream.XStream;

public class UserXmlStream extends XStream {

	public UserXmlStream() {
        alias("users", List.class);
        alias("User", User.class);
        omitField(User.class, "numLeagues");
        omitField(User.class, "numTeams");
        setMode(XStream.NO_REFERENCES);
	}
	
}
