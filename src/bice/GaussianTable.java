package bice;


class GaussianTable {
  int size, halfSize, zero;
  float[] values;
  
  GaussianTable(float sigma, float mu) {
    this(sigma, mu, 3f);
  }
  
  GaussianTable(float sigma, float mu, float sigmaThreshold) {
    // halfSize is the distance in pixels
    //  between the center of the center pixel (the maximum value)
    //  and the center of a pixel on the boundary
    halfSize = (int)(sigma*sigmaThreshold);
    size = halfSize*2 + 1;
    values = new float[size*size];
    zero = size*halfSize + halfSize; // index of the center pixel
    Gaussian g = new Gaussian(sigma, mu);
    // TODO: make a 1D table to optimize
    float sum = 0;
    for(int x = -halfSize; x <= halfSize; x++) {
      for(int y = -halfSize; y <= halfSize; y++) {
        float val = g.at(x)*g.at(y);
        sum += val;
        values[zero + x + y*size] = val;
      }
    }
    // we've truncated, so let's be careful that we sum to 1
    float normalizer = 1f/sum;
    for(int i = 0; i < values.length; i++) {
      values[i] *= normalizer;
    }
  }
  float at(int x, int y) {
    if(x < -halfSize || x > halfSize || y < -halfSize || y > halfSize)
      throw new IllegalArgumentException();
    return values[zero + x + y*size];
  }
}