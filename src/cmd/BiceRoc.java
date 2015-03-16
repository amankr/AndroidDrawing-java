package cmd;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import dataset.Util;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import bice.Bice;

public class BiceRoc {

  class Classification implements Comparable<Classification> {
    float similarity;
    boolean positive;
    
    public Classification(float _similarity, boolean _positive) {
      similarity = _similarity;
      positive = _positive;
    }

    // sort in descending order
    public int compareTo(Classification that) {
      return -Float.compare(this.similarity, that.similarity);
    }
  }
  
  PImage
  left, leftInterior,
  right, rightInterior;
  
  void run(String[] args) {
    if(args.length < 2) {
      printUsage();
    }
    // load the two images
    PApplet p = new PApplet();
    left = p.loadImage(args[0]);
    leftInterior = p.loadImage(args[1]);
    right = p.loadImage(args[2]);
    rightInterior = p.loadImage(args[3]);
    
    List<PVector> roc4 = calcRoc(Bice.solidShadowdrawVersion(4));
    List<PVector> roc8 = calcRoc(Bice.solidShadowdrawVersion(8));
    List<PVector> roc16 = calcRoc(Bice.solidShadowdrawVersion(16));
    List<PVector> roc = calcRoc(Bice.shadowdrawVersion());
    
    int w = 400, h = 400;
    int border = 10;
    p.g = p.createGraphics(w+border, h+border, PApplet.JAVA2D);
    p.g.beginDraw();
    {
      p.background(255);
      p.noFill();
      p.stroke(0);
      p.smooth();
      for(int y = 0; y <= h; y += h/10) p.line(0, y, border-1, y);
      for(int x = border-1; x < w+border; x += w/10) p.line(x, h, x, h+border);
      p.strokeWeight(3);
      
      p.stroke(0,0,150,200);
      p.beginShape(); for(PVector v : roc) p.vertex(border+v.x*w, h-v.y*h); p.endShape();
      
      p.stroke(220,0,0,200);
      p.beginShape(); for(PVector v : roc4) p.vertex(border+v.x*w, h-v.y*h); p.endShape();
      
      p.stroke(240,180,0,200);
      p.beginShape(); for(PVector v : roc8) p.vertex(border+v.x*w, h-v.y*h); p.endShape();
          
      p.stroke(0,240,40,200);
      p.beginShape(); for(PVector v : roc16) p.vertex(border+v.x*w, h-v.y*h); p.endShape();
    }
    p.g.endDraw();
    p.saveFrame("/Users/markluffel/Desktop/roc.png");
  }
  
  private List<PVector> calcRoc(Bice bice) {
    int patchSize = 64*1;
    int patchStride = 32*1;
    
    // compute all of the left
    bice.setImage(left);
    bice.setInteriorImage(leftInterior);
    List<BitSet> leftDescriptors = new ArrayList<BitSet>();
    for(int y = 0; y < left.height-patchSize; y += patchStride) {
      for(int x = 0; x < left.width-patchSize; x += patchStride) {
        leftDescriptors.add(bice.calc(x,y,patchSize));
      }
    }
    // compute all of the right
    bice.setImage(right);
    bice.setInteriorImage(rightInterior);
    List<BitSet> rightDescriptors = new ArrayList<BitSet>();
    for(int y = 0; y < left.height-patchSize; y += patchStride) {
      for(int x = 0; x < left.width-patchSize; x += patchStride) {
        rightDescriptors.add(bice.calc(x,y,patchSize));
      }
    }
    
    // compare all pairwise
    List<Classification> results = new ArrayList<Classification>();
    for(int i = 0; i < leftDescriptors.size(); i++) {
      for(int j = i; j < rightDescriptors.size(); j++) {
        BitSet l = leftDescriptors.get(i);
        BitSet r = rightDescriptors.get(j);
        if(l != null && r != null) {
          results.add(new Classification(
              Util.jaccardSimilarity(l,r),
              i == j
          ));
        }
      }
    }
    
    Collections.sort(results);
    int pos = 0, neg = 0;
    for(Classification c : results) {
      if(c.positive) pos++; else neg++;
    }
    
    int tp = 0, fp = 0; // true positives and false positives
    List<PVector> roc = new ArrayList<PVector>();
    for(Classification c : results) {
      if(c.positive) tp++; else fp++;
      float x = fp/(float)neg, y = tp/(float)pos;
      roc.add(new PVector(x,y,0));
    }
    return roc;
  }

  void printUsage() {
    // the two images should be the same size, have the same content, be roughly aligned
    // we're measuring how robust the descriptor is to the difference between the images
    System.err.println("usage: dataset.BiceRoc leftImage rightImage");
    System.exit(1);
  }
  
  public static void main(String[] args) {
    new BiceRoc().run(args);
  }
}
