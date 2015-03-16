package bice;

import static processing.core.PApplet.*;

class Histogram3 {
  float[][][] values;
  float[][] sums;
  float xLow,xMult,yLow,yMult,tLow,tMult;
  int xBuckets, yBuckets, tBuckets;
  int invalid = 0, valid = 0;
  
  Histogram3(int xBuckets_, int yBuckets_, int tBuckets_) {
    xBuckets = xBuckets_;
    yBuckets = yBuckets_;
    tBuckets = tBuckets_;
    values = new float[tBuckets][xBuckets][yBuckets];
  }
  Histogram3(
    float minX, float maxX, int xBuckets_,
    float minY, float maxY, int yBuckets_,
    float minT, float maxT, int tBuckets_
  ) {
    xBuckets = xBuckets_;
    yBuckets = yBuckets_;
    tBuckets = tBuckets_;
    xMult = xBuckets/(maxX-minX); xLow = minX;
    yMult = yBuckets/(maxY-minY); yLow = minY;
    tMult = tBuckets-0.01f/(maxT-minT); tLow = minT; // subtract a small amount to keep PI values in range
    values = new float[tBuckets][xBuckets][yBuckets];
  }
  void increment(float x, float y, float t, float gHat) {
    // TODO: linear soft binning, update several buckets (8?)
    int
    tb = (int)((t-tLow)*tMult),
    xb = (int)((x-xLow)*xMult),
    yb = (int)((y-yLow)*yMult);
    try {
      values[tb][xb][yb] += gHat;
      valid++;
    } catch(ArrayIndexOutOfBoundsException ex) {
      invalid++;
    }
  }
  
  // parameters are from the input space
  float getValue(float x, float y, float t) {
    // this ignores the circular-ness of t
    float
    tb = constrain((t-tLow)*tMult - 0.5f, 0, tBuckets-1),
    xb = constrain((x-xLow)*xMult - 0.5f, 0, xBuckets-1),
    yb = constrain((y-yLow)*yMult - 0.5f, 0, yBuckets-1);
    return getBucketSoft(tb,xb,yb);
  }
  
  // non-integer bucket indices
  float getBucketSoft(float xb, float yb, float tb) {
    int
    tbl = (int)tb, tbr = (int)ceil(tb),
    xbl = (int)xb, xbr = (int)ceil(xb),
    ybl = (int)yb, ybr = (int)ceil(yb);
    float tt = tb-tbl, tx = xb-xbl, ty = yb-ybl; 
    try {
      return
      lerp(
        lerp(
            lerp(values[tbl][xbl][ybl], values[tbr][xbl][ybl], tt),
            lerp(values[tbl][xbr][ybl], values[tbr][xbr][ybl], tt),
            tx),
        lerp(
            lerp(values[tbl][xbl][ybr], values[tbr][xbl][ybr], tt),
            lerp(values[tbl][xbr][ybr], values[tbr][xbr][ybr], tt),
            tx),
        ty);
    } catch(ArrayIndexOutOfBoundsException ex) {
      return 0;
    }
  }
  
  Histogram3 blur() {
    Histogram3 output1 = new Histogram3(xBuckets, yBuckets, tBuckets);
    Histogram3 output2 = new Histogram3(xBuckets, yBuckets, tBuckets);
    this._blur(output1, 0, 1);
    output1._blur(output2, 1, 1);
    output2._blur(output1, 2, 3);
    return output1;
  }
  
