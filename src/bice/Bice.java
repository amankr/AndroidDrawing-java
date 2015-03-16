package bice;

import static processing.core.PApplet.*;


import java.util.Arrays;
import java.util.BitSet;

import processing.core.PImage;

public class Bice {
  
  float preblurRadius = 0.5f;
  // normalization parameters
  float normalizationNeighborhood = 3; // sigma on the gaussian blur of the gradient neighborhood 
  float normalizationEpsilon = 4; // prevent divide by zero, or inflating tiny gradients
  // histogram parameters
  int xBuckets = 32, yBuckets = 32, tBuckets = 16, lBuckets = 1; // t -> theta, size of histogram
  int xBuckets2 = 18, yBuckets2 = 6, tBuckets2 = 4; // output resampling
  float histogramRange = 1f/sqrt(2); // how wide the histogram should be, this should keep everything, could try "1" too
  // coherent edge parameters
  float edgeAlpha = 2, edgeBeta = 8; // sigmoid parameters for distributing gradient magnitude between long and short edges
  // binarizing parameters
  float thresholdPercentage = 0.15f;
  // empty patch parameters
  float minimumEdgePerPatch = 0.01f; // what percentage of pixels per patch must be "active" for a descriptor to be "non-empty"
  float activePixelThreshold = 0.05f; // what how large must the normalized gradient of a pixel be to be considered active
  int interiorSampleStride = 0;
  
  // performance testing
  public long
    binTime = 0,
    resampleTime = 0,
    thresholdTime = 0,
    blurTime = 0,
    extractCoherentTime = 0,
    makeBitsetTime = 0;
  
  public boolean testMode = false; // enable/disable expensive code that checks intermediate results
  
  // caching of normalized image gradients
  float[] g, th;
  int h,w;
  BitSet interiorImage;
  
  private Bice() {}
  
  public static Bice shadowdrawVersion() {
    return new Bice();
  }
  
  public static Bice solidShadowdrawVersion(int stride) {
    return new Bice();
  }
  
  public static Bice defaultVersion() {
    Bice b = new Bice();
    b.xBuckets = 32; b.yBuckets = 32; b.tBuckets = 20; b.lBuckets = 2;
    b.xBuckets2 = 24; b.yBuckets2 = 8; b.tBuckets2 = 12;
    return b;
  }
  
  public int getNumBits() {
    return xBuckets2*yBuckets2*tBuckets2*lBuckets;
  }
  
  public void setImage(PImage im) {
    if(im == null) return;
    
    im.filter(BLUR, preblurRadius); // reduce noise
    im.loadPixels();

    int[] px = im.pixels;

    g = new float[px.length]; // gradient magnitude
    th = new float[px.length]; // gradient angle
    float[] gNei = new float[px.length];
    
    h = im.height;
    w = im.width;
    int eh = h-1, ew = w-1;
    // calc gradients
    for(int y = 0; y < eh; y++) {
      for(int x = 0; x < ew; x++) {
        int i = x + y*w;
        float ci = brightness(px[i]);
        float ciy = brightness(px[i+w]);
        float cix = brightness(px[i+1]);
        float dx = cix-ci, dy = ciy-ci;
        g[i] = sqrt(dx*dx + dy*dy); 
        th[i] = atan2(dy,dx);
      }
    }

    if(testMode) testImageBlurring(g, gNei);
    
    // we have two implementations, this one is faster
    blurSeparate(g, gNei, w, h, normalizationNeighborhood);
    
    for(int y = 0; y < eh; y++) {
      for(int x = 0; x < ew; x++) {
        int i = x + y*w;
        g[i] = g[i]/max(gNei[i], normalizationEpsilon);
      }
    }
  }
  
  public void setInteriorImage(PImage interiorImg) {
    interiorImage = new BitSet(interiorImg.width*interiorImg.width);
    interiorImg.loadPixels();
    int[] pixels = interiorImg.pixels;
    for(int i = 0; i < pixels.length; i++) {
      if((pixels[i]&0xff) < 128) interiorImage.set(i);
    }
  }
  
