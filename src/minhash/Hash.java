package minhash;

import java.util.BitSet;
import java.util.Random;

public class Hash {
  public int[][] indices;
  int bph;
  
  /**
   * numMins = number of mins to find per sketch
   * 2^numCheckBits = number of bits to check per min find
   * 
   */
  Hash(int numMins, int bitsPerHash, int numDescriptorBits) {
    bph = bitsPerHash;
    Random r = new Random();
    int numBitsToCheck = (int)Math.pow(2,bitsPerHash);
    indices = new int[numMins][numBitsToCheck];
    for(int i = 0; i < numMins; i++) {
      for(int j = 0; j < numBitsToCheck; j++) {
        indices[i][j] = r.nextInt(numDescriptorBits); // TODO: make this avoid duplicates
      }
    }
  }
  
  // for loading from disk
  public Hash(int[][] indices) {
    this.indices = indices;
    int r = indices.length, c = indices[0].length;
    // need enough bits to covert the range of possible values
    bph = (int) Math.ceil(Math.log(c)/Math.log(2));
    indices = new int[r][c];
  }
  
  
  int calcSignature(BitSet descriptor) {
    int numMins = indices.length, numBitsToCheck = indices[0].length;
    int sig = 0;
    for(int i = 0; i < numMins; i++) { // for each min to find
      sig <<= bph; // shuffle over existing bits
      for(int j = 0; j < numBitsToCheck; j++) {
        if(descriptor.get(indices[i][j])) {
          sig |= j;
          break;
        }
      }
    }
    return sig;
  }

  public int getSize() {
    return indices.length*indices[0].length*4;
  }
}
