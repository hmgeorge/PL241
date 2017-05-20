import java.util.Hashtable;
import java.util.HashSet;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;
import java.util.Iterator;

public class ParseResult{
    
    public SymbolTable gst;
    public FuncDetails main_fn;
    public Hashtable<String, FuncDetails> fdetails;  
    static String path ="";
    ParseResult( ){
	gst = null;
	main_fn = null;
	fdetails = null;
    }

    void set( SymbolTable symtab, 
	      Hashtable<String, FuncDetails> fd, FuncDetails m){
	gst = symtab;
	main_fn = m;
	fdetails = fd;
    }

    private void WriteCFGFunction( BasicBlock function, String filename ) throws IOException{
	
	BufferedWriter out = new BufferedWriter( new FileWriter( path + filename + ".vcg" ) );
	out.write( "graph: { title: \"" + filename + "\"\n" );
	
	HashSet<BasicBlock> to_visit = new HashSet<BasicBlock>( );
	HashSet<BasicBlock> visited = new HashSet<BasicBlock>( );

	to_visit.add( function );
	
	while( to_visit.isEmpty( ) == false ){
	    BasicBlock b = (to_visit.iterator( )).next( );
	    to_visit.remove( b );
	    visited.add( b );
	    out.write( "node: { title:\"B" + b.GetBlockId( ) + "\"\n");
	    
	    //print out all phi's and other instructions
	    out.write( "label:\"" + b.toString( ) + "\"}\n" );
	    
	    for( int i = 0; i < b.NumNext( ); i++ ){
		BasicBlock nb = b.GetNext(i);

		String edge_string = "edge: { ";

		if( nb.IsLoopHeader( ) && visited.contains( nb ) ){
		    System.out.println( "back edge from " + b.GetBlockId( ) + "to" + nb.GetBlockId( ) );
		    edge_string = "back" + edge_string;
		}

		edge_string += "sourcename:\"B" + b.GetBlockId( ) + "\"";
		edge_string += " targetname:\"B" + nb.GetBlockId( ) + "\"" 
		    + " }\n";
		    
		out.write( edge_string );

		if( visited.contains( nb ) == false )
		    to_visit.add( nb );
	    }
	}
	out.write("}\n");
	out.close( );
    }

    private void WriteDominatorTree( BasicBlock fblock, String filename ) throws IOException {
	BufferedWriter out = new BufferedWriter( new FileWriter( path + filename + ".vcg" ) );
	out.write( "graph: { title: \"" + filename + "\"\n" );
	
	HashSet<BasicBlock> to_visit = new HashSet<BasicBlock>( );
	HashSet<BasicBlock> visited = new HashSet<BasicBlock>( );

	to_visit.add( fblock );

	while( to_visit.isEmpty( ) == false ){
	    BasicBlock b = (to_visit.iterator( )).next( );
	    to_visit.remove( b );
	    visited.add( b );
	    out.write( "node: { title:\"B" + b.GetBlockId( ) + "\"\n");
	    
	    //print out all phi's and other instructions
	    out.write( "label:\"" + b.toString( ) + "\"}\n" );

	    BasicBlock immDomBlock = b.GetImmDom( );
	    String edge_string = "backedge: { ";
		
	    if( immDomBlock != null ) {
		edge_string += "sourcename:\"B" + b.GetBlockId( ) + "\"";
		edge_string += " targetname:\"B" + immDomBlock.GetBlockId( ) + "\"" 
		    + " }\n";
		out.write( edge_string );
	    }

	    for( int i = 0; i < b.NumNext( ); i++ ){
		BasicBlock nb = b.GetNext(i);
	    
		if( visited.contains( nb ) == false )
			to_visit.add( nb );
	    }
	}
	
	out.write("}\n");
	out.close( );
    }
    	
    void WriteVCG( String filename ) throws IOException{
	Set<String> fnames = fdetails.keySet( );
	Iterator<String> f_iterator = fnames.iterator( );
	
	while( f_iterator.hasNext( ) ){
	    String fname = f_iterator.next( );
	    FuncDetails fdetail = fdetails.get( fname );
	    WriteCFGFunction( fdetail.body, filename + "-" + fname + "-cfg");
	    WriteDominatorTree( fdetail.body, filename + "-" + fname + "-dom" );
	}

	WriteCFGFunction( main_fn.body, filename + "-main-cfg" );
	WriteDominatorTree( main_fn.body , filename + "-main-dom" );
    }

    
	
}