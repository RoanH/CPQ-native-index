#ifndef include_dev_roanh_cpqindex_core
#define include_dev_roanh_cpqindex_core

#include <jni.h>
#include <nausparse.h>

/**
 * Constructs a sparse graph from the given adjacency list
 * representation of a graph.
 */
void constructSparseGraph(JNIEnv*, jobjectArray*, sparsegraph*);

/**
 * Constructs the graph coloring information arrays 'labels' and 'ptn'
 * from the given color data array.
 */
void parseColoring(JNIEnv*, int, jintArray*, int*, int*);

#endif
