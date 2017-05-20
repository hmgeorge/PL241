//optimizations

import java.util.Hashtable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Set;

/*
 * operands for anchor - add, sub, mul, div, load/store
 *
 */

class Anchor {

    Hashtable<Opcode, ArrayList<Instruction> > anchors;

    Anchor( ){
	anchors = new Hashtable<Opcode, ArrayList<Instruction> >( );
	anchors.put( Opcode.add, new ArrayList<Instruction>( ) ); 
	anchors.put( Opcode.sub, new ArrayList<Instruction>( ) ); 
	anchors.put( Opcode.mul, new ArrayList<Instruction>( ) ); 	
	anchors.put( Opcode.div, new ArrayList<Instruction>( ) ); 

	//arrays, load, stores later.
	//note: must add the adda instruction also.. 
	//but for that the adda instruction _must_ be generated
	//just before the load/store
	ArrayList<Instruction> ld_str_instrs = new ArrayList<Instruction>( );
	anchors.put( Opcode.load, ld_str_instrs ); 
	anchors.put( Opcode.store, ld_str_instrs ); 
//	anchors.put( Opcode.adda, new ArrayList<Instruction>( ) ); 
    }
}

public class Opt{

    static Hashtable<BasicBlock, Anchor> anchors;

    private static Anchor CreateAnchorFor( BasicBlock b ){

	/* 
	 * for each instruction in the basic block.
	 * if it is one of _the_ kind, add it to 0 position
	 * in the arraylist for that opcode
	 */
	Anchor a = anchors.get( b );
	
	if( a == null ){
	    a = new Anchor( );
	}

	ArrayList<Instruction> instr_list;

	for( int i = 0; i < b.GetNumInstr( ); i++ ){
	    Instruction instr = b.GetInstruction( i );
	    
	    switch( instr.GetOpcode( ) ){
		case add:
		    instr_list = a.anchors.get(Opcode.add);
		    instr_list.add( 0, instr );
		    break;
		case sub:
		    instr_list = a.anchors.get(Opcode.sub);
		    instr_list.add( 0, instr );
		    break;
		case mul:
		    instr_list = a.anchors.get(Opcode.mul);
		    instr_list.add( 0, instr );
		    break;
		case div:
		    instr_list = a.anchors.get(Opcode.div);
		    instr_list.add( 0, instr );
		    break;
		//loads and stores disambiguation
		case load:
		    instr_list = a.anchors.get(Opcode.load);
		    instr_list.add( 0, instr );
		    break;
		case store:
		    instr_list = a.anchors.get(Opcode.store);
		    instr_list.add( 0, instr );
		    
		    BasicBlock join_block = b.GetJoinBlock( );

		    if( join_block == null )
			break;

		    //5: add (3) (4)
		    //6: add c0 FPBASE0
		    //7: adda (6) (5)
		    //8: store x1 (7)

		    /*
                     * get the adda before the store.. 
		     * if its not an adda, somethings wrong.
		     * get the first operand of the adda,
		     * first operand of that (SSAValue is the name of the
		     * array. Put a kill instruction  (a store would be
		     * sufficient? ) in the anchor of this block's
		     * join block.
		     *
		     */
		    SSAValue adda_op = instr.Operands( ).get(1);
		    Instruction adda_instr = adda_op.GetInstruction( );
		    
		    if( adda_instr == null ){
			throw new Error("instruction cannot be null");
		    }

		    if( adda_instr.GetOpcode( ) != Opcode.adda ){	
			throw new Error("ADDA expected before store");
		    }
		    
		    //Instruction i1 = adda_instr.Operands( ).get( 0).GetInstruction( );
		    //SSAValue arra = i1.Operands( ).get(0);
			
		    /*
			  
		    SSAValue add_value = add_instr.Operands( ).get( 0 );
		    Instruction adda_instr = add_value.GetInstruction( );
		    
		    if(adda_instr.GetOpcode( ) != Opcode.adda ){
			throw new Error("ADDA having array base was expected");
		    }
		    */
		    
		    SSAValue array_value = adda_instr.Operands( ).get( 0 );
		 
		    //create a new kill (store?) instruction
		    ArrayList<SSAValue> operands = new ArrayList<SSAValue>( );
		    operands.add( array_value );

		    Instruction kill_instr = new Instruction( Opcode.store,
							      operands );

		    if( join_block.IsLoopHeader( ) ){
			//anchor already exists. so take it
			//and add this to the end.

			Anchor join_anchor = anchors.get( join_block );
			if( join_anchor == null ){
			    throw new Error("For loop, anchor should not be null");
			}

			join_anchor.anchors.get(Opcode.store).add( kill_instr );
		    }
		    else{

			Anchor join_anchor = new Anchor( );
			join_anchor.anchors.get(Opcode.store).add( 0, kill_instr );
		    }		    
		    	    
		    break;
		default:
		    break;
	    }

	}
	return a;
    }

