package cc.fantasy.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @hibernate.class table="FAN_POSITION_T"
 * 
 * @author ccaron
 *
 * <pre>
 * TODO: Class Description
 * </pre>
 */
public class Position extends  ModelBase {

    /**
     * @hibernate.id
     *      generator-class="increment"
     *      column="ID"
     * @hibernate.generator-param
     *      name="sequence"
     *      value="FAN_POSITION_ID"
     */
    public Integer getHibernateId() {
        return getId() < 0 ? null : getId();
    }
    
    /**
     * @return Returns the franchise.
     * 
     * @hibernate.many-to-one
     *      class="cc.fantasy.model.Franchise"
     *      column="FRANCHISE_ID"
     */
    public Franchise getFranchise() {
        return franchise;
    }
    
    public void setFranchise(Franchise franchise) {
        this.franchise = franchise;
    }

    /**
     * @hibernate.property column="LONG_NAME"
     * @return
     */
    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    /**
     * @hibernate.property column="NAME"
     * @return
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Stat> getStats() {
    	if (stats == null)
    		stats = new ArrayList();
        return stats;
    }

    public void setStats(List<Stat> stats) {
    	this.stats = replaceList(getStats(), stats);
    }
    
    public void addStat(Stat stat) {
    	addToList(stat, getStats());
    }

    /**
     * @hibernate.property column="PLAYER_COLUMN"
     * @return
     */
	public String getPlayerColumn() {
		return playerColumn;
	}

	public void setPlayerColumn(String playerColumn) {
		this.playerColumn = playerColumn;
	}

    /**
     * @hibernate.property column="SPREADSHEET_FILE"
     * @return
     */
	public String getSpreadSheetFile() {
		return spreadSheetFile;
	}

	public void setSpreadSheetFile(String spreadSheetFile) {
		this.spreadSheetFile = spreadSheetFile;
	}

    public List<String> getOmitStats() {
		if (omitStats == null)
			omitStats = new ArrayList();
		return omitStats;
	}

	public void setOmitStats(List<String> omitStats) {
		this.omitStats = omitStats;
	}
    
    /**
     * @hibernate.property column="OMIT_STATS"
     * @return
     */
    public String getHibernateOmitStats() {
        String s = getOmitStats().toString();
        return s.substring(1, s.length()-1);
    }
    
    public void setHibernateOmitStats(String statsStr) {
        if (statsStr == null)
            statsStr = "";
        setOmitStats(Arrays.asList(statsStr.split("[, ]+")));
    }
    
    public void addOmitStat(String name) {
    	if (!getOmitStats().contains(name))
    		getOmitStats().add(name);
    }
    
    public Stat getStat(String name) {
        Iterator<Stat> it = getStats().iterator();
        while (it.hasNext()) {
            Stat stat = it.next();
            if (stat.getName().equals(name))
                return stat;
        }
        return null;
    }

    void setParent(Object o) {
    	franchise = (Franchise)o;
    }

    // fields used for equals
    String name;

    // fields ommited from equals
    private String longName;
    private String spreadSheetFile;
    private String playerColumn;
    private List<Stat> stats;
    private List<String> omitStats;    
    private Franchise franchise;

}
