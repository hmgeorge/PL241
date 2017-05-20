import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Hashtable;

class FileReader{
    
    private String filename;
    char sym; //copy of current character
    FileInputStream fstream;
    
    public char GetSym(){
	//return current and advance to the next character on the input. 
	try {			
	    int ret = fstream.read(); //this should by itself put -1 (0xff) in sym
	    //returns current character and also advances stream by one character
	    if( ret == -1 ){
		sym = 0xff;
	    }
	    else{
		sym = (char)ret;
	    }
	    
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    System.out.println("coming here?");
	    sym = 0x00;
	}
	
	return sym;
    }
    
    public void Error( String errorMsg, int lineno ){
	System.err.println("error:"  + filename + ":" + lineno + " " + errorMsg );
    }
    
    public FileReader( String fname ){
	filename = fname;
	File f = new File( filename );
	try {
	    fstream = new FileInputStream( f );
	}
	catch (NullPointerException e ){
	    System.err.println("Error: null value passed to FileReader, exiting");
	    System.exit( 0 );
	}catch (FileNotFoundException e) {
	    System.err.println("File not found: " + e.getMessage( ));
	    System.exit( 1 );
	}
    }
    
    //note no fseek with this.
}

public class Lexer{
    
    private char inputSym;
    public int number;
    public int id;
    public int val;
    private FileReader freader;
    private Pattern pattern = Pattern.compile("[\\[\\]+-\\\\*/<>=!\\{}\\\\.()]");
    private Pattern l_pattern = Pattern.compile("[a-zA-Z]");
    private Pattern d_pattern = Pattern.compile("[0-9]");
    private int num_lines = 1;
    
    private Hashtable<String, Integer> str_id_table = null;
    private Hashtable<Integer, String> id_str_table = null;

    int unique_id;
    private Hashtable<String, Integer> keywords;

    private void initkeywords( ){

	keywords = new Hashtable<String, Integer>();

	keywords.put("main", Token.T_MAIN );
	keywords.put("if", Token.T_IF );
	keywords.put("fi", Token.T_FI );
	keywords.put("while", Token.T_WHILE );
	keywords.put("let", Token.T_LET );
	keywords.put("call", Token.T_CALL );
	keywords.put("do", Token.T_DO );
	keywords.put("od", Token.T_OD );
	keywords.put("function", Token.T_FUNCTION );
	keywords.put("procedure", Token.T_PROCEDURE );
	keywords.put("else", Token.T_ELSE );
	keywords.put("var", Token.T_VAR );
	keywords.put("array", Token.T_ARRAY );
	keywords.put("return", Token.T_RETURN );
	keywords.put("then", Token.T_THEN );
    }
    private void Next() { inputSym = freader.GetSym(); }
    
    public int GetSym( ){
	//return current token and advance to next token
	
	int sym_t = Token.T_ERROR;
	
	while( Character.isWhitespace( inputSym ) == true ){
	    
	    if( inputSym == '\n')
		num_lines++;
	    Next();
	}
	
	if( isletter( inputSym ) == true ){
	    
	    if( inputSym == 0xff ){
		System.out.println("WHAT!");
		System.exit(1);
	    }
	    String idstr = new String( "" );
	    idstr = idstr + inputSym;
	    Next();
	    while( isalnum( inputSym ) == true  ){
		idstr = idstr + inputSym;
		Next();
	    }
	   
	    sym_t = idtotoken( idstr  ); //takes into account keywords.
	    
	    //add to table, returns id of this element, 
	    if( sym_t == Token.T_IDENT ){
		id = addtotable( idstr );
	    }
	   	    
	}
	else if( isdigit( inputSym ) == true ){
	    String idstr = new String( "" );
	    idstr = idstr + inputSym;
	    Next();
	    while( isdigit( inputSym ) == true  ){
		idstr = idstr + inputSym;
		Next();
		
	    }
	    //store integer value of this number.
	    val = new Integer( idstr );
	    sym_t = Token.T_NUMBER;
	}
	else if( ispunct(inputSym ) == true ){
	    char p_char = inputSym;
	    Next( );
	    sym_t = pttotoken( p_char );
	    
	    
	}
	else{

	    if( inputSym == 0xff ){
		sym_t = Token.T_EOF;
	    }
	    //else, its an error: token not recognized.
	}
	
	return sym_t;
    }
    
