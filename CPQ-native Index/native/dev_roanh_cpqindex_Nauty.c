#include <dev_roanh_cpqindex_Nauty.h>
#include <core.h>

/**
 * Computes the canonical form of the given colored graph using the sparse
 * version of nauty. Returns the time in nanoseconds required for computations.
 * @param The JNI environment.
 * @param Calling class.
 * @param adj The input graph in adjacency list format, n arrays with
 *        each the indices of the neighbors of the n-th vertex.
 * @param colors The array containing raw color information data. Contains vertex
 *        indices in blocks of the same color with the start of a block of the same
 *        color being indicated by a negated value. All vertex indices are also always
 *        one higher than their actual index in the graph.
 * @return An array with two elements, first the time in nanoseconds it
 *         took to construct the graph and second the time in nanoseconds
 *         it took to compute the canonical form of the graph.
 */
JNIEXPORT jlongArray JNICALL Java_dev_roanh_cpqkeys_algo_Nauty_computeCanonSparse(JNIEnv* env, jclass obj, jobjectArray adj, jintArray colors){
	SG_DECL(graph);

	constructSparseGraph(env, &adj, &graph);

	DYNALLSTAT(int, labels, labels_sz);
	DYNALLSTAT(int, ptn, ptn_sz);
	DYNALLSTAT(int, orbits, orbits_sz);

	static DEFAULTOPTIONS_SPARSEDIGRAPH(options);
	statsblk stats;
	options.getcanon = TRUE;
	options.defaultptn = FALSE;

	int n = graph.nv;
	DYNALLOC1(int, labels, labels_sz, n, "malloc");
	DYNALLOC1(int, ptn, ptn_sz, n, "malloc");
	DYNALLOC1(int, orbits, orbits_sz, n, "malloc");

	parseColoring(env, n, &colors, labels, ptn);

	//compute canonical form and labeling
	SG_DECL(canon);
	sparsenauty(&graph, labels, ptn, orbits, &options, &stats, &canon);

	//return times
	jlongArray result = (*env)->NewLongArray(env, 2);

	jlong data[2];
	data[0] = 0;
	data[1] = 0;
	(*env)->SetLongArrayRegion(env, result, 0, 2, data);

	return result;
}
