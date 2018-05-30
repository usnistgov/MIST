/// ================================================================
/// 
/// Disclaimer:  IMPORTANT:  This software was developed at theNT
/// National Institute of Standards and Technology by employees of the
/// Federal Government in the course of their official duties.
/// Pursuant to title 17 Section 105 of the United States Code this
/// software is not subject to copyright protection and is in the
/// public domain.  This is an experimental system.  NIST assumes no
/// responsibility whatsoever for its use by other parties, and makes
/// no guarantees, expressed or implied, about its quality,
/// reliability, or any other characteristic.  We would appreciate
/// acknowledgement if the software is used.  This software can be
/// redistributed and/or modified freely provided that any derivative
/// works bear some notice that they are derived from it, and any
/// modified versions bear some notice that they have been modified.
/// 
/// ================================================================

// ================================================================
// 
// Author: Timothy Blattner
// Date:   Wed Nov 30 12:36:40 2011 EScufftDoubleComplex
// 
// Functions that execute on the graphics card for doing
// Vector computation.
// 
// ================================================================

#include <cuda.h>
#include <cufft.h>
#include<float.h>

#define THREADS_PER_BLOCK 256
#define MIN_DISTANCE 1.0

// ================================================================
__device__ double distance(int x1, int x2, int y1, int y2)
{
	return ((double(x1-x2))*(double(x1-x2)))+
			((double(y1-y2))*(double(y1-y2)));
}

__device__ bool checkDistance(int *maxesRow, 
		int *maxesCol, int nMax, 
		int curIdx, int width)
{
	int row = curIdx / width;
	int col = curIdx % width;
	int j;
	//double dist;
	for (j = 0; j < nMax; j++)
	{
			if (maxesRow[j] == row && maxesCol[j] == col)
  			return false;

		//dist = distance(maxesRow[j], row, maxesCol[j], col);

		//if (dist < MIN_DISTANCE)
		//	return false;


	}

	return true;
}

__device__ bool checkDistance(volatile int *maxesRow, 
		volatile int *maxesCol, int nMax, 
		int curIdx, int width)
{
	int row = curIdx / width;
	int col = curIdx % width;
	int j;
	//double dist;
	for (j = 0; j < nMax; j++)
	{

		if (maxesRow[j] == row && maxesCol[j] == col)
			return false;

//		dist = distance(maxesRow[j], row, maxesCol[j], col);

//		if (dist < MIN_DISTANCE)
//			return false;


	}

	return true;
}



extern "C"
	__global__ void
elt_prod_conj(cufftDoubleComplex *fc, cufftDoubleComplex * c1, 
		cufftDoubleComplex * c2, int size)
{
	__shared__ cufftDoubleComplex sfc[THREADS_PER_BLOCK];
	__shared__ cufftDoubleComplex sc1[THREADS_PER_BLOCK];
	__shared__ cufftDoubleComplex sc2[THREADS_PER_BLOCK];

	int idx = threadIdx.x + blockIdx.x * THREADS_PER_BLOCK;

	if (idx >= size)
		return;

	sc1[threadIdx.x] = c1[idx];
	sc2[threadIdx.x] = c2[idx];

	__syncthreads();

	sfc[threadIdx.x] = cuCmul(sc1[threadIdx.x], cuConj(sc2[threadIdx.x]));

	double mag = cuCabs(sfc[threadIdx.x]);
	
	if (mag == 0 || isnan(mag))
	{
		mag = DBL_EPSILON;
		sfc[threadIdx.x].x = DBL_EPSILON;
	}
	
	fc[idx] = make_cuDoubleComplex(cuCreal(sfc[threadIdx.x]) / mag,
			cuCimag(sfc[threadIdx.x]) / mag);
}

extern "C"
	__global__ void
elt_prod_conj_v2(cufftDoubleComplex *fc, cufftDoubleComplex * c1, 
		cufftDoubleComplex * c2, int size)
{
	__shared__ cufftDoubleComplex sfc[THREADS_PER_BLOCK];

	int idx = threadIdx.x + blockIdx.x * THREADS_PER_BLOCK;

	if (idx >= size)
		return;


	//cufftDoubleComplex fc_res;

	sfc[threadIdx.x] = cuCmul(c1[idx], cuConj(c2[idx]));

	__syncthreads();

	double mag;

	//  mag = sqrt(fc_res.x * fc_res.x + fc_res.y * fc_res.y);
	mag = sqrt(sfc[threadIdx.x].x * sfc[threadIdx.x].x +
			sfc[threadIdx.x].y * sfc[threadIdx.x].y);

	if (isnan(mag) || mag == 0)
	{
		mag = DBL_EPSILON; //cuCabs(sfc[threadIdx.x]);
		sfc[threadIdx.x].x = DBL_EPSILON;
	}

	
//	if (mag == 0)
//		mag = DBL_EPSILON;
	
	
	fc[idx] = make_cuDoubleComplex(sfc[threadIdx.x].x / mag,
			sfc[threadIdx.x].y / mag);
}

extern "C"
	__global__ void
elt_prod_conj_v3(cufftDoubleComplex *fc, cufftDoubleComplex * c1,
		cufftDoubleComplex *c2, int size)
{
	int idx = threadIdx.x + blockIdx.x * THREADS_PER_BLOCK;

	if (idx >= size)
		return;

	cufftDoubleComplex _c1 = c1[idx];
	cufftDoubleComplex _c2 = c2[idx];
	cufftDoubleComplex _fc = cuCmul(_c1, cuConj(_c2));
	double mag = sqrt(_fc.x * _fc.x +
			_fc.y * _fc.y);

	if (isnan(mag) || mag == 0)
		mag = cuCabs(_fc);
	
	if (mag == 0)
		mag = DBL_EPSILON;
	
	fc[idx] = make_cuDoubleComplex(_fc.x / mag, _fc.y / mag);
}

