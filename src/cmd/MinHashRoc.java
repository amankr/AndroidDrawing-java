package cmd;

import java.util.BitSet;
import java.util.List;
import java.util.Random;

import minhash.Entry;
import minhash.Index;
import minhash.ResultSet;

public class MinHashRoc {

  /**
   * Build a random database containing numItems,
   * where each input descriptor contains about percentageFull true bits.
   * 
   */
  public static void main (String args[])
  {
    if(args.length < 3) {
      System.out.println("usage: java minHash.MinHash numItems percentageFull descriptorSize");
      return;
    }
    int numItems = Integer.parseInt(args[0]);
    float percentageFull = Float.parseFloat(args[1]);
    int descriptorSize = Integer.parseInt(args[2]);
    
    int numSet = (int)(descriptorSize*percentageFull); 
    
    BitSet[] descriptors = new BitSet[numItems];
    
    int sketchesPerInput = 20;
    Index index = new Index(3,sketchesPerInput,4,descriptorSize);
    index.generateHashes();
    Random r = new Random();
    
    for(int i = 0; i < descriptors.length; i++) {
      // set some random bits in the descriptor
      descriptors[i] = new BitSet();
      for(int j = 0; j < numSet; j++) {
        descriptors[i].set(r.nextInt(descriptorSize));
      }
      index.addEntry(descriptors[i], i, 0, 0);
    }
    index.reverseIndex();
    
    
    for(int i = 0; i < 200; i++) {
      BitSet target = (BitSet) descriptors[i].clone();
      ResultSet found = index.findEntry(target);
      int numCorrect = 0;
      int numFound = 0;
      for(List<Entry> es : found.getResults()) {
        for(Entry e : es) {
          if(e.imgId == 0) {
            numCorrect++;
          }
          numFound++;
          if(numFound >= 20) break; // here's the free variable along the ROC curve frontier
        }
      }
      System.out.println(i+" bits changed -> precision: "+numCorrect/(float)numFound
          + " recall: "+numCorrect/(float)sketchesPerInput);
      
      // flip a random bit
      target.flip(r.nextInt(descriptorSize));
    }
  }
}
