import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Iterator;

class SymbolTableEntry{

    SymbolType type;
    int ssa_subscript; //meant to hold current ssa_subscript.
                       //valid for variables of type IDENT only.
    int backup_subscript;

    SSAValue value;
    SSAValue backup_value;
    int offset; //keeping positive for now. the ebp has to be 
                //added with the -ive of this number

    //BasicBlock funcbody;
    //might have to add address (for variable) later

    SymbolTableEntry(SymbolType t ){
	type = t;
	ssa_subscript = 0; //init to 0
	backup_subscript = -1;
	value = null;
	backup_value = null;
        //funcbody = null;
	offset = -1;
    }

    void SetOffset( int o ){
	offset = o;
    }

    int GetOffset( ){
	return offset;
    }

    //SymbolTableEntry(SymbolType t , BasicBlock fbody ){
    //type = t;
    //ssa_subscript = -1; //init to 0
    //funcbody = fbody;
    //}

    public String toString( ){
	String sestring = "" ;
	sestring += type.toString( );
	return sestring;
    }	

    public SymbolType GetSymType( ){
	return type;
    }
    
    //public BasicBlock GetFuncBody( ){
    //return funcbody;
    //}

    public SSAValue GetValue( ){
	if( backup_value != null ){
	    return backup_value;
	}

	return value;
    }

    public void PutValue( SSAValue v ){
	value = v;
	backup_value = null;
    }

    public void SaveBackupValue( SSAValue v ){
	backup_value = v;
    }

    public void SaveBackupSubscript( int v ){
	backup_subscript = v;
    }

    public int GetSSASubscript( ){
	if( backup_subscript != -1 ){
	    return backup_subscript;
	}

	return ssa_subscript;
    }

    public int IncSSASubscript( ){
	backup_subscript = -1;
	return ++ssa_subscript;
    }
}
	

public class SymbolTable{

    Hashtable<String, SymbolTableEntry> table;
    public SymbolTable parent; //parent yet to be
                               //really activated

    int base_reg;

    public SymbolTable( ){
	table = new Hashtable<String, SymbolTableEntry>( );
	parent = null; 
	base_reg = -1;
    }

    public String toString( ){
	String symtab = "";

	Enumeration<String> e = table.keys();
	
	while( e.hasMoreElements( ) ){
	    String sym = e.nextElement( );
	    SymbolTableEntry se = table.get( sym );
	    symtab += sym + " " + se.toString( ) + "\n";
	}
	return symtab;
    }

    public void AddtoSymbolTable( String symName, SymbolType t ){
	table.put ( symName, new SymbolTableEntry( t ) );	
    }	

    //public void AddtoSymbolTable( String symName, 
    //		   SymbolType t, BasicBlock fbody ){
    //table.put ( symName, new SymbolTableEntry( t , fbody ) );	
    //}

    //public BasicBlock GetFuncBody( String fname ){
       
    //SymbolTableEntry f = table.get( fname );
    //if( f == null || (f.GetSymType().GetType() != SymbolType.FuncType))
    //    return null;
    //
    //return f.GetFuncBody( );
    //}	

    public SymbolTableEntry GetEntry( String name ){
	return table.get( name );
    }

    public boolean IsDefined( String name ){
	return table.containsKey( name );
    }

    public void SaveBackupValue( String name, SSAValue sv ){
	SymbolTableEntry se = table.get( name );
	
	if( se.GetSymType( ).GetType() != SymbolType.VarType ){
	    throw new Error("SSA valid for VarType only");
	}

	se.SaveBackupValue( sv );
    }

    public void SaveBackupSubscript( String name, int s ){
	SymbolTableEntry se = table.get( name );
	
	if( se.GetSymType( ).GetType() != SymbolType.VarType ){
	    throw new Error("SSA valid for VarType only");
	}

	se.SaveBackupSubscript( s );
    }

    public int GetSSASubscript( String name ){
	SymbolTableEntry se = table.get( name );
	
	if( se.GetSymType( ).GetType() != SymbolType.VarType ){
	    throw new Error("SSA valid for VarType only");
	}

	return se.GetSSASubscript( );
    }

    public int IncSSASubscript( String name ){
	SymbolTableEntry se = table.get( name );
	
	if( se.GetSymType( ).GetType() != SymbolType.VarType ){
	    throw new Error("SSA valid for VarType only");
	}

	return se.IncSSASubscript( );
    }

    public SymbolType GetSymType( String name ){

	SymbolTableEntry e = table.get( name );
	if( e == null )
	    return null;
	
	return e.GetSymType( );
    }

    SSAValue GetValue( String vname ){
       
	SymbolTableEntry v = table.get( vname );

	if( v == null || (v.GetSymType().GetType() == SymbolType.FuncType) )
	    throw new Error(); //this is an error

	return v.GetValue( );
    }

    void PutValue( String name , SSAValue new_value ){
	SymbolTableEntry v = table.get( name );

	if( v == null || (v.GetSymType().GetType() == SymbolType.FuncType) )
	    throw new Error(); //this is an error

	v.PutValue( new_value );
	return;
    }

    public boolean IsSameType( String sym1, String sym2 ){

	SymbolTableEntry e1 = table.get( sym1 );
	SymbolTableEntry e2 = table.get( sym2 );

	if( e1 == null || e2 == null )
	    return false;

	SymbolType st1 = e1.GetSymType( );
	SymbolType st2 = e2.GetSymType( );

	return st1.IsSameType( st2 );
    }

    Iterator<String> entries( ) {
	return table.keySet().iterator( );
    }
}