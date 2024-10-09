package cc.lib.geoloc;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;

import javax.xml.parsers.SAXParserFactory;

public abstract class GPXParser {

    private class GPXHandler extends DefaultHandler {

        private final Stack<String> elems = new Stack<String>();
        private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            try {
                if (elems.size() > 0) {
                    String elem = elems.peek();
                    String txt = new String(ch, start, length);
                    if (elem.equals("ele")) {
                        elevation = Integer.parseInt(txt);
                    } else if (elems.peek().equals("time")) {
                        Date d = dateFormatter.parse(txt);
                        utcTime = d.getTime();
                    } else if (elem.equals("name")) {
                        name = txt;
                    }
                }
            } catch (Exception e) {
                throw new SAXException(e);
            }
        }

        private double lat, lon;
        private long utcTime;
        private String name;
        private int elevation;

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (!qName.equals(elems.peek()))
                throw new SAXException("End element <" + qName + "> found but stack has <" + elems.peek() + "> at top");
            if (elems.peek().equals("wpt")) {
                onWaypoint(lat, lon, elevation, utcTime, name);
            }
            elems.pop();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attr) throws SAXException {
            String top = elems.size() > 0 ? "<" + elems.peek() + ">" : "EMPTY";
            if (qName.equals("wpt")) {
                if (!elems.peek().equals("gpx")) {
                    throw new SAXException("<wpt> found inside " + top + " elem but only allowed within <gpx>");
                }
                lat = Double.parseDouble(attr.getValue("lat"));
                lon = Double.parseDouble(attr.getValue("lon"));
                utcTime = 0;
                name = "";
                elevation = 0;

            } else if (qName.equals("ele") || qName.equals("time") || qName.equals("name")) {
                if (!elems.peek().equals("wpt")) {
                    throw new SAXException("<" + qName + "> found inside " + top + " but only allowed within <wpt>");
                }
            } else if (qName.equals("gpx")) {
                if (elems.size() > 0) {
                    throw new SAXException("<gpx> elem found inside " + top + " but only allowed at document root");
                }
            }
            elems.push(qName);
        }

        @Override
        public void setDocumentLocator(Locator arg0) {
            super.setDocumentLocator(arg0);
            locator = arg0;
        }

    }

    ;

    private Locator locator = null;

    public void parse(File gpxFile) throws IOException, SAXException {
        FileInputStream input = new FileInputStream(gpxFile);
        try {
            XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            GPXHandler handler = new GPXHandler();
            reader.setContentHandler(handler);
            reader.parse(new InputSource(input));
        } catch (Exception e) {
            throw new SAXException("Error in file '" + gpxFile + "' line: " + locator.getLineNumber() + " :" + e.getClass().getSimpleName() + " " + e.getMessage(), e);
        } finally {
            input.close();
        }
    }

    protected abstract void onWaypoint(double lat, double lng, int elevationMeters, long utcTime, String name);
}
