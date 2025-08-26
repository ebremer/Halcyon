package com.ebremer.halcyon.lib.imaging.converters;

import java.awt.Color;
import java.awt.Graphics2D;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    
    public static void main(String[] args) {
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
}
