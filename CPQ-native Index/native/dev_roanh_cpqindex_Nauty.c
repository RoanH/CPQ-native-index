#include <dev_roanh_cpqindex_Nauty.h>
#include <core.h>
#include <naututil.h>//TODO remove?

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
JNIEXPORT jintArray JNICALL Java_dev_roanh_cpqindex_Nauty_computeCanonSparse(JNIEnv* env, jclass obj, jobjectArray adj, jintArray colors){
	SG_DECL(graph);

	constructSparseGraph(env, &adj, &graph);

	DYNALLSTAT(int, labels, labels_sz);
	DYNALLSTAT(int, ptn, ptn_sz);
	DYNALLSTAT(int, orbits, orbits_sz);

	static DEFAULTOPTIONS_SPARSEDIGRAPH(options);
	statsblk stats;
	options.getcanon = TRUE;//TODO false
	options.defaultptn = FALSE;

	int n = graph.nv;
	DYNALLOC1(int, labels, labels_sz, n, "malloc");
	DYNALLOC1(int, ptn, ptn_sz, n, "malloc");
	DYNALLOC1(int, orbits, orbits_sz, n, "malloc");

	parseColoring(env, n, &colors, labels, ptn);

	//compute canonical form and labeling
	SG_DECL(canon);
	sparsenauty(&graph, labels, ptn, orbits, &options, &stats, &canon);

	//output
	FILE* outf = fopen("test.txt", "w");
	putcanon_sg(outf, labels, &canon, 0);
	fclose(outf);

	//return canonical labeling
	jintArray result = (*env)->NewIntArray(env, n);

	jint data[n];
	for(int i = 0; i < n; i++){
		data[i] = labels[i];
	}
	(*env)->SetIntArrayRegion(env, result, 0, n, data);

	return result;
}
