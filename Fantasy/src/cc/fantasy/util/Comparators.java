package cc.fantasy.util;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.HashMap;

import cc.fantasy.model.Team;
import cc.fantasy.model.TeamPlayer;

public class Comparators {

	public static Comparator getLeagueRankComparator() {
		if (leagueRankComparator == null) {
			leagueRankComparator = new Comparator() {
				public int compare(Object arg0, Object arg1) {
					Team t0 = (Team)arg0;
					Team t1 = (Team)arg1;
					return t0.getPoints() - t1.getPoints();
				}
			};
		}
		return leagueRankComparator;
	}
	
	public static Comparator getTeamPlayerRankComparator() {
		if (teamPlayerRankComparator==null) {
			teamPlayerRankComparator = new Comparator() {
				public int compare(Object arg0, Object arg1) {
					TeamPlayer t0 = (TeamPlayer)arg0;
					TeamPlayer t1 = (TeamPlayer)arg1;
					
					int r0 = t0.getRank();
					int r1 = t1.getRank();
					
					if (r0 == 0)
						r0 = Integer.MAX_VALUE;
					if (r1 == 0)
						r1 = Integer.MAX_VALUE;
					
					return r0 - r1;
				}				
			};
		}
		return teamPlayerRankComparator;
	}
	
	public static Comparator getTeamRankComparator() {
		if (teamRankComparator == null) {
			teamRankComparator = new Comparator() {
				public int compare(Object arg0, Object arg1) {
					Team t0 = (Team)arg0;
					Team t1 = (Team)arg1;
					return t0.getPoints() - t1.getPoints();
				}
			};
		}
		return teamRankComparator;
	}
	
	private static Field findField(Object o, String field) {
		Field [] fields = o.getClass().getDeclaredFields();
		for (int i=0; i<fields.length; i++) {
			if (fields[i].getName().equalsIgnoreCase(field))
				return fields[i];
		}
		throw new RuntimeException("Cannot find field '" + field + "' for class " + o.getClass());
	}
	
	public static Comparator getObjectFieldComparator(String _fieldName) {
		final String fieldName = _fieldName.toLowerCase();
		if (!objectFieldComparators.containsKey(fieldName)) {
			objectFieldComparators.put(fieldName, new Comparator() {
				public int compare(Object arg0, Object arg1) {					
					Field field = findField(arg0, fieldName);
					try {
						field.setAccessible(true);
						Comparable v0 = (Comparable)field.get(arg0);
						Comparable v1 = (Comparable)field.get(arg1);
						if (v0 == null && v1 == null)
							return 0;
						if (v1 == null)
							return -1;
						if (v0 == null)
							return 1;
						return v0.compareTo(v1);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			});
		}
		return objectFieldComparators.get(fieldName);
	}

	private static Comparator leagueRankComparator=null;
	private static Comparator teamPlayerRankComparator=null;
	private static Comparator teamRankComparator=null;
	private static HashMap<String, Comparator> objectFieldComparators = new HashMap();
}
