import java.io.IOException;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;

public class Parser{

    private int scannerSym;
    private Lexer lex ;
    private ParseResult root_result ;
    //private SymbolTable gsym_table;

    private void Next( ){ scannerSym = lex.GetSym(); }

    private void MustBe( int sym, int token, String errMsg ){
	
	if( sym != token ){
	    //System.out.println( sym + " " + token );
	    FatalSyntaxError( errMsg );
	}
	
    }
        
    public Parser( Lexer l ){
	lex = l;
	root_result = null;
	//gsym_table = new SymbolTable( );
	Next() ; //'load' first token in scannerSym
    }

    public ParseResult parse( ){
	root_result = do_parse( );
	return root_result;
    }

    private ParseResult do_parse( ){
	ParseResult res = null; //just like that
	res = computation( );
	return res;
    }

    private void SyntaxError( String errmessage ){
	lex.Error( errmessage );
    }

    private void FatalSyntaxError( String errmessage ){
	SyntaxError( errmessage );
	System.exit( 2 );
    }

    private ParseResult computation() {
	
	SymbolTable gst = new SymbolTable( );
	gst.AddtoSymbolTable( "FPBASE", 
			      new SymbolType( SymbolType.VarType ) );
	SSAValue fbase = new SSAValue( "FPBASE", Kind.S_IDENT );
	fbase.SetReg( 28 );
	gst.PutValue( "FPBASE",  fbase );
	
	Hashtable<String, FuncDetails> fdetails = new Hashtable<String, FuncDetails>();
 
	MustBe( scannerSym, Token.T_MAIN, "main keyword not found" );
       	Next();

	if( scannerSym == Token.T_VAR || scannerSym == Token.T_ARRAY ){
	
	    //handle variable declarations
	    while( scannerSym == Token.T_VAR || scannerSym == Token.T_ARRAY ){
		varDecl( gst );
	    }  
	    
	    lex.ClearTables( );
	}
	
	if( scannerSym == Token.T_FUNCTION || scannerSym == Token.T_PROCEDURE ){
	    
	    //handle function declarations
	    while( scannerSym == Token.T_FUNCTION || scannerSym == Token.T_PROCEDURE ){
	    
		FuncDetails curr_func = funcDecl( gst );
		fdetails.put( curr_func.fname, curr_func );	    
		lex.ClearTables( );
	    }
	}

	System.out.println( gst.toString( ) );
	
	MustBe( scannerSym, Token.T_LCURL, "missing { for main function" );

	Next();
	
	BasicBlock enter_block = new BasicBlock( 0, gst );

	BasicBlock exit_block = new BasicBlock( 0 , gst ); //deal with branch numbers!
	exit_block.InitPhiTable( );

	exit_block.SetImmDom( enter_block );

	BasicBlock main_block = new BasicBlock( 0, gst );
	main_block.SetImmDom( enter_block );


	//for the load/store CSE
	main_block.SetJoinBlock( exit_block );
	enter_block.SetJoinBlock( exit_block );

	BasicBlock new_block = statSequence( main_block, exit_block );
	
	enter_block.AddNext( main_block ); //branch 1
	enter_block.AddNext( exit_block ); //branch 2
	new_block.AddNext( exit_block ); // branch 1

	main_block.AddPrev( enter_block );
	exit_block.AddPrev( new_block );  //branch 1
	exit_block.AddPrev( enter_block ); //branch 2 , number needed as param?

	CommitPhi( exit_block, null, 0 );

	//not calling Next() because different statements can cause different points of
	//ending, so to be on safe side, each statement type will make sure that scannerSym
	//will point to the correct one to start the next operation.
	//System.out.println( scannerSym );

	MustBe( scannerSym, Token.T_RCURL, "missing closing } for main function" );
	Next( );
	MustBe( scannerSym, Token.T_DOT , "missing closing . for main function" );
	Next( );
	MustBe( scannerSym, Token.T_EOF , "EOF not reached" );
	
	FuncDetails main_fn = new FuncDetails( );
	main_fn.setnode( enter_block );

	//main_fn cannot be null. 
	root_result = new ParseResult( );
	root_result.set( gst, fdetails, main_fn );
	//System.out.println( main_fn.toString( 0 ) );
	return root_result;
    }

