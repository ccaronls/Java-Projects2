package cc.lib.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class MyArchivable extends Reflector<MyArchivable> {
    
    SomeEnum myEnum = SomeEnum.ENUM1;
    String myString;
    int myInt;
    Integer myInteger;
    boolean myBool;
    Boolean myBoolean;
    float myFloat;
    Float myFloatingNum;
    long myLong;
    Long myLongNum;
    double myDouble;
    Double myDoubleNum;
    MyArchivable myArchivable;
    List<String> myList = new LinkedList<>();
    int [] myArray = { 1,2,3,4,5 };
    String [] myStringArray = { "a", null, "b", "c" };
    String [] myEmptyStringArray = { };
    String [] myNullStringArray = null;
    
    int [] myIntArray = { 1, 2, 3, 4 };
    int [] myEmptyIntArray = { };
    int [] myNullIntArray = null;
    
    float [] myFloatArray = { 10,11,12,13,14 };
    float [] myEmptyFloatArray = { };
    float [] myNullFloatArray = null;
    
    long [] myLongArray = { 1000,2000,3000,4000 };
    long [] myEmptyLongArray = { };
    long [] myNullLongArray = null;
    
    double [] myDoubleArray = { 11111,22222,33333,444444,555555 };
    double [] myEmptyDoubleArray = { };
    double [] myNullDoubleArray = null;
    
    boolean [] myBooleanArray = { true, false, false, true };
    boolean [] myEmptyBooleanArray;// = { };
    boolean [] myNullBooleanArray = null;
    
    SomeEnum [] myEnumArray = { SomeEnum.ENUM2, SomeEnum.ENUM3, SomeEnum.ENUM1 };
    SomeEnum [] myEmptyEnumArray = { };
    SomeEnum [] myNullEnumArray = null;
    
    Reflector [] myArchivableArray = null;
    Reflector [] myEmptyArchivableArray = { };
    Reflector [] myNullArchivableArray = null;

    Reflector [][] my2DArchivableArray = null;

    int [][] my2DIntArray = new int[3][];
    String [][] my2DNullStringArray = new String[3][];
    
    double [][][] my3DDoubleArray = new double[4][][];

    Collection myCollection;
    Collection<Integer> myIntList = new LinkedList<Integer>();
    Collection<String> myStringSet = new HashSet<>();


    Collection [] collectionArray = {
            new ArrayList()
    };
    Collection [][] collectionArray2D = {
            { new ArrayList(), new ArrayList() },
            { new HashSet(), new HashSet() },
    };
    Collection [][][] collectionArray3D = {

    };
    
    public MyArchivable() {
    }

    void populate() {
        myList.add("A");
        myList.add("B");
        myList.add("C");
        my2DIntArray[0] = new int [] { 1,2,3 };
        my2DIntArray[1] = new int [] { 4,5,6 };
        my2DIntArray[2] = new int [] { 7,8,9 };
        for (int i=0; i<my3DDoubleArray.length; i++) {
            my3DDoubleArray[i] = new double[3][];
            for (int ii=0; ii<my3DDoubleArray[i].length; ii++) {
                my3DDoubleArray[i][ii] = new double[] { i*ii+0, i*ii+1, i*ii+2 };
            }
        }
    }
    
    static {
        addAllFields(MyArchivable.class);
        /*
        try {
            Field [] fields = MyArchivable.class.getDeclaredFields();
            for (Field f: fields) {
                addField(MyArchivable.class, f.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        
        /*
        addField(MyArchivable.class, "myStringArray");
        addField(MyArchivable.class, "myEmptyStringArray");
        addField(MyArchivable.class, "myNullStringArray");
        addField(MyArchivable.class, "myIntArray");
        addField(MyArchivable.class, "myEmptyIntArray");
        addField(MyArchivable.class, "myNullIntArray");
        addField(MyArchivable.class, "myFloatArray");
        addField(MyArchivable.class, "myEmptyFloatArray");
        addField(MyArchivable.class, "myNullFloatArray");
        addField(MyArchivable.class, "myLongArray");
        addField(MyArchivable.class, "myEmptyLongArray");
        addField(MyArchivable.class, "myNullLongArray");
        addField(MyArchivable.class, "myDoubleArray");
        addField(MyArchivable.class, "myEmptyDoubleArray");
        addField(MyArchivable.class, "myNullDoubleArray");
        addField(MyArchivable.class, "myBooleanArray");
        addField(MyArchivable.class, "myEmptyBooleanArray");
        addField(MyArchivable.class, "myNullBooleanArray");
        addField(MyArchivable.class, "myEnumArray");
        addField(MyArchivable.class, "myEmptyEnumArray");
        addField(MyArchivable.class, "myNullEnumArray");
        addField(MyArchivable.class, "myArchivableArray");
        addField(MyArchivable.class, "myEmptyArchivableArray");
        addField(MyArchivable.class, "myNullArchivableArray");
        addField(MyArchivable.class, "myEnum");
        addField(MyArchivable.class, "myString");
        addField(MyArchivable.class, "myInt");
        addField(MyArchivable.class, "myBoolean");
        addField(MyArchivable.class, "myFloat");
        addField(MyArchivable.class, "myDouble");
        addField(MyArchivable.class, "myArchivable");
        addField(MyArchivable.class, "my2DIntArray");
        addField(MyArchivable.class, "my2DNullStringArray");
        addField(MyArchivable.class, "my3DDoubleArray");
        addField(MyArchivable.class, "myIntList");
        addField(MyArchivable.class, "myStringSet");*/
    }
}