    public void Error( String errormsg ){
	freader.Error( errormsg , num_lines );
    }
    
    private int pttotoken(char pchar) {
	
	int pt = Token.T_ERROR;
	
	switch( pchar ){
	    
	    case '*':
		pt = Token.T_MULT;
		break;
	    case '/':
		pt = Token.T_DIV;
		break;
	    case '+':
		pt = Token.T_PLUS;
		break;
	    case '-':
		pt = Token.T_MINUS;
		break;
	    case '=':
		if( inputSym == '='){
		    pt = Token.T_EQ;
		    Next( );
		}
		else{
		    //returns invalid token
		}
		break;
	    case '!':
		if( inputSym == '='){
		    pt = Token.T_NEQ;
		    Next( );
		}
		else{
		    //returns invalid token
		}
		break;
	    case '<':
		
		if( inputSym == '='){
		    pt = Token.T_LEQ;
		    Next( );
		}
		else if( inputSym == '-' ){
		    pt = Token.T_BECOMES;
		    Next( );
		}
		else{
		    pt = Token.T_LESS;
		}
		break;
	    case '>':
		
		if( inputSym == '='){
		    pt = Token.T_GEQ;
		    Next( );
		}
		else{
		    pt = Token.T_GREAT;
		}
		break;
	    case '.':
		pt = Token.T_DOT;
		break;
	    case ',':
		pt = Token.T_COMMA;
		break;
	    case ')':
		pt = Token.T_RPAREN;
		break;
	    case '(':
		pt = Token.T_LPAREN;
		break;
	    case ';':
		pt = Token.T_SCOLON;
		break;
	    case '}':
		pt = Token.T_RCURL;
		break;
	    case '{':
		pt = Token.T_LCURL;
		break;
	    case '[':
		pt = Token.T_LSQUARE;
		break;
	    case ']':
		pt = Token.T_RSQUARE;
		break;
	    default:
		//invalid punct error			
	}
	
	return pt;
    }
    
    private boolean ispunct( Character c ){
    	Matcher m = pattern.matcher( c.toString() );
        if( m.find( ) == true )
            return true;
	
    	return false;
    }

    private boolean isletter( Character c ){
    	Matcher m = l_pattern.matcher( c.toString() );
        if( m.find( ) == true )
            return true;
	
    	return false;
    }

    private boolean isdigit( Character c ){
    	Matcher m = d_pattern.matcher( c.toString() );
        if( m.find( ) == true )
            return true;
	
    	return false;
    }

    private boolean isalnum( Character c ){
	return isletter(c) || isdigit( c) ;
    }

    private boolean isKeyword( String str ){

	if( keywords.get(str) == null )
	    return false;

	return true;
    }
    
    private int idtotoken(String str ) {
	Integer i;
	i = (Integer)keywords.get( str );
	if( i == null )
	    return Token.T_IDENT;
	
	return i;
    }
    
    private int addtotable(String id) {
	int i = unique_id++;
	str_id_table.put(id, i );
	id_str_table.put(i, id );

	return i;
    }

    public String Id2String( int id ){
	return (String)id_str_table.get( id );
    }

    public int String2Id( String s ){
	return (Integer)str_id_table.get( s );
    }

    public void ClearTables( ){
	id_str_table.clear( );
	str_id_table.clear( );
    }

    public int GetId( ){
	return id;
    }
    
    public int GetVal( ){
	return val;
    }

    public Lexer( String fn ){ 
	
	freader = new FileReader(fn);
	str_id_table = new Hashtable<String, Integer>();
	id_str_table = new Hashtable<Integer, String>();
	unique_id = 1;
	
	initkeywords();
	Next(); //just to have the first character 'loaded' in inputSym

    }
}