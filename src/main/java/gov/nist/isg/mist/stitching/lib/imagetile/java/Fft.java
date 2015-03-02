/****************************************************************************
 * Copyright (c) 2009, Colorado School of Mines and others. All rights reserved. This program and
 * accompanying materials are made available under the terms of the Common Public License - v1.0,
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Original source code repository found: https://github.com/dhale/jtk
 * 
 * Any license provisions that differ from above are offered by the license below.
 ****************************************************************************/

// ================================================================
//
// Disclaimer: IMPORTANT: This software was developed at the National
// Institute of Standards and Technology by employees of the Federal
// Government in the course of their official duties. Pursuant to
// title 17 Section 105 of the United States Code this software is not
// subject to copyright protection and is in the public domain. This
// is an experimental system. NIST assumes no responsibility
// whatsoever for its use by other parties, and makes no guarantees,
// expressed or implied, about its quality, reliability, or any other
// characteristic. We would appreciate acknowledgement if the software
// is used. This software can be redistributed and/or modified freely
// provided that any derivative works bear some notice that they are
// derived from it, and any modified versions bear some notice that
// they have been modified.
//
// ================================================================

// ================================================================
//
// Modified by: tjb3
// Date: Aug 1, 2013 4:25:55 PM EST
//
// Time-stamp: <Aug 1, 2013 4:25:55 PM tjb3>
//
//
// ================================================================

package gov.nist.isg.mist.stitching.lib.imagetile.java;

import edu.mines.jtk.dsp.FftComplex;
import edu.mines.jtk.dsp.FftReal;
import edu.mines.jtk.dsp.Sampling;
import edu.mines.jtk.util.Check;

/**
 * FFT function adapted from mines.edu
 * 
 * @author Tim Blattner
 * @version 1.0
 * 
 */
public class Fft {
  /**
   * Constructs an FFT with specified numbers of space samples. Sampling intervals are 1.0 and first
   * sample coordinates are 0.0.
   * 
   * @param nx1 number of samples in the 1st dimension.
   * @param nx2 number of samples in the 2nd dimension.
   */
  public Fft(int nx1, int nx2) {
    this(new Sampling(nx1, 1.0, 0.0), new Sampling(nx2, 1.0, 0.0));
  }

  /**
   * Constructs an FFT with specified space sampling.
   * 
   * @param sx1 space sampling for the 1st dimension.
   * @param sx2 space sampling for the 2nd dimension.
   */
  public Fft(Sampling sx1, Sampling sx2) {
    this(sx1, sx2, null);
  }


  /**
   * Constructs an FFT with specified space sampling.
   * 
   * @param sx1 space sampling for the 1st dimension.
   * @param sx2 space sampling for the 2nd dimension.
   * @param sx3 space sampling for the 3rd dimension.
   */
  public Fft(Sampling sx1, Sampling sx2, Sampling sx3) {
    this._sx1 = sx1;
    this._sx2 = sx2;
    this._sx3 = sx3;
    this._sign1 = -1;
    this._sign2 = -1;
    this._sign3 = -1;
    updateSampling1();
    updateSampling2();
    updateSampling3();
  }

  /**
   * Gets the frequency sampling for the 1st dimension.
   * 
   * @return the frequency sampling.
   */
  public Sampling getFrequencySampling1() {
    return this._sk1;
  }

  /**
   * Gets the frequency sampling for the 2nd dimension.
   * 
   * @return the frequency sampling.
   */
  public Sampling getFrequencySampling2() {
    return this._sk2;
  }

  /**
   * Gets the frequency sampling for the 3rd dimension.
   * 
   * @return the frequency sampling.
   */
  public Sampling getFrequencySampling3() {
    return this._sk3;
  }

  /**
   * Sets the type of input (output) values for forward (inverse) transforms. The default type is
   * real.
   * 
   * @param complex true, for complex values; false, for real values.
   */
  public void setComplex(boolean complex) {
    if (this._complex != complex) {
      this._complex = complex;
      updateSampling1();
    }
  }