    private SSAValue ParseArrayDim( ArrayList<Integer> arraydims, 
				    int rem_dims, BasicBlock block ){
	
	SSAValue dim_value;

	Next( );

	SSAValue access_val = Expression( block );

	int offset = 1;
	for( int i = rem_dims; i < arraydims.size( ); i++ ){
	    offset *= arraydims.get(i);
	}	       
	
	SSAValue word_val = new SSAValue( offset * 4 );
	ArrayList<SSAValue> ops = new ArrayList<SSAValue>( );
	
	ops.add( access_val );
	ops.add( word_val );
	
	Instruction muli_instr = new Instruction( Opcode.mul, ops);
	block.add( muli_instr );
	access_val.AddUse( muli_instr );
	word_val.AddUse( muli_instr );

	dim_value = new SSAValue( muli_instr, Kind.S_INSTR );
	return dim_value;
		
    }

    
    private SSAValue Designator(BasicBlock block ){

	//moved next() to here. this is to allow
	//designator to handle the symbol that we are seeing now.
	//things like check symbol table etc.	

	int id = lex.GetId( );
	String idName = lex.Id2String( id );

	SSAValue desig_value = null;

	int subscript = -1;

	SymbolTable symtab = block.GetSymbolTable();

	if( symtab.IsDefined( idName ) == true ){
	    
	    SymbolType stype = symtab.GetSymType( idName );
	    
	    if( stype.GetType( ) == SymbolType.VarType ){
		//subscript = symtab.GetSSASubscript( idName );
		desig_value = symtab.GetValue( idName );
	    }
	}
	//else if( symtab.parent != null && symtab.parent.IsDefined( idName ) == true ) {
	//  subscript = symtab.parent.GetSSASubscript( idName );
	//} //commented out this code because of  SSA renaming in the presence of function calls
	else{
	    throw new Error("undefined variable used in assignment " + idName);
	} 
	
	//if( desig_value == null ){
	//  desig_value = new SSAValue( idName, Kind.S_IDENT ); // by default, result will be an identifier
	//  symtab.PutValue( idName, desig_value );
	//}

	Next( );

	if( scannerSym == Token.T_LSQUARE ){

	    SSAValue arraybase =  symtab.GetValue( idName );
	    SymbolType arraytype = symtab.GetSymType( idName );
	    
	    ArrayList<Integer> arraydims = arraytype.GetDims( );
	    
	    int dim_position = 1;
	    
	    SSAValue dim_value = ParseArrayDim( arraydims, 1, block );
	    
	    Next( );

	    Instruction addr_calc;

	    while( scannerSym == Token.T_LSQUARE ){
		
		SSAValue temp_value = ParseArrayDim( arraydims, dim_position++,
						     block );
		
		ArrayList<SSAValue> add_ops = new ArrayList<SSAValue>( );
		add_ops.add( dim_value );
		add_ops.add( temp_value );
		
		addr_calc = new Instruction( Opcode.add, add_ops );
 		block.add( addr_calc );
		
		dim_value.AddUse( addr_calc );
		temp_value.AddUse( addr_calc );

		dim_value = new SSAValue( addr_calc, Kind.S_INSTR );

		MustBe( scannerSym, Token.T_RSQUARE, "missing closing ] in designator");
		
		Next( );

	    }
	    
	    
	    //SSAValue fpbase = new SSAValue( "FPBASE", Kind.S_IDENT );
	    //SSAValue fpbase = symtab.GetValue( "FPBASE" );
	    //ArrayList<SSAValue> ops = new ArrayList<SSAValue>( );
	    //ops.add( arraybase );
	    //ops.add( fpbase );
	    
	    //addr_calc = new Instruction( Opcode.adda, ops );
	    //block.add( addr_calc );
	    //fpbase.AddUse( addr_calc );
	    //arraybase.AddUse( addr_calc );

	    //SSAValue array_address = new SSAValue( addr_calc, Kind.S_MEMADDRESS);

	    ArrayList<SSAValue> ops = new ArrayList<SSAValue>( );
	    ops.add( arraybase );
	    ops.add( dim_value );

	    addr_calc = new Instruction( Opcode.adda, ops );
	    block.add( addr_calc );
	    arraybase.AddUse( addr_calc );
	    dim_value.AddUse( addr_calc );
	    
	    desig_value = new SSAValue( addr_calc, Kind.S_MEMADDRESS );
	    //note: type checking not done
	}

	return desig_value;
    }

    private SSAValue Factor( BasicBlock block ){

	SSAValue val = null;

	switch ( scannerSym ){
	    case Token.T_IDENT:
		//moved next to inside designator
		val = Designator( block );
		if( val.GetSSAValueType( ) == Kind.S_MEMADDRESS ){
		    //have to emit a load here
		    ArrayList<SSAValue> load_ops = new ArrayList<SSAValue>( );
		    load_ops.add( val );
		    Instruction instr = new Instruction( Opcode.load, load_ops );
		    block.add( instr );
		    val.AddUse( instr );

		    val = new SSAValue( instr, Kind.S_INSTR );
		}
		//no need to call next here
		break;
	    case Token.T_NUMBER:
		//store the value of the number before calling next
		int num = lex.GetVal();
		val = new SSAValue( num );
		//System.out.println( "saw number " + val );		
		Next( );
		break;
	    case Token.T_LPAREN:
		Next( );
		val = Expression( block );
		MustBe( scannerSym, Token.T_RPAREN, "missing ) in factor");
		Next( );
		break;
	    case Token.T_CALL:
		Next();
		val = funcCall( block );
		break;
	    default:
		FatalSyntaxError("undefined factor term " + scannerSym);
		break;
	}

	return val;
		
    }