    private static void DFS( BasicBlock block, 
			     ArrayList<BasicBlock> blocks ,
	                     HashSet<BasicBlock> discovered){
	
	/*
	 * Do DFS on each of the blocks predecessors, 
	 * if not discovered already, add to discovered and add to
	 * blocks. if the node is a loop header, node can be put
	 * after processing its first previous block,
	 * loop body comes later.
	 */

	//System.out.print ("Previous for " + block.GetBlockId( ) + "->" );

        //for( int i = 0; i < block.NumPrev( ); i++ ){
	//    System.out.print( block.GetPrev(i).GetBlockId( ) + " " );
	//}

	//System.out.print("\n");

	if( discovered.contains( block ) ){
	    return;
	}

	if( block.NumPrev( ) == 0 ){
	    discovered.add( block );
	    //System.out.println("Adding block " + block.GetBlockId( ) );
	    blocks.add( block );
	    return;
	}

	BasicBlock next_block = block.GetPrev( 0 );
	
	DFS( next_block, blocks, discovered );
	
	if( block.IsLoopHeader( ) ){
	    discovered.add( block );
	    //System.out.println("Adding block " + block.GetBlockId( ) );
	    blocks.add( block );
	}
	
	for( int i = 1; i < block.NumPrev( ); i++ ){
	    DFS( block.GetPrev(i), blocks, discovered );
	}
	
	if( block.IsLoopHeader( ) == false ){
	    discovered.add( block );
	    //System.out.println("Adding block " + block.GetBlockId( ) );
	    blocks.add( block );
	}

	return;
    }

    static ArrayList<BasicBlock> TopSort( BasicBlock enter_block ){

	//create an adjaceny list representation
/*
	HashSet<BasicBlock> to_visit = new HashSet<BasicBlock>( );
	HashSet<BasicBlock> visited = new HashSet<BasicBlock>( );
	
	to_visit.add( enter_block );

	while( to_visit.isEmpty( ) == false ){
	    BasicBlock b = (to_visit.iterator( )).next( );
	    to_visit.remove( b );
	    visited.add( b );
	    	    
	    for( int i = 0; i < b.NumNext( ); i++ ){
		BasicBlock nb = b.GetNext(i);
	    
		if( visited.contains( nb ) == false )
			to_visit.add( nb );
	    }
	}
*/
	ArrayList<BasicBlock> blocks = new ArrayList<BasicBlock>( );
	HashSet<BasicBlock> discovered = new HashSet<BasicBlock>( );
	DFS( enter_block.GetNext( 1 ), blocks , discovered );

	System.out.print("Top Sort \t");

	for( int i = 0; i < blocks.size( ); i++ ){
	    System.out.print( " " + blocks.get(i).GetBlockId( ) );
	}
	
	System.out.println();
	
	return blocks;
    }


    static void CopyPropFunction( FuncDetails fn ){

	ArrayList<BasicBlock> block_order = TopSort( fn.body );
//	for( int i = 0; i < block_order.size( ); i++ ){
	//  System.out.println( block_order.get(i).GetBlockId( ) );
//	}

	for( int i = 0; i < block_order.size( ); i++) {
	    BasicBlock b = block_order.get(i);
	    DoCopyPropagate( b );
	}

    }