  /**
   * Sets the ability of this transform to overwrite specified arrays. The array specified in an
   * inverse transform is either copied or overwritten internally by the inverse transform. Copying
   * preserves the values in the specified array, but wastes memory in the case when those values
   * are no longer needed. If overwrite is true, then the inverse transform will be performed in
   * place, so that no copy is necessary. The default is false.
   * 
   * @param overwrite true, to overwrite; false, to copy.
   */
  public void setOverwrite(boolean overwrite) {
    this._overwrite = overwrite;
  }

  /**
   * Applies a forward space-to-frequency transform of a 2D array.
   * 
   * @param f the array to be transformed, a sampled function of space.
   * @return the transformed array, a sampled function of frequency.
   */
  public float[][] applyForward(float[][] f) {
    ensureSamplingX2(f);
    float[][] fpad = pad(f);
    applyForwardPadded(fpad);
    return fpad;
  }

  /**
   * Applies a forward space-to-frequency transform of a 2D array. f is already padded.
   * 
   * @param f the array to be transformed, a sampled function of space.
   */
  public void applyForwardPadded(float[][] f) {
    int nx2 = this._sx2.getCount();
    if (this._complex) {
      this._fft1c.complexToComplex1(this._sign1, nx2, f, f);
      this._fft2.complexToComplex2(this._sign2, this._nfft1, f, f);
    } else {
      this._fft1r.realToComplex1(this._sign1, nx2, f, f);
      this._fft2.complexToComplex2(this._sign2, this._nfft1 / 2 + 1, f, f);
    }
    phase(f);
    center(f);
  }

  /**
   * Applies an inverse frequency-to-space transform of a 2D array.
   * 
   * @param g the array to be transformed, a sampled function of frequency.
   * @return the transformed array, a sampled function of space.
   */
  public float[][] applyInverse(float[][] g) {
    ensureSamplingK2(g);
    float[][] gpad = (this._overwrite) ? g : copy(g);
    int nx1 = this._sx1.getCount();
    int nx2 = this._sx2.getCount();
    uncenter(gpad);
    unphase(gpad);
    if (this._complex) {
      this._fft2.complexToComplex2(-this._sign2, this._nfft1, gpad, gpad);
      this._fft2.scale(this._nfft1, nx2, gpad);
      this._fft1c.complexToComplex1(-this._sign1, nx2, gpad, gpad);
      this._fft1c.scale(nx1, nx2, gpad);
      return ccopy(nx1, nx2, gpad);
    }
    this._fft2.complexToComplex2(-this._sign2, this._nfft1 / 2 + 1, gpad, gpad);
    this._fft2.scale(this._nfft1 / 2 + 1, nx2, gpad);
    this._fft1r.complexToReal1(-this._sign1, nx2, gpad, gpad);
    this._fft1r.scale(nx1, nx2, gpad);
    return copy(nx1, nx2, gpad);
  }

  // /////////////////////////////////////////////////////////////////////////
  // private

  private FftReal _fft1r;
  private FftComplex _fft1c, _fft2, _fft3;
  private Sampling _sx1, _sx2, _sx3;
  private Sampling _sk1, _sk2, _sk3;
  @SuppressWarnings("unused")
  private int _sign1, _sign2, _sign3;
  private int _nfft1, _nfft2, _nfft3;
  private int _padding1, _padding2, _padding3;
  private boolean _center1, _center2, _center3;
  private boolean _complex;
  private boolean _overwrite;

