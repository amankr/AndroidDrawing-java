package dataset;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import static dataset.Util.*;

public class SegmentationToImage {

  File input, output;
  Map<String,List<File>> segFiles = new HashMap<String, List<File>>();
  
  void run(String[] args) {
    if(args.length < 2) printUsage();
    input = new File(args[0]);
    output = new File(args[1]);
    if(!input.exists()) {
      System.err.println("input directory doesn't exist");
      printUsage();
    }
    if(output.exists()) {
      System.err.println("output directory should not exist (but it does)");
      printUsage();
    }
    
    // collect files
    walk(input);
    PApplet papplet = new PApplet();
    
    output.mkdirs();
    for(Map.Entry<String, List<File>> entry : segFiles.entrySet()) {
      PImage img = createEdges(entry.getValue(), papplet);
      String outputFile = new File(output, entry.getKey().replace(".seg", ".jpg")).getAbsolutePath();
      System.out.println(outputFile);
      img.save(outputFile);
    }
  }
  
  void printUsage() {
    System.err.println("usage: dataset.ShowSegmentation inputDir outputDir");
    System.err.println("       takes several directories of .seg files and produces images of their edges");
    System.exit(1);
  }
  
  // collect all of the files with the same name into a map
  void walk(File dir) {
    File listFile[] = dir.listFiles();
    if(listFile != null) {
      for(int i = 0; i < listFile.length; i++) {
        if(listFile[i].isDirectory()) {
          walk(listFile[i]);
        } else {
          if(listFile[i].getName().endsWith(".seg")) {
            String name = listFile[i].getName();
            List<File> files = segFiles.get(name);
            if(files == null) {
              files = new ArrayList<File>();
              segFiles.put(name, files);
            }
            files.add(listFile[i]);
          }
        }
      }
    }
  }

  PImage createEdges(List<File> files, PApplet papplet) {
      
    int width = 0, height = 0;
    int[] counts = new int[0];
    for(File f : files) {
      String[] lines = papplet.loadStrings(f.getAbsolutePath());
      boolean data = false;
      for(int j = 0; j < lines.length; j++) {
        String line = lines[j];
        if(data) {
          int[] vals = lineInts(line);
           int s = vals[0], r = vals[1], c1 = vals[2], c2 = vals[3];
           if(c1 > 0) counts[c1 + r*width]++;
           //if(c2 < width) counts[c2 + r*width]++;
        } else {
          if(line.startsWith("data")) {
            if(width == 0 || height == 0) throw new IllegalStateException();
            if(counts.length == 0) {
              counts = new int[width*height];
            }
            data = true;
          } else if(line.startsWith("width")) {
            width = Integer.parseInt(line.split(" ")[1]);
          } else if(line.startsWith("height")) {
            height = Integer.parseInt(line.split(" ")[1]);
          }
          // TODO: parse header
        }
      }
    }
    PImage img = papplet.createImage(width, height, PConstants.RGB);
    img.loadPixels();
    for(int i = 0; i < img.pixels.length; i++) {
      img.pixels[i] = papplet.color(255-counts[i]*255f/files.size());
    }
    img.updatePixels();
    img.filter(PConstants.BLUR, 0.7f); 
    return img;
  }

  public static void main(String[] args) {
    new SegmentationToImage().run(args);
  }
}