    private SSAValue Term( BasicBlock block ){
	
	SSAValue lhs = Factor( block );
	Opcode op = Opcode.invalid;

	while( ( scannerSym == Token.T_MULT ) || ( scannerSym == Token.T_DIV ) ){
	  
	    if( scannerSym == Token.T_MULT ){
		op = Opcode.mul;
	    }
	    else if( scannerSym == Token.T_DIV ){
		op = Opcode.div;
	    }

	    Next( );
	    
	    SSAValue rhs = Factor( block );

	    ArrayList<SSAValue> operands = new ArrayList<SSAValue>( );
	    operands.add( lhs );
	    operands.add( rhs );

	    Instruction i = new Instruction( op, operands );
	    block.add( i );
	    lhs.AddUse( i );
	    rhs.AddUse( i );

	    SSAValue temp  = new SSAValue(i, Kind.S_INSTR ); 

	    lhs = temp;	    
	}

	return lhs;

    }

    private SSAValue Expression( BasicBlock block ){


	SSAValue lhs = Term( block );
       
	Opcode op = Opcode.invalid;
	
	//no need to call next
	while( ( scannerSym == Token.T_PLUS ) || ( scannerSym == Token.T_MINUS ) ){

	    if( scannerSym == Token.T_PLUS ){
		op = Opcode.add;
	    }
	    else if( scannerSym == Token.T_MINUS ){
		op = Opcode.sub;
	    }

	    Next( );
	    
	    SSAValue rhs = Term( block );

	    ArrayList<SSAValue> operands = new ArrayList<SSAValue>( );
	    operands.add( lhs );
	    operands.add( rhs );

	    Instruction i = new Instruction( op, operands );
	    block.add( i );
	    lhs.AddUse( i );
	    rhs.AddUse( i );

	    SSAValue temp  = new SSAValue(i, Kind.S_INSTR ); 

	    lhs = temp;
	}

	return lhs;

    }

    //note that Relation returns an instruction. 
    //the second operand will be put by the calling if/while and code will be
    //emitted in the current basic block itself.
    //code will be emitted as
    //e.g. beq x, @Block2 
    private Instruction Relation(BasicBlock block ){
	
	//please comment - Expression would have done all the required moves and loads, if
	//the lhs has arrays, compare with assignment function
	SSAValue lhs = Expression( block );
	
	//no need to call next( )
	Opcode op = Opcode.invalid;

	//needs to be changed to inverse logic

	switch( scannerSym ){
	    case Token.T_LESS:
		op = Opcode.bge; //Opcode.blt;
		break;
	    case Token.T_LEQ:
		op = Opcode.bgt; //Opcode.ble;
		break;
	    case Token.T_GREAT:
		op = Opcode.ble; //Opcode.bgt;
		break;
	    case Token.T_GEQ:
		op = Opcode.blt; //Opcode.bge;
		break;
	    case Token.T_NEQ:
		op = Opcode.beq; //Opcode.bne;
		break;
	    case Token.T_EQ:
		op = Opcode.bne; //Opcode.beq;
		break;
	    default:
		FatalSyntaxError( "relational operator expected");
		break;
	}

	Next( );
	
	SSAValue rhs = Expression( block );
	ArrayList<SSAValue> operands = new ArrayList<SSAValue>( );
	operands.add( lhs );
	operands.add( rhs );
	
	Instruction cond_i = new Instruction( Opcode.cmp , operands );
	block.add( cond_i );
	lhs.AddUse( cond_i );
	rhs.AddUse( cond_i );

	SSAValue cond_value = new SSAValue( cond_i, Kind.S_INSTR );
	operands = new ArrayList<SSAValue>( );
	operands.add( cond_value );
	operands.add( null ); // must be replaced by caller

	//second operand must be set by IF, WHILE statements and then added to the block
	Instruction br_i = new Instruction( op, operands );
	cond_value.AddUse( br_i ); //no use chain for the label.

	return br_i;
    }

