//base result
public class Node{

    public static int STATSEQ = 1;
    public static int STAT = 2;
    public static int ASSIGN = 3;
    public static int IF = 4;
    public static int WHILE = 5;
    public static int RETURN = 6;
    public static int EXPR = 7;
    public static int IDENT = 8;
    public static int ARRAY = 9;
    public static int NUMBER = 10;
    public static int CALLEXPR = 11;
    public static int OP = 12;
    public static int CALLSTAT = 13;

    public Node( ){
    
    }
    
    public String toString( int indent ){
	throw new Error( "toString called on Node object" );
    }

    public String IdentTabs( int ident ){
	
	String tstring = "";
	for( int i = 0; i < ident; i++ ){
	    tstring += " ";
	}

	return tstring;
    }

    public int kind( ){
	throw new Error("invalid call of kind of Node object");
    }
       
} 