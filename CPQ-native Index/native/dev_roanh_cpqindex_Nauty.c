#include <dev_roanh_cpqindex_Nauty.h>
#include <core.h>

/**
 * Computes and returns the canonical form of the given colored graph using
 * the sparse version of nauty.
 * @param The JNI environment.
 * @param Calling class.
 * @param adj The input graph in adjacency list format, n arrays with
 *        each the indices of the neighbors of the n-th vertex.
 * @param colors The array containing raw color information data. Contains vertex
 *        indices in blocks of the same color with the start of a block of the same
 *        color being indicated by a negated value. All vertex indices are also always
 *        one higher than their actual index in the graph.
 * @return The relabeling function that can be used to constructed the canonical graph.
 *         The returned array has the same size as there were vertices in the graph. For
 *         each index the former index of that vertex is indicated. For example if at index
 *         0 the value 4 is stored, then this means that in the input graph vertex 0 was
 *         labeled as vertex 4.
 */
JNIEXPORT jintArray JNICALL Java_dev_roanh_cpqindex_Nauty_computeCanonSparse(JNIEnv* env, jclass obj, jobjectArray adj, jintArray colors){
	//construct input graph
	SG_DECL(graph);
	constructSparseGraph(env, &adj, &graph);

	//set nauty settings
	static DEFAULTOPTIONS_SPARSEDIGRAPH(options);
	statsblk stats;
	options.getcanon = FALSE;//we only need the relabeling function
	options.defaultptn = FALSE;

	//allocated data structures
	int n = graph.nv;
	DYNALLSTAT(int, labels, labels_sz);
	DYNALLSTAT(int, ptn, ptn_sz);
	DYNALLSTAT(int, orbits, orbits_sz);
	DYNALLOC1(int, labels, labels_sz, n, "jni canon sparse");
	DYNALLOC1(int, ptn, ptn_sz, n, "jni canon sparse");
	DYNALLOC1(int, orbits, orbits_sz, n, "jni canon sparse");

	//initialise the coloring of the graph
	parseColoring(env, n, &colors, labels, ptn);

	//compute canonical form and labeling
	sparsenauty(&graph, labels, ptn, orbits, &options, &stats, NULL);

	//check for errors
	if(stats->errstatus != 0){
		return NULL;
	}

	//return canonical labeling
	jintArray result = (*env)->NewIntArray(env, n);

	jint data[n];
	for(int i = 0; i < n; i++){
		data[i] = labels[i];
	}
	(*env)->SetIntArrayRegion(env, result, 0, n, data);

	return result;
}