  private void updateSampling1() {
    if (this._sx1 == null)
      return;
    int nx = this._sx1.getCount();
    double dx = this._sx1.getDelta();
    int npad = nx + this._padding1;
    int nfft, nk;
    double dk, fk;
    if (this._complex) {
      nfft = FftComplex.nfftSmall(npad);
      dk = 1.0 / (nfft * dx);
      if (this._center1) {
        boolean even = nfft % 2 == 0;
        nk = even ? nfft + 1 : nfft;
        fk = even ? -0.5 / dx : -0.5 / dx + 0.5 * dk;
      } else {
        nk = nfft;
        fk = 0.0;
      }
      if (this._fft1c == null || this._nfft1 != nfft) {
        this._fft1c = new FftComplex(nfft);
        this._fft1r = null;
        this._nfft1 = nfft;
      }
    } else {
      nfft = FftReal.nfftSmall(npad);
      dk = 1.0 / (nfft * dx);
      if (this._center1) {
        nk = nfft + 1;
        fk = -0.5f / dx;
      } else {
        nk = nfft / 2 + 1;
        fk = 0.0;
      }
      if (this._fft1r == null || this._nfft1 != nfft) {
        this._fft1r = new FftReal(nfft);
        this._fft1c = null;
        this._nfft1 = nfft;
      }
    }
    this._sk1 = new Sampling(nk, dk, fk);
    // trace("sk1: nfft="+nfft+" nk="+nk+" dk="+dk+" fk="+fk);
  }

  private void updateSampling2() {
    if (this._sx2 == null)
      return;
    int nx = this._sx2.getCount();
    double dx = this._sx2.getDelta();
    int npad = nx + this._padding2;
    int nfft = FftComplex.nfftSmall(npad);
    double dk = 1.0 / (nfft * dx);
    double fk;
    int nk;
    if (this._center2) {
      boolean even = nfft % 2 == 0;
      nk = even ? nfft + 1 : nfft;
      fk = even ? -0.5 / dx : -0.5 / dx + 0.5 * dk;
    } else {
      nk = nfft;
      fk = 0.0;
    }
    if (this._fft2 == null || this._nfft2 != nfft) {
      this._fft2 = new FftComplex(nfft);
      this._nfft2 = nfft;
    }
    this._sk2 = new Sampling(nk, dk, fk);
    // trace("sk2: nfft="+nfft+" nk="+nk+" dk="+dk+" fk="+fk);
  }

  private void updateSampling3() {
    if (this._sx3 == null)
      return;
    int nx = this._sx3.getCount();
    double dx = this._sx3.getDelta();
    int npad = nx + this._padding3;
    int nfft = FftComplex.nfftSmall(npad);
    double dk = 1.0 / (nfft * dx);
    double fk;
    int nk;
    if (this._center3) {
      boolean even = nfft % 2 == 0;
      nk = even ? nfft + 1 : nfft;
      fk = even ? -0.5 / dx : -0.5 / dx + 0.5 * dk;
    } else {
      nk = nfft;
      fk = 0.0;
    }
    if (this._fft3 == null || this._nfft3 != nfft) {
      this._fft3 = new FftComplex(nfft);
      this._nfft3 = nfft;
    }
    this._sk3 = new Sampling(nk, dk, fk);
    // trace("sk3: nfft="+nfft+" nk="+nk+" dk="+dk+" fk="+fk);
  }

  private float[][] pad(float[][] f) {
    int nk1 = this._sk1.getCount();
    int nk2 = this._sk2.getCount();
    float[][] fpad = new float[nk2][2 * nk1];
    if (this._complex) {
      ccopy(f[0].length / 2, f.length, f, fpad);
    } else {
      copy(f[0].length, f.length, f, fpad);
    }
    return fpad;
  }

  private void ensureSamplingX1(float[] f) {
    Check.state(this._sx1 != null, "sampling sx1 exists for 1st dimension");
    int l1 = f.length;
    int n1 = this._sx1.getCount();
    if (this._complex)
      n1 *= 2;
    Check.argument(n1 == l1, "array length consistent with sampling sx1");
  }

  private void ensureSamplingX2(float[][] f) {
    Check.state(this._sx2 != null, "sampling sx2 exists for 2nd dimension");
    ensureSamplingX1(f[0]);
    int l2 = f.length;
    int n2 = this._sx2.getCount();
    Check.argument(n2 == l2, "array length consistent with sampling sx2");
  }