  public BitSet calc(int patchX, int patchY, int patchSize) {
    if(g == null) throw new IllegalStateException();
    
    float valRange = patchSize*histogramRange;
    Histogram3 hist = new Histogram3(
     -valRange, valRange, xBuckets,
     -valRange, valRange, yBuckets,
     -PI, PI, tBuckets
    );

    int patchBottom = patchY+patchSize;
    int patchRight = patchX+patchSize;

    float gSum = 0;
    // collect into histogram
    long start = System.nanoTime();
    int cx = patchX+patchSize/2, cy = patchY+patchSize/2;
    for(int y = patchY; y < patchBottom; y++) {
      for(int x = patchX; x < patchRight; x++) {
        int i = x + y*w;
        float lx = x-cx, ly = y-cy;
        float cosTheta = cos(th[i]);
        float sinTheta = sin(th[i]);
        float xPrime = lx*cosTheta - ly*sinTheta;
        float yPrime = lx*sinTheta + ly*cosTheta;
        hist.increment(xPrime, yPrime, th[i], g[i]);
        if(g[i] > activePixelThreshold) gSum++;
      }
    }
    
    binTime += System.nanoTime()-start;
    if(gSum < patchSize*patchSize*minimumEdgePerPatch) {
      return null; // a more-or-less blank region
    }
    BitSet interior = new BitSet();
    if(interiorSampleStride > 0) {
      for(int y = patchY; y < patchBottom; y += interiorSampleStride) {
        for(int x = patchX; x < patchRight; x += interiorSampleStride) {
          int i = x + y*w;
          interiorImage.set(i);
        }
      }
    }
    
    BitSet result;
    if(lBuckets > 1) {
      if(lBuckets > 2) {
        throw new IllegalArgumentException();
      }
      start = System.nanoTime();
      Histogram3 coherent = extractCoherentEdges(hist); // this modifies hist
      Histogram3 texture = hist;
      extractCoherentTime += System.nanoTime()-start;
      
      //testHistogramBlurring(hist);
  
      // blur both
      start = System.nanoTime();
      texture = texture.blur();
      coherent = coherent.blur();
      blurTime += System.nanoTime()-start;
  
      start = System.nanoTime();
      float[] textureValues = texture.resample(xBuckets2, yBuckets2, tBuckets2);
      float[] coherentValues = coherent.resample(xBuckets2, yBuckets2, tBuckets2);
      resampleTime += System.nanoTime()-start;
      
      start = System.nanoTime();
      float textureTh = getThreshold(textureValues, thresholdPercentage);
      float coherentTh = getThreshold(coherentValues, thresholdPercentage);
      thresholdTime += System.nanoTime()-start;
      
      start = System.nanoTime();
      result = bitset(textureValues, textureTh, coherentValues, coherentTh, interior);
      makeBitsetTime += System.nanoTime()-start;
    } else {
      extractCoherentTime = 0;
      
      // blur both
      start = System.nanoTime();
      hist = hist.blur();
      blurTime += System.nanoTime()-start;
  
      start = System.nanoTime();
      float[] histValues = hist.resample(xBuckets2, yBuckets2, tBuckets2);
      resampleTime += System.nanoTime()-start;
      
      start = System.nanoTime();
      float histTh = getThreshold(histValues, thresholdPercentage);
      thresholdTime += System.nanoTime()-start;
      
      start = System.nanoTime();
      result = bitset(histValues, histTh, new float[0], 0, interior);
      makeBitsetTime += System.nanoTime()-start;
    }
    return result;
  }
  
  Histogram3 extractCoherentEdges(Histogram3 input) {
    float[][][] values = input.values;
    Histogram3 coherent = new Histogram3(xBuckets, yBuckets, tBuckets);
    float[][][] cvalues = coherent.values;
    for(int t = 0; t < tBuckets; t++) {
     for(int x = 0; x < xBuckets; x++) {
       float sum = 0;
       for(int y = 0; y < yBuckets; y++) {
         sum += values[t][x][y];
       }
       float weight = constrain((sum-edgeAlpha)/edgeBeta, 0, 1);
       for(int y = 0; y < yBuckets; y++) {
         float v = values[t][x][y];
         cvalues[t][x][y] = v*weight;
         values[t][x][y] = v*(1-weight);
       }
     }
    }
    return coherent;
  }
  
  
  float getThreshold(float[] values, float level) {
    float[] sorted = new float[values.length];
    System.arraycopy(values, 0, sorted, 0, sorted.length);
    Arrays.sort(sorted);
    return sorted[(int)(sorted.length*(1-level))];
  }
  
