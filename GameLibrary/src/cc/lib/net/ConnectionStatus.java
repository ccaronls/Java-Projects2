package cc.lib.net;

import cc.lib.annotation.Keep;

@Keep
public enum ConnectionStatus {
    RED(500), YELLOW(200), GREEN(0), UNKNOWN(-1);

    final int minTime;

    ConnectionStatus(int minTime) {
        this.minTime = minTime;
    }

    public static ConnectionStatus from(int speed) {
        for (ConnectionStatus s : values()) {
            if (speed > s.minTime) {
                return s;
            }
        }
        return UNKNOWN;
    }
}