    private BasicBlock assignment( BasicBlock block, BasicBlock join_block ){
	
        //we have already seen the LET keyword

	MustBe( scannerSym, Token.T_IDENT, "missing lvalue in assignment" );

	SymbolTable symtab = block.GetSymbolTable( );

	SSAValue lval = Designator( block ); //will take care of next. 
	               //because we have to deal with either
	               //var or array types.
	               //so no need to call next

	MustBe( scannerSym, Token.T_BECOMES, "assignment operator missing") ;

	Next( );

	SSAValue rval = Expression( block );

	Instruction instr = null;
	ArrayList<SSAValue> ops = null;

	if( rval.GetSSAValueType( ) == Kind.S_MEMADDRESS ){
	    //emit load
	    ArrayList<SSAValue> load_ops = new ArrayList<SSAValue>( );
	    load_ops.add( rval );
	    instr = new Instruction( Opcode.load, load_ops );
	    block.add( instr );
	    rval.AddUse( instr );

	    rval = new SSAValue( instr, Kind.S_INSTR );
	}

	ops = new ArrayList<SSAValue>( );
	ops.add( rval );
	
	if( lval.GetSSAValueType( ) == Kind.S_IDENT ){
	    
	    String name = lval.GetName( );
	    SSAValue backup_value = symtab.GetValue( name );

	    lval = new SSAValue( name, Kind.S_IDENT );
	    lval.SetSSASubscript( symtab.IncSSASubscript( name ) );
	    symtab.PutValue( name, lval );
	    
	    ops.add( lval ); // move y x => x = y;
	    instr = new Instruction( Opcode.move, ops );
	    block.add( instr );

	    //lval.AddUse( instr );
	    rval.AddUse( instr );

	    //int subscript = symtab.GetSSASubscript( name  );
	    //System.out.println( subscript );
	 
	    SSAValue new_value = lval;

	    //note: commitphi should generate a new subscript for name 
	    //at the join node
	    InsertPhi( join_block, block.GetBranchNo() , new_value , 
		       backup_value );
	}
	else if( lval.GetSSAValueType( ) == Kind.S_MEMADDRESS ){

	    ops.add( lval ); // move y x => x = y;
	    instr = new Instruction( Opcode.store, ops );
	    block.add( instr );
	    //lval.AddUse( instr );
	    rval.AddUse( instr );
	}
	else {
	    throw new Error( "undefined lval type in assignment ");
	}

	return block;
	//dont think we will have to handle Next( )
    }
    
    private SSAValue funcCall(BasicBlock block ){

	Opcode callcode = Opcode.call;
	int argc = 0;
	//we have already seen CALL keyword
	SymbolTable symtab = block.GetSymbolTable( );

	MustBe( scannerSym, Token.T_IDENT, "missing function name in call");

	int id = lex.GetId( );
	String idName = lex.Id2String( id );

	//skipping checking for function name validity and also type checking 
	//of the function parameters. can't do it while you are parsing
	//and generating the ssa at the same time.

	Next( );
	System.out.println( idName );
	ArrayList<SSAValue> paramlist = new ArrayList<SSAValue>( );

	if( idName.compareTo( "InputNum") == 0 ){
	    callcode = Opcode.read;
	}
	else if ( idName.compareTo("OutputNum") == 0 ){
	    callcode = Opcode.write;
	}
	else if ( idName.compareTo("OutputNewLine") == 0 ){
	    callcode = Opcode.wln;
	}
	else{
	    argc = 1;
	    SSAValue fname = new SSAValue( idName, Kind.S_FUNCTION );
	    paramlist.add( fname ); //yeah..
	}

	if( scannerSym == Token.T_LPAREN ){
	    Next( );
	    if( isExpressionStart( scannerSym ) ){
		
		SSAValue e = Expression( block );
		paramlist.add( e );

		while( scannerSym == Token.T_COMMA ){
		    Next( );
		    e = Expression( block );
		    paramlist.add( e );
		}
	    }

	    MustBe( scannerSym, Token.T_RPAREN, "missing closing ) for function call" );
	    Next( );
	}
	
	Instruction call_instr = new Instruction( callcode, paramlist );
	block.add( call_instr );
	for( int i = argc; i < paramlist.size( ); i++ ){
	    paramlist.get(i).AddUse( call_instr );
	}

	SSAValue call_value = new SSAValue( call_instr, Kind.S_INSTR );
	
        //no need to call next
	return call_value;
    }

