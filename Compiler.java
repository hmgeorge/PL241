import java.io.*;

public class Compiler{

    public static void main( String args[ ] ) throws IOException{
    	//string buffer is needed to modify a string?
    	Lexer l = new Lexer( args[0] );
    	System.out.println("opened file");
	Parser p = new Parser( l );
	ParseResult p_result = p.parse( );

    	if( p_result != null ){
	    
	    System.out.println("parse successful");
	    
	    Opt.CopyPropagate( p_result );
	    Opt.CSE( p_result );
	    Opt.CSE( p_result ); //always twice?
	    
	    //p_result.WriteVCG( args[0] );
	    
	    //do the reg alloc for each function
	    RegAlloc.Do( p_result );
	    
	    //p_result.WriteVCG( args[0] );
	    	        
	    //do the code gen for each function
	    int [ ] code = CodeGen.Do( p_result );
	    
	    p_result.WriteVCG( args[0] );

	    CodeGen.DumpCode( );

	    DLX.load( code );
	    DLX.execute( );
            
	} 
	
	/*while( true ){
    		int t = l.GetSym();

		if( t == Token.T_ERROR ){
		    l.Error("Invalid token in input");
		}

    		if( t == Token.T_EOF  )
    			break;

    		if( t == Token.T_IDENT ){
		    System.out.println( l.Id2String( l.id ) );
		}
		else{
		    System.out.println( t );
		}
		}*/
	
    }
} //cat