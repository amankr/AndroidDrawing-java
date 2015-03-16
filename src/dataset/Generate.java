package dataset;

import java.io.File;
import java.util.BitSet;

import bice.Bice;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

import minhash.Index;

/**
 * 
 * Generate a min-hash inverted file database by generating Bice descriptors for patches images in a specified directory.
 *
 */

public class Generate {

  File input, output;
  Index index;
  int patchStride = 32*1;
  int patchSize = 64*1;
  Bice bice;
  
  PApplet imageLoader;
  int imageId = 0; // current image
  
  int nonemptyDescriptors = 0, emptyDescriptors = 0;
  // for performance testing
  long 
    imageInputTime = 0,
    imageGradientsTime = 0,
    biceTime = 0,
    hashTime = 0,
    sortTime = 0,
    outputTime = 0,
    totalTime = 0;
  
  void run(String[] args) {
    if(args.length < 1) {
      printUsage();
    }
    //we'll have a look in the data folder
    input = new File(args[0]);
    output = new File(args[1]);
    if(!input.exists()) printUsage();
    if(output.exists()) {
      System.err.println("Output directory '"+args[1]+"' already exists, please pick a different location");
      System.exit(1);
    }
    
    long firstStart = System.nanoTime();
    bice = Bice.shadowdrawVersion();
    index = new Index(20,3,5,bice.getNumBits());
    index.generateHashes();
    imageLoader = new PApplet();
        
    walk(input);
    
    long start = System.nanoTime();
    index.reverseIndex();
    sortTime = System.nanoTime()-start;
    start = System.nanoTime();
    String sourceName = input.toString();
    new IndexStorage(output).write(sourceName, index);
    outputTime = System.nanoTime()-start;
    totalTime = System.nanoTime()-firstStart;
    
    System.out.println("imageInputTime: "+imageInputTime/1.0e9);
    System.out.println("imageGradientsTime: "+imageGradientsTime/1.0e9);
    System.out.println("biceTime: "+biceTime/1.0e9);
      System.out.println("  binTime: "+bice.binTime/1.0e9);
      System.out.println("  resampleTime: "+bice.resampleTime/1.0e9);
      System.out.println("  thresholdTime: "+bice.thresholdTime/1.0e9);
      System.out.println("  blurTime: "+bice.blurTime/1.0e9);
      System.out.println("  extractCoherentTime: "+bice.extractCoherentTime/1.0e9);
      System.out.println("  makeBitsetTime: "+bice.makeBitsetTime/1.0e9);
    System.out.println("hashTime: "+hashTime/1.0e9);
    System.out.println("sortTime: "+sortTime/1.0e9);
    System.out.println("outputTime: "+outputTime/1.0e9);
    System.out.println("\ntotalTime: "+totalTime/1.0e9);
    
    System.out.println("\n");
    System.out.println("percentageEmptyDescriptors: "+emptyDescriptors/((float)emptyDescriptors+nonemptyDescriptors));
  }

  void walk(File dir) {
    File listFile[] = dir.listFiles();
    if(listFile != null) {
      for(int i = 0; i < listFile.length; i++) {
        if(listFile[i].isDirectory()) {
          walk(listFile[i]);
        } else {
          if(isImage(listFile[i])) {
            processImage(listFile[i]);
          }
        }
      }
    }
  }
  
  void processImage(File f) {
    long start;
    // clean up the file name
    String imgName = f.getAbsolutePath().replace(input.getAbsolutePath(), "");
    if(imgName.startsWith("/")) imgName = imgName.substring(1);
    index.addImage(imgName);
    System.out.println("index size: "+bytesHumanize(index.getSize()));
    System.out.println("adding image: "+imgName);
    
    start = System.nanoTime();
    PImage img = imageLoader.loadImage(f.getAbsolutePath());
    imageInputTime += System.nanoTime()-start;
    start = System.nanoTime();
    bice.setImage(img); // precalc normalized gradients
    imageGradientsTime += System.nanoTime()-start;
    
    // this may lose up to 31 pixels from the right and bottom
    for(int y = 0; y < img.height-patchSize; y += patchStride) {
      for(int x = 0; x < img.width-patchSize; x += patchStride) {
        start = System.nanoTime();
        BitSet desc = bice.calc(x,y,patchSize);
        biceTime += System.nanoTime()-start;
        start = System.nanoTime();
        if(desc != null) {
          index.addEntry(desc, imageId, x, y);
          nonemptyDescriptors++;
        } else {
          emptyDescriptors++;
        }
        hashTime += System.nanoTime()-start;
      }
    }
    imageId++;
  }
  
  boolean isImage(File f) {
    return f.getName().endsWith(".jpg") || f.getName().endsWith(".jpeg");
  }
  
  String bytesHumanize(long bytes) {
    String[] suffix = new String[] {"b", "Kb", "Mb", "Gb", "Tb", "Pb"};
    double num = bytes;
    for(int i = 0; i < suffix.length; i++) {
      if(num < 1024) {
        return num+suffix[i];
      }
      num /= 1024;
    }
    return num+"Eb"; // Exb?
  }
  
  void printUsage() {
    System.err.println(
      "usage: dataset.Generate imageDirectory datasetOutputDirectory\n"+
      "  assumes that the input images are JPEGs"
    );
    System.exit(1);
  }
  
  public static void main(String[] args) {
    new Generate().run(args);
  }
}