    private BasicBlock ifStatement(BasicBlock block, BasicBlock join_block ){
	
	//we have already seen IF keyword

	Instruction cond_instr = Relation( block ); //returns instruction because we dont 
	                                            //know the value of the second operand yet
                                                    //this will set by if after [else]
	                                            //have been processed.
	block.add( cond_instr );
	
	//dont think we have to call next here
	MustBe( scannerSym, Token.T_THEN, "missing then" );
	
	Next( );

	BasicBlock if_join_block = new BasicBlock( block.GetBranchNo( ),
						   block.GetSymbolTable( ) );
	if_join_block.InitPhiTable( );

	//for the load/store CSE
	if_join_block.SetJoinBlock( join_block );

	BasicBlock else_block = null;

	BasicBlock then_block = new BasicBlock( 0, block.GetSymbolTable( ) );
	block.AddNext( then_block );
	then_block.AddPrev( block );
	then_block.SetImmDom( block );

	//for the load/store CSE
	then_block.SetJoinBlock( if_join_block );

	then_block = statSequence( then_block , if_join_block );

	then_block.JumpTo( if_join_block );
	
	if( scannerSym == Token.T_ELSE ){
	    Next( );

	    //add code to store backup values from the phi_table

	    if_join_block.InitPhiIterator( );
	    while( if_join_block.HasMorePhi( ) ){

		SymbolTable symtab = block.GetSymbolTable( );

                String phi_name = if_join_block.NextPhiName( );
                Instruction phi_instr = if_join_block.GetPhiInstr( phi_name );

		SSAValue phi_value = phi_instr.GetBackupValue( );
		String name = phi_value.GetName( );

		symtab.SaveBackupValue( name, phi_value );
	    }

	    else_block = new BasicBlock( 1, block.GetSymbolTable( ) );
	    block.AddNext( else_block );
	    else_block.AddPrev( block );
	    else_block.SetImmDom( block );

	    
	    //for the load/store CSE
	    else_block.SetJoinBlock( if_join_block );

	    cond_instr.ReplaceOperand(1, 
				      new SSAValue( else_block.GetBlockId() ) );
	    else_block = statSequence( else_block, if_join_block ); //else part
	}
	
	MustBe( scannerSym, Token.T_FI, "missing ending fi" );
	
	Next( ); //must call next
     
	//add code to link among the basic blocks, commit the phi and return the join block

	CommitPhi( if_join_block, join_block, if_join_block.GetBranchNo( ) ); 

	then_block.AddNext( if_join_block );
	if_join_block.AddPrev( then_block );
	
	//set the dom for the join block here
	if( else_block != null ){
	    else_block.AddNext( if_join_block );
	    if_join_block.AddPrev( else_block );
	
	    HashSet<BasicBlock> alive_branches = new HashSet<BasicBlock>( );
	    if( then_block.returnSeen( ) == false ){
		alive_branches.add( then_block );
	    }

	    if( else_block.returnSeen( ) == false ){
		alive_branches.add( else_block );
	    }

	    if_join_block.SetImmDom( Dominator.ClosestCommonDominator( alive_branches ) );
	}
	else{
	    block.AddNext( if_join_block );
	    //System.out.println( block.GetBlockId( ) );
	    if_join_block.AddPrev( block );
	    cond_instr.ReplaceOperand(1,
				      new SSAValue( if_join_block.GetBlockId() ) );
	    //guaranteed that the immDom is the block itself.
	    if_join_block.SetImmDom( block );
	}	
	
	return if_join_block;
    }

    private BasicBlock whileStatement(BasicBlock block, BasicBlock join_block ){
	

	//we have already seen the WHILE keyword
	
	BasicBlock loop_header_block = new BasicBlock( block.GetBranchNo( ),
						  block.GetSymbolTable( ) );
	loop_header_block.SetLoopHeader( );
	loop_header_block.InitPhiTable( );
	loop_header_block.SetImmDom( block );
	loop_header_block.AddPrev( block ); //0

	//for the load/store CSE
	loop_header_block.SetJoinBlock( join_block );

	block.AddNext( loop_header_block );

	Instruction cond_instr = Relation( loop_header_block );

	loop_header_block.add( cond_instr );
	
	MustBe( scannerSym, Token.T_DO, "missing do in while");

	Next( );
	
	BasicBlock loop_body_block =  new BasicBlock( 1 ,
						  block.GetSymbolTable( ) );

	//cond_instr.ReplaceOperand(1, new SSAValue( loop_body_block.GetBlockId() ) );

	loop_header_block.AddNext( loop_body_block );
	loop_body_block.AddPrev( loop_header_block );
	loop_body_block.SetImmDom( loop_header_block );

	//for the load/store CSE
	loop_body_block.SetJoinBlock( loop_header_block );

	loop_body_block = statSequence( loop_body_block, loop_header_block  );
	//note: when you put the jump, jump should be to loop_header_block
	
	loop_body_block.AddNext( loop_header_block ); //backedge
	loop_header_block.AddPrev( loop_body_block ); //1

/* - This should not be inserted here. We keep note that we have
     insert the jump to loop_header here and emit the jump later
     during code gen.

	ArrayList<SSAValue> jmp_ops = new ArrayList<SSAValue>( );
	jmp_ops.add( new SSAValue( loop_header_block.GetBlockId() ));

	Instruction jmp_instr = new Instruction( Opcode.bra, jmp_ops );
	loop_body_block.add( jmp_instr );

*/
	loop_body_block.JumpTo( loop_header_block );
	
	MustBe( scannerSym, Token.T_OD, "missing od in while" );

	Next( ); //must call next

//	System.out.println( loop_header_block.toString( ) );
//	System.out.println( loop_body_block.toString( ) );
	
	CommitPhi( loop_header_block, join_block, loop_header_block.GetBranchNo( ) );
	
	BasicBlock follow_block = new BasicBlock( block.GetBranchNo( ),
						  block.GetSymbolTable( ) );
	loop_header_block.AddNext( follow_block );
	follow_block.AddPrev( loop_header_block );
	follow_block.SetImmDom( loop_header_block );
	follow_block.SetJoinBlock( join_block );

	cond_instr.ReplaceOperand(1, new SSAValue( follow_block.GetBlockId() ) );

	
//	System.out.println( follow_block.toString( ) );
	
	return follow_block;

    }

    private boolean isExpressionStart( int sym ){
	return (sym == Token.T_CALL)||(sym == Token.T_IDENT)||
	    (sym == Token.T_NUMBER)||(sym == Token.T_LPAREN);
    }

    private BasicBlock returnStatement(BasicBlock block, BasicBlock join_block ){

	
	//no next() here because we have to consider the case
	//where there is no expression after return

 	ArrayList<SSAValue> ret_operands = new ArrayList<SSAValue>();
	
 	Instruction ret_instr = new Instruction( Opcode.ret, ret_operands );
 
  	if( isExpressionStart( scannerSym ) == true ){
  	    //do whatever
	    SSAValue ret_e = Expression( block );
 	    ret_operands.add( ret_e );
	    ret_e.AddUse( ret_instr );
 	}

 	block.add( ret_instr );
 	
 	//removes all references of this branch ( block.GetBranchNo() ) from 
 	//the join block
 
 	join_block.InitPhiIterator( );
 	
 	while( join_block.HasMorePhi( ) ){
 	    String phi_name = join_block.NextPhiName( );
 	    join_block.ReplacePhiOperand( phi_name, block.GetBranchNo(), null );
  	}
  	
  	//no next() here because we have to consider the case
  	//where there is no expression after return
 
 	return block;
    }
    
    private BasicBlock statement( BasicBlock block, BasicBlock join_block ) {

        /* 
	   rationale: 
	   if statement is assignment or call, same block will be returned.
	   if statement is IF - new blocks are created, links set up 
	                        and the join block of if will be returned.
	   if statement is WHILE - the new follow block will be created and returned
	   if statement is return - inside return , delete all details of this branch from
	                            its join node, sets returnSeen( ) of curr_block to true
				    and returns this block itself.
	*/
	BasicBlock ret_block = null;
	
	//each function returns the block where the next instruction must be added.
	switch( scannerSym ){
	    case Token.T_LET:
		Next( );
		assignment( block, join_block ); //will return same block itself.
		ret_block = block;
		break;
	    case Token.T_CALL:
		Next( );
		SSAValue call_res = funcCall( block ); //will return same block itself. 
		call_res.SetReg( 1 ); //?
		ret_block = block; 
                //? - not needed for now, Call is both an expr as well as a statement
		break;
	    case Token.T_IF:
		Next( );
		ret_block = ifStatement( block, join_block ); //returns its join block as next block
		break;
	    case Token.T_WHILE:
		Next( );
		ret_block = whileStatement( block, join_block ); //returns new block as next block
		break;
	    case Token.T_RETURN:
		Next( );
		returnStatement( block, join_block );
		block.SetReturnSeen(  );
		ret_block = block;
		break;
	    default:
		System.out.println( scannerSym );
		FatalSyntaxError("undefined statement start");
		break;
	}
	return ret_block;

    }
    
    private BasicBlock statSequence(BasicBlock new_block, BasicBlock curr_join_block ) {
	
        //has to call next before returning, depending on the statement . 

	SymbolTable symtab = curr_join_block.GetSymbolTable( );

	if( curr_join_block == null ){
	    throw new Error("empty current join block");
	}

	//we need atleast one statement, also we have scannerSym pointing to one of
	//let, call, if, while , return. 
	BasicBlock curr_block = statement( new_block , curr_join_block ); 
	/* 
	   rationale: 
	   if statement is assignment or call, same block will be returned.
	   if statement is IF - new blocks are created, links set up 
	                        and the join block of if will be returned.
	   if statement is WHILE - the new follow block will be created and returned
	   if statement is return - inside return , delete all details of this branch from
	                            its join node, sets returnSeen( ) of curr_block to true
				    and returns this block itself.
	*/

	while( scannerSym == Token.T_SCOLON ){
	    
	    Next( );
	    
	    if( isStatementStart( scannerSym ) ){
		
		if( curr_block.returnSeen( ) == true ){
		    FatalSyntaxError("Unreachable code");
		}
		else{
		    curr_block = statement( curr_block , curr_join_block ); 
		}
	    }
	}
	    
	return curr_block;
	//dont think you have to call Next here
    }

    private boolean isStatementStart( int sym ){
	return (sym == Token.T_LET)||(sym == Token.T_CALL)||(sym == Token.T_IF)
	    ||(sym == Token.T_WHILE)||(sym == Token.T_RETURN);
    }