  BitSet bitset(
      float[] textureValues, float textureThreshold,
      float[] coherentValues, float coherentThreshold,
      BitSet interior) {
    BitSet bits = new BitSet(textureValues.length+coherentValues.length+interior.size());
    int j = 0;
    for(int i = 0; i < textureValues.length; i++) {
      if(textureValues[i] > textureThreshold) bits.set(j);
      j++;
    }
    for(int i = 0; i < coherentValues.length; i++) {
      if(coherentValues[i] > coherentThreshold) bits.set(j);
      j++;
    }
    for(int i = 0; i < interior.size(); i++) {
      if(interior.get(i)) bits.set(j);
      j++;
    }
    return bits;
  }
    
  @Deprecated
  void threshold(Histogram3 hist, float level) {
    float[] vals = hist.flatten();
    Arrays.sort(vals);
    hist.threshold(vals[(int)(vals.length*(1-level))]);
  }
  
  @Deprecated
  BitSet bitset(Histogram3 shortH, Histogram3 longH) {
    BitSet bits = new BitSet(shortH.getSize()+longH.getSize());
    int i = 0;
    for(int t = 0; t < tBuckets; t++) {
     for(int y = 0; y < yBuckets; y++) {
       for(int x = 0; x < xBuckets; x++) {
         if(shortH.values[t][x][y] > 0) {
           bits.set(i);
         }
         i++;
       }
     }
    }
    for(int t = 0; t < tBuckets; t++) {
     for(int y = 0; y < yBuckets; y++) {
       for(int x = 0; x < xBuckets; x++) {
         if(longH.values[t][x][y] > 0) {
           bits.set(i);
         }
         i++;
       }
     }
    }
    return bits;
  }

  float[] _hsb = new float[3];;
  float brightness(int v) {
    RGBtoHSB((v >> 16) & 0xff, (v >> 8) & 0xff, v & 0xff, _hsb);
    return _hsb[2]*255;
  }
  
  void blurTogether(float[] in, float[] out, int width, int height, float sigma) {
    GaussianTable gt = new GaussianTable(normalizationNeighborhood, 0);
    for(int y = 0; y < h; y++) {
      for(int x = 0; x < w; x++) {
        int i = x + y*w;
        float weightSum = 0;
        float sum = 0;
        // bounds: we don't calculate gradient values at the last row and column,
        // so we shouldn't include those in the gradient
        int nyEnd = min(h-1, y+gt.halfSize);
        int nxEnd = min(w-1, x+gt.halfSize);
        for(int ny = max(0,y-gt.halfSize); ny <= nyEnd; ny++) {
          for(int nx = max(0,x-gt.halfSize); nx <= nxEnd; nx++) {
            int ni = nx + ny*w;
            float weight = gt.at(x-nx, y-ny);
            sum += in[ni]*weight; // correlation vs convolution
            weightSum += weight;
          }
        }
        out[i] = sum / weightSum;
      }
    }
  }
  
