package com.example.shadowdraw;

import java.io.File;
import java.util.BitSet;

import dataset.IndexStorage;

import android.os.Debug;
import android.os.Environment;
import android.util.Log;
import bice.Bice;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

import minhash.Index;

public class Generate {
	File input, output;
	  Index index;
	  int patchSize = 64*2;
	  int patchStride = 32*2;
	  Bice bice;
	  long 
	    imageInputTime = 0,
	    imageGradientsTime = 0,
	    biceTime = 0,
	    hashTime = 0,
	    sortTime = 0,
	    outputTime = 0,
	    totalTime = 0;
	  
	  
	  PApplet imageLoader;
	  int imageId = 0; // current image
	  
	  int nonemptyDescriptors = 0, emptyDescriptors = 0;
	  
	  public Generate(){
		  input = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + 
	                "/Download/shadow");
		  output = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + 
	                "/Download/hashes");
		  
	  }
	  public void generate(){
		  
		  
		  bice = Bice.shadowdrawVersion();
		  index = new Index(20,3,5,bice.getNumBits());
		  index.generateHashes();
		  imageLoader = new PApplet();
		  
		  index.reverseIndex();
		  Log.e("Hashing", "begin");
		  walk(input);
		  Log.e("Hashing", "Done");
		  String sourceName = input.toString();
		  new IndexStorage(output).write(sourceName, index);
		  
		  
	  }
	  Index loadIndex(){
		  return  new IndexStorage(output).load();
	  }
	  void walk(File dir) {
		    File listFile[] = dir.listFiles();
		    if(listFile != null) {
		      for(int i = 0; i < listFile.length; i++) {
		        if(listFile[i].isDirectory()) {
		          walk(listFile[i]);
		        } else {
		          if(isImage(listFile[i])) {
		        	  Log.e("Hashing",listFile[i].getName() );
		        	  processImage(listFile[i]);
		          }
		        }
		      }
		    }
		  }
	  void processImage(File f) {
		  	Log.e("Hashing","process Image");
		  	Log.e("error",f.toString() );
		    long start;
		    // clean up the file name
		    String imgName = f.getAbsolutePath().replace(input.getAbsolutePath(), "");
		    if(imgName.startsWith("/")) imgName = imgName.substring(1);
		    index.addImage(imgName);
		  //  System.out.println("index size: "+bytesHumanize(index.getSize()));
		   // System.out.println("adding image: "+imgName);
		    
		    start = System.nanoTime();
		    Log.e("error", "6");
		    PImage img = imageLoader.loadImage(f.getAbsolutePath());
		    Log.e("error", "7");
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
		    Log.e("Hashing","Non - emptyDescriptors  : "+nonemptyDescriptors +" empty : "+ emptyDescriptors);
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
}