extern "C"
	__global__ void
reduce_max_final(double *g_idata, double *g_odata, 
		int * max_idx, unsigned int n, int blockSize)
{
	__shared__ double sdata[THREADS_PER_BLOCK];
	__shared__ int idxData[THREADS_PER_BLOCK];
	unsigned int tid = threadIdx.x;
	unsigned int i = blockIdx.x*(blockSize*2) + tid;
	unsigned int gridSize = blockSize*2*gridDim.x;


	double myMax = 0.0;
	int myMaxIndex;

	while (i < n)
	{
		if (myMax < g_idata[i])
		{
			myMax = g_idata[i];
			myMaxIndex = max_idx[i];
		}

		if (i+blockSize < n)
		{
			if (myMax < g_idata[i+blockSize])
			{
				myMax = g_idata[i+blockSize];
				myMaxIndex = max_idx[i+blockSize];
			}
		}

		i += gridSize;
	}

	sdata[tid] = myMax;
	idxData[tid] = myMaxIndex;

	__syncthreads();

	if (blockSize >= 512)
	{
		if (tid < 256)
		{
			if (myMax < sdata[tid + 256])
			{
				sdata[tid] = myMax = sdata[tid+256];
				idxData[tid] = idxData[tid+256];
			}
		}
		__syncthreads();
	}

	if (blockSize >= 256)
	{
		if (tid < 128)
		{
			if (myMax < sdata[tid + 128])
			{
				sdata[tid] = myMax = sdata[tid+128];
				idxData[tid] = idxData[tid+128];
			}
		}
		__syncthreads();
	}

	if (blockSize >= 128)
	{
		if (tid <   64)
		{
			if(myMax < sdata[tid +   64])
			{
				sdata[tid] = myMax = sdata[tid+64];
				idxData[tid] = idxData[tid+64];
			}
		}
		__syncthreads();
	}

	volatile double *vdata = sdata;
	volatile int *vidxData = idxData;

	if (tid < 32)
	{
		if (blockSize >=  64)
			if (myMax < vdata[tid + 32])
			{
				vdata[tid] = myMax = vdata[tid+32];
				vidxData[tid] = vidxData[tid+32];
			}

		if (blockSize >=  32)
			if (myMax < vdata[tid + 16])
			{
				vdata[tid] = myMax = vdata[tid+16];
				vidxData[tid] = vidxData[tid+16];
			}

		if (blockSize >=  16)
			if (myMax < vdata[tid +  8])
			{
				vdata[tid] = myMax = vdata[tid+8];
				vidxData[tid] = vidxData[tid+8];
			}

		if (blockSize >=    8)
			if (myMax < vdata[tid +  4])
			{
				vdata[tid] = myMax = vdata[tid+4];
				vidxData[tid] = vidxData[tid+4];
			}

		if (blockSize >=    4)
			if (myMax < vdata[tid+2])
			{
				vdata[tid] = myMax = vdata[tid+2];
				vidxData[tid] = vidxData[tid+2];
			}

		if (blockSize >=    2)
			if (myMax < vdata[tid +  1])
			{
				vdata[tid] = myMax = vdata[tid+1];
				vidxData[tid] = vidxData[tid+1];
			}
		__syncthreads();
	}

	if (tid == 0)
	{
		g_odata[blockIdx.x] = sdata[0];
		max_idx[blockIdx.x] = idxData[0];
	}
}

extern "C"
	__global__ void
reduce_max_main(double *g_idata, double *g_odata, 
		int * max_idx, unsigned int n, int blockSize)
{
	__shared__ double sdata[THREADS_PER_BLOCK];
	__shared__ int idxData[THREADS_PER_BLOCK];
	unsigned int tid = threadIdx.x;
	unsigned int i = blockIdx.x*(blockSize) + tid;
	unsigned int gridSize = blockSize*gridDim.x;


	double myMax = 0.0;
	int myMaxIndex;
	double val;

	while (i < n)
	{
		val = g_idata[i];
		if (myMax < val)
		{
			myMax = val;
			myMaxIndex = i;
		}

		if (i+blockSize < n)
		{
			val = g_idata[i+blockSize];
			if (myMax < val)
			{
				myMax = val;
				myMaxIndex = i+blockSize;
			}
		}

		i += gridSize;
	}

	sdata[tid] = myMax;
	idxData[tid] = myMaxIndex;

	__syncthreads();

	if (blockSize >= 512)
	{
		if (tid < 256)
		{
			if (myMax < sdata[tid + 256])
			{
				sdata[tid] = myMax = sdata[tid+256];
				idxData[tid] = idxData[tid+256];
			}
		}
		__syncthreads();
	}

	if (blockSize >= 256)
	{
		if (tid < 128)
		{
			if (myMax < sdata[tid + 128])
			{
				sdata[tid] = myMax = sdata[tid+128];
				idxData[tid] = idxData[tid+128];
			}
		}
		__syncthreads();
	}

	if (blockSize >= 128)
	{
		if (tid <   64)
		{
			if(myMax < sdata[tid +   64])
			{
				sdata[tid] = myMax = sdata[tid+64];
				idxData[tid] = idxData[tid+64];
			}
		}
		__syncthreads();
	}

	volatile double *vdata = sdata;
	volatile int *vidxData = idxData;

	if (tid < 32)
	{
		if (blockSize >=  64)
			if (myMax < vdata[tid + 32])
			{
				vdata[tid] = myMax = vdata[tid+32];
				vidxData[tid] = vidxData[tid+32];
			}

		if (blockSize >=  32)
			if (myMax < vdata[tid + 16])
			{
				vdata[tid] = myMax = vdata[tid+16];
				vidxData[tid] = vidxData[tid+16];
			}

		if (blockSize >=  16)
			if (myMax < vdata[tid +  8])
			{
				vdata[tid] = myMax = vdata[tid+8];
				vidxData[tid] = vidxData[tid+8];
			}

		if (blockSize >=    8)
			if (myMax < vdata[tid +  4])
			{
				vdata[tid] = myMax = vdata[tid+4];
				vidxData[tid] = vidxData[tid+4];
			}

		if (blockSize >=    4)
			if (myMax < vdata[tid+2])
			{
				vdata[tid] = myMax = vdata[tid+2];
				vidxData[tid] = vidxData[tid+2];
			}

		if (blockSize >=    2)
			if (myMax < vdata[tid +  1])
			{
				vdata[tid] = myMax = vdata[tid+1];
				vidxData[tid] = vidxData[tid+1];
			}
		__syncthreads();
	}

	if (tid == 0)
	{
		g_odata[blockIdx.x] = sdata[0];
		max_idx[blockIdx.x] = idxData[0];
	}
}