  void blurSeparate(float[] in, float[] out, int width, int height, float sigma) {
    int blurRadius = (int)(3 * sigma);
    int blurKernelSize = blurRadius*2 + 1;
    int zero = blurKernelSize/2; 
    Gaussian gau = new Gaussian(sigma, 0);
    float[] blurKernel = new float[blurKernelSize];
    for(int i = 0; i < blurKernel.length; i++) {
      blurKernel[i] = gau.at(i-zero);
    }
    
    float[] b2 = new float[out.length];
    
    float sum, cb;
    int read, ri, ym, ymi, bk0;
    int yi = 0;

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        //cb = cg = cr = sum = 0;
        cb = sum = 0;
        read = x - blurRadius;
        if (read<0) {
          bk0=-read;
          read=0;
        } else {
          if (read >= width)
            break;
          bk0=0;
        }
        for (int i = bk0; i < blurKernelSize; i++) {
          if (read >= width)
            break;
          float c = in[read + yi];
          float mult = blurKernel[i];
          cb += c*mult;
          sum += mult;
          read++;
        }
        ri = yi + x;
        b2[ri] = cb / sum;
      }
      yi += width;
    }

    yi = 0;
    ym=-blurRadius;
    ymi=ym*width;

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        //cb = cg = cr = sum = 0;
        cb = sum = 0;
        if (ym<0) {
          bk0 = ri = -ym;
          read = x;
        } else {
          if (ym >= height)
            break;
          bk0 = 0;
          ri = ym;
          read = x + ymi;
        }
        for (int i = bk0; i < blurKernelSize; i++) {
          if (ri >= height)
            break;
          float c = b2[read];
          float mult = blurKernel[i];
          cb += c*mult;
          sum += mult;
          ri++;
          read += width;
        }
        out[x+yi] = (cb/sum);
      }
      yi += width;
      ymi += width;
      ym++;
    }
  }
  
  private void testHistogramBlurring(Histogram3 hist) {
    // check that the fast and slow histogram blurring are close to the same
    Histogram3 hist2 = hist.blur();
    Histogram3 hist3 = hist.blurSlow();
    float avgDiff = 0, avgSmooth = 0;
    int numSet = 0;
    for(int t = 0; t < tBuckets; t++) {
      for(int x = 0; x < xBuckets; x++) {
        for(int y = 0; y < yBuckets; y++) {
          if(hist.values[t][x][y] > 0) {
            avgDiff += Math.abs(hist2.values[t][x][y] - hist3.values[t][x][y]);
            avgSmooth += Math.abs(hist3.values[t][x][y] - hist.values[t][x][y]);
            numSet++;
          }
        }
      }
    }
    
    avgDiff /= (float)numSet;
    avgSmooth /= (float)numSet;
    System.out.println("avgDiff: "+avgDiff);
    System.out.println("avgSmooth: "+avgSmooth);    
  }
  
  private void testImageBlurring(float[] g1, float[] gNei) {
    float[] g2 = new float[g1.length];
    // this is the original implementation
    blurTogether(g1, g2, w, h, normalizationNeighborhood);
    // here's a test to show that the two are very close to the same
    float avgDiff = 0, avgSmooth = 0;
    for(int i = 0; i < g2.length; i++) {
      avgDiff += Math.abs(g2[i]-gNei[i]);
      avgSmooth += Math.abs(g1[i]-gNei[i]);
    }
    System.out.println("avgDiff: "+avgDiff/g1.length); // this is < 1e-4, might be smaller if boundaries excluded
    System.out.println("avgSmooth: "+avgSmooth/g1.length); // this is ~= 8, showing that values are in fact smoothed
  }

  public static float[] RGBtoHSB(int r, int g, int b, float[] hsbvals) {
      float hue, saturation, brightness;
      if (hsbvals == null) {
          hsbvals = new float[3];
      }
      int cmax = (r > g) ? r : g;
      if (b > cmax) cmax = b;
      int cmin = (r < g) ? r : g;
      if (b < cmin) cmin = b;

      brightness = ((float) cmax) / 255.0f;
      if (cmax != 0)
          saturation = ((float) (cmax - cmin)) / ((float) cmax);
      else
          saturation = 0;
      if (saturation == 0)
          hue = 0;
      else {
          float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
          float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
          float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
          if (r == cmax)
              hue = bluec - greenc;
          else if (g == cmax)
              hue = 2.0f + redc - bluec;
          else
              hue = 4.0f + greenc - redc;
          hue = hue / 6.0f;
          if (hue < 0)
              hue = hue + 1.0f;
      }
      hsbvals[0] = hue;
      hsbvals[1] = saturation;
      hsbvals[2] = brightness;
      return hsbvals;
  }
}