    private BasicBlock funcBody( SymbolTable lst ){
	
	//StatSeqNode seqNode = null;
	BasicBlock enter_block = new BasicBlock( 0, lst );
	BasicBlock exit_block = new BasicBlock( 0 , lst );
	exit_block.InitPhiTable( );
	
	//for the load/store CSE
	enter_block.SetJoinBlock( exit_block );
	
        //Next( );
	//System.out.println( scannerSym );
	MustBe( scannerSym, Token.T_LCURL, "missing left curl");
	
	Next( );
	BasicBlock main_block = new BasicBlock( 0, lst );
	BasicBlock new_block  = main_block;
	
	//for the load/store CSE
	main_block.SetJoinBlock( exit_block );
		
	if( isStatementStart( scannerSym ) == true )
	    new_block = statSequence( main_block, exit_block );

	enter_block.AddNext( main_block ); //branch 1
	enter_block.AddNext( exit_block ); //branch 2
	
	exit_block.AddPrev( new_block );  //branch 1
	exit_block.AddPrev( enter_block ); //branch 2 , number needed as param?

	new_block.AddNext( exit_block ); // branch 1
	
	CommitPhi( exit_block, null, 0 );

	//not calling next.
	MustBe( scannerSym, Token.T_RCURL, "missing right curl");
	return enter_block;
    }

    private void FormalParams( ArrayList<String> map ){ 
	//we already know that scannerSym is LPAREN.
	//so not checking it here.
	
	Next( );
	
	if( scannerSym == Token.T_IDENT ){
	    //we have formal parameters 
	    map.add( lex.Id2String( lex.GetId( ) ) );

	    Next( );
	    while( scannerSym == Token.T_COMMA ){
		Next( );
		MustBe( scannerSym, Token.T_IDENT, "missing identifier");
		map.add( lex.Id2String( lex.GetId( ) ) );
		Next( );
	    }
	}
	MustBe( scannerSym, Token.T_RPAREN, "missing closing ) for function");
	
	Next( );
	
	if( scannerSym == Token.T_IDENT ){
	    //what is this ident for???
	    Next( ); 
	}
	
    }
    
    private FuncDetails funcDecl( SymbolTable symtab ) {

	FuncDetails fdetail = new FuncDetails( );

	Next( );
	
	MustBe( scannerSym, Token.T_IDENT, "missing function name" );

	String fname = lex.Id2String( lex.GetId( ) );

	fdetail.setname( fname );

	SymbolType ftype = new SymbolType( SymbolType.FuncType );
	
	Next( );

	SymbolTable lst = new SymbolTable( );
	lst.AddtoSymbolTable( "FPBASE", 
			      new SymbolType( SymbolType.VarType ) );
	SSAValue fbase = new SSAValue( "FPBASE", Kind.S_IDENT );
	lst.PutValue( "FPBASE",  fbase );
	fbase.SetReg( 27 );

	ArrayList<String> map = new ArrayList<String>();

	if( scannerSym == Token.T_LPAREN ) { //we have formal parameters 
	    FormalParams( map ); //dont think we will have to call Next below.
	                     //because we will check for the [ ident ] identifier
	                     //which is optional
	    
	    for( int i = 0; i < map.size(); i++ ){
		
		String param = map.get( i );
		SymbolType type = new SymbolType( SymbolType.VarType );
		lst.AddtoSymbolTable( param, type);
		SSAValue idvalue = new SSAValue( param, Kind.S_IDENT ); 
		lst.PutValue( param, idvalue );
		ftype.AddFormalParam( type );
		fdetail.AddFParamValue( idvalue );
	    }
	}

	MustBe( scannerSym, Token.T_SCOLON, "missing semi-colon after formal paramters" );
	
	Next( );

	if( scannerSym == Token.T_VAR || scannerSym == Token.T_ARRAY ){
	
	    //handle variable declarations
	    while( scannerSym == Token.T_VAR || scannerSym == Token.T_ARRAY ){
		varDecl( lst );
	    }	  
  
	    //now we have seen all variables defined in this function.
	    //we must now update the formal paramter information in the
	    //symbol type.
	    /*
	      for( int i = 0; i < map.size(); i++ ){
		String param = map.get( i );
		SymbolType type = lst.GetSymType( param );
		if( type == null ){
		    //this is a semantic error
		    throw new Error( " " ); //change to FatalSemanticError
		}
		else{
		    ftype.AddFormalParam( type );
		}
		}
	    */
	}

	System.out.println( lst.toString( ) );

        BasicBlock bodyNode = funcBody( lst );

	//System.out.println( bodyNode.toString( 0 ) );

	symtab.AddtoSymbolTable( fname, ftype );

	Next( );
	
	MustBe( scannerSym, Token.T_SCOLON, "missing semi-colon after function body" );

	fdetail.settype( ftype );
	fdetail.setnode( bodyNode );
	fdetail.setsymtab( lst );
	fdetail.setfpmap( map );

	Next( ); 

	return fdetail;
		
    }
    
    private void HandleArrayDim( SymbolType type  ){
	
	MustBe( scannerSym, Token.T_LSQUARE, "missing [" );
	Next( );
	
	MustBe( scannerSym, Token.T_NUMBER, "only constants as dimensions" );

	type.AddDimension( lex.GetVal( ) ); 

	Next( );

	MustBe( scannerSym, Token.T_RSQUARE, "missing ]" );
	Next( );
	
	return;
    }
    
