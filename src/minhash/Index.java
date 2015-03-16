package minhash;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;


public class Index {
    public int sketchesPerInput;
    public int hashesPerSketch;
    public int bitsPerHash;
    public int bitsPerDescriptor;
    
    public Table[] tables;
    public List<String> imageNames;
    
	// the constructor will take in the BiCE Descriptor size and the values of n and k for tuning
	public Index(int sketchesPerInput_, int hashesPerSketch_, int bitsPerHash_, int bitsPerDescriptor_) {
		sketchesPerInput = sketchesPerInput_;
		hashesPerSketch = hashesPerSketch_;
		bitsPerHash = bitsPerHash_;
		bitsPerDescriptor = bitsPerDescriptor_;
		
		tables = new Table[sketchesPerInput];
		imageNames = new ArrayList<String>();
		
		for(int i = 0; i < tables.length; i++) {
      tables[i] = new Table(null);
    }
	}
	
	public void generateHashes() {
    for(int i = 0; i < tables.length; i++) {
      tables[i].hash = new Hash(hashesPerSketch, bitsPerHash, bitsPerDescriptor);
    }
	}
	
	// call after reverseIndex only
	public ResultSet findEntry(BitSet descriptor) {
	  // lookup from each table
	  ResultSet found = new ResultSet(tables.length);
    for(int i = 0; i < tables.length; i++) {
      tables[i].find(descriptor, found);
    }
    found.sortByMatchCount();
    return found;
  }
	
	public void addEntry(BitSet descriptor, int imgId, int x, int y) {
	  // add to each table
    for(int i = 0; i < tables.length; i++) {
      tables[i].addEntry(descriptor, imgId, x, y);
    }
  }
	
	public void reverseIndex() {
	  for(int i = 0; i < tables.length; i++) {
      tables[i].reverseIndex();
    }
	}

  public void addImage(String name) {
    imageNames.add(name);
  }

  public long getSize() {
    long sum = 0;
    for(int i = 0; i < tables.length; i++) {
      sum += tables[i].getSize();
    }
    return sum;
  }
}
