package cc.game.kaiser.core;

import java.io.BufferedReader;
import java.io.EOFException;

import cc.lib.utils.Reflector;

/**
 * Keeps track of those variable relevant to a single team.
 * A team has 2 players, a customizable name, a current score and a bid.
 * @author ccaron
 *
 */
public final class Team extends Reflector<Team> {
    static {
        addAllFields(Team.class);
    }
    
    int [] players = new int[2];
    Bid bid = Bid.NO_BID;
    int totalPoints;
    int roundPoints;
    String name;
    
    public Team() {
        this(null);
    }
    
    Team(String name) {
        this.name = name;
    }
    
    public String toString() {
        return "Team " + name + " (" + players[0] + "/" + players[1] + ") " 
                    + (bid != null ? "Bid: " + bid : "")
                    + (" pts: " + totalPoints + " rnd: " + roundPoints);
    }

    public int getPlayerA() {
        return players[0];
    }

    public int getPlayerB() {
        return players[1];
    }
    public String getName() {
        return this.name;
    }
    
    public Bid getBid() {
        return bid;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public int getRoundPoints() {
        return roundPoints;
    }

    void parseTeamInfo(BufferedReader input) throws Exception
    {
        while (true) {
            String line = input.readLine();
            if (line == null)
                throw new EOFException();
    
            if (line.trim().startsWith("}"))
                break;
    
            int colon = line.indexOf(':');
            if (colon < 0)
                throw new Exception("Invalid line.  Not of format: <NAME>:<VALUE>");
    
            String name = line.substring(0, colon).trim();
            String value = line.substring(colon+1).trim();
            
            if (name.equalsIgnoreCase("BID")) {
                this.bid = Bid.parseBid(value);
            } else if (name.equalsIgnoreCase("ROUND_PTS")) {
                this.roundPoints = Integer.parseInt(value);
            }else if (name.equalsIgnoreCase("TOTAL_PTS")) {
                this.totalPoints = Integer.parseInt(line.substring(colon+1));
            } else {
                throw new Exception("Unknown key '" + name + "' while parsing Team");
            }
        }
    }    
};
