package cc.game.soc.swing;

public interface IClient {

    boolean isConnected();

    void disconnect();

    void connect(String host);

}
