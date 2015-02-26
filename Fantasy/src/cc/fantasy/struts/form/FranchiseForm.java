package cc.fantasy.struts.form;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletRequest;

import org.apache.struts.action.ActionMapping;

import cc.fantasy.model.Franchise;
import cc.fantasy.model.Player;
import cc.fantasy.model.Position;
import cc.fantasy.model.Stat;
import cc.fantasy.struts.util.Common;

public class FranchiseForm extends BaseForm {

	// for listing
    private List franchises;

    // for edit
    private String name;
    private String category;
    private List categories;
    private int franchiseId=-1;
    private boolean active;
    private int playerId=-1;
    private String position;
    private String positionNameLong;
    private String spreadSheetFile;
    private String playerColumn;
    
    private List<Player> players        = new ArrayList();
    private List<Position> positions    = new ArrayList();
    private List<Stat> stats            = new ArrayList();
    private List<String> omitFields     = new ArrayList();
    
    public void reset(ActionMapping arg0, ServletRequest arg1) {
        super.reset(arg0, arg1);
        position = null;
        positionNameLong = null;
        spreadSheetFile = null;
        playerColumn = null;
        stats = new ArrayList();
    }

    public List getFranchises() {
        return franchises;
    }

    public void setFranchises(List franchises) {
        this.franchises = franchises;
    }

	public List getCategories() {
		return categories;
	}

	public void setCategories(List categories) {
		this.categories = categories;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public int getFranchiseId() {
		return franchiseId;
	}

	public void setFranchiseId(int id) {
		this.franchiseId = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public boolean getActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	public int getPlayerId() {
		return playerId;
	}

	public void setPlayerId(int playerId) {
		this.playerId = playerId;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}
    
	public String getPlayerColumn() {
        return playerColumn;
    }

    public void setPlayerColumn(String playerColumn) {
        this.playerColumn = playerColumn;
    }

    public String getPositionNameLong() {
        return positionNameLong;
    }

    public void setPositionNameLong(String positionNameLong) {
        this.positionNameLong = positionNameLong;
    }

    public String getSpreadSheetFile() {
        return spreadSheetFile;
    }

    public void setSpreadSheetFile(String spreadSheetFile) {
        this.spreadSheetFile = spreadSheetFile;
    }

    public List<Player> getPlayers() {
		return players;
	}

	public void setPlayers(List<Player> players) {
		this.players = players;
	}

	public List<Position> getPositions() {
		return positions;
	}

	public void setPositions(List<Position> positions) {
		this.positions = positions;
	}

    public List<Stat> getStats() {
        return stats;
    }

    public void setStats(List<Stat> stats) {
        this.stats = stats;
    }
    
    public List<String> getOmitFields() {
        return omitFields;
    }

    public void setOmitFields(List<String> omitFields) {
        this.omitFields = omitFields;
    }

    public String getStatsString(Position position) {
    	return Common.makeStatsString(position.getStats());
    }
    
    public int getNumLeagues(Franchise franchise) {
    	return getService().getLeaguesCount(franchise);
    }
}
