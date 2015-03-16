package minhash;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Table {
  
  class EntryComparator implements Comparator<Entry> {
	  public int compare(Entry a, Entry b) {
	    return a.sketch - b.sketch;
	  }
	}
	
  public List<Entry> entries;
  public Map<Integer,Integer> lookup;
  public Hash hash;
	
	public Table(Hash hash_) {
		entries = new ArrayList<Entry>();
		lookup = new TreeMap<Integer,Integer>();
		hash = hash_;
	}
	
	void addEntry(BitSet descriptor, int imgId, int x, int y) {
	  Entry entry = new Entry(hash.calcSignature(descriptor), imgId, x, y);
    entries.add(entry);
	}
	
	void reverseIndex() {
	  Collections.sort(entries, new EntryComparator());
	  buildLookup();
	}
	
	public void buildLookup() {
	  lookup.clear();
    int lastSketch = -1;
    for(int i = 0; i < entries.size(); i++) {
      Entry e = entries.get(i);
      if(e.sketch != lastSketch) {
        lastSketch = e.sketch;
        lookup.put(e.sketch, i);
      }
    }
	}
	
	void find(BitSet descriptor, ResultSet found) {
	  Integer startIndex = lookup.get(hash.calcSignature(descriptor));
	  if(startIndex == null) return; // if not found, return
	  // if found, add everything from the bucket
	  int i = startIndex;
	  Entry e = entries.get(i);
	  int sketch = e.sketch;
	  while(e.sketch == sketch) {
	    found.add(e);
	    i++;
	    if(i >= entries.size()) break;
	    e = entries.get(i);
	  }
  	}

  public long getSize() {
    return entries.size()*16
         + hash.getSize()
         + lookup.size()*8; // this is an underestimate
  }	
}