  private void ensureSamplingK1(float[] f) {
    Check.state(this._sk1 != null, "sampling sk1 exists for 1st dimension");
    int l1 = f.length;
    int n1 = this._sk1.getCount();
    Check.argument(2 * n1 == l1, "array length consistent with sampling sk1");
  }

  private void ensureSamplingK2(float[][] f) {
    Check.state(this._sk2 != null, "sampling sk2 exists for 2nd dimension");
    ensureSamplingK1(f[0]);
    int l2 = f.length;
    int n2 = this._sk2.getCount();
    Check.argument(n2 == l2, "array length consistent with sampling sk2");
  }


  private void center1(float[] f) {
    if (!this._center1)
      return;
    int nk1 = this._sk1.getCount();
    int nfft1 = this._nfft1;
    boolean even1 = nfft1 % 2 == 0;
    if (this._complex) {
      if (even1) {
        // complex, nfft = 8
        // 0 1 2 3 4 5 6 7 | 8
        // 4 5 6 7 0 1 2 3 | 4
        cswap(nfft1 / 2, 0, nfft1 / 2, f);
        f[2 * (nk1 - 1)] = f[0];
        f[2 * (nk1 - 1) + 1] = f[1];
      } else {
        // complex, nfft = 7
        // 0 1 2 3 4 5 6
        // 4 5 6 3 0 1 2
        // 4 5 6 0 1 2 3
        cswap((nfft1 - 1) / 2, 0, (nfft1 + 1) / 2, f);
        crotateLeft((nfft1 + 1) / 2, (nfft1 - 1) / 2, f);
      }
    } else {
      // real, nfft = 8
      // 0 1 2 3 4
      // 0 1 2 3 0 1 2 3 4
      cshift(nfft1 / 2 + 1, 0, nfft1 / 2, f);
    }
  }

  private void center(float[][] f) {
    if (this._center1) {
      for (int i2 = 0; i2 < this._nfft2; ++i2)
        center1(f[i2]);
    }
    center2(f);
    if (this._center1 && !this._complex)
      creflect(this._nfft1 / 2, this._nfft1 / 2, f);
  }

  private void center2(float[][] f) {
    if (!this._center2)
      return;
    int nk2 = this._sk2.getCount();
    int nfft2 = this._nfft2;
    boolean even2 = nfft2 % 2 == 0;
    if (even2) {
      // nfft even
      // 0 1 2 3 4 5 6 7 | 8
      // 4 5 6 7 0 1 2 3 | 4
      cswap(nfft2 / 2, 0, nfft2 / 2, f);
      ccopy(f[0], f[nk2 - 1]);
    } else {
      // nfft odd
      // 0 1 2 3 4 5 6
      // 4 5 6 3 0 1 2
      // 4 5 6 0 1 2 3
      cswap((nfft2 - 1) / 2, 0, (nfft2 + 1) / 2, f);
      crotateLeft((nfft2 + 1) / 2, (nfft2 - 1) / 2, f);
    }
  }

  private void uncenter1(float[] f) {
    if (!this._center1)
      return;
    int nfft1 = this._nfft1;
    boolean even1 = nfft1 % 2 == 0;
    if (this._complex) {
      if (even1) {
        // complex, nfft = 8
        // 4 5 6 7 0 1 2 3 | 8
        // 0 1 2 3 4 5 6 7 | 8
        cswap(nfft1 / 2, 0, nfft1 / 2, f);
      } else {
        // complex, nfft = 7
        // 4 5 6 0 1 2 3
        // 4 5 6 3 0 1 2
        // 0 1 2 3 4 5 6
        crotateRight((nfft1 + 1) / 2, (nfft1 - 1) / 2, f);
        cswap((nfft1 - 1) / 2, 0, (nfft1 + 1) / 2, f);
      }
    } else {
      // real, nfft = 8
      // 4 3 2 1 0 1 2 3 4
      // 0 1 2 3 4 1 2 3 4
      cshift(nfft1 / 2 + 1, nfft1 / 2, 0, f);
    }
  }

