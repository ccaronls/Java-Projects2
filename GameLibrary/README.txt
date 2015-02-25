GameLibrary

Has various common utilities

Revision History

1.0 - Added cc.lilb.net (see below).  All JUnit tests passed. 




cc.lib.net
----------

This is a library that provides a framework for Peer 2 Peer network systems like games.
The library provides:
 - server connection/reconnection management
 - handshaking management
 - data encryption

GameServer - Class that listens for connections and executes callbacks to the Listener.
GameClient - Base class for connecting to GameServer(s)
GameCommand - Extend this class to define your own commands as key/value string pairs.

Added GameServer and GameClient P2P over TCP with optional encryption.  The Protocol defined by GameCommand/GameCommandType
 