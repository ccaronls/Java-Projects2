package cc.fantasy.model;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public abstract class ModelBase implements Serializable {

	static private class FormattedList extends ArrayList {
	
		private final static String [] indent = new String[30];

	    static {
	    	String s = "";
	    	for (int i=0; i<indent.length; i++) {
	    		indent[i] = s;
	    		s += "  ";
	    	}
	    }
	    
	    private static String getIndent(int depth) {
	        if (depth >= indent.length)
	            return indent[indent.length-1];
	        return indent[depth];
	    }

	    static int depth = 0;
		FormattedList(Collection items) {
			super(items);
		}
		
		public String toString() {
			StringBuffer buf = new StringBuffer();
			Iterator it = iterator();
			depth++;
			while (it.hasNext()) {
				buf.append("\n" + 
						getIndent(depth) + "[" + it.next() + getIndent(depth) + "]");
			}
			depth--;
			return buf.toString();
		}
		
	}
	
//  class for displaying a hashmap as a tree instead of big garbled mess
    static private class FormatedHashMap extends HashMap
    {
        public String toString() {
            Object [] array = keySet().toArray(new Object[0]);
            Arrays.sort(array);
            StringBuffer buf = new StringBuffer();
            for (int i=0; i<array.length; i++) {
                Object key = array[i];
                Object val = this.get(key);
                buf.append(FormattedList.getIndent(FormattedList.depth++) + key + "=" + val + "\n");
                FormattedList.depth--;                
            }
            return "\n" + buf.toString();
        }
    }

    private int id = -1;
    
    public int getId() {
    	return id;
    }
    
    public void setId(int id) {
    	this.id = id;
    }
    
    public void setHibernateId(Integer id) {
        setId(id == null ? -1 : id.intValue());
    }

    public String toString() {
        Class thisClass = getClass();
        HashMap fieldValuesMap = new FormatedHashMap();
        while (thisClass != null) {
            Field[] fields = thisClass.getDeclaredFields();
            String fieldName = null;
            Object fieldValue = null;
     
            for (int i = 0; i < fields.length; i++) {
                try {
                	fields[i].setAccessible(true);
                    fieldName = fields[i].getName();
                    fieldValue = fields[i].get(this);
                    if (fieldValue == null) {
                        fieldValue = new String("null");
                    } else if (fieldValue instanceof ModelBase) {
                    	fieldValue = fieldValue.getClass();
                    } else if (fieldValue instanceof Collection) {
                    	fieldValue = new FormattedList((Collection)fieldValue);
                    }
                    
                    fieldValuesMap.put(fieldName, fieldValue);
                } catch (IllegalAccessException ignore) {
                    // ignore private members
                }
            }
            
            thisClass = thisClass.getSuperclass();
        }
        
        return getClass().getName() + fieldValuesMap;
    }
    
    public boolean equals(Object o) {
        if (o == this)
            return true;
        
        if (!o.getClass().equals(getClass()))
            return false;
        
        // compare ids if they exist and they have been set correctly
        try {
        	
        	int myId = getId();
        	int hisId = ((ModelBase)o).getId();
        
        	if (myId < 0 || hisId < 0)
        		throw new RuntimeException();
        	
        	return myId == hisId;
        	
        } catch (Exception e) {
        }
        
        Field[] fields = getClass().getDeclaredFields();
        Object myFieldValue = null;
        Object hisFieldValue = null;
        
        for (int i = 0; i < fields.length; i++) {
            try {
            	
            	//String name = fields[i].getName();
            	
            	// compare fields that are not private (ids and back references)
                myFieldValue = fields[i].get(this);
                hisFieldValue = fields[i].get(o);
 
                if (myFieldValue == null && hisFieldValue == null)
                	continue;
                
                if (myFieldValue == null || hisFieldValue == null)
                	return false;
                
                if (!myFieldValue.equals(hisFieldValue))
                	return false;
                
            } catch (IllegalAccessException ignore) {
                // ignore private members
            }
            
        }

        return true;
    }

    public int find(List list) {
    	int index = 0;
    	Iterator it = list.iterator();
    	while (it.hasNext()) {
    		Object o = it.next();
    		if (o.equals(this))
    			return index;
    		index ++;
    	}
    	return -1;
    }

    public <T extends ModelBase> List<T> replaceList(List<T> oldList, List<T> newList) {

    	newList.remove(null);
    	
    	// iterate over the old list and copy the id over if it is found
    	Iterator<T> it = newList.iterator();
    	while (it.hasNext()) {
    		T x = it.next();
    		x.setParent(this);
    		int index = x.find(oldList);
    		if (index >= 0) {
    			ModelBase y = oldList.get(index);
    			x.setId(y.getId());
    		}
    	}    	
    	
    	return newList;
    }
    
    public <T extends ModelBase> void addToList(T x, List<T> list) {
    	int index = x.find(list);
    	if (index >= 0) {
    		ModelBase y = list.get(index);
    		x.setId(y.getId());
    		list.remove(index);
    	}
		list.add(x);
		x.setParent(this);
    }
    
    abstract void setParent(Object o);
}
