package cc.lib.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.StringWriter;

import cc.lib.utils.Reflector;

/**
 * Created by Chris Caron on 11/6/23.
 */
class SocketCommandSerialilzer implements GameCommand.Serializer {

    final DataInputStream in;
    final DataOutputStream out;

    SocketCommandSerialilzer(DataInputStream in, DataOutputStream out) {
        this.in = in;
        this.out = out;
    }


    @Override
    public void write(GameCommand cmd) throws Exception {
        out.writeUTF(cmd.getType().name());
        out.writeInt(cmd.getArguments().size());
        for (String key : cmd.getArguments().keySet()) {
            out.writeUTF(key);
            Object o = cmd.getArguments().get(key);
            if (o == null) {
                out.writeByte(TYPE_NULL);
            } else if (o instanceof Boolean) {
                out.writeByte(TYPE_BOOL);
                out.writeBoolean((Boolean) o);
            } else if (o instanceof Integer) {
                out.writeByte(TYPE_INT);
                out.writeInt((Integer) o);
            } else if (o instanceof Long) {
                out.writeByte(TYPE_LONG);
                out.writeLong((Long) o);
            } else if (o instanceof Float) {
                out.writeByte(TYPE_FLOAT);
                out.writeFloat((Float) o);
            } else if (o instanceof Double) {
                out.writeByte(TYPE_DOUBLE);
                out.writeDouble((Double) o);
            } else if (o instanceof String) {
                out.writeByte(TYPE_STRING);
                out.writeUTF((String) o);
            } else if (o instanceof Reflector) {
                out.writeByte(TYPE_REFLECTOR);
                StringWriter buf = new StringWriter(256);
                ((Reflector) o).serialize(new Reflector.MyPrintWriter(buf));
                out.writeUTF(buf.toString());
            } else {
                out.writeByte(TYPE_STRING);
                out.writeUTF(o.toString());
            }
        }
        out.flush();
    }

    @Override
    public GameCommand read() throws Exception {
        String cmd = in.readUTF();
        GameCommandType type = GameCommandType.valueOf(cmd);
        GameCommand command = new GameCommand(type);
        int numArgs = in.readInt();
        for (int i = 0; i < numArgs; i++) {
            String key = in.readUTF();
            int itype = in.readUnsignedByte();
            switch (itype) {
                case TYPE_NULL:
                    break;
                case TYPE_BOOL:
                    command.setArg(key, in.readBoolean());
                    break;
                case TYPE_INT:
                    command.setArg(key, in.readInt());
                    break;
                case TYPE_LONG:
                    command.setArg(key, in.readLong());
                    break;
                case TYPE_FLOAT:
                    command.setArg(key, in.readFloat());
                    break;
                case TYPE_DOUBLE:
                    command.setArg(key, in.readDouble());
                    break;
                case TYPE_STRING:
                    command.setArg(key, in.readUTF());
                    break;
                case TYPE_REFLECTOR: {
                    command.setArg(key, in.readUTF());
                    break;
                }
                default:
                    throw new cc.lib.utils.GException("Unhandled type " + itype);
            }
        }
        return command;
    }

    private final static int TYPE_NULL = 0;
    private final static int TYPE_BOOL = 1;
    private final static int TYPE_INT = 2;
    private final static int TYPE_LONG = 3;
    private final static int TYPE_FLOAT = 4;
    private final static int TYPE_DOUBLE = 5;
    private final static int TYPE_STRING = 6;
    private final static int TYPE_REFLECTOR = 7;

}
