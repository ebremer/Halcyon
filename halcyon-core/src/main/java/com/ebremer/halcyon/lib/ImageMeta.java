package com.ebremer.halcyon.lib;

import java.util.ArrayList;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author erich
 */
public class ImageMeta {
    private final ArrayList<ImageScale> scales;
    private final int series;
    private final int width;
    private final int height;
    private final int tileSizeX;
    private final int tileSizeY;
    private final Double magnification;
    private final float aspectratio;
    private final Model meta;
    private static final Logger logger = LoggerFactory.getLogger(ImageMeta.class);
    
    private ImageMeta(Builder builder) {
        this.series = builder.series;
        this.width = builder.width;
        this.height = builder.height;
        this.tileSizeX = builder.tileSizeX;
        this.tileSizeY = builder.tileSizeY;
        this.magnification = builder.magnification;
        this.scales = builder.scales;
        this.aspectratio = builder.aspectratio;
        this.meta = builder.meta;
    }
    
    public ArrayList<ImageScale> getScales() {
        return scales;
    }
    
    public Model getRDF() {
        return meta;
    }
    
    public ImageScale getBestMatch(double ratio) {
        int c = scales.size();
        ImageScale scale;
        do {            
            c--;
            scale = scales.get(c);
        } while ((c>0)&&(ratio<=scale.scale));
        return scale;
    }

    public Double getMagnification() {
        return magnification;
    }
    
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
    
    public int getTileSizeX() {
        return tileSizeX;
    }

    public int getTileSizeY() {
        return tileSizeY;
    }
    
    public int getMainImageSeries() {
        return series;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(width).append("x").append(height).append(" ").append(tileSizeX).append("x").append(tileSizeY).append("\n");
        for (ImageScale s : scales) {
            sb.append(s.scale).append("->").append(s.width).append("x").append(s.height).append("\n");
        }
        return sb.toString();
    }

    public record ImageScale(int series, int scale, int width, int height, double aspectratio) implements Comparable<ImageScale> {
        @Override
        public int compareTo(ImageScale other) {
            return Float.compare(this.scale, other.scale);
        }
        
        public ImageRegion Validate(ImageRegion region) {
           if (((region.getX()+region.getWidth()) < width)&&((region.getY()+region.getHeight()) < height)) {
                return region;
            }
            int w = region.getX()+region.getWidth();
            int h = region.getY()+region.getHeight();
            w = (w >= width) ? width - region.getX(): region.getWidth();
            h = (h >= height) ? height - region.getY(): region.getHeight();
            return new ImageRegion(region.getX(),region.getY(),w,h);
        }
    };
    
    public static class Builder {
        private final ArrayList<ImageScale> scales;
        private int series;
        private int width;
        private int height;
        private int tileSizeX;
        private int tileSizeY;
        private Double magnification;
        private final boolean useWidth;
        private final float aspectratio;
        private boolean filter = true;
        private final Model meta = ModelFactory.createDefaultModel();

        private Builder(int series, int width, int height) {
            this.series = series;
            this.width = width;
            this.height = height;
            this.aspectratio = ((float)width)/((float)height);
            useWidth = (width>=height);
            scales = new ArrayList<>();
            //scales.add(new ImageScale(0,1,width,height,((float)width)/((float)height)));
        }
        
        public Builder setSeries(int series) {
            this.series = series;
            return this;
        }
        
        public Builder setMeta(Model m) {
            meta.add(m);
            return this;
        }
        
        public Builder filter(boolean filter) {
            this.filter = filter;
            return this;
        }
        
        public Builder setTileSizeX(int tileSizeX) {
            this.tileSizeX = tileSizeX;
            return this;
        }

        public Builder setTileSizeY(int tileSizeY) {
            this.tileSizeY = tileSizeY;
            return this;
        }
        
        public Builder setMagnification(Double magnification) {
            this.magnification = magnification;
            return this;
        }
        
        public Builder addScale(int series, int width, int height) {
            int scale = Math.round((useWidth) ? ((float) this.width)/((float) width) : ((float) this.height)/((float) height));
            addScale(series, scale, width, height);
            return this;
        }
        
        public Builder addScale(int series, int scale, int width, int height) {
            float ratio = ((float)width)/((float)height);
            float d = Math.abs(ratio-aspectratio);
            d = d/aspectratio;
            //System.out.println(d);
            if (!filter||(d<0.04)) {                
                logger.trace("Adding Scale "+series+"  "+d+"  "+ratio+"  "+scales.size()+" "+width+" x "+height);
               // System.out.println("Adding Scale "+series+"  "+d+"  "+ratio+"  "+scales.size()+" "+width+" x "+height);
                scales.add(new ImageScale(series,scale,width,height,ratio));
            } else {
               // System.out.println("Aspect Ratio different "+series+"  "+d+"  "+ratio+"  "+scales.size()+" "+width+" x "+height);
                logger.trace("Aspect Ratio different "+series+"  "+d+"  "+ratio+"  "+scales.size()+" "+width+" x "+height);
            }
            return this;
        }
        
        public static Builder getBuilder(int series, int width, int height) {
            return new Builder(series, width, height);
        }
        
        public ImageMeta build() {
            return new ImageMeta(this);
        }
    }
}

