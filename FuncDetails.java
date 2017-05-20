import java.util.ArrayList;
import java.util.HashSet;

public class FuncDetails {

    public String fname;
    public SymbolType type;
    public SymbolTable symtab;
    public BasicBlock body;
    public ArrayList<String> fpmap;
    ArrayList<Integer> callee_sr_codes;
    ArrayList<Integer> caller_sr_codes;
    ArrayList<SSAValue> v_reg_codes;
    ArrayList<SSAValue> fparams;

    FuncDetails( ){
	fname = null;
	type = null;
	symtab = null;
	body = null;
	fpmap = null;
	callee_sr_codes = new ArrayList<Integer>( );
	caller_sr_codes = new ArrayList<Integer>( );
	v_reg_codes = new ArrayList<SSAValue>( );
	fparams = new ArrayList<SSAValue>( );
    }

    public void setname( String name ){
	fname = name;
    }

    public void settype( SymbolType t ){
	type = t;
    }

    public void setnode( BasicBlock bn ){
	body = bn;
    }

    public void setsymtab( SymbolTable tab ){
	symtab = tab;
    }

    public void setfpmap( ArrayList<String> map ){
	fpmap = map;
    }

    /* add register number for callee saved registers */
    public void AddCalleeSRCode( int reg ){
	if( callee_sr_codes.contains( reg ) == false )
	    callee_sr_codes.add( reg );
    }

    public ArrayList<Integer> GetCalleeSRCodes( ){
	return callee_sr_codes;
    }

    /* add register number for caller saved registers */
    public void AddCallerSRCode( int reg ){
	if( caller_sr_codes.contains( reg ) == false )
	    caller_sr_codes.add( reg );
    }

    public ArrayList<Integer> GetCallerSRCodes( ){
	return caller_sr_codes;
    }
    
    public void AddVRegCode( SSAValue v ){
	v_reg_codes.add( v );
    }

    public ArrayList<SSAValue> GetVRegCodes( ){
	return v_reg_codes;
    }

    public void AddFParamValue( SSAValue v ){
	fparams.add( v );
    }

    public ArrayList<SSAValue> GetFParamValues( ){
	return fparams;
    }
}