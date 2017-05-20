//an instruction in SSA according to the format given in the notes
import java.util.ArrayList;

public class Instruction{

    private static int unique_id=1;

    public static int I_NORMAL = 1;
    public static int I_PHI = 2;

    Opcode ocode;
    int otype; 

    ArrayList<SSAValue> operands; //2 in most, 2 for phi, N for functions?
    //remember, an operand can be null.. because of deletions when dealing
    //with returns
    SSAValue phi_value; // take ssa_val from symbol table?
    SSAValue backup_value;

    SSAValue instr_value; //hack: stores the SSAValue created for this
                          //instruction (back pointer to SSAValue)
    Integer value_number; // the value number of this instruction.
                          // used only for printing and for quick 
                          // comparisions.
    boolean deleted;
    
    Integer n; // actual instruction number. number given by 
               //first topologically sorting the blocks, then giving
               //a number to each undeleted instruction

    Instruction( Opcode oc, ArrayList<SSAValue> os ){
	ocode = oc;
	operands = os;
	otype = I_NORMAL;
	value_number = unique_id++;
	deleted = false;
	n = -1;
	instr_value = null;
    }
    
    Instruction( SSAValue p_val, ArrayList<SSAValue> os, SSAValue back_val ){
	phi_value = p_val;
	backup_value = back_val;
	operands = os;
	otype = I_PHI;
	value_number = unique_id++;
	deleted = false;
	n = -1;
	instr_value = null;
    }

    public ArrayList<SSAValue> Operands( ){
	return operands;
    }

    public void ReplaceOperand( SSAValue old_value, SSAValue new_value ){
	
	for( int i = 0; i < operands.size( ) ; i++ ){
	    if( operands.get(i) == old_value ){
		ReplaceOperand( i, new_value );
	    }
	}
	
    }

    public void ReplaceOperand( int index, SSAValue new_value ){
	if( index >= operands.size( ) ){
	    throw new Error(" operand index > operand size ");
	}
	SSAValue old_value = operands.get( index );
	if( old_value != null )
	    old_value.RemoveUse( this );

	if( new_value != null )
	    new_value.AddUse( this );

	operands.set( index, new_value );
    }

    public SSAValue GetPhiValue( ){
	
	if( otype != I_PHI ){
	    throw new Error("phi value not valid for normal instructions");
	}	
	return phi_value;
    }

    public SSAValue GetBackupValue( ){
	
	if( otype != I_PHI ){
	    throw new Error("phi value not valid for normal instructions");
	}	
	return backup_value;
    }

    public String toString( ){
	
	String i_string = "" ;

	if( instr_value != null )
	    i_string += instr_value;
	else
	    i_string += value_number;
	
	if( otype == I_PHI ){
	    i_string +=  ": " + phi_value.toString( )
		                            + " = phi";
	}
	else{
	    i_string +=  ": " + ocode.toString( );
	}

	for( int i = 0; i < operands.size( ); i++ ){
	    SSAValue op = operands.get(i);
	    if( op != null ){
		i_string += " " + op.toString( );
	    }
	}

	return i_string;
    }

    public Opcode GetOpcode( ){
	return ocode;
    }

    public void SetEnclosingValue( SSAValue s ){
	instr_value = s;
    }

    public SSAValue GetEnclosingValue( ){
	return instr_value;
    }


    public int GetValNumber( ){
	return value_number;
    }

    public void SetDeleted( ){
	deleted = true;
    }

    public boolean IsDeleted( ){
	return deleted;
    }
}