package main.gov.nist.isg.mist.stitching.lib.imagetile.utilfns;

public class IndexValuePair implements Comparable<IndexValuePair>{

  private int index;
  private double value;


  /**
   * @param index
   * @param value
   */
  public IndexValuePair(int index, double value) {
    super();
    this.index = index;
    this.value = value;
  }

  /**
   * @return the index
   */
  public int getIndex() {
    return this.index;
  }

  /**
   * @param index the index to set
   */
  public void setIndex(int index) {
    this.index = index;
  }

  /**
   * @return the value
   */
  public double getValue() {
    return this.value;
  }

  /**
   * @param value the value to set
   */
  public void setValue(double value) {
    this.value = value;
  }

  @Override
  public int compareTo(IndexValuePair arg0) {
    int val = Double.compare(arg0.getValue(),this.getValue());

    if (val == 0)
    {
      return new Integer(this.getIndex()).compareTo(arg0.getIndex());
    }
    return val;

  }


}