    static void CopyPropagate( ParseResult p_result ){
 
	//process blocks in topological order.
	//for each move instruction, replace each
	//use of the destination SSAValue with address larger
	//than this one with the source SSAValue

	Set<String> fnames = p_result.fdetails.keySet( );
	Iterator<String> f_iterator = fnames.iterator( );

	while( f_iterator.hasNext( ) ){
	    String fname = f_iterator.next( );
	    FuncDetails fdetail = p_result.fdetails.get( fname );
	    CopyPropFunction( fdetail );
	}
	
	CopyPropFunction( p_result.main_fn );
/*
	HashSet<BasicBlock> to_visit = new HashSet<BasicBlock>( );
	HashSet<BasicBlock> visited = new HashSet<BasicBlock>( );
	
	to_visit.add( p_result.main_fn );

	while( to_visit.isEmpty( ) == false ){
	    BasicBlock b = (to_visit.iterator( )).next( );
	    to_visit.remove( b );
	    visited.add( b );
	    	    
	    for( int i = 0; i < b.NumNext( ); i++ ){
		BasicBlock nb = b.GetNext(i);
	    
		if( visited.contains( nb ) == false )
			to_visit.add( nb );
	    }
	}
*/		
    }

    private static void DoCopyPropagate( BasicBlock b ){

	boolean propagated = false;

	for( int i = 0; i < b.GetNumInstr( ); i++ ){
	    Instruction instr = b.GetInstruction( i );
	    int instr_num = instr.GetValNumber( );

	    if( instr.GetOpcode( ) == Opcode.move ){
		ArrayList<SSAValue> operands = instr.Operands( );
		SSAValue source = operands.get(0);
		SSAValue dest = operands.get(1);
	    
		//for each instruction in use of dest with 
		//address greater than value_number, replace
		Iterator<Instruction> d_it = dest.use_iterator( );
		while( d_it.hasNext( ) ){
		    Instruction use_instr = d_it.next( );
		    System.out.println( "For " + instr + " use " + use_instr );
		    //	    if( use_instr.GetValNumber( ) > instr_num ){
			System.out.println("Deleted");
			ArrayList<SSAValue> use_instr_operands = use_instr.Operands( );
			for( int j = 0; j < use_instr_operands.size( ); j++ ){
			    if( use_instr_operands.get(j) == dest ){
				use_instr_operands.set(j, source);
				source.AddUse( use_instr );
				d_it.remove( );
			    }
			}
		        //propagated = true;
			//use_instr.ReplaceOperand( source, dest );
			//}
		}
		instr.SetDeleted( );		
	    }

	    propagated = false;
	}
    }		

    static void CSEFunction( FuncDetails fn ){
	ArrayList<BasicBlock> block_order = TopSort( fn.body );
	
	//create anchors for all blocks
	anchors = new Hashtable<BasicBlock, Anchor>( );
	
	for( int i = 0; i < block_order.size( ); i++ ){
	    BasicBlock b = block_order.get(i);
	    anchors.put( b, CreateAnchorFor( b ) );
	}

	//take each block in the block_order, and for
	//each operand in its anchor, check for CSE in the block
	//and also continue doing so along its dominator block(s)

	for( int i = 0; i < block_order.size( ); i++ ){
	    BasicBlock b = block_order.get( i );
	    doCSE( b , anchors.get( b ) );
	}
    }

    static void CSE( ParseResult p_result ){
	
	Set<String> fnames = p_result.fdetails.keySet( );
	Iterator<String> f_iterator = fnames.iterator( );

	while( f_iterator.hasNext( ) ){
	    String fname = f_iterator.next( );
	    FuncDetails fdetail = p_result.fdetails.get( fname );
	    CSEFunction( fdetail );
	}

	CSEFunction( p_result.main_fn );

    }

    private static void doCSE( BasicBlock block, Anchor block_anchor ){
	/*
	 * Take each arraylist. 
	 */

	//note: The order in which these are applied matters.
	//e.g. with 
	/*

	   let x <- 4;
	   let y <- 3;

	   let z <- a[ x + y - 3 ];
	   let x <- a[ x + y - 3 ];

	   it requires two CSE calls to eliminate all CSE.
           
         */
	Opcode opcodes[ ] = {Opcode.add, Opcode.mul, Opcode.sub, Opcode.div};
	
	for( int i = 0; i < opcodes.length; i++ ){
	    ArrayList<Instruction> instr_anchor = block_anchor.anchors.get( opcodes[i] );
	    doCSEForOp( block, instr_anchor );
	}
	
	//here the CSE for loads and stores go.
	

    }

    private static void doCSEForOp( BasicBlock b , ArrayList<Instruction> op_anchor ){

	//for each instruction in this anchor
	// check for each instruction above this and also instructions in the dominating 
	// blocks
	
	for( int i = 0; i < op_anchor.size( ); i++ ){

	    Instruction instr = op_anchor.get( i );
	    CSEStartFrom( instr, i + 1, op_anchor , b );
	}

    }

    private static void CSEStartFrom( Instruction instr, 
				      int start_index,  
				      ArrayList<Instruction> anchor, BasicBlock block ){
	Instruction prev_instr;
	ArrayList<SSAValue> operands = instr.Operands( );
	Opcode ocode = instr.GetOpcode( );

	if( ocode == Opcode.load || ocode == Opcode.store ){
	    
	    for( int i = start_index; i < anchor.size( ); i++ ){
		Instruction prev_ldstr_instr = anchor.get( i );
		ArrayList<SSAValue> prev_ldstr_ops = prev_ldstr_instr.Operands( );

		if( prev_ldstr_instr.GetOpcode( ) == Opcode.store /* ||
		    prev_instr.GetOpcode( ) == Opcode.kill*/ ){
		    return;
		}
		else{
		
		    //load and operands match, do the replacing
		    if( operands.get( 0 ).IsSame( prev_ldstr_ops.get( 0 ) ) ){
			System.out.println("Match b/w " + instr + " and " 
					   + prev_ldstr_instr );
		    }
		}		
	    }
	    
	}
	else{

	    for( int i = start_index; i < anchor.size( ); i++ ){
		prev_instr = anchor.get( i );
		ArrayList<SSAValue> prev_operands = prev_instr.Operands( );
		
		//compare operands
		if( operands.get( 0 ).IsSame( prev_operands.get( 0 ) ) &&
		    operands.get( 1 ).IsSame( prev_operands.get( 1 ) )  ){
		    //we have a match.
		    //replace all uses of instr with address greater than instr number
		    //(that should be the case by default ) with prev_instr;
		    
		    //need the use chain of the value produced by this instruction
		    SSAValue value = instr.GetEnclosingValue( );
		    SSAValue prev_value = prev_instr.GetEnclosingValue( );
		    
		    //for each instruction in use of dest with 
		    //address greater than value_number, replace
		    int instr_num = instr.GetValNumber( );
		    
		    ReplaceAllUses( prev_value, value, instr_num );
		    
		    instr.SetDeleted( );
		    //System.out.println( "Match between " + prev_instr + " and " + instr );
		}
	    }
	}
	
	/* 
	 *  1) Implement the actual replacing, can be replaced directly i think
	 *  2) Test for multiple instructions
	 */

	BasicBlock immDomBlock = block.GetImmDom( );
	
	if( immDomBlock != null ){
	    Anchor immDomAnchor = anchors.get( immDomBlock );
	    CSEStartFrom( instr, 0, immDomAnchor.anchors.get( instr.GetOpcode( ) ),
			  immDomBlock  );
	}
    }

    
    private static void ReplaceAllUses( SSAValue new_value,
					SSAValue old_value,
					int old_value_number){

	 Iterator<Instruction> v_it = old_value.use_iterator( );
		    
	 while( v_it.hasNext( ) ){
	     Instruction use_instr = v_it.next( );
	     // System.out.println( use_instr );
	     if( use_instr.GetValNumber( ) > old_value_number ){
		 
		 ArrayList<SSAValue> use_instr_operands = use_instr.Operands( );
		 
		 for( int j = 0; j < use_instr_operands.size( ); j++ ){
		     if( use_instr_operands.get(j) == old_value ){
			 use_instr_operands.set(j, new_value );
			 new_value.AddUse( use_instr );
			 v_it.remove( );
		     }
		 }
		 //use_instr.ReplaceOperand( source, dest );
	     }
	 }
    }

}