package com.ebremer.halcyon.lib.imaging.converters;

import java.awt.Color;
import java.awt.Graphics2D;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.imageio.ImageIO;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

public class RasterToPolygon {

    /**
     * Convert black regions of a BufferedImage into JTS Polygons.
     *
     * @param img       input image
     * @param gf        GeometryFactory (set SRID if desired)
     * @param originX   world X at pixel (0,0) corner
     * @param originY   world Y at pixel (0,0) corner
     * @param pixelW    world width of one pixel (X scale)
     * @param pixelH    world height of one pixel (Y scale). Use negative if you want Y-up coordinates.
     * @param thresh    0..255; a pixel is “black” if luminance <= thresh and alpha > 0
     * @param simplifyTolerance optional simplification tolerance (0 for none)
     * @return 
     */
    public static List<Polygon> blackRegionsToPolygons(BufferedImage img,
                                                       GeometryFactory gf,
                                                       double originX, double originY,
                                                       double pixelW, double pixelH,
                                                       int thresh,
                                                       double simplifyTolerance) {
        final int w = img.getWidth();
        final int h = img.getHeight();

        // Precompute mask of black pixels
        final boolean[][] black = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                black[y][x] = isBlack(argb, thresh);
            }
        }

        // Collect boundary segments between black and non-black
        List<LineString> edges = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!black[y][x]) continue;

                // Left edge (neighbor outside or white)
                if (x == 0 || !black[y][x - 1]) {
                    edges.add(edge(gf, originX + x * pixelW,       originY + y * pixelH,
                                        originX + x * pixelW,       originY + (y + 1) * pixelH));
                }
                // Right edge
                if (x == w - 1 || !black[y][x + 1]) {
                    edges.add(edge(gf, originX + (x + 1) * pixelW, originY + y * pixelH,
                                        originX + (x + 1) * pixelW, originY + (y + 1) * pixelH));
                }
                // Top edge
                if (y == 0 || !black[y - 1][x]) {
                    edges.add(edge(gf, originX + x * pixelW,       originY + y * pixelH,
                                        originX + (x + 1) * pixelW, originY + y * pixelH));
                }
                // Bottom edge
                if (y == h - 1 || !black[y + 1][x]) {
                    edges.add(edge(gf, originX + x * pixelW,       originY + (y + 1) * pixelH,
                                        originX + (x + 1) * pixelW, originY + (y + 1) * pixelH));
                }
            }
        }

        // Polygonize
        Polygonizer polygonizer = new Polygonizer(true);
        polygonizer.add(edges);

        @SuppressWarnings("unchecked")
        Collection<Polygon> rawPolys = polygonizer.getPolygons();

        // Optional simplification
        List<Polygon> out = new ArrayList<>(rawPolys.size());
        if (simplifyTolerance > 0) {
            for (Polygon p : rawPolys) {
                Geometry g = DouglasPeuckerSimplifier.simplify(p, simplifyTolerance);
                // Ensure we return Polygons (not MultiPolygons) split into parts if simplification created them
                switch (g) {
                    case Polygon polygon -> out.add(polygon);
                    case MultiPolygon mp -> {
                        for (int i = 0; i < mp.getNumGeometries(); i++) {
                            out.add((Polygon) mp.getGeometryN(i));
                        }
                    }
                    default -> {
                    }
                }
            }
        } else {
            out.addAll(rawPolys);
        }
        return out;
    }

    private static boolean isBlack(int argb, int thresh) {
        int a = (argb >>> 24) & 0xFF;
        if (a == 0) return false; // treat fully transparent as background
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        // NTSC-luma-ish grayscale
        int y = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
        return y <= thresh;
    }

    private static LineString edge(GeometryFactory gf, double x1, double y1, double x2, double y2) {
        return gf.createLineString(new Coordinate[]{
                new Coordinate(x1, y1),
                new Coordinate(x2, y2)
        });
    }

    public static List<Polygon> blackRegionsToPolygons(BufferedImage img, int thresh) {
        return blackRegionsToPolygons(
                img,
                new GeometryFactory(),
                0.0, 0.0,
                1.0, 1.0,   // Y-down; set pixelH = -1.0 if you want Y-up
                thresh,
                0.0
        );
    }
    
    public static String toWKT(BufferedImage img, int thresh) {
        List<Polygon> polys = RasterToPolygon.blackRegionsToPolygons(img, thresh);
        if (polys.isEmpty()) {
            return "GEOMETRYCOLLECTION EMPTY";
        }

        Geometry geom;
        if (polys.size() == 1) {
            geom = polys.get(0);
        } else {
            geom = polys.get(0).getFactory().createMultiPolygon(polys.toArray(new Polygon[0]));
        }

        WKTWriter writer = new WKTWriter();
        return writer.write(geom);
    }
    
    public static List<String> toPolygonWKTList(BufferedImage img, int thresh) {
        List<Polygon> polys = RasterToPolygon.blackRegionsToPolygons(img, thresh);
        List<String> results = new ArrayList<>();
        WKTWriter writer = new WKTWriter();
        for (Polygon p : polys) {
            Geometry g = p;  // just in case simplification created multi parts earlier
            switch (g) {
                case MultiPolygon mp -> {
                    for (int i = 0; i < mp.getNumGeometries(); i++) {
                        results.add(writer.write(mp.getGeometryN(i)));
                    }
                }
                case Polygon polygon -> results.add(writer.write(polygon));
                default -> {
                }
            }
        }
        return results;
    }
    
    public static void drawPolygonsAndSave(BufferedImage img, List<String> polygons, File outFile) {
        GeometryFactory gf = new GeometryFactory();
        WKTReader reader = new WKTReader(gf);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.GREEN);
        g2d.setStroke(new java.awt.BasicStroke(2.0f));  // thicker outline
        try {
            for (String wkt : polygons) {
                Geometry geom = reader.read(wkt);
                if (geom instanceof Polygon poly) {
                    drawPolygon(g2d, poly);
                } else if (geom instanceof MultiPolygon mp) {
                    for (int i = 0; i < mp.getNumGeometries(); i++) {
                        drawPolygon(g2d, (Polygon) mp.getGeometryN(i));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            g2d.dispose();
        }
        try {
            ImageIO.write(img, "png", outFile);
                System.out.println("Saved image with polygons to " + outFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void drawPolygon(Graphics2D g2d, Polygon poly) {
        drawLineString(g2d, poly.getExteriorRing());
        for (int i = 0; i < poly.getNumInteriorRing(); i++) {
            drawLineString(g2d, poly.getInteriorRingN(i));
        }
    }

    private static void drawLineString(Graphics2D g2d, LineString ls) {
        Coordinate[] coords = ls.getCoordinates();
        int n = coords.length;
        int[] xPoints = new int[n];
        int[] yPoints = new int[n];

        for (int i = 0; i < n; i++) {
            xPoints[i] = (int) Math.round(coords[i].x);
            yPoints[i] = (int) Math.round(coords[i].y);
        }

        g2d.drawPolygon(xPoints, yPoints, n);
    }
    
    public static BufferedImage LoadPNG(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new IOException("Failed to load image, file is not a valid PNG.");
            }
            System.out.println("Image loaded: " + image.getWidth() + "x" + image.getHeight());
            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static void main2(String[] args) {
        int w = 100, h = 100;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(10, 10, 80, 80);
        g.setColor(Color.WHITE);
        g.fillRect(30, 30, 40, 40);
        g.dispose();
        String wkt = RasterToPolygon.toWKT(img, 32);
        System.out.println("Resulting WKT:");
        System.out.println(wkt);
    }

    public static void main(String[] args) {
        File img = new File("D:\\utah\\phase3\\Stack1-With-IHC\\WSI\\Stack3.png");
        File file = new File("D:\\utah\\phase3\\Stack1-With-IHC\\WSI\\Stack3-IHC-mask.png");
        File out = new File("D:\\utah\\phase3\\Stack1-With-IHC\\WSI\\Stack3-IHC-mask-out.png");
        BufferedImage bi = LoadPNG(file);
        List<String> wkt = RasterToPolygon.toPolygonWKTList(bi, 32);
        drawPolygonsAndSave(LoadPNG(img), wkt, out);
        drawPolygonsAndSave(bi, wkt, out);
        
    }    

}
