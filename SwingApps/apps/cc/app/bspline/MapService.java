package cc.app.bspline;

public class MapService {

	/*
	// CHANGE THIS: the output path of the image to be created
    private static final String IMAGE_FILE_PATH = "/some/user/path/map.png";

    // CHANGE THIS: image width in pixel
    private static final int IMAGE_WIDTH_IN_PX = 300;

    // CHANGE THIS: image height in pixel
    private static final int IMAGE_HEIGHT_IN_PX = 500;

    // CHANGE THIS: minimum padding in pixel
    private static final int MINIMUM_IMAGE_PADDING_IN_PX = 50;

    // formula for quarter PI
    private final static double QUARTERPI = Math.PI / 4.0;

    // some service that provides the county boundaries data in longitude and latitude
    private CountyService countyService;

    public void run() throws Exception {
        // configuring the buffered image and graphics to draw the map
        BufferedImage bufferedImage = new BufferedImage(IMAGE_WIDTH_IN_PX,
                                                        IMAGE_HEIGHT_IN_PX,
                                                        BufferedImage.TYPE_INT_RGB);

        Graphics2D g = bufferedImage.createGraphics();
        Map<RenderingHints.Key, Object> map = new HashMap<RenderingHints.Key, Object>();
        map.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        map.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        map.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RenderingHints renderHints = new RenderingHints(map);
        g.setRenderingHints(renderHints);

        // min and max coordinates, used in the computation below
        Point2D.Double minXY = new Point2D.Double(-1, -1);
        Point2D.Double maxXY = new Point2D.Double(-1, -1);

        // a list of counties where each county contains a list of coordinates that form the county boundary
        Collection<Collection<Point2D.Double>> countyBoundaries = new ArrayList<Collection<Point2D.Double>>();

        // for every county, convert the longitude/latitude to X/Y using Mercator projection formula
        for (County county : countyService.getAllCounties()) {
            Collection<Point2D.Double> lonLat = new ArrayList<Point2D.Double>();

            for (CountyBoundary countyBoundary : county.getCountyBoundaries()) {
                // convert to radian
                double longitude = countyBoundary.getLongitude() * Math.PI / 180;
                double latitude = countyBoundary.getLatitude() * Math.PI / 180;

                Point2D.Double xy = new Point2D.Double();
                xy.x = longitude;
                xy.y = Math.log(Math.tan(QUARTERPI + 0.5 * latitude));

                // The reason we need to determine the min X and Y values is because in order to draw the map,
                // we need to offset the position so that there will be no negative X and Y values
                minXY.x = (minXY.x == -1) ? xy.x : Math.min(minXY.x, xy.x);
                minXY.y = (minXY.y == -1) ? xy.y : Math.min(minXY.y, xy.y);

                lonLat.add(xy);
            }

            countyBoundaries.add(lonLat);
        }

        // readjust coordinate to ensure there are no negative values
        for (Collection<Point2D.Double> points : countyBoundaries) {
            for (Point2D.Double point : points) {
                point.x = point.x - minXY.x;
                point.y = point.y - minXY.y;

                // now, we need to keep track the max X and Y values
                maxXY.x = (maxXY.x == -1) ? point.x : Math.max(maxXY.x, point.x);
                maxXY.y = (maxXY.y == -1) ? point.y : Math.max(maxXY.y, point.y);
            }
        }

        int paddingBothSides = MINIMUM_IMAGE_PADDING_IN_PX * 2;

        // the actual drawing space for the map on the image
        int mapWidth = IMAGE_WIDTH_IN_PX - paddingBothSides;
        int mapHeight = IMAGE_HEIGHT_IN_PX - paddingBothSides;

        // determine the width and height ratio because we need to magnify the map to fit into the given image dimension
        double mapWidthRatio = mapWidth / maxXY.x;
        double mapHeightRatio = mapHeight / maxXY.y;

        // using different ratios for width and height will cause the map to be stretched. So, we have to determine
        // the global ratio that will perfectly fit into the given image dimension
        double globalRatio = Math.min(mapWidthRatio, mapHeightRatio);

        // now we need to readjust the padding to ensure the map is always drawn on the center of the given image dimension
        double heightPadding = (IMAGE_HEIGHT_IN_PX - (globalRatio * maxXY.y)) / 2;
        double widthPadding = (IMAGE_WIDTH_IN_PX - (globalRatio * maxXY.x)) / 2;

        // for each country, draw the boundary using polygon
        for (Collection<Point2D.Double> points : countyBoundaries) {
            Polygon polygon = new Polygon();

            for (Point2D.Double point : points) {
                int adjustedX = (int) (widthPadding + (point.getX() * globalRatio));

                // need to invert the Y since 0,0 starts at top left
                int adjustedY = (int) (IMAGE_HEIGHT_IN_PX - heightPadding - (point.getY() * globalRatio));

                polygon.addPoint(adjustedX, adjustedY);
            }

            g.drawPolygon(polygon);
        }

        // create the image file
        ImageIO.write(bufferedImage, "PNG", new File(IMAGE_FILE_PATH));
    }
    */
}