  void _blur(Histogram3 output, int dimension, float sigma) {
    int blurRadius = (int)(3 * sigma);
    int blurKernelSize = blurRadius*2 + 1;
    int zero = blurKernelSize/2; 
    Gaussian gau = new Gaussian(sigma, 0);
    float[] blurKernel = new float[blurKernelSize];
    for(int i = 0; i < blurKernel.length; i++) {
      blurKernel[i] = gau.at(i-zero);
    }
    
    float sum, cb;
    int read, bk0, width;
    float[][][] ovalues = output.values;
    
    if(dimension == 0) {
      width = tBuckets;
      for(int t = 0; t < tBuckets; t++) {
        for(int x = 0; x < xBuckets; x++) {
          for(int y = 0; y < yBuckets; y++) {
            cb = sum = 0;
            read = t - blurRadius;
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
              float c = values[read][x][y];
              float mult = blurKernel[i];
              cb += c*mult;
              sum += mult;
              read++;
            }
            ovalues[t][x][y] = cb / sum;
          }
        }
      }
    } else if(dimension == 1) {
      width = xBuckets;
      for(int t = 0; t < tBuckets; t++) {
        for(int x = 0; x < xBuckets; x++) {
          for(int y = 0; y < yBuckets; y++) {
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
              float c = values[t][read][y];
              float mult = blurKernel[i];
              cb += c*mult;
              sum += mult;
              read++;
            }
            ovalues[t][x][y] = cb / sum;
          }
        }
      }
    } else if(dimension == 2) {
      width = yBuckets;
      for(int t = 0; t < tBuckets; t++) {
        for(int x = 0; x < xBuckets; x++) {
          for(int y = 0; y < yBuckets; y++) {
            cb = sum = 0;
            read = y - blurRadius;
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
              float c = values[t][x][read];
              float mult = blurKernel[i];
              cb += c*mult;
              sum += mult;
              read++;
            }
            ovalues[t][x][y] = cb / sum;
          }
        }
      }
    }
  }
  
  Histogram3 blurSlow() {
    Histogram3 output = new Histogram3(xBuckets, yBuckets, tBuckets);
    GaussianTable3D gt3d = new GaussianTable3D();
    
    float[][][] ovalues = output.values;
    for(int t = 0; t < tBuckets; t++) {
      for(int y = 0; y < yBuckets; y++) {
        for(int x = 0; x < xBuckets; x++) {
          float weightSum = 0;
          float sum = 0;
          int ntEnd = min(tBuckets, t+gt3d.tHalfSize);
          int nyEnd = min(yBuckets, y+gt3d.yHalfSize);
          int nxEnd = min(xBuckets, x+gt3d.xHalfSize);
          for(int nt = max(0,t-gt3d.tHalfSize); nt < ntEnd; nt++) {
            for(int nx = max(0,x-gt3d.xHalfSize); nx < nxEnd; nx++) {
              for(int ny = max(0,y-gt3d.yHalfSize); ny < nyEnd; ny++) {
                float weight = gt3d.at(t-nt, x-nx, y-ny);
                sum += values[nt][nx][ny]*weight;
                weightSum += weight;
              }
            }
          }
          ovalues[t][x][y] = sum/weightSum;
        }
      }
    }
    return output;
  }

  float[] resample(int xBucketsNew, int yBucketsNew, int tBucketsNew) {
    float[] vals = new float[tBucketsNew*yBucketsNew*xBucketsNew];
    int i = 0;
    float
    tStep = tBuckets/(float)tBucketsNew,
    yStep = yBuckets/(float)yBucketsNew,
    xStep = xBuckets/(float)xBucketsNew;
    for(float t = tStep/2; t < tBucketsNew; t += tStep) {
      for(float y = yStep/2; y < yBucketsNew; y += yStep) {
        for(float x = xStep/2; x < xBucketsNew; x += xStep) {
          vals[i] = getBucketSoft(x, y, t);
          i++;
        }
      }
    }
    return vals;
  }

  float[] flatten() {
    float[] vals = new float[getSize()];
    int i = 0;
    for(int t = 0; t < tBuckets; t++) {
      for(int y = 0; y < yBuckets; y++) {
        for(int x = 0; x < xBuckets; x++) {
          vals[i] = values[t][x][y]; i++;
        }
      }
    }
    return vals;
  }

  void threshold(float value) {
    for(int t = 0; t < tBuckets; t++) {
      for(int y = 0; y < yBuckets; y++) {
        for(int x = 0; x < xBuckets; x++) {
          if(values[t][x][y] > value) { 
            values[t][x][y] = 1;
          } else {
            values[t][x][y] = 0;
          }
        }
      }
    }
  }
  
  int getSize() {
    return tBuckets*yBuckets*xBuckets;
  }
  
  float getBinSum() {
    float sum = 0;
    for(int t = 0; t < tBuckets; t++) {
      for(int x = 0; x < xBuckets; x++) {
        for(int y = 0; y < yBuckets; y++) {
          sum += values[t][x][y];
        }
      }
    }
    return sum;
  }
}
