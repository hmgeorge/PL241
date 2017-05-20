import java.util.ArrayList;
import java.util.Hashtable;

//i think you need to keep track of them separate from the Nodes because these are defined
//before they are used (results keep track of uses)

//could have split into different classes.
public class SymbolType{
    public static int VarType = 1;
    public static int ArrayType = 2;
    public static int FuncType = 3;
    public static int GlobalVarType = 4;

    public int type;
    public ArrayList<Integer> arraydims;
    public ArrayList<SymbolType> formalparams; // yeah cannot be a function

    public SymbolType( int t ){

	if( t == ArrayType ){
	    arraydims = new ArrayList<Integer>();
	}
	else if( t == FuncType ){
	    formalparams = new ArrayList<SymbolType>();
	}
	type = t;
    }

    private String printarraydims( ){
	String astring = "";
	for( int i = 0; i < arraydims.size( ); i++ ){
	    astring += "[ " + arraydims.get( i ) + " ] ";
	}

	return astring;
    }

    private String printformalparams( ){

	String fstring = "( ";

	if( formalparams.size() != 0 ){
	    fstring += formalparams.get( 0 ).toString( );
	}
	
	for( int i = 1; i < formalparams.size( ); i++ ){
	    fstring += ", " + formalparams.get( i ).toString();
	}

	fstring += ")";
	return fstring;
    }
    

    public String toString( ){

	if( type == VarType ){
	    return "var ";
	}
	else if( type == ArrayType ){
	    return "array " + printarraydims( );
	}
	else if( type == FuncType ){
	    return "function " + printformalparams( );
	}
	
	return null;
    }

    public void AddDimension( int d ){
	if( type == ArrayType ){
	    arraydims.add( d );
	}
	else {
	    //this is an error 
	}

	return;
    }

    public boolean IsSameDims( ArrayList<Integer> dims ){
	
	if( dims == null || type != ArrayType )
	    return false;

	if( dims.size( ) != arraydims.size( ) ){
	    return false;
	} // no default arguments

	for( int i = 0; i < dims.size( ); i++ ){
	    if( dims.get(i) != arraydims.get(i) ){
		return false;
	    }
	}

	return true;
    }	

    public boolean IsSameType( SymbolType st ){

	//not checking for null
	if( st.GetType( ) == VarType && type == VarType )
	    return true;
	else if( st.GetType( ) == ArrayType && 
		 IsSameDims( st.GetDims( ) ) )
	    return true;
	else if( st.GetType( ) == FuncType &&
		 IsSameParams( st.GetParams( ) ) )
	    return true;

	return false;
	
    }

    //in a function call, you will first collect a list of
    //arraylist<symboltype> of the parameters, (what to do about
    //numbers? this list will be compared against the fparams list
    //of the function.
    //also we will keep a mapping of which parameter maps to which
    //identifier.
    public boolean IsSameParams( ArrayList<SymbolType> fparams ){
	
	if( fparams == null || type != FuncType )
	    return false;

	if( fparams.size( ) != formalparams.size( ) )
	    return false;

	for( int i = 0; i < fparams.size( ); i++ ){

	    SymbolType t1 = fparams.get(i);
	    SymbolType t2 = formalparams.get(i);

	    //note: t1 or t2 must not (and cannot be) a function type.
	    if( t1.IsSameType( t2 ) == false )
		return false;
	}

	return true;
    }

    public void AddFormalParam( SymbolType t ){
	if( type == FuncType ){
	    formalparams.add( t );
	}
	else {
	    //this is an error
	}
    }
    
    public int GetType( ){
	return type;
    }

    public ArrayList<Integer> GetDims( ){
	return arraydims;
    }

    public ArrayList<SymbolType> GetParams( ){
	return formalparams;
    }
}