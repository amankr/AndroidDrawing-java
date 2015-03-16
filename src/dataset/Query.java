package dataset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

import processing.core.PApplet;
import processing.core.PImage;

import bice.Bice;

import minhash.Entry;
import minhash.Index;
import minhash.ResultSet;

public class Query {

  void run(String[] args) {
    if(args.length < 2) {
      printUsage();
    }
    // load the dataset
    File dataset = new File(args[0]);
    File imgFile = new File(args[1]);
    if(!imgFile.exists()) {
      System.err.println("image file does not exist");
      printUsage();
    }
    long start = System.nanoTime();
    Index index = new IndexStorage(dataset).load();
    double loadTime = (System.nanoTime()-start)/1.0e9;
    if(index == null) System.exit(2);
    
    int targetIndex = findImageId(index.imageNames, imgFile);
    // load the image file and compute descriptors
    start = System.nanoTime();
    PApplet imageLoader = new PApplet();
    PImage img = imageLoader.loadImage(imgFile.getAbsolutePath());
    Bice bice = Bice.shadowdrawVersion();
    bice.setImage(img);
    int patchSize = 64*1;
    int patchStride = 32*1;
    
    List<BitSet> descriptors = new ArrayList<BitSet>();
    for(int y = 0; y < img.height-patchSize; y += patchStride) {
      for(int x = 0; x < img.width-patchSize; x += patchStride) {
        BitSet desc = bice.calc(x,y,patchSize);
        if(desc != null) { descriptors.add(desc); }
      }
    }
    double descriptorTime = (System.nanoTime()-start)/1.0e9;
    
    int[] thresholdValues = new int[] {1,2,4,8,16,32,64};
    start = System.nanoTime();
    
    for(BitSet desc : descriptors) {
      ResultSet found = index.findEntry(desc); // lookup
      
      // for several different thresholds
      for(int retrievalThreshold : thresholdValues) {
        int numCorrect = 0;
        int numFound = 0;
        for(List<Entry> es : found.getResults()) {
          for(Entry e : es) { // each of these is equally match worthy
            if(e.imgId == targetIndex) {
              numCorrect++;
            }
            numFound++;
          }
          if(numFound >= retrievalThreshold) break;
        }
        float pre = numCorrect/(float)numFound, rec = numCorrect/5f; // we hope that it matches nearby stuff
        System.out.println("precision/recall: "+pre+" "+rec+"  at threshold: "+ retrievalThreshold);
        // assume that the same # of patches existed in the input
      }
    }
    double lookupTime = (System.nanoTime()-start)/1.0e9;
    
    System.out.println("loadTime: "+loadTime);
    System.out.println("computeDescriptorTime: "+descriptorTime);
    System.out.println("lookupTime: "+lookupTime);
    System.out.println("timePerLookup: "+lookupTime/descriptors.size());
  }
  
  int findImageId(List<String> imageNames, File imgFile) {
    String name = imgFile.getName();
    int id = imageNames.indexOf(name); // if there's no prefix directory
    if(id >= 0) return id;
    for(int i = 0; i < imageNames.size(); i++) { // if maybe there's some extra stuff
      if(name.equals(new File(imageNames.get(i)).getName())) return i;
    }
    return -1;
  }

  void printUsage() {
    System.err.println("usage: dataset.Query dataset inputImage");
    System.exit(1);
  }
  
  public static void main(String[] args) {
    new Query().run(args);
  }
}
