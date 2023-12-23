package cc.lib.reflector;

/**
 * Created by Chris Caron on 12/1/23.
 */
class Archivers {

    static Archiver byteArchiver = new AArchiver() {

        @Override
        public Object parse(String s) {
            return s.equals("null") ? null : Byte.parseByte(s);
        }

    };

    static Archiver integerArchiver = new AArchiver() {

        @Override
        public Object parse(String s) {
            return s.equals("null") ? null : Integer.parseInt(s);
        }

    };

    static Archiver longArchiver = new AArchiver() {

        @Override
        public Object parse(String s) {
            return s.equals("null") ? null : Long.parseLong(s);
        }

    };

    static Archiver floatArchiver = new AArchiver() {

        @Override
        public Object parse(String s) {
            return s.equals("null") ? null : Float.parseFloat(s);
        }

    };

    static Archiver doubleArchiver = new AArchiver() {

        @Override
        public Object parse(String s) {
            return s.equals("null") ? null : Double.parseDouble(s);
        }

    };

    static Archiver booleanArchiver = new AArchiver() {

        @Override
        public Object parse(String s) {
            return s.equals("null") ? null : Boolean.parseBoolean(s);
        }

    };

    static Archiver stringArchiver = new StringArchiver();

    static Archiver enumArchiver = new EnumArchiver();

    static Archiver dirtyArchiver = new DirtyArchiver();

    static Archiver collectionArchiver = new CollectionArchiver();

    static Archiver mapArchiver = new MapArchiver();

    static Archiver arrayArchiver = new ArrayArchiver();

    static Archiver archivableArchiver = new ArchivableArchiver();

}
