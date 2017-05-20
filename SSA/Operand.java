//class to store an operand used in an SSA form.

public class Operand{

    String name;
    int num;
    int ssa_val;
    int operand_type;
    
    public Operand( String s ){
	name = s;
	ssa_val = 0;
	operand_type = Kind.S_IDENT;
    }

    public Operand( int i ){
	name = null;
	num = i;
	ssa_val = -1; //undefined
	operand_type = Kind.S_NUM;
    }

    public void SetSSAVal( int new_val ){
	ssa_val = new_val;
    }

    public int GetSSAVal(  ) {
	return ssa_val;
    }

    public toString( ){
	String s = "";
	if( operand_type == Kind.S_IDENT ){
	    s = name + ssa_val;
	}
	else if( operand.type == Kind.S_NUM ){
	    s = num;
	}
    }
}
    
    