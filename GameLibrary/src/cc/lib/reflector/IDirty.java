package cc.lib.reflector;

import java.io.IOException;

/**
 * Created by Chris Caron on 11/30/23.
 */
public interface IDirty {
    boolean isDirty();

    void markClean();

    void serializeDirty(RPrintWriter out) throws IOException;
}
