package cc.lib.dungeondice;

import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

public class UI extends DDungeon {

    private static Logger log = LoggerFactory.getLogger(UI.class);

    private static UI instance = null;

    public static UI getInstance() {
        return instance;
    }


}
