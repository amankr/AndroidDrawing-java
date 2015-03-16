package bice;

// TODO: replace this with 1D convolutions along each dimension
class GaussianTable3D {
  static final float xBlurSigma = 1, yBlurSigma = 3, tBlurSigma = 1;

  float[][][] table;
  int xHalfSize, yHalfSize, tHalfSize;
  
  GaussianTable3D() {
    tHalfSize = (int)(tBlurSigma*3f);
    xHalfSize = (int)(xBlurSigma*3f);
    yHalfSize = (int)(yBlurSigma*3f);
    int tSize = tHalfSize*2+1, xSize = xHalfSize*2+1, ySize = yHalfSize*2+1;
    Gaussian tGau = new Gaussian(tBlurSigma,0);
    Gaussian xGau = new Gaussian(xBlurSigma,0);
    Gaussian yGau = new Gaussian(yBlurSigma,0);
    table = new float[tSize][xSize][ySize];
    float sum = 0;
    for(int t = 0; t < tSize; t++) {
      for(int x = 0; x < xSize; x++) {
        for(int y = 0; y < ySize; y++) {
          // FIXME: is this offset by half a pixel?
          float lt = t-tHalfSize;
          float lx = x-xHalfSize;
          float ly = y-yHalfSize;
          float val = xGau.at(lx)*yGau.at(ly)*tGau.at(lt);
          sum += val;
          table[t][x][y] = val;
        }
      }
    }
    // normalize to 1, may not be crucial
    float normalizer = 1f/sum;
    for(int t = 0; t < tSize; t++) {
      for(int x = 0; x < xSize; x++) {
        for(int y = 0; y < ySize; y++) {
          table[t][x][y] *= normalizer;
        }
      }
    }
  }
  float at(int t, int x, int y) {
    return table[t+tHalfSize][x+xHalfSize][y+yHalfSize];
  }
}