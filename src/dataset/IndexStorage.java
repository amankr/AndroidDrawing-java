package dataset;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import minhash.Entry;
import minhash.Hash;
import minhash.Index;
import minhash.Table;

import static dataset.Util.*;

public class IndexStorage {

  File dir;
  public IndexStorage(File output_) {
    dir = output_;
  }
  
  public void write(String sourceName, Index index) {
    try {
      if(!dir.mkdirs()) throw new IllegalStateException();
      
      PrintWriter f;
      f = writer("info");
      f.println(index.sketchesPerInput);
      f.println(index.hashesPerSketch);
      f.println(index.bitsPerHash);
      f.println(index.bitsPerDescriptor);
      f.println(sourceName);
      f.close();
      // TODO: sanity check that these values match the data below that they refer to
      
      f = writer("images");
      for(String fname : index.imageNames) {
        f.println(fname);
      }
      f.close();
      
      // write out the hash tables
      for(int i = 0; i < index.tables.length; i++) {
        f = writer("table"+i);
        Table table = index.tables[i];
        for(Entry e : table.entries) {
          f.println(e.sketch+" "+e.imgId+" "+e.x+" "+e.y);
        }
        f.close();
      }
      
      // write out the hash function "permutation" tables
      for(int i = 0; i < index.tables.length; i++) {
        f = writer("hash"+i);
        Hash hash = index.tables[i].hash;
        for(int j = 0; j < hash.indices.length; j++) {
          for(int k = 0; k < hash.indices[j].length; k++) {
            f.print(hash.indices[j][k]);
            f.print(" ");
          }
          f.println();
        }
        f.close();
      }
      
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  PrintWriter writer(String name) throws IOException {
    return new PrintWriter(new BufferedWriter(new FileWriter(new File(dir, name))));
  }

  BufferedReader reader(String name) throws FileNotFoundException {
    return new BufferedReader(new FileReader(new File(dir, name)));
  }
  
  int readInt(BufferedReader br) throws NumberFormatException, IOException {
    return Integer.parseInt(br.readLine());
  }
  
  public Index load() {
    try {
      if(!dir.exists()) throw new IllegalStateException();
      
      BufferedReader f;
      f = reader("info");
      Index index = new Index(
         readInt(f),readInt(f),readInt(f),readInt(f)
      );
      f.close();
      
      f = reader("images");
      String name = f.readLine();
      while(name != null) {
        index.imageNames.add(name);
        name = f.readLine();
      }
      f.close();
      
      // write out the hash tables
      for(int i = 0; i < index.tables.length; i++) {
        f = reader("table"+i);
        Table table = index.tables[i];
        String line = f.readLine();
        while(line != null) {
          int[] vals = lineInts(line);
          table.entries.add(new Entry(vals[0], vals[1], vals[2], vals[3]));
          line = f.readLine();
        }
        table.buildLookup();
        f.close();
      }
      
      
      // write out the hash function "permutation" tables
      for(int i = 0; i < index.tables.length; i++) {
        f = reader("hash"+i);
        int[][] indices = new int[index.hashesPerSketch][];
        String line = f.readLine();
        int j = 0;
        while(line != null) {
           indices[j] = lineInts(line);
           j++;
           line = f.readLine();
        }
        index.tables[i].hash = new Hash(indices);
        f.close();
      }
      return index;
      
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}
