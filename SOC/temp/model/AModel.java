package cc.game.soc.model;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

abstract class AModel implements Comparable<AModel> {

	private int id = -1;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	@Override
	public int compareTo(AModel model) {
		return model.id - id;
	}

	@Override
	public boolean equals(Object arg0) {
		return getClass().equals(arg0.getClass()) && ((AModel)arg0).id == id;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString();
	}

	public static <T>String toTable(List<T> list) {
		if (list == null || list.size() == 0)
			return "EMPTY LIST";

		Set<Field> allFields = new HashSet<Field>();
		
		Class<?> clazz = list.get(0).getClass();
		while (clazz != null && !clazz.equals(Object.class)) {
			allFields.addAll(Arrays.asList(clazz.getDeclaredFields())); 
			clazz = clazz.getSuperclass();
		}

		// generate a array of the width of each column
		int [] colWidths = new int[allFields.size()];
		
		String [][] table = new String[list.size()+1][];
		
		table[0] = new String[allFields.size()];
		
		// first do the header
		{
			int colIndex = 0;
			for (Field f : allFields) {
				colWidths[colIndex] = f.getName().length();
				table[0][colIndex] = f.getName();
				colIndex ++;
			}
		}
		
		int rowIndex = 1;
		for (T t : list) { 
		
			table[rowIndex] = new String[allFields.size()]; 
			
			Iterator<Field> it = allFields.iterator();
			int colIndex = 0;
			while (it.hasNext()) {
				String val = "null";
				try {
					Object obj = it.next().get(t);
					if (obj != null) {
						if (obj instanceof AModel) {
							val = String.valueOf(((AModel)obj).getId());
						} else {
							val = obj.toString();
						}
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				table[rowIndex][colIndex] = val;
				colWidths[colIndex] = Math.max(colWidths[colIndex], val.length());
				colIndex ++;
			}
			rowIndex++;
		}

		StringBuffer buf = new StringBuffer();
		for (int i=0; i<table.length; i++) {
			// each row
			for (int ii=0; ii<table[i].length; ii++) {
				// each col
				if (colWidths[ii] > 0)
					buf.append(String.format("%-" + colWidths[ii] + "s ", table[i][ii]));
			}
			buf.append("\n");
		}

		return buf.toString();
	}
}
