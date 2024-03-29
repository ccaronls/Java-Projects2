package cc.lib.swing;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import javax.swing.JApplet;

import cc.lib.utils.Reflector;

public abstract class AWTApplet extends JApplet {

    @Override
    public final void init() {
        Reflector.DISABLED = true;
        AWTImageMgr.applet = this;
        initApp();
    }

    public abstract URL getAbsoluteURL(String imagePath) throws MalformedURLException;

    protected abstract void initApp();

    public <T extends Enum<T>> List<T> getEnumListProperty(String property, Class enumClass, List<T> defaultList) {
        return defaultList;
    }

    public String getStringProperty(String property, String defaultValue) {
        return defaultValue;
    }

    public void setStringProperty(String s, String v) {}

    public <T extends Enum<T>> void setEnumListProperty(String s, Collection<T> l) {}

    public void setIntProperty(String s, int value) {}

    public int getIntProperty(String s, int defaultValue) { return defaultValue; }

}