  private void uncenter2(float[][] f) {
    if (!this._center2)
      return;
    int nfft2 = this._nfft2;
    boolean even2 = nfft2 % 2 == 0;
    if (even2) {
      // nfft even
      // 4 5 6 7 0 1 2 3 | 8
      // 0 1 2 3 4 5 6 7 | 8
      cswap(nfft2 / 2, 0, nfft2 / 2, f);
    } else {
      // nfft odd
      // 4 5 6 0 1 2 3
      // 4 5 6 3 0 1 2
      // 0 1 2 3 4 5 6
      crotateRight((nfft2 + 1) / 2, (nfft2 - 1) / 2, f);
      cswap((nfft2 - 1) / 2, 0, (nfft2 + 1) / 2, f);
    }
  }

  private void uncenter(float[][] f) {
    uncenter2(f);
    if (this._center1) {
      for (int i2 = 0; i2 < this._nfft2; ++i2)
        uncenter1(f[i2]);
    }
  }

  private static void crotateLeft(int n, int j, float[] f) {
    float fjr = f[j * 2];
    float fji = f[j * 2 + 1];
    int i = j + 1, ir = 2 * i, ii = ir + 1;
    for (int k = 1; k < n; ++k, ir += 2, ii += 2) {
      f[ir - 2] = f[ir];
      f[ii - 2] = f[ii];
    }
    f[ir - 2] = fjr;
    f[ii - 2] = fji;
  }

  private static void crotateLeft(int n, int j, float[][] f) {
    // nfft odd
    // 4 5 6 3 0 1 2
    // 4 5 6 0 1 2 3
    // crotateLeft(n=4,j=3,f);
    float[] fj = f[j];
    int m = j + n;
    int i;
    for (i = j + 1; i < m; ++i)
      f[i - 1] = f[i];
    f[i - 1] = fj;
  }

  private static void crotateRight(int n, int j, float[] f) {
    int m = j + n - 1;
    float fmr = f[m * 2];
    float fmi = f[m * 2 + 1];
    int i = m, ir = 2 * i, ii = ir + 1;
    for (int k = 1; k < n; ++k, ir -= 2, ii -= 2) {
      f[ir] = f[ir - 2];
      f[ii] = f[ii - 2];
    }
    f[ir] = fmr;
    f[ii] = fmi;
  }

  private static void crotateRight(int n, int j, float[][] f) {
    int m = j + n - 1;
    float[] fm = f[m];
    int i;
    for (i = m; i > j; --i)
      f[i] = f[i - 1];
    f[i] = fm;
  }


  private static void cswap(int n, int i, int j, float[][] f) {
    for (int k = 0; k < n; ++k, ++i, ++j) {
      float[] fi = f[i];
      f[i] = f[j];
      f[j] = fi;
    }
  }

  private static void cswap(int n, int i, int j, float[] f) {
    int ir = 2 * i, ii = ir + 1;
    int jr = 2 * j, ji = jr + 1;
    for (int k = 0; k < n; ++k, ir += 2, ii += 2, jr += 2, ji += 2) {
      float fir = f[ir];
      f[ir] = f[jr];
      f[jr] = fir;
      float fii = f[ii];
      f[ii] = f[ji];
      f[ji] = fii;
    }
  }

  private static void cshift(int n, int i, int j, float[] f) {
    if (i < j) {
      int ir = 2 * (i + n - 1), ii = ir + 1;
      int jr = 2 * (j + n - 1), ji = jr + 1;
      for (int k = 0; k < n; ++k, ir -= 2, ii -= 2, jr -= 2, ji -= 2) {
        f[jr] = f[ir];
        f[ji] = f[ii];
      }
    } else {
      int ir = 2 * i, ii = ir + 1;
      int jr = 2 * j, ji = jr + 1;
      for (int k = 0; k < n; ++k, ir += 2, ii += 2, jr += 2, ji += 2) {
        f[jr] = f[ir];
        f[ji] = f[ii];
      }
    }
  }

