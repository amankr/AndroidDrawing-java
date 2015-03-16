package dataset;

import java.util.BitSet;

public class Util {
  
  public static int[] lineInts(String line) {
    String[] vals = line.split(" ");
    int[] result = new int[vals.length];
    for(int i = 0; i < vals.length; i++) {
      result[i] = Integer.parseInt(vals[i]);
    }
    return result;
  }
  
  public static float jaccardSimilarity(BitSet a, BitSet b) {
    BitSet and = (BitSet) a.clone();
    BitSet or = (BitSet) a.clone();
    and.and(b);
    or.or(b);
    return and.cardinality()/(float)or.cardinality();
  }
}