extern "C"
	__global__ void
reduce_max_filter_final(double *g_idata, double *g_odata, 
		int * max_idx, unsigned int n, unsigned int width, 
		int blockSize, 
		int *maxes, int nMax)
{
	__shared__ int smaxesRow[10];
	__shared__ int smaxesCol[10];
	__shared__ int smaxesVal[10];
	__shared__ double sdata[THREADS_PER_BLOCK];
	__shared__ int idxData[THREADS_PER_BLOCK];
	unsigned int tid = threadIdx.x;
	unsigned int i = blockIdx.x*(blockSize*2) + tid;
	unsigned int gridSize = blockSize*2*gridDim.x;

	if (tid < nMax)
	{
		smaxesVal[tid] = maxes[tid];
		smaxesRow[tid] = smaxesVal[tid] / width;		
		smaxesCol[tid] = smaxesVal[tid] % width;		
	}
	__syncthreads();

	double myMax = 0.0;
	int myMaxIndex;

	while (i < n)
	{
		if (myMax < g_idata[i])
		{
			if (checkDistance(smaxesRow, smaxesCol,
						nMax, max_idx[i], width))
			{
				myMax = g_idata[i];
				myMaxIndex = max_idx[i];
			}
		}

		if (i+blockSize < n)
		{
			if (myMax < g_idata[i+blockSize])
			{
				if (checkDistance(smaxesRow, smaxesCol, 
							nMax, 
							max_idx[i+blockSize], 
							width))
				{

					myMax = g_idata[i+blockSize];
					myMaxIndex = max_idx[i+blockSize];
				}
			}
		}

		i += gridSize;
	}

	sdata[tid] = myMax;
	idxData[tid] = myMaxIndex;

	__syncthreads();

	if (blockSize >= 512)
	{
		if (tid < 256)
		{
			if (myMax < sdata[tid + 256])
			{
				if (checkDistance(smaxesRow, smaxesCol, 
							nMax, idxData[tid+256], 
							width))
				{
					sdata[tid] = myMax = sdata[tid+256];
					idxData[tid] = idxData[tid+256];
				}
			}
		}
		__syncthreads();
	}

	if (blockSize >= 256)
	{
		if (tid < 128)
		{
			if (myMax < sdata[tid + 128])
			{
				if (checkDistance(smaxesRow, smaxesCol, 
							nMax, idxData[tid+128], 
							width))
				{
					sdata[tid] = myMax = sdata[tid+128];
					idxData[tid] = idxData[tid+128];
				}
			}
		}
		__syncthreads();
	}

	if (blockSize >= 128)
	{
		if (tid <   64)
		{
			if(myMax < sdata[tid +   64])
			{
				if (checkDistance(smaxesRow, smaxesCol, 
							nMax, idxData[tid+64], 
							width))
				{
					sdata[tid] = myMax = sdata[tid+64];
					idxData[tid] = idxData[tid+64];
				}
			}
		}
		__syncthreads();
	}

	volatile double *vdata = sdata;
	volatile int *vidxData = idxData;

	volatile int *vsmaxesRow = smaxesRow;
	volatile int *vsmaxesCol = smaxesCol;
	
	if (tid < 32)
	{
		if (blockSize >=  64)
			if (myMax < vdata[tid + 32])
			{
				if (checkDistance(vsmaxesRow, vsmaxesCol, 
							nMax, vidxData[tid+32], 
							width))
				{
					vdata[tid] = myMax = vdata[tid+32];
					vidxData[tid] = vidxData[tid+32];
				}
			}

		if (blockSize >=  32)
			if (myMax < vdata[tid + 16])
			{
				if (checkDistance(vsmaxesRow, vsmaxesCol, 
							nMax, vidxData[tid+16], 
							width))
				{
					vdata[tid] = myMax = vdata[tid+16];
					vidxData[tid] = vidxData[tid+16];
				}
			}

		if (blockSize >=  16)
			if (myMax < vdata[tid +  8])
			{
				if (checkDistance(vsmaxesRow, vsmaxesCol, 
							nMax, vidxData[tid+8], 
							width))
				{
					vdata[tid] = myMax = vdata[tid+8];
					vidxData[tid] = vidxData[tid+8];
				}
			}

		if (blockSize >=    8)
			if (myMax < vdata[tid +  4])
			{
				if (checkDistance(vsmaxesRow, vsmaxesCol, 
							nMax, vidxData[tid+4], 
							width))
				{
					vdata[tid] = myMax = vdata[tid+4];
					vidxData[tid] = vidxData[tid+4];
				}
			}

		if (blockSize >=    4)
			if (myMax < vdata[tid+2])
			{
				if (checkDistance(vsmaxesRow, vsmaxesCol, 
							nMax, vidxData[tid+2], 
							width))
				{
					vdata[tid] = myMax = vdata[tid+2];
					vidxData[tid] = vidxData[tid+2];
				}
			}

		if (blockSize >=    2)
			if (myMax < vdata[tid +  1])
			{
				if (checkDistance(vsmaxesRow, vsmaxesCol, 
							nMax, vidxData[tid+1], 
							width))
				{
					vdata[tid] = myMax = vdata[tid+1];
					vidxData[tid] = vidxData[tid+1];
				}

			}
		__syncthreads();
	}
	

	if (tid == 0)
	{
		g_odata[blockIdx.x] = sdata[0];
		max_idx[blockIdx.x] = idxData[0];

		if (gridDim.x == 1)
    		maxes[nMax] = idxData[0];
	}
}


