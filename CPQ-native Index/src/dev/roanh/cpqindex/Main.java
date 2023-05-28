package dev.roanh.cpqindex;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import dev.roanh.gmark.conjunct.cpq.CPQ;
import dev.roanh.gmark.conjunct.cpq.QueryGraphCPQ;
import dev.roanh.gmark.conjunct.cpq.QueryGraphCPQ.Vertex;
import dev.roanh.gmark.core.graph.Predicate;
import dev.roanh.gmark.util.GraphPanel;
import dev.roanh.gmark.util.UniqueGraph;

public class Main{
	/**
	 * The command line options.
	 */
	public static final Options options;

	public static void main(String[] args){
		//initialise native bindings
		try{
			loadNatives();
		}catch(IOException | UnsatisfiedLinkError e){
			e.printStackTrace();
			return;
		}
		
		CommandLineParser parser = new DefaultParser();
		try{
			CommandLine cli = parser.parse(options, args);
			if(!cli.hasOption('h')){
				handleInput(cli);
				return;
			}
		}catch(ParseException e){
			System.out.println(e.getMessage());
		}
		
		HelpFormatter help = new HelpFormatter();
		help.setWidth(120);
		help.printHelp("index", options, true);
	}
	
	/**
	 * Handles the input arguments.
	 * @param cli The command line arguments.
	 */
	private static void handleInput(CommandLine cli){
		Path data = Paths.get(cli.getOptionValue('d'));
		int k = Integer.parseInt(cli.getOptionValue('k'));
		boolean cores = cli.hasOption('c');
		boolean labels = cli.hasOption('l');
		int threads = Integer.parseInt(cli.getOptionValue('t', "1"));
		int intersections = Integer.parseInt(cli.getOptionValue('i', String.valueOf(Integer.MAX_VALUE)));
		boolean verbose = cli.hasOption('v');
		String logFile = verbose ? cli.getOptionValue('v', null) : null;
		Path output = Paths.get(cli.getOptionValue('o'));
		boolean full = cli.hasOption('f');
		
		try(InputStream in = Files.newInputStream(data)){
			Path name = data.getFileName();
			if(name == null){
				throw new IllegalArgumentException("Input file has no name");
			}
			
			Index index;
			if(name.endsWith(".idx")){
				index = new Index(in);
				if(cores){
					index.computeCores(threads);
				}
			}else{
				index = new Index(
					IndexUtil.readGraph(in),
					k,
					cores,
					labels,
					threads,
					intersections,
					verbose ? (logFile == null ? ProgressListener.LOG : ProgressListener.stream(new PrintStream(logFile, StandardCharsets.UTF_8))) : ProgressListener.NONE
				);
			}

			System.out.println("Total cores: " + index.getTotalCores());
			try(OutputStream out = new BufferedOutputStream(Files.newOutputStream(output))){
				index.write(out, full);
				System.out.println("Index succesfully saved to disk.");
			}
		}catch(IllegalArgumentException | InterruptedException | IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Small testing subroutine to find random CPQ cores.
	 */
	public static void findRandomCore(){
		while(true){
			CPQ q = CPQ.generateRandomCPQ(10, 2);
			System.out.println("Testing: " + q);
			QueryGraphCPQ g = q.toQueryGraph();
			System.out.println("computing core...");
			UniqueGraph<Vertex, Predicate> core = g.computeCore().toUniqueGraph();
			if(core.getEdgeCount() != g.getEdgeCount()){
				GraphPanel.show(g);
				GraphPanel.show(core, g::getVertexLabel, Predicate::getAlias);
				return;
			}
		}
	}
	
	/**
	 * Loads the compiled JNI libraries required for nauty.
	 * @throws IOException When an IOException occurs.
	 * @throws UnsatisfiedLinkError When loading a native library fails.
	 */
	public static final void loadNatives() throws IOException, UnsatisfiedLinkError{
		try(DirectoryStream<Path> libs = Files.newDirectoryStream(Paths.get("lib"), Files::isRegularFile)){
			for(Path lib : libs){
				Path name = lib.getFileName();
				if(name != null && !name.toString().endsWith(".jar")){
					System.out.println("Loading native library: " + name);
					System.load(lib.toAbsolutePath().toString());
				}
			}
		}
	}
	
	static{
		options = new Options();
		options.addOption("h", "help", false, "Prints this help text");
		options.addOption(Option.builder("d").required().longOpt("data").hasArg().argName("file").desc("The graph file to create an index for or a saved index file.").build());
		options.addOption(Option.builder("k").required().longOpt("diameter").hasArg().argName("k").desc("The value of k (diameter) to compute the index for.").build());
		options.addOption(Option.builder("c").longOpt("cores").desc("If passed then cores will be computed.").build());
		options.addOption(Option.builder("l").longOpt("labels").desc("If passed then labels will be computed.").build());
		options.addOption(Option.builder("t").longOpt("threads").hasArg().argName("number").desc("The number of threads to use for core computation (1 by default).").build());
		options.addOption(Option.builder("i").longOpt("intersections").hasArg().argName("max").desc("The maximum number of branches for intersection cores (unlimited by default).").build());
		options.addOption(Option.builder("v").longOpt("verbose").hasArg().optionalArg(true).argName("file").desc("Turns on verbose logging of construction steps, optionally to a file.").build());
		options.addOption(Option.builder("o").required().longOpt("output").hasArg().argName("file").desc("The file to save the constructed index to.").build());
		options.addOption(Option.builder("f").longOpt("full").desc("If passed the saved index has all information required to compute cores later.").build());
	}
}