    private SymbolType typeDecl( ){
	
	SymbolType type = null;

	if( scannerSym == Token.T_VAR ){
	    //this is an integer variable
	    type = new SymbolType( SymbolType.VarType );
	    Next( );
	}
	else if( scannerSym == Token.T_ARRAY ){

	    type = new SymbolType( SymbolType.ArrayType );
	    Next( );
	    
	    HandleArrayDim( type ); //we need atleast one dimension

	    while( scannerSym == Token.T_LSQUARE ){
		HandleArrayDim( type );
	    }
	}

	return type;

    }

    private void varDecl( SymbolTable symtab ) {
	
	//dont call next here. we handle based on scannerSym
	SymbolType type = typeDecl( );

	MustBe( scannerSym, Token.T_IDENT, "missing identifier" );

	String idName = lex.Id2String( lex.GetId( ) );
	
	symtab.AddtoSymbolTable( idName, type);
	
	SSAValue idvalue = new SSAValue( idName, Kind.S_IDENT ); 
	
	if( type.GetType( ) == SymbolType.VarType ){
	    idvalue.setundefined( );
	}

	symtab.PutValue( idName, idvalue );
	
	Next( );

	while( scannerSym == Token.T_COMMA ){
	    Next();
	    MustBe( scannerSym, Token.T_IDENT, "missing identifier" );
	    idName = lex.Id2String( lex.GetId( ) );
	    symtab.AddtoSymbolTable( idName ,
				     type );
	    idvalue = new SSAValue( idName, Kind.S_IDENT ); 
	    symtab.PutValue( idName, idvalue );
	    Next();
	}

	MustBe( scannerSym, Token.T_SCOLON, "missing semicolon" );
	Next( ); //added to handle main { } case. 
    }
    
    private void InsertPhi( BasicBlock join_b, int branch_no, 
			    SSAValue new_value, SSAValue bk_value ){

	String name = new_value.GetName( );
	//System.out.println( "name is " + name );
	if( join_b.ContainsPhi( name ) == false ){
	    
	    ArrayList<SSAValue> phi_ops = new ArrayList<SSAValue>(2);
	    phi_ops.add( bk_value );
	    phi_ops.add( bk_value );
	    
	    SSAValue phi_value = new SSAValue( name, Kind.S_IDENT ); //should a new subscript be generated here?

	    //SymbolTable symtab = join_b.GetSymbolTable( );
	    //int subscript = symtab.IncSSASubscript( name );
	    //phi_value.SetSSASubscript(  );

	    Instruction phi_instr = new Instruction( phi_value,
						     phi_ops,
						     bk_value );
	    join_b.AddPhi( name, phi_instr );
	    
	    if( join_b.IsLoopHeader( ) ){
		//do the renaming thing
		//we have to rename all occurences of bk_value
		
		//since its a loop header, we are guaranteed that the first
		//Next() entry is the loop-body, the follow block is added only 
		//later
		//BasicBlock loop_body = join_b.GetNext( 0 );
		int value_number = join_b.GetInstruction( 0 ).GetValNumber( ); //first non-phi
		Iterator<Instruction> it = bk_value.use_iterator( );

		//System.out.println("Backup value is" + bk_value.toString( ) );

		while( it.hasNext( ) ){
		    Instruction instr = it.next( );
		    //for all instructions greater in value than value_number,
		    //replace operand bk_value by phi_value.
		    //System.out.println("Use of " + bk_value.toString() + " in " +
		    //instr.toString( ) );
		    if( instr.GetValNumber( ) >= value_number ){
			//System.out.println( "Replacing " + instr.toString( ) );
			ArrayList<SSAValue> operands = instr.Operands( );
			for( int i = 0; i < operands.size( ); i++ ){
			    if( operands.get(i) == bk_value ){
				operands.set(i, phi_value);
			
			    }
			}
		    
		    }
		}  
	    }
	    //System.out.println("recorded use " + bk_value.toString( )
	    //	       + " " + phi_instr.toString( ) );
	    bk_value.AddUse( phi_instr );
	    bk_value.AddUse( phi_instr );
	}
	
	join_b.ReplacePhiOperand( name, branch_no, new_value );
    }

    private void CommitPhi( BasicBlock b, BasicBlock outer_block, 
			    int outer_br_no ){
	b.InitPhiIterator( );

	SymbolTable symtab = b.GetSymbolTable( );

	while( b.HasMorePhi( ) ){
	   		
	    String phi_name = b.NextPhiName( );
	    Instruction phi_instr = b.GetPhiInstr( phi_name );

	    SSAValue phi_value = phi_instr.GetPhiValue( );
	    String name = phi_value.GetName( );
	    int subscript = symtab.IncSSASubscript( name );
	    phi_value.SetSSASubscript( subscript );
	    symtab.PutValue( name, phi_value );

	    SSAValue backup_value = phi_instr.GetBackupValue( );
	    
	    if( outer_block != null ){
		InsertPhi( outer_block, outer_br_no, phi_value, backup_value);
	    }
	}
    }
	
}