extern "C"
	__global__ void
reduce_max_filter_main(double *g_idata, double *g_odata, 
		int * max_idx, unsigned int width, unsigned int height,
		int blockSize, 
		int *maxes, int nMax)
{
	__shared__ int smaxesRow[10];
	__shared__ int smaxesCol[10];
	__shared__ int smaxesVal[10];
	__shared__ double sdata[THREADS_PER_BLOCK];
	__shared__ int idxData[THREADS_PER_BLOCK];
	unsigned int tid = threadIdx.x;
	unsigned int i = blockIdx.x*(blockSize) + tid;
	unsigned int gridSize = blockSize*gridDim.x;
	if (tid < nMax)
	{
		smaxesVal[tid] = maxes[tid];
		smaxesRow[tid] = smaxesVal[tid] / width;		
		smaxesCol[tid] = smaxesVal[tid] % width;		
	}
	__syncthreads();

	double myMax = -INFINITY;
	int myMaxIndex;
	double val;

	while (i < width * height)
	{
		val = g_idata[i];
		if (myMax < val)
		{
			// compute distance . . .
			if (checkDistance(smaxesRow, smaxesCol, 
						nMax, i, width))
			{
				myMax = val;
				myMaxIndex = i;
			}
		}

		if (i+blockSize < width * height)
		{
			val = g_idata[i+blockSize];
			if (myMax < val)
			{

				if (checkDistance(smaxesRow, smaxesCol, 
							nMax, i+blockSize, width))
				{
					myMax = val;
					myMaxIndex = i+blockSize;
				}
			}
		}

		i += gridSize;
	}

	sdata[tid] = myMax;
	idxData[tid] = myMaxIndex;

	__syncthreads();

	if (blockSize >= 512)
	{
		if (tid < 256)
		{
			if (myMax < sdata[tid + 256])
			{
				if (checkDistance(smaxesRow, smaxesCol, 
							nMax, idxData[tid+256],
							width))
				{
					sdata[tid] = myMax = sdata[tid+256];
					idxData[tid] = idxData[tid+256];
				}
			}
		}
		__syncthreads();
	}

	if (blockSize >= 256)
	{
		if (tid < 128)
		{
			if (myMax < sdata[tid + 128])
			{
				if (checkDistance(smaxesRow, smaxesCol, 
							nMax, idxData[tid+128],
							width))
				{
					sdata[tid] = myMax = sdata[tid+128];
					idxData[tid] = idxData[tid+128];
				}
			}
		}
		__syncthreads();
	}

	if (blockSize >= 128)
	{
		if (tid <   64)
		{
			if(myMax < sdata[tid +   64])
			{
				if (checkDistance(smaxesRow, smaxesCol, 
							nMax, idxData[tid+64], 
							width))
				{
					sdata[tid] = myMax = sdata[tid+64];
					idxData[tid] = idxData[tid+64];
				}
			}
		}
		__syncthreads();
	}

	volatile double *vdata = sdata;
	volatile int *vidxData = idxData;

	volatile int *vsmaxesRow = smaxesRow;
	volatile int *vsmaxesCol = smaxesCol;
	
	if (tid < 32)
	{
		if (blockSize >=  64)
			if (myMax < vdata[tid + 32])
			{
				if (checkDistance(vsmaxesRow, vsmaxesCol, 
							nMax, vidxData[tid+32], 
							width))
				{
					vdata[tid] = myMax = vdata[tid+32];
					vidxData[tid] = vidxData[tid+32];
				}
			}

		if (blockSize >=  32)
			if (myMax < vdata[tid + 16])
			{

				if (checkDistance(vsmaxesRow, vsmaxesCol, 
							nMax, vidxData[tid+16], 
							width))
				{
					vdata[tid] = myMax = vdata[tid+16];
					vidxData[tid] = vidxData[tid+16];
				}
			}

		if (blockSize >=  16)
			if (myMax < vdata[tid +  8])
			{
				if (checkDistance(vsmaxesRow, vsmaxesCol, 
							nMax, vidxData[tid+8], 
							width))
				{
					vdata[tid] = myMax = vdata[tid+8];
					vidxData[tid] = vidxData[tid+8];
				}
			}

		if (blockSize >=    8)
			if (myMax < vdata[tid +  4])
			{
				if (checkDistance(vsmaxesRow, vsmaxesCol, 
							nMax, vidxData[tid+4], 
							width))
				{
					vdata[tid] = myMax = vdata[tid+4];
					vidxData[tid] = vidxData[tid+4];
				}
			}

		if (blockSize >=    4)
			if (myMax < vdata[tid+2])
			{
				if (checkDistance(vsmaxesRow, vsmaxesCol, 
							nMax, vidxData[tid+2], 
							width))
				{
					vdata[tid] = myMax = vdata[tid+2];
					vidxData[tid] = vidxData[tid+2];
				}
			}

		if (blockSize >=    2)
			if (myMax < vdata[tid +  1])
			{
				if (checkDistance(vsmaxesRow, vsmaxesCol, 
							nMax, vidxData[tid+1], 
							width))
				{
					vdata[tid] = myMax = vdata[tid+1];
					vidxData[tid] = vidxData[tid+1];
				}
			}
		__syncthreads();
	}

	if (tid == 0)
	{
		g_odata[blockIdx.x] = sdata[0];
		max_idx[blockIdx.x] = idxData[0];

		if (gridDim.x == 1)
    		maxes[nMax] = idxData[0];
	}
}


// ================================================================
// ================================================================
// ================================================================
// ================================================================
// ======================= Float versions =========================
// ================================================================
// ================================================================
// ================================================================
// ================================================================

// ================================================================
__device__ float distancef(int x1, int x2, int y1, int y2)
{
	return ((float(x1-x2))*(float(x1-x2)))+
			((float(y1-y2))*(float(y1-y2)));
}

__device__ bool checkDistancef(int *maxesRow, 
		int *maxesCol, int nMax, 
		int curIdx, int width)
{
	int row = curIdx / width;
	int col = curIdx % width;
	int j;
	for (j = 0; j < nMax; j++)
	{
			if (maxesRow[j] == row && maxesCol[j] == col)
  			return false;

		//dist = distance(maxesRow[j], row, maxesCol[j], col);

		//if (dist < MIN_DISTANCE)
		//	return false;


	}

	return true;
}

__device__ bool checkDistancef(volatile int *maxesRow,
		volatile int *maxesCol, int nMax,
		int curIdx, int width)
{
	int row = curIdx / width;
	int col = curIdx % width;
	int j;
	for (j = 0; j < nMax; j++)
	{

		if (maxesRow[j] == row && maxesCol[j] == col)
			return false;


	}

	return true;
}

extern "C"
__global__ void
elt_prod_conjf(cufftComplex *fc, cufftComplex * c1,
		cufftComplex * c2, int size)
{
	__shared__ cufftComplex sfc[THREADS_PER_BLOCK];
	__shared__ cufftComplex sc1[THREADS_PER_BLOCK];
	__shared__ cufftComplex sc2[THREADS_PER_BLOCK];

	int idx = threadIdx.x + blockIdx.x * THREADS_PER_BLOCK;

	if (idx >= size)
		return;

	sc1[threadIdx.x] = c1[idx];
	sc2[threadIdx.x] = c2[idx];

	__syncthreads();

	sfc[threadIdx.x] = cuCmulf(sc1[threadIdx.x], cuConjf(sc2[threadIdx.x]));

	float mag = cuCabsf(sfc[threadIdx.x]);

	if (mag == 0 || isnan(mag))
	{
		mag = FLT_EPSILON;
		sfc[threadIdx.x].x = FLT_EPSILON;
	}

	fc[idx] = make_cuComplex(cuCrealf(sfc[threadIdx.x]) / mag,
			cuCimagf(sfc[threadIdx.x]) / mag);
}

extern "C"
__global__ void
elt_prod_conj_v2f(cufftComplex *fc, cufftComplex * c1,
		cufftComplex * c2, int size)
{
	__shared__ cufftComplex sfc[THREADS_PER_BLOCK];

	int idx = threadIdx.x + blockIdx.x * THREADS_PER_BLOCK;

	if (idx >= size)
		return;


	//cufftDoubleComplex fc_res;

	sfc[threadIdx.x] = cuCmulf(c1[idx], cuConjf(c2[idx]));

	__syncthreads();

	float mag;

	//  mag = sqrt(fc_res.x * fc_res.x + fc_res.y * fc_res.y);
	mag = sqrtf(sfc[threadIdx.x].x * sfc[threadIdx.x].x +
			sfc[threadIdx.x].y * sfc[threadIdx.x].y);

	if (isnan(mag) || mag == 0)
	{
		mag = FLT_EPSILON; //cuCabs(sfc[threadIdx.x]);
		sfc[threadIdx.x].x = FLT_EPSILON;
	}


//	if (mag == 0)
//		mag = DBL_EPSILON;


	fc[idx] = make_cuComplex(sfc[threadIdx.x].x / mag,
			sfc[threadIdx.x].y / mag);
}

extern "C"
__global__ void
elt_prod_conj_v3f(cufftComplex *fc, cufftComplex * c1,
		cufftComplex *c2, int size)
{
	int idx = threadIdx.x + blockIdx.x * THREADS_PER_BLOCK;

	if (idx >= size)
		return;

	cufftComplex _c1 = c1[idx];
	cufftComplex _c2 = c2[idx];
	cufftComplex _fc = cuCmulf(_c1, cuConjf(_c2));
	float mag = sqrtf(_fc.x * _fc.x +
			_fc.y * _fc.y);

	if (isnan(mag) || mag == 0)
		mag = cuCabsf(_fc);

	if (mag == 0)
		mag = FLT_EPSILON;

	fc[idx] = make_cuComplex(_fc.x / mag, _fc.y / mag);
}

extern "C"
__global__ void
reduce_max_finalf(float *g_idata, float *g_odata,
		int * max_idx, unsigned int n, int blockSize)
{
	__shared__ float sdata[THREADS_PER_BLOCK];
	__shared__ int idxData[THREADS_PER_BLOCK];
	unsigned int tid = threadIdx.x;
	unsigned int i = blockIdx.x*(blockSize*2) + tid;
	unsigned int gridSize = blockSize*2*gridDim.x;


	float myMax = 0.0;
	int myMaxIndex;

	while (i < n)
	{
		if (myMax < g_idata[i])
		{
			myMax = g_idata[i];
			myMaxIndex = max_idx[i];
		}

		if (i+blockSize < n)
		{
			if (myMax < g_idata[i+blockSize])
			{
				myMax = g_idata[i+blockSize];
				myMaxIndex = max_idx[i+blockSize];
			}
		}

		i += gridSize;
	}

	sdata[tid] = myMax;
	idxData[tid] = myMaxIndex;

	__syncthreads();

	if (blockSize >= 512)
	{
		if (tid < 256)
		{
			if (myMax < sdata[tid + 256])
			{
				sdata[tid] = myMax = sdata[tid+256];
				idxData[tid] = idxData[tid+256];
			}
		}
		__syncthreads();
	}

	if (blockSize >= 256)
	{
		if (tid < 128)
		{
			if (myMax < sdata[tid + 128])
			{
				sdata[tid] = myMax = sdata[tid+128];
				idxData[tid] = idxData[tid+128];
			}
		}
		__syncthreads();
	}

	if (blockSize >= 128)
	{
		if (tid <   64)
		{
			if(myMax < sdata[tid +   64])
			{
				sdata[tid] = myMax = sdata[tid+64];
				idxData[tid] = idxData[tid+64];
			}
		}
		__syncthreads();
	}

	volatile float *vdata = sdata;
	volatile int *vidxData = idxData;

	if (tid < 32)
	{
		if (blockSize >=  64)
			if (myMax < vdata[tid + 32])
			{
				vdata[tid] = myMax = vdata[tid+32];
				vidxData[tid] = vidxData[tid+32];
			}

		if (blockSize >=  32)
			if (myMax < vdata[tid + 16])
			{
				vdata[tid] = myMax = vdata[tid+16];
				vidxData[tid] = vidxData[tid+16];
			}

		if (blockSize >=  16)
			if (myMax < vdata[tid +  8])
			{
				vdata[tid] = myMax = vdata[tid+8];
				vidxData[tid] = vidxData[tid+8];
			}

		if (blockSize >=    8)
			if (myMax < vdata[tid +  4])
			{
				vdata[tid] = myMax = vdata[tid+4];
				vidxData[tid] = vidxData[tid+4];
			}

		if (blockSize >=    4)
			if (myMax < vdata[tid+2])
			{
				vdata[tid] = myMax = vdata[tid+2];
				vidxData[tid] = vidxData[tid+2];
			}

		if (blockSize >=    2)
			if (myMax < vdata[tid +  1])
			{
				vdata[tid] = myMax = vdata[tid+1];
				vidxData[tid] = vidxData[tid+1];
			}
		__syncthreads();
	}

	if (tid == 0)
	{
		g_odata[blockIdx.x] = sdata[0];
		max_idx[blockIdx.x] = idxData[0];
	}
}

extern "C"
__global__ void
reduce_max_mainf(float *g_idata, float *g_odata,
		int * max_idx, unsigned int n, int blockSize)
{
	__shared__ float sdata[THREADS_PER_BLOCK];
	__shared__ int idxData[THREADS_PER_BLOCK];
	unsigned int tid = threadIdx.x;
	unsigned int i = blockIdx.x*(blockSize) + tid;
	unsigned int gridSize = blockSize*gridDim.x;


	float myMax = 0.0;
	int myMaxIndex;
	float val;

	while (i < n)
	{
		val = g_idata[i];
		if (myMax < val)
		{
			myMax = val;
			myMaxIndex = i;
		}

		if (i+blockSize < n)
		{
			val = g_idata[i+blockSize];
			if (myMax < val)
			{
				myMax = val;
				myMaxIndex = i+blockSize;
			}
		}

		i += gridSize;
	}

	sdata[tid] = myMax;
	idxData[tid] = myMaxIndex;

	__syncthreads();

	if (blockSize >= 512)
	{
		if (tid < 256)
		{
			if (myMax < sdata[tid + 256])
			{
				sdata[tid] = myMax = sdata[tid+256];
				idxData[tid] = idxData[tid+256];
			}
		}
		__syncthreads();
	}

	if (blockSize >= 256)
	{
		if (tid < 128)
		{
			if (myMax < sdata[tid + 128])
			{
				sdata[tid] = myMax = sdata[tid+128];
				idxData[tid] = idxData[tid+128];
			}
		}
		__syncthreads();
	}

	if (blockSize >= 128)
	{
		if (tid <   64)
		{
			if(myMax < sdata[tid +   64])
			{
				sdata[tid] = myMax = sdata[tid+64];
				idxData[tid] = idxData[tid+64];
			}
		}
		__syncthreads();
	}

	volatile float *vdata = sdata;
	volatile int *vidxData = idxData;

	if (tid < 32)
	{
		if (blockSize >=  64)
			if (myMax < vdata[tid + 32])
			{
				vdata[tid] = myMax = vdata[tid+32];
				vidxData[tid] = vidxData[tid+32];
			}

		if (blockSize >=  32)
			if (myMax < vdata[tid + 16])
			{
				vdata[tid] = myMax = vdata[tid+16];
				vidxData[tid] = vidxData[tid+16];
			}

		if (blockSize >=  16)
			if (myMax < vdata[tid +  8])
			{
				vdata[tid] = myMax = vdata[tid+8];
				vidxData[tid] = vidxData[tid+8];
			}

		if (blockSize >=    8)
			if (myMax < vdata[tid +  4])
			{
				vdata[tid] = myMax = vdata[tid+4];
				vidxData[tid] = vidxData[tid+4];
			}

		if (blockSize >=    4)
			if (myMax < vdata[tid+2])
			{
				vdata[tid] = myMax = vdata[tid+2];
				vidxData[tid] = vidxData[tid+2];
			}

		if (blockSize >=    2)
			if (myMax < vdata[tid +  1])
			{
				vdata[tid] = myMax = vdata[tid+1];
				vidxData[tid] = vidxData[tid+1];
			}
		__syncthreads();
	}

	if (tid == 0)
	{
		g_odata[blockIdx.x] = sdata[0];
		max_idx[blockIdx.x] = idxData[0];
	}
}

extern "C"
__global__ void
reduce_max_filter_finalf(float *g_idata, float *g_odata,
		int * max_idx, unsigned int n, unsigned int width, 
		int blockSize, 
		int *maxes, int nMax)
{
	__shared__ int smaxesRow[10];
	__shared__ int smaxesCol[10];
	__shared__ int smaxesVal[10];
	__shared__ float sdata[THREADS_PER_BLOCK];
	__shared__ int idxData[THREADS_PER_BLOCK];
	unsigned int tid = threadIdx.x;
	unsigned int i = blockIdx.x*(blockSize*2) + tid;
	unsigned int gridSize = blockSize*2*gridDim.x;

	if (tid < nMax)
	{
		smaxesVal[tid] = maxes[tid];
		smaxesRow[tid] = smaxesVal[tid] / width;		
		smaxesCol[tid] = smaxesVal[tid] % width;		
	}
	__syncthreads();

	float myMax = 0.0;
	int myMaxIndex;

	while (i < n)
	{
		if (myMax < g_idata[i])
		{
			if (checkDistancef(smaxesRow, smaxesCol, 
					nMax, max_idx[i], width))
			{		
				myMax = g_idata[i];
				myMaxIndex = max_idx[i];
			}
		}

		if (i+blockSize < n)
		{
			if (myMax < g_idata[i+blockSize])
			{
				if (checkDistancef(smaxesRow, smaxesCol, 
						nMax,
						max_idx[i+blockSize],
						width))
				{

					myMax = g_idata[i+blockSize];
					myMaxIndex = max_idx[i+blockSize];
				}
			}
		}

		i += gridSize;
	}

	sdata[tid] = myMax;
	idxData[tid] = myMaxIndex;

	__syncthreads();

	if (blockSize >= 512)
	{
		if (tid < 256)
		{
			if (myMax < sdata[tid + 256])
			{
				if (checkDistancef(smaxesRow, smaxesCol, 
						nMax, idxData[tid+256],
						width))
				{
					sdata[tid] = myMax = sdata[tid+256];
					idxData[tid] = idxData[tid+256];
				}
			}
		}
		__syncthreads();
	}

	if (blockSize >= 256)
	{
		if (tid < 128)
		{
			if (myMax < sdata[tid + 128])
			{
				if (checkDistancef(smaxesRow, smaxesCol, 
						nMax, idxData[tid+128],
						width))
				{
					sdata[tid] = myMax = sdata[tid+128];
					idxData[tid] = idxData[tid+128];
				}
			}
		}
		__syncthreads();
	}

	if (blockSize >= 128)
	{
		if (tid <   64)
		{
			if(myMax < sdata[tid +   64])
			{
				if (checkDistancef(smaxesRow, smaxesCol, 
						nMax, idxData[tid+64],
						width))
				{
					sdata[tid] = myMax = sdata[tid+64];
					idxData[tid] = idxData[tid+64];
				}
			}
		}
		__syncthreads();
	}

	volatile float *vdata = sdata;
	volatile int *vidxData = idxData;
	
	volatile int *vsmaxesRow = smaxesRow;
	volatile int *vsmaxesCol = smaxesCol;

	if (tid < 32)
	{
		if (blockSize >=  64)
			if (myMax < vdata[tid + 32])
			{
				if (checkDistancef(vsmaxesRow, vsmaxesCol,
						nMax, vidxData[tid+32],
						width))
				{
					vdata[tid] = myMax = vdata[tid+32];
					vidxData[tid] = vidxData[tid+32];
				}
			}

		if (blockSize >=  32)
			if (myMax < vdata[tid + 16])
			{
				if (checkDistancef(vsmaxesRow, vsmaxesCol, 
						nMax, vidxData[tid+16],
						width))
				{
					vdata[tid] = myMax = vdata[tid+16];
					vidxData[tid] = vidxData[tid+16];
				}
			}

		if (blockSize >=  16)
			if (myMax < vdata[tid +  8])
			{
				if (checkDistancef(vsmaxesRow, vsmaxesCol, 
						nMax, vidxData[tid+8],
						width))
				{
					vdata[tid] = myMax = vdata[tid+8];
					vidxData[tid] = vidxData[tid+8];
				}
			}

		if (blockSize >=    8)
			if (myMax < vdata[tid +  4])
			{
				if (checkDistancef(vsmaxesRow, vsmaxesCol, 
						nMax, vidxData[tid+4],
						width))
				{
					vdata[tid] = myMax = vdata[tid+4];
					vidxData[tid] = vidxData[tid+4];
				}
			}

		if (blockSize >=    4)
			if (myMax < vdata[tid+2])
			{
				if (checkDistancef(vsmaxesRow, vsmaxesCol, 
						nMax, vidxData[tid+2],
						width))
				{
					vdata[tid] = myMax = vdata[tid+2];
					vidxData[tid] = vidxData[tid+2];
				}
			}

		if (blockSize >=    2)
			if (myMax < vdata[tid +  1])
			{
				if (checkDistancef(vsmaxesRow, vsmaxesCol, 
						nMax, vidxData[tid+1],
						width))
				{
					vdata[tid] = myMax = vdata[tid+1];
					vidxData[tid] = vidxData[tid+1];
				}

			}
		__syncthreads();
	}

	if (tid == 0)
	{
		g_odata[blockIdx.x] = sdata[0];
		max_idx[blockIdx.x] = idxData[0];
		
		if (gridDim.x == 1)		
			maxes[nMax] = idxData[0];
	}
}


extern "C"
__global__ void
reduce_max_filter_mainf(float *g_idata, float *g_odata,
		int * max_idx, unsigned int width, unsigned int height,
		int blockSize, 
		int *maxes, int nMax)
{
	__shared__ int smaxesRow[10];
	__shared__ int smaxesCol[10];
	__shared__ int smaxesVal[10];
	__shared__ float sdata[THREADS_PER_BLOCK];
	__shared__ int idxData[THREADS_PER_BLOCK];
	unsigned int tid = threadIdx.x;
	unsigned int i = blockIdx.x*(blockSize) + tid;
	unsigned int gridSize = blockSize*gridDim.x;
	if (tid < nMax)
	{
		smaxesVal[tid] = maxes[tid];
		smaxesRow[tid] = smaxesVal[tid] / width;		
		smaxesCol[tid] = smaxesVal[tid] % width;		
	}
	__syncthreads();

	float myMax = -INFINITY;
	int myMaxIndex;
	float val;

	while (i < width * height)
	{
		val = g_idata[i];
		if (myMax < val)
		{
			// compute distance . . .
			if (checkDistancef(smaxesRow, smaxesCol, 
					nMax, i, width))
			{
				myMax = val;
				myMaxIndex = i;
			}
		}

		if (i+blockSize < width * height)
		{
			val = g_idata[i+blockSize];
			if (myMax < val)
			{

				if (checkDistancef(smaxesRow, smaxesCol, 
						nMax, i+blockSize, width))
				{
					myMax = val;
					myMaxIndex = i+blockSize;
				}
			}
		}

		i += gridSize;
	}

	sdata[tid] = myMax;
	idxData[tid] = myMaxIndex;

	__syncthreads();

	if (blockSize >= 512)
	{
		if (tid < 256)
		{
			if (myMax < sdata[tid + 256])
			{
				if (checkDistancef(smaxesRow, smaxesCol, 
						nMax, idxData[tid+256],
						width))
				{
					sdata[tid] = myMax = sdata[tid+256];
					idxData[tid] = idxData[tid+256];
				}
			}
		}
		__syncthreads();
	}

	if (blockSize >= 256)
	{
		if (tid < 128)
		{
			if (myMax < sdata[tid + 128])
			{
				if (checkDistancef(smaxesRow, smaxesCol, 
						nMax, idxData[tid+128],
						width))
				{
					sdata[tid] = myMax = sdata[tid+128];
					idxData[tid] = idxData[tid+128];
				}
			}
		}
		__syncthreads();
	}

	if (blockSize >= 128)
	{
		if (tid <   64)
		{
			if(myMax < sdata[tid +   64])
			{
				if (checkDistancef(smaxesRow, smaxesCol, 
						nMax, idxData[tid+64],
						width))
				{
					sdata[tid] = myMax = sdata[tid+64];
					idxData[tid] = idxData[tid+64];
				}
			}
		}
		__syncthreads();
	}

	volatile float *vdata = sdata;
	volatile int *vidxData = idxData;
	
	volatile int *vsmaxesRow = smaxesRow;
	volatile int *vsmaxesCol = smaxesCol;

	if (tid < 32)
	{
		if (blockSize >=  64)
			if (myMax < vdata[tid + 32])
			{
				if (checkDistancef(vsmaxesRow, vsmaxesCol, 
						nMax, vidxData[tid+32],
						width))
				{
					vdata[tid] = myMax = vdata[tid+32];
					vidxData[tid] = vidxData[tid+32];
				}
			}

		if (blockSize >=  32)
			if (myMax < vdata[tid + 16])
			{

				if (checkDistancef(vsmaxesRow, vsmaxesCol, 
						nMax, vidxData[tid+16],
						width))
				{
					vdata[tid] = myMax = vdata[tid+16];
					vidxData[tid] = vidxData[tid+16];
				}
			}

		if (blockSize >=  16)
			if (myMax < vdata[tid +  8])
			{
				if (checkDistancef(vsmaxesRow, vsmaxesCol, 
						nMax, vidxData[tid+8],
						width))
				{
					vdata[tid] = myMax = vdata[tid+8];
					vidxData[tid] = vidxData[tid+8];
				}
			}

		if (blockSize >=    8)
			if (myMax < vdata[tid +  4])
			{
				if (checkDistancef(vsmaxesRow, vsmaxesCol, 
						nMax, vidxData[tid+4],
						width))
				{
					vdata[tid] = myMax = vdata[tid+4];
					vidxData[tid] = vidxData[tid+4];
				}
			}

		if (blockSize >=    4)
			if (myMax < vdata[tid+2])
			{
				if (checkDistancef(vsmaxesRow, vsmaxesCol, 
						nMax, vidxData[tid+2],
						width))
				{
					vdata[tid] = myMax = vdata[tid+2];
					vidxData[tid] = vidxData[tid+2];
				}
			}

		if (blockSize >=    2)
			if (myMax < vdata[tid +  1])
			{
				if (checkDistancef(vsmaxesRow, vsmaxesCol, 
						nMax, vidxData[tid+1],
						width))
				{
					vdata[tid] = myMax = vdata[tid+1];
					vidxData[tid] = vidxData[tid+1];
				}
			}
		__syncthreads();
	}

	if (tid == 0)
	{
		g_odata[blockIdx.x] = sdata[0];
		max_idx[blockIdx.x] = idxData[0];
		
		if (gridDim.x == 1)
			maxes[nMax] = idxData[0];
		
	}
}




// ================================================================

// Local Variables:
// time-stamp-line-limit: 30
// End:

