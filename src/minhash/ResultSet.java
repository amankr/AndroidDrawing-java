package minhash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultSet {
  Map<Entry,Integer> map;
  private List<List<Entry>> results; 
  
  ResultSet(int maxCount) {
    map = new HashMap<Entry,Integer>();
    results = new ArrayList<List<Entry>>();
    for(int i = 0; i < maxCount; i++) {
      getResults().add(new ArrayList<Entry>());
    }
  }
  
  public void add(Entry e) {
    Integer count = map.get(e);
    if(count == null) {
      map.put(e, 1);
    } else {
      map.put(e, count+1);
    }
  }

  public void sortByMatchCount() {
    int maxCount = results.size();
    for(Map.Entry<Entry, Integer> kv : map.entrySet()) {
      getResults().get(maxCount-kv.getValue()).add(kv.getKey());
    }
  }

  public List<List<Entry>> getResults() {
    return results;
  }

}