  @SuppressWarnings("unused")
  private static void creflect(int n, int i, float[] f) {
    int ir = 2 * (i + 1), ii = ir + 1;
    int jr = 2 * (i - 1), ji = jr + 1;
    for (int k = 0; k < n; ++k, ir += 2, ii += 2, jr -= 2, ji -= 2) {
      f[jr] = f[ir];
      f[ji] = -f[ii];
    }
  }

  private static void creflect(int n, int i, float[][] f) {
    int n2 = f.length;
    for (int i2 = 0, j2 = n2 - 1; i2 < n2; ++i2, --j2) {
      int ir = 2 * (i + 1), ii = ir + 1;
      int jr = 2 * (i - 1), ji = jr + 1;
      float[] fj2 = f[j2];
      float[] fi2 = f[i2];
      for (int k = 0; k < n; ++k, ir += 2, ii += 2, jr -= 2, ji -= 2) {
        fj2[jr] = fi2[ir];
        fj2[ji] = -fi2[ii];
      }
    }
  }


  private void phase(float[][] f) {
    phase(this._sign1, this._sign2, f);
  }

  private void unphase(float[][] f) {
    phase(-this._sign1, -this._sign2, f);
  }

  private void phase(int sign1, int sign2, float[][] f) {
    double fx1 = this._sx1.getFirst();
    double fx2 = this._sx2.getFirst();
    if (fx1 == 0.0 && fx2 == 0.0)
      return;
    int nk1 = (this._complex) ? this._nfft1 : this._nfft1 / 2 + 1;
    int nk2 = this._nfft2;
    double dp1 = sign1 * 2.0 * Math.PI * this._sk1.getDelta() * fx1;
    double dp2 = sign2 * 2.0 * Math.PI * this._sk2.getDelta() * fx2;
    for (int i2 = 0; i2 < nk2; ++i2) {
      double p2 = i2 * dp2;
      float[] f2 = f[i2];
      for (int i1 = 0, ir = 0, ii = 1; i1 < nk1; ++i1, ir += 2, ii += 2) {
        float p = (float) (i1 * dp1 + p2);
        float cosp = (float) Math.cos(p);
        float sinp = (float) Math.sin(p);
        float fr = f2[ir];
        float fi = f2[ii];
        f2[ir] = fr * cosp - fi * sinp;
        f2[ii] = fi * cosp + fr * sinp;
      }
    }
  }

  private static void copy(int n1, float[] rx, float[] ry) {
    for (int i1 = 0; i1 < n1; ++i1)
      ry[i1] = rx[i1];
  }

  private static void ccopy(int n1, float[] cx, float[] cy) {
    copy(2 * n1, cx, cy);
  }

  private static void ccopy(float[] cx, float[] cy) {
    ccopy(cx.length / 2, cx, cy);
  }

  private static void ccopy(int n1, int n2, float[][] cx, float[][] cy) {
    for (int i2 = 0; i2 < n2; ++i2)
      ccopy(n1, cx[i2], cy[i2]);
  }

  private static void copy(int n1, int n2, float[][] rx, float[][] ry) {
    for (int i2 = 0; i2 < n2; ++i2)
      copy(n1, rx[i2], ry[i2]);
  }

  private static float[] copy(int n1, float[] rx) {
    float[] ry = new float[n1];
    copy(n1, rx, ry);
    return ry;
  }

  private static float[] copy(float[] rx) {
    return copy(rx.length, rx);
  }

  private static float[][] copy(float[][] rx) {
    int n2 = rx.length;
    float[][] ry = new float[n2][];
    for (int i2 = 0; i2 < n2; ++i2)
      ry[i2] = copy(rx[i2]);
    return ry;
  }

  private static float[][] ccopy(int n1, int n2, float[][] cx) {
    float[][] cy = new float[n2][2 * n1];
    ccopy(n1, n2, cx, cy);
    return cy;
  }

  private static float[][] copy(int n1, int n2, float[][] rx) {
    float[][] ry = new float[n2][n1];
    copy(n1, n2, rx, ry);
    return ry;
  }

}
