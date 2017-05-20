import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

public class BasicBlock{

    private static int unique_id = 0;

    private int branch_num;
    private Integer block_id;

    ArrayList<BasicBlock> next;
    ArrayList<BasicBlock> prev;
    ArrayList<Instruction> instrs;
    Hashtable<String, Instruction > phi_assigns ; 
    Iterator<String> phi_iterator;

    //semantics of phi says that all phis are executed
    //concurrently, so ordering is not an issue.
    
    SymbolTable symbol_table;

    //each block also has a set of phi_instructions.
    //treating phi instructions just like moves, i.e
    //phi x x1 x2 x3 is equivalent to x4 = phi( x1, x2, x3)
    //therefore by committing phi, you are assigning a new subscript for
    //x, use of later on will use this new subscript
    
    boolean rseen;
    boolean loop_header;

    BasicBlock immDom;  //note: cannot be self. 
    //is a set of all blocks that dominates this block needed?
    //self is one of them

    BasicBlock joinBlock; //hack!, can be null. for exit block.
    BasicBlock jmpBlock; // needed for then and while.

    public BasicBlock( int br_no, SymbolTable symtab ){
	
	branch_num = br_no;
	next = new ArrayList<BasicBlock>( );
	prev = new ArrayList<BasicBlock>( );
	instrs = new ArrayList<Instruction>( );
	rseen = false;
	loop_header = false;
	symbol_table = symtab;
	//backup_values = new Hashtable<String, SSAValue>( );
	block_id = ++unique_id;
	immDom = null;
	joinBlock = null;
	jmpBlock = null;
    }

    public void SetImmDom( BasicBlock b ){
	immDom = b;
    }

    public BasicBlock GetImmDom( ) {
	return immDom; //note: can be null
    }

    public void AddNext( BasicBlock b ){
	next.add( b ); //index number = branch number (for now)
    }

    public void AddPrev( BasicBlock b ){
	prev.add( b ); //same way, index number = branch number
	               //to this block
    }

    public void SetNext( int index , BasicBlock b){
	next.set( index, b );
    }

    public void SetPrev( int index , BasicBlock b){
	prev.set( index, b );
    }
   
    public int NumNext( ){
	return next.size( );
    }

    public Instruction GetInstruction( int i ){
	if( i > instrs.size( ) ){
	    throw new Error("requested index greater than number of instructions");
	}
	
	return instrs.get( i );
    }

    public int GetNumInstr( ){
	return instrs.size( );
    }

    //llly we might have to insert/append instructions later
	
    public BasicBlock GetNext( int i ){
	return next.get(i);
    }

    public BasicBlock GetPrev( int i ){
	return prev.get(i);
    }

    public int NumPrev( ){
	return prev.size( );
    }

    
    public void add( Instruction i ){
	instrs.add( i );
    }

    public SymbolTable GetSymbolTable( ){
	return symbol_table;
    }

    public void SetBranchNo( int num ){
	branch_num = num;
    }

    public int GetBranchNo( ) {
	return branch_num;
    }

    public void SetReturnSeen( ){
	rseen = true;
    }

    public void SetLoopHeader( ){
	loop_header = true;
    }

    public boolean returnSeen( ){
	return rseen;
    }

    public boolean IsLoopHeader( ){
	return loop_header;
    }

    public void InitPhiTable( ){
	phi_assigns = new Hashtable<String, Instruction >( );
    }

    public boolean ContainsPhi( String name ){
	return phi_assigns.containsKey( name );
    }

    public void AddPhi( String name, Instruction phi_instr ){
	phi_assigns.put( name, phi_instr);
    }

    public void ReplacePhiOperand( String name, 
				   int index_no, SSAValue new_value ){
	Instruction phi_instr = phi_assigns.get( name );
	//System.out.println("ReplacePhiOperand on " + phi_instr.toString( ) + index );
 	phi_instr.ReplaceOperand( index_no, new_value );
    }

    public void InitPhiIterator( ){
	if( phi_assigns == null ){
	    phi_iterator = null;
	}
	else{
	    Set<String> phi_name_set  = phi_assigns.keySet( );
	    phi_iterator = phi_name_set.iterator( );
	}

    }

    public boolean HasMorePhi( ){

	if( phi_iterator == null )
	    return false;
	else
	    return phi_iterator.hasNext( );
    }

    public String NextPhiName( ){
        String phi_name = phi_iterator.next( );
        return phi_name;
    }

    public Instruction GetPhiInstr( String name ){
        return phi_assigns.get( name );
    }

    public String toString( ){

	String bstring = "";
	
	if( phi_assigns != null ){
	    this.InitPhiIterator( );
	    while( this.HasMorePhi( ) ){
              String name = this.NextPhiName( );
                Instruction phi_instr = this.GetPhiInstr( name );

                if( phi_instr.IsDeleted( ) )
		    continue;

		bstring += "\n" + phi_instr.toString( );
	    }
	}

	for( int i = 0; i < instrs.size( ); i++ ){
	    if( instrs.get(i).IsDeleted( ) )
		continue;
	    
	    bstring += "\n" + instrs.get(i).toString( );
	}
	
	return bstring;
    }

    public Integer GetBlockId( ){
	return block_id;
    }

    public void SetJoinBlock( BasicBlock b ){
	joinBlock = b;
    }

    public BasicBlock GetJoinBlock( ){
	return joinBlock ; //note: can be null
    }

    public void JumpTo( BasicBlock b ){
	jmpBlock = b;
    }

    public BasicBlock GetJump( ){
	return jmpBlock; //note: can be null
    }
    
}