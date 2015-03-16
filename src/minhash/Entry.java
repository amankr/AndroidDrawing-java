package minhash;

public class Entry implements Comparable<Entry> {
  public int sketch, imgId, x, y;
  public Entry(int _sketch, int _imgId, int _x, int _y) {
    sketch = _sketch;
    imgId = _imgId;
    x = _x;
    y = _y;
  }
  
  // compare based on the "intrinsic" values,
  // so that two entries from different tables are treated as if equal
  public int compareTo(Entry that) {
    return this.hashCode()-that.hashCode();
  }
  
  @Override
  public int hashCode() {
    return imgId ^ (x<<12) ^ (y<<8);
  }
  
  public boolean equals(Object _that) {
    if(!(_that instanceof Entry)) return false;
    Entry that = (Entry)_that;
    return this.imgId == that.imgId && this.x == that.x && this.y == that.y;
  }
}
