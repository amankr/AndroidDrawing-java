package bice;


class Gaussian {
  float normalizer, mu, twoSigmaSq;
  Gaussian(float sigma, float mu_) {
    mu = mu_;
    twoSigmaSq = 2*sigma*sigma;
    normalizer = (float) (1/Math.sqrt(Math.PI*twoSigmaSq));
  }
  float at(float x) {
    x -= mu;
    return (float) (normalizer*Math.exp(-x*x/twoSigmaSq));
  }
}

