//code generator
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class CodeGen{

    private static FuncDetails function;
    private static Hashtable<String, Integer> function_address;
    
    private static BasicBlock enter_block;
    private static BasicBlock exit_block;
    private static SymbolTable symtab;

    private static ArrayList<BasicBlock> block_order;
    
    private static ArrayList<Integer> code;
    private static int PC;

    private static Hashtable<Integer,Integer> block_address;
    
    private static class Branch{
	int pc;
	int op;
	int a;
    };

    //stores for each Block, Branches that want to jump to it
    private static Hashtable<Integer,ArrayList<Branch>> fix_ups;

    private static Hashtable<Integer, String> call_names;

    private static int symtab_space = 0;
    private static int return_register, return_offset;

    private static int wordsize = 4;

    static int [ ] Do( ParseResult p ){

	PC = 0;
	code = new ArrayList<Integer>( );
	function_address = new Hashtable<String, Integer>( );
	call_names = new Hashtable<Integer, String>( );
	
	//emit code for main first because execution starts
	//at 0

	emit( 0 );

	return_register = 0;
	return_offset = 0;
	DoForFunction( p.main_fn );

	return_register = 31;
	return_offset = 1;
	Set<String> fnames = p.fdetails.keySet( );
	Iterator<String> f_iterator = fnames.iterator( );

	while( f_iterator.hasNext( ) ){
	    String fname = f_iterator.next( );
	    FuncDetails fdetail = p.fdetails.get( fname );
	    //save PC as start of this function
	    //PC*4 for JSR and RET handling in DLX
	    function_address.put( fname, PC*4 );
	    DoForFunction( fdetail );
	}

	//Do fixes for calls here
	Iterator<Integer> cit = call_names.keySet().iterator( );
	while( cit.hasNext( ) ){
	    int call_pc = cit.next( );
	    String fname = call_names.get( call_pc ); 
	    int faddress = function_address.get( fname );
	    int instr = DLX.assemble( DLX.JSR, faddress );
	    code.set( call_pc, instr ); //!
	}

	int instr = DLX.assemble( DLX.ADDI, 29, 0, ( code.size( ) + 2 )*wordsize );
	code.set( 0, instr );

	System.out.println("Emit complete");
	
	int [ ] program = new int [ code.size( ) ];
	for( int i = 0; i < code.size( ); i++ )
	    program[i] = code.get(i);
    
	return program;
	
    }
    static void DoForFunction( FuncDetails fn ){
	
	/* Order
           1) Delete phis 
	   2) Put jumps in then and while blocks
	   3) Add callee registers save/restore
	   4) Add stack allocation code for variables/arrays 
	   5) Add prologue/epilogue for functions (save ebp, ret address, push params)
	   6) Convert from high IR to low flat IR (resolve branches to actual numbers)
	      (Assign instruction format numbers also)
	   7) assemble to DLX.


	   //R0 - 0
           //R1, R2 - proxy registers
           //R3 - R13 (caller)
           //R14 - R26 (callee)
           //R27 - return value
           //R28 - frame pointer (ebp)
           //R29 - stack pointer
           //R30 - global variables base
           //R31 - return address (note, must be saved before
           //                     each function call)
	
	   Each symbol table will have a base register - (28 for most), 30
	   for globals. also allocate stack space for
	   a) arrays - store offset to the first element in symbol table entry
	   b) gident identifiers - store offset in symbol table entry
	   c) values that are assigned virtual registers.
	   d) all these are indexed as an offset (-ive or +ive) from the base
	      pointer.. 
	   e) caller saved registers

	   parameters are always accessed as an offset (+ive or -ive) from the
	   stack pointer
	   */

	function = fn;
	enter_block = fn.body;
	exit_block = enter_block.GetNext( 1 );
	symtab = enter_block.GetSymbolTable( );
	symtab_space = 0;

	block_order = Opt.TopSort( enter_block );

	GenMoves( );
	GenJumps( );

	block_order = Opt.TopSort( enter_block );

	/* stores the starting address (PC) of each block */
	block_address = new Hashtable<Integer,Integer>( );

	/*
	   stores for each block, which PCs are waiting to be fixed -
	   These PCs will be branches/jumps 
	*/
	fix_ups = new Hashtable<Integer, ArrayList<Branch>>( );
	
	for( int i = 0; i < block_order.size( ); i++ ){
	    BasicBlock b = block_order.get( i );
	    
	    block_address.put( b.GetBlockId( ) , -1 );
	    fix_ups.put( b.GetBlockId( ), new ArrayList<Branch>( ) );
	}

	EmitCalleeSaveCode( );
	EmitFunctionCode( );
	EmitCalleeRestoreCode( );

    }

    //after emit, PC equals next PC..
    private static void emit( int instr ){

	//stores an instruction into code and also increments.
	code.add( instr );
	PC++;
    }

    static void DumpCode( ){
	//System.out.println( code.size( ) );
	for( int i = 0; i < code.size( ); i++ ){
	    System.out.print(i + ": " + DLX.disassemble( code.get(i) ));
	}
    }

    private static void CallPrologue( ArrayList<SSAValue> operands ){
	
	ArrayList<Integer> caller_regs = function.GetCallerSRCodes( );
	int instr;
	int reg_a;

	//save caller registers
	for( int i = 0; i < caller_regs.size( ); i++ ){
	    int reg = caller_regs.get( i );
	    instr = DLX.assemble( DLX.PSH, reg, 29, 1*wordsize );
	    emit( instr );
	}

	//save args
	//first operand is function name, skip over it
	for( int i = 1; i < operands.size( ); i++ ){
	    SSAValue arg = operands.get( i );
	    if( arg.value_type == Kind.S_NUM ){
		instr = DLX.assemble( DLX.ADDI, 1, 0, arg.GetNum( ) );
		emit( instr );
		reg_a = 1;
	    }
	    else if( arg.IsAllocVirtual( ) ){
		instr = DLX.assemble( DLX.LDW, 1, 28, arg.GetReg( )*wordsize );
		emit( instr );
		reg_a = 1;
	    }
	    else{
		if( arg.GetReg( ) == -1 ){
		    throw new Error("Register not allocated to arg in call");
		}

		reg_a = arg.GetReg( );
	    }

	    instr = DLX.assemble( DLX.PSH, reg_a, 29, 1*wordsize );
	    emit( instr );
	}
    }

    private static void CallEpilogue( ArrayList<SSAValue> operands ){
	
	int instr;
	int reg_a;
	int argc = operands.size( ) - 1; //1 for the function name;

	ArrayList<Integer> caller_regs = function.GetCallerSRCodes( );

	//subtract that much from the stack
	instr = DLX.assemble( DLX.SUBI, 29, 29, argc*wordsize );
	emit( instr );

	//restore caller save registers
	for( int i = caller_regs.size( ) - 1 ; i >= 0; i-- ){
	    int reg = caller_regs.get( i );
	    instr = DLX.assemble( DLX.POP, reg, 29, -1*wordsize );
	    emit( instr );
	}
    }

    private static void EmitCalleeSaveCode( ){

	
	/*
	 *  pushl   %ebp
	 *  movl    %esp, %ebp
	 *  addl    <stack space>, %esp
         */

	//save return address. stack grows upwards
	int instr = DLX.assemble( DLX.PSH, return_register, 29,
				  return_offset*wordsize);
	emit( instr );

	//save old ebp;
	instr = DLX.assemble( DLX.PSH, 28, 29, 1*wordsize);
	emit( instr ); //PC after emit is the next PC.
	
       	//symtab.base_reg = ebp;

	//make current value of esp, new ebp
	instr = DLX.assemble( DLX.ADDI, 28, 29, 0);
	emit( instr );
	
	//make space for arrays and global variables -
	//keep note of the size of the space allocated. 
	Iterator<String> sym_entries = symtab.entries( );

	//global symbol table starts from ebp + 1
	
	int esp  = 0;

	ArrayList<SSAValue> fparams = function.GetFParamValues( );
	
	esp = fparams.size( );
	symtab_space = fparams.size( );

	while( sym_entries.hasNext( ) ){

	    String name = sym_entries.next( );
	    SymbolTableEntry en = symtab.GetEntry( name );
	    SymbolType t = en.GetSymType( );
	    
	    if( t.GetType( ) == SymbolType.ArrayType ){
		
		en.SetOffset( esp + 1 );

		//space to be allocated is the total size of the array
		ArrayList<Integer> dims = t.GetDims( );
		int size = 1;
		for( int i = 0; i < dims.size( ); i++ )size *= dims.get( i );
		
		esp = esp + size;
		symtab_space += size;
	    }
	    else if( t.GetType( ) == SymbolType.GlobalVarType ){
		en.SetOffset( esp + 1 );
		esp++;
		symtab_space += 1;
	    }
	}

	//allocate space for the values assigned to virtual registers
	//also. also keep note of the offset assigned to it as
	//the virtual reg number. since we have a flag saying
	//if a value is virtual, we first check that and then use
	// base + offset to access the element.
	ArrayList<SSAValue> v_regs = function.GetVRegCodes( );

	for( int i = 0; i < v_regs.size( ); i++ ){
	    SSAValue v = v_regs.get( i );
	    v.SetReg( esp++ );
	    symtab_space += 1;
	}

	//allocated space for the symbol table
	instr = DLX.assemble( DLX.ADDI, 29, 29, symtab_space*wordsize  );
	emit( instr );
	
	ArrayList<Integer> save_registers = function.GetCalleeSRCodes( );
	//push current values of callee save registers.
	//we should know size of callee save registers
	//for the restore
	for( int i = 0; i < save_registers.size( ); i++ ){
	    int reg = save_registers.get( i );
	    instr = DLX.assemble( DLX.PSH, reg, 29, 1*wordsize );
	    emit( instr );
	}

	//as i understand, stack can stop here.
	//each time a call is made first caller saved registers saved
	//then parameters saved
    }

    private static void EmitFunctionCode( ){
	//first thing is to save the argyments to the corresponding
	//registers or memory locations

	//assumption: if a formal paramter was assigned register -1,
	//it was never live, and so a move to get the corresponding
	//paramter need not be generated. ??

	int param_offset = -2; //<- confirm
	int instr;
	ArrayList<SSAValue> fparams = function.GetFParamValues( );
	for( int i = fparams.size( ) - 1 ; i >= 0; i-- ){
	    SSAValue fvalue = fparams.get( i );
	    
	    if( fvalue.GetReg( ) == -1 ){
		//assumption
	    }
	    else{
		if( fvalue.IsAllocVirtual( ) ){
		    //emit store to the assigned stack location
		    //using proxy register 1
		    instr = DLX.assemble( DLX.LDW, 1, 28, param_offset*wordsize );
		    emit( instr );
		    
		    instr = DLX.assemble( DLX.STW, 1, 28, fvalue.GetReg()*wordsize);
		    emit( instr );		    
		}
		else{
		    //all parameters _are_ assigned virtual regs
		    //emit store to the assigned register
		    instr = DLX.assemble( DLX.LDW, fvalue.GetReg( ),
					  28, param_offset*wordsize );
		    emit( instr );
		}
		param_offset--;
	    }
	}

	//now go through each instruction. 
	//take care of the call instructions
	int prev_pc = 0;

	for( int i = 0; i < block_order.size( ); i++ ){

	    //save current PC as the starting address of this block
	    BasicBlock b = block_order.get( i );
	    block_address.put( b.GetBlockId(), PC );

	    EmitBlockCode( b );

	    //System.out.println("B" + b.GetBlockId( ) );
	    //for( int j = prev_pc; j < code.size( ); j++ )
	    //System.out.print( "\t" + DLX.disassemble( code.get( j ) ) );
	    //prev_pc = code.size( );
	   
	}

	//Do the required fixups (jumps, branches ) here
	//branches need to be fix linked here
	//get the PC part in the branches and subtract 
	//it from block address.

	for( int i = 0; i < block_order.size( ); i++ ){
	       BasicBlock b = block_order.get( i );
	       ArrayList<Branch> instrs = fix_ups.get( b.GetBlockId( ) );
	       int block_pc = block_address.get( b.GetBlockId( ) );

	       //have to save PC and the operand for the fix_ups.
	       //also if abs( block_pc - fix_pc ) & ~0xFFFF != 0
	       //we need to rewrite the instruction as a jump (RET)

	       for( int j = 0; j < instrs.size( ); j++ ){
		   int fix_pc = instrs.get( j ).pc;
		   int op = instrs.get(j).op;
		   int reg_a = instrs.get(j).a;

		   //System.out.print( "Fixing: " + DLX.disassemble( code.get( fix_pc ) ) );
		   //System.out.println( "fix_pc = " + fix_pc + "block_pc = " + block_pc ); 
				       
		   int c = block_pc - fix_pc;
		   if( c < 0 ){
		       c ^= 0xFFFF0000;
		   }
		   
		   if( ( c & ~0xFFFF ) == 0 ){
		       instr = DLX.assemble( op, reg_a, block_pc - fix_pc );
		       code.set( fix_pc, instr );
		   }
		   else{
		       throw new Error("\nBranch too far away\n" +
				       "Must be replaced by RET");
		   }
	       }

	}

        //all returns only move the return value to the return register
	//and jump to the exit block. 
	//actual return instruction will be there.
	
    
    }

    private static void EmitBlockCode( BasicBlock b ){
	//iterate through each instruction in the block and emit.
	//save PC as block starting address.
	for( int i = 0; i < b.GetNumInstr( ); i++ ){
	    Instruction instr = b.GetInstruction( i );
	    if( instr.IsDeleted( ) ) continue;
	    EmitInstrCode( instr );
	}
    }

    private static void EmitInstrCode( Instruction in ){
	
	Opcode ocode = in.GetOpcode( );
	ArrayList<SSAValue> operands = in.Operands();
	SSAValue res_value = in.GetEnclosingValue( );//just name result!

	SSAValue a,b,c;
	int res, instr;
	int reg_a, reg_b, reg_c;
	Branch br;

	switch( ocode ){
	    case add: // a = b + c
		a = res_value;
		b = operands.get(0);
		c = operands.get(1);

		EmitAdd( a, b, c );
		break;
	    case sub: 
		a = res_value;
		b = operands.get(0);
		c = operands.get(1);

		EmitSub( a, b, c );
		break;
	    case mul:		
		a = res_value;
		b = operands.get(0);
		c = operands.get(1);

		EmitMul( a, b, c );
		break;
	    case div:
		a = res_value;
		b = operands.get(0);
		c = operands.get(1);
		
		EmitDiv( a, b, c );		
		break;
	    case cmp:
		a = res_value;
		b = operands.get( 0 );
		c = operands.get( 1 );
		
		EmitCmp( a, b, c );		
		break;
	    case bra:
		//jump to a start of a block
		c = operands.get( 0 ); //this is block number
		//the c in this instruction has to be fixed later
		//so keep track o
		instr = DLX.assemble( DLX.BEQ, 0, 0 );
		br = new Branch( );
		br.pc = PC;
		br.op = DLX.BEQ;
		br.a = 0;
		fix_ups.get( c.GetNum( ) ).add( br );

		emit( instr );
		break;
	    case bne: //bne a, c
		a = operands.get( 0 );
		c = operands.get( 1 );

		br = EmitControl( a, DLX.BNE );
		fix_ups.get( c.GetNum( ) ).add( br );

		break;
	    case beq:
		a = operands.get( 0 );
		c = operands.get( 1 );

		br = EmitControl( a, DLX.BEQ );
		fix_ups.get( c.GetNum( ) ).add( br );

		break;
	    case ble:
		a = operands.get( 0 );
		c = operands.get( 1 );

		br = EmitControl( a, DLX.BLE );
		fix_ups.get( c.GetNum( ) ).add( br );

		break;
	    case blt:
		a = operands.get( 0 );
		c = operands.get( 1 );

		br = EmitControl( a,  DLX.BLT );
		fix_ups.get( c.GetNum( ) ).add( br );

		break;
	    case bge:
		a = operands.get( 0 );
		c = operands.get( 1 );

		br = EmitControl( a, DLX.BGE );
		fix_ups.get( c.GetNum( ) ).add( br );

		break;
	    case bgt:
		a = operands.get( 0 );
		c = operands.get( 1 );

		br = EmitControl( a, DLX.BGT );
		fix_ups.get( c.GetNum( ) ).add( br );

		break;
	    case ret:
		
		if( operands.size( ) != 0 ){
		    //move the return value to R27
		    a = operands.get( 0 );
		    res = 0;
		    int op = DLX.ADD;

		    if( a.value_type == Kind.S_NUM ){
			res = a.GetNum( );
			reg_a = 0;
			op = DLX.ADDI;
		    }
		    else if( a.IsAllocVirtual( ) ){
			instr = DLX.assemble( DLX.LDW, 1, 28, a.GetReg()*wordsize);
			emit( instr );			
			reg_a = 1;
		    }
		    else{
			reg_a = a.GetReg( );
		    }
		    		    
		    //move the value to 27
		    instr = DLX.assemble( op, 27, reg_a, res );
		    emit( instr );
		}
			
		//jump to exit_block;
		instr = DLX.assemble( DLX.BEQ, 0, 0 );
		br = new Branch( );
		br.pc = PC;
		br.op = DLX.BLE;
		br.a = 0;
		
		fix_ups.get( exit_block.GetBlockId() ).add( br );
		emit( instr );
		break;
	    case call:
		
		//the big one
		
		//first operand is the function name.
		//index 1 to size( ) are the parameters.
		//enclosing value is the place to store
		//the result

		//the enclosing value can have a -1 reg value
		//if the value of the function is not used at all.
		//if so, then dont save the return value in 27 anywhere.

		EmitCall( operands, res_value );
		break;
	    case move: //these are inserted when the phis are eliminated
		// move b a => a = b == r.a = r.b + r.0
		b = operands.get( 0 );
		a = operands.get( 1 );
		
		if( a.IsAllocVirtual( ) ){
		    reg_a = 1;
		}
		else{
		    reg_a = a.GetReg( );
		}
		
		if( b.IsAllocVirtual( ) ){
		    //getreg is now the offset
		    instr = DLX.assemble( DLX.LDW, 1, 28, b.GetReg( )*wordsize );
		    emit( instr );
		    reg_b = 1;
		}
		else if( b.value_type == Kind.S_NUM ){
		    instr = DLX.assemble( DLX.ADDI, 1, 0, b.GetNum( ) );
		    emit( instr );
		    reg_b = 1;
		}
		else{ //b has to be assigned to a phy register; if number it will
		    //never reach that point
		    reg_b = b.GetReg( );
		}

		//System.out.println( in + " and reg_a = " + reg_a + " and reg_b = " + reg_b );
		instr = DLX.assemble( DLX.ADD, reg_a, reg_b, 0 );
				
		emit( instr );
		
		if( a.IsAllocVirtual( ) ){
		    instr = DLX.assemble( DLX.STW, reg_a, 28, a.GetReg( )*wordsize );
		    emit( instr );
		}
		break;
	    case adda:
		//you can skip? no .
		
		a = res_value; //enclosing value
		b = operands.get( 0 ); //base 
		c = operands.get( 1 ); //index

		String name = b.GetName( );
		SymbolTableEntry en = null;
		int array_base_offset = 0;

		if( symtab.IsDefined( name ) ){
		    en = symtab.GetEntry( name );
		    array_base_offset = en.GetOffset( );
		}
		//else if( symtab.parent != null && 
		//symtab.parent.IsDefined( name ) ){
		//en = symtab.parent.GetEntry( name );
		//array_base_offset = en.GetOffset( );
		//}
		else{
		    throw new Error("undefined array came in store emit");
		}
		
		if( a.IsAllocVirtual( ) ){
		    reg_a  = 1;
		}
		else{
		    reg_a = a.GetReg( );
		}

		if( c.value_type == Kind.S_NUM ){
		    int address = array_base_offset*wordsize + c.GetNum( );
		    instr = DLX.assemble( DLX.ADDI, reg_a,
					  0, address );
		    emit( instr );
		}
		else{

		    if( c.IsAllocVirtual( ) ){
			
			instr = DLX.assemble( DLX.LDW, 1, 28, c.GetReg( )*wordsize );
			emit( instr );
			
			instr = DLX.assemble( DLX.ADDI, reg_a, 1, 
					      array_base_offset*wordsize );
			emit( instr );
			
		    }
		    else{
			instr = DLX.assemble( DLX.ADDI, reg_a, c.GetReg( ),
					      array_base_offset*wordsize );
			emit( instr );
		    }
		    
		}

		if( a.IsAllocVirtual( ) ){

		    instr = DLX.assemble( DLX.STW, reg_a, a.GetReg( )*wordsize );
		    emit( instr );
		}
		break;
	    case load:
		//note, the indices are being multiplied by 4.
		//this might cause problems.
		
		//assumption: the first (and only ) operand of 
		//load is the result of adda that has the base and the index.
		a = res_value;
		SSAValue adda_value = operands.get( 0 ); //this points t
		EmitLoad( a, adda_value );
		break;
	    case store:
		a = operands.get( 0 );
		adda_value = operands.get( 1 );
		EmitStore( a, adda_value );		
		break;
	    case read:
		//read has no operands, but may have a res_value;
		a = res_value;
		
		if( a.GetReg( ) != - 1){
		    if( a.IsAllocVirtual( ) ) {
			reg_a = 1;
		    }
		    else{
			reg_a = a.GetReg( );
		    }
		    
		    instr = DLX.assemble( DLX.RDI, reg_a );
		    emit( instr );
		
		    if( a.IsAllocVirtual( ) ){
			instr = DLX.assemble( DLX.STW, 1, 28, a.GetReg( )*wordsize);
			emit( instr );
		    }
		}
		else{
		    //this value is not used?
		    instr = DLX.assemble( DLX.RDI, 1 );
		    emit( instr );
		}
		break;
	    case write:

		b = operands.get( 0 ); //this is the value to write
		
		if( b.value_type == Kind.S_NUM ){
		    instr = DLX.assemble( DLX.ADDI, 1, 0, b.GetNum( ));
		    emit(instr);
		    reg_b = 1;
		}
		else if( b.IsAllocVirtual( ) ){
		    instr = DLX.assemble( DLX.LDW, 1, 28, b.GetReg( )*wordsize );
		    emit( instr );
		    reg_b = 1;
		}
		else{
		    if( b.GetReg( ) == -1 ){
			throw new Error("No register assigned to operand"+
					"of write");
		    }
		    reg_b = b.GetReg( );
		}
		
		instr = DLX.assemble( DLX.WRD, reg_b );
		emit( instr );
		break;
	    case wln:

		instr = DLX.assemble( DLX.WRL );
		emit( instr );

		break;
	    default:
		throw new Error("Emit error:Invalid opcode in instruction " + in);
	}
    }

    private static void 
    EmitAdd( SSAValue a, SSAValue b, SSAValue c){

	int reg_a;
	int res;
	int instr;
	
	if( a.GetReg( ) == -1 ){
	    throw new Error("No register allocated to result of add");
	}

	if( b.value_type == Kind.S_NUM &&
	    c.value_type == Kind.S_NUM ){
	 	    
	    if( a.IsAllocVirtual( ) ){
		reg_a = 1;
	    }
	    else{
		reg_a = a.GetReg( );
	    }
	    
	    res = b.GetNum( ) + c.GetNum( );
	    instr = DLX.assemble( DLX.ADDI, reg_a, 0, res ); 
	    emit( instr );
	    
	    if( a.IsAllocVirtual( ) ){
		instr = DLX.assemble( DLX.STW, reg_a, 28, a.GetReg( )*wordsize );
		emit( instr );
	    }
	}
	else{
	    EmitArith( a, b, c, DLX.ADD, DLX.ADDI );
	}
/*	
	int reg_a, reg_b, reg_c;
	int instr;
	int res;

	if( a.GetReg( ) == -1 ){
	    throw new Error("No register allocated to result of add");
	}
		
	if( a.IsAllocVirtual( ) ){
	    reg_a = 1;
	}
	else{
	    reg_a = a.GetReg( );
	}
	
	if( b.IsAllocVirtual( ) ){
	    //getreg is now the offset
	    instr = DLX.assemble( DLX.LDW, 1, 28, b.GetReg( ) );
	    emit( instr );
	    reg_b = 1;
	}
	else{ //b has to be assigned to a phy register; if number it will
	    //never reach that point
	    reg_b = b.GetReg( );
	}
	
	if( c.IsAllocVirtual( ) ){
	    instr = DLX.assemble( DLX.LDW, 2, 28, c.GetReg( ) );
	    emit( instr );
	    reg_c = 2;
	}
	else{ //c has to be assigned to a phy register
	    reg_c = c.GetReg( );
	}
	
	if( b.value_type == Kind.S_NUM &&
	    c.value_type == Kind.S_NUM ){
	    
	    res = b.GetNum( ) + c.GetNum( );
	    instr = DLX.assemble( DLX.ADDI, a.GetReg( ), res, 0 ); 
	}
	else if( b.value_type == Kind.S_NUM ){
	    
	    instr = DLX.assemble( DLX.ADDI, 1, 0, b.GetNum( ) );
	    emit( instr );
	    
	    reg_b = 1;
	    instr = DLX.assemble( DLX.ADD, a.GetReg( ), reg_b, reg_c );
	}
	else if ( c.value_type == Kind.S_NUM ){
	    
	    instr = DLX.assemble( DLX.ADDI, a.GetReg( ), reg_b, c.GetNum() );
	}
	else{
	    
	    instr = DLX.assemble( DLX.ADD, a.GetReg( ), reg_b, reg_c );
	}
	emit( instr );
	
	if( a.IsAllocVirtual( ) ){
	    instr = DLX.assemble( DLX.STW, reg_a, 28, a.GetReg( ) );
	    emit( instr );
	}
*/
    }

    private static void 
    EmitSub( SSAValue a, SSAValue b, SSAValue c){
	
	int reg_a, res;
	int instr;

	if( a.GetReg( ) == -1 ){
	    throw new Error("No register allocated to result of sub");
	}
	
	if( b.value_type == Kind.S_NUM &&
	    c.value_type == Kind.S_NUM ){
		    
	    if( a.IsAllocVirtual( ) ){
		reg_a = 1;
	    }
	    else{
		reg_a = a.GetReg( );
	    }
	    
	    res = b.GetNum( ) - c.GetNum( );
	    instr = DLX.assemble( DLX.ADDI, a.GetReg( ), 0, res ); 
	    emit( instr );
	    
	    if( a.IsAllocVirtual( ) ){
		instr = DLX.assemble( DLX.STW, reg_a, 28, a.GetReg( )*wordsize );
		emit( instr );
	    }
	}
	else{
	    EmitArith( a, b, c, DLX.SUB, DLX.SUBI );
	}
    }
    
    private static void
    EmitMul( SSAValue a, SSAValue b, SSAValue c ){
	
	int reg_a, res;
	int instr;

	if( a.GetReg( ) == -1 ){
	    throw new Error("No register allocated to result of mul");
	}

	if( b.value_type == Kind.S_NUM &&
	    c.value_type == Kind.S_NUM ){
		    
	    if( a.IsAllocVirtual( ) ){
		reg_a = 1;
	    }
	    else{
		reg_a = a.GetReg( );
	    }
	    
	    res = b.GetNum( ) * c.GetNum( );
	    instr = DLX.assemble( DLX.ADDI, a.GetReg( ), 0, res ); 
	    emit( instr );
	    
	    if( a.IsAllocVirtual( ) ){
		instr = DLX.assemble( DLX.STW, reg_a, 28, a.GetReg( )*wordsize );
		emit( instr );
	    }
	    
	}
	else{
	    EmitArith( a, b, c, DLX.MUL, DLX.MULI );
	}
    }
    private static void
    EmitDiv( SSAValue a, SSAValue b, SSAValue c ){
	
	int reg_a, res;
	int instr;

	if( c.value_type == Kind.S_NUM &&
	    c.GetNum( ) == 0 ){
	    throw new Error("Divide by zero exception in program" );
	}

	if( a.GetReg( ) == -1 ){
	    throw new Error("No register allocated to result of div");
	}
	
	if( b.value_type == Kind.S_NUM &&
	    c.value_type == Kind.S_NUM ){
	    
	    if( a.IsAllocVirtual( ) ){
		reg_a = 1;
	    }
	    else{
		reg_a = a.GetReg( );
	    }
	    
	    res = b.GetNum( ) / c.GetNum( );
	    instr = DLX.assemble( DLX.ADDI, a.GetReg( ), 0, res ); 
	    emit( instr );
	    
	    if( a.IsAllocVirtual( ) ){
		instr = DLX.assemble( DLX.STW, reg_a, 28, a.GetReg( )*wordsize );
		emit( instr );
	    }
	}
	else{
	    EmitArith( a, b, c, DLX.DIV, DLX.DIVI );
	}
    }
    
    private static void
    EmitCmp( SSAValue a, SSAValue b, SSAValue c ){
	
	int reg_a, res;
	int instr;

	if( a.GetReg( ) == -1 ){
	    throw new Error("No register allocated to result of cmp");
	}

	if( b.value_type == Kind.S_NUM &&
	    c.value_type == Kind.S_NUM ){
		    
	    if( a.IsAllocVirtual( ) ){
		reg_a = 1;
	    }
	    else{
		reg_a = a.GetReg( );
	    }
	    
	    instr = DLX.assemble( DLX.ADDI, 1, 0, b.GetNum( ) );
	    emit( instr );
	    
	    instr = DLX.assemble( DLX.ADDI, 2, 0, c.GetNum( ) );
	    emit( instr );
	    
	    instr = DLX.assemble( DLX.CMP, reg_a, 1, 2 );
	    emit( instr );

	    if( a.IsAllocVirtual( ) ){
		instr = DLX.assemble( DLX.STW, reg_a, 28, a.GetReg( )*wordsize );
		emit( instr );
	    }	    
	}
	else {
	    EmitArith( a, b, c, DLX.CMP, DLX.CMPI );
	}
    }
    
    private static void 
    EmitArith( SSAValue a, SSAValue b, SSAValue c, int op, int op_imm){
	
	int reg_a, reg_b, reg_c;
	int instr;
	int res;

	if( a.IsAllocVirtual( ) ){
	    reg_a = 1;
	}
	else{
	    reg_a = a.GetReg( );
	}
		
	if( b.IsAllocVirtual( ) ){
	    //getreg is now the offset
	    instr = DLX.assemble( DLX.LDW, 1, 28, b.GetReg( )*wordsize );
	    emit( instr );
	    reg_b = 1;
	}
	else{ //b has to be assigned to a phy register; if number it will
	    //never reach that point
	    reg_b = b.GetReg( );
	}
	
	if( c.IsAllocVirtual( ) ){
	    instr = DLX.assemble( DLX.LDW, 2, 28, c.GetReg( )*wordsize );
	    emit( instr );
	    reg_c = 2;
	}
	else{ //c has to be assigned to a phy register
	    reg_c = c.GetReg( );
	}
	
	//both b and c cannot be NUM
	if( b.value_type == Kind.S_NUM ){
	    
	    instr = DLX.assemble( op_imm, 1, 0, b.GetNum( ) );
	    emit( instr );
	    
	    reg_b = 1;
	    instr = DLX.assemble( op, reg_a, reg_b, reg_c );
	}
	else if ( c.value_type == Kind.S_NUM ){
	    
	    instr = DLX.assemble( op_imm, reg_a, reg_b, c.GetNum() );
	}
	else{
	    
	    instr = DLX.assemble( op, reg_a, reg_b, reg_c );
	}
	
	emit( instr );

	if( a.IsAllocVirtual( ) ){
	    instr = DLX.assemble( DLX.STW, reg_a, 28, a.GetReg( )*wordsize );
	    emit( instr );
	}
    }

    private static Branch
    EmitControl( SSAValue a, int op ){
	
	int instr;
	int reg_a;
	int fix_pc;
	Branch br = new Branch( );

	br.op= op;

	if( a.IsAllocVirtual( ) ){
	    instr = DLX.assemble( DLX.LDW, 1, 28, a.GetReg( )*wordsize );
	    emit( instr );
	    reg_a = 1;
	}
	else{
	    reg_a = a.GetReg( );
	}
	
	br.pc = PC;
	br.a = reg_a;
	instr = DLX.assemble( op, reg_a, 0 ); //0 is the PC to be fixed
	emit( instr );
	return br;
    }

    private static void
    EmitCall( ArrayList<SSAValue> operands, SSAValue result ){
	int instr;
	//System.out.println( "Operand = " + operands.get(0) );
	String fname = operands.get( 0 ).GetName( );
	int reg_a;
	CallPrologue( operands );

	// mark call for fix up
	call_names.put( PC, fname );

	//emit actual call instruction.
	instr = DLX.assemble( DLX.JSR, 0 ); //0 is temporary address.
	                                    //will be fixed later
	emit(instr);

 	CallEpilogue( operands );
	
	//emit save result in R27 to register allocated to result.
	if( result.GetReg( ) != - 1){
	    if( result.IsAllocVirtual( ) ){
		instr = DLX.assemble( DLX.STW, 27, 28, result.GetReg()*wordsize);
		emit( instr );
	     
	    }
	    else{
		instr = DLX.assemble( DLX.ADD, result.GetReg( ), 27, 0);
		emit( instr );
	    }
	}
	
    }

    private static void
    EmitLoad( SSAValue a, SSAValue addr_value ){
	int reg_b;
	int reg_a;
	int instr;
	int access_op;

	if( a.GetReg( ) == -1 ){
	    throw new Error("register unassigned for load result");
	}

	if( a.IsAllocVirtual( ) ){
	    reg_a = 1;
	}
	else{
	    reg_a = a.GetReg( );
	}

	if( addr_value.IsAllocVirtual( ) ){

	    instr = DLX.assemble( DLX.LDW, 2, 28, addr_value.GetReg( )*wordsize );
	    emit( instr );
	    reg_b = 2;
	}
	else{
	    reg_b = addr_value.GetReg( );
	}

	instr = DLX.assemble( DLX.LDX, reg_a, 28, reg_b );
	emit( instr );    
		
	if( a.IsAllocVirtual( ) ){
	    instr = DLX.assemble( DLX.STW, reg_a, reg_b, a.GetReg( )*wordsize );
	    emit( instr );
	}
    }
    
    private static void
    EmitStore( SSAValue a, SSAValue addr_value ){
	
	int instr;
	int reg_a, reg_b;
	int access_op;

	//a can be a number, or a phy reg or a virt reg
	if( a.value_type == Kind.S_NUM ){
	    //move it to R1
	    instr = DLX.assemble( DLX.ADDI, 1, 0, a.GetNum( ) );
	    emit( instr );
	    reg_a = 1;
	}
	else if( a.IsAllocVirtual( ) ){

	    instr = DLX.assemble( DLX.LDW, 1, 28, a.GetReg( )*wordsize );
	    emit( instr );
	    reg_a = 1;
	}
	else if( a.GetReg( ) == - 1){
	    throw new Error("no register assigned to operand for store");
	}
	else{
	    reg_a = a.GetReg( );
	}
		
	if( addr_value.IsAllocVirtual( ) ){

	    instr = DLX.assemble( DLX.LDW, 2, 28, addr_value.GetReg( )*wordsize );
	    emit( instr );
	    reg_b = 2;
	}
	else{
	    reg_b = addr_value.GetReg( );
	}
    
	instr = DLX.assemble( DLX.STX, reg_a, 28, reg_b );
	emit( instr ); 
    
    }
    

    private static void EmitCalleeRestoreCode( ){
	
	int instr;

	ArrayList<Integer> save_registers = function.GetCalleeSRCodes( );
	//push current values of callee save registers.
	//we should know size of callee save registers
	//for the restore
	
	for( int i = save_registers.size( ) - 1; i >= 0; i-- ){
	    int reg = save_registers.get( i );
	    instr = DLX.assemble( DLX.POP, reg, 29, -1*wordsize );
	    emit( instr );
	}
	
	//deallocate space for the symbol table
	//this includes space allocated for arrays, global vars and
	//virtual registers
	instr = DLX.assemble( DLX.SUBI, 29, 29, symtab_space*wordsize  );
	emit( instr );
	
	//get old ebp;
	instr = DLX.assemble( DLX.POP, 28, 29, -1*wordsize);
	emit( instr ); //PC after emit is the next PC.

	//get return address.
	instr = DLX.assemble( DLX.POP, return_register, 29, -1*return_offset*wordsize );
	emit( instr );
	
	//return to the address given in R31 or 0 for main
	instr = DLX.assemble( DLX.RET, return_register );
	emit( instr );
    }

    private static void GenJumps( ){
	
	for( int i = 0; i < block_order.size( ); i++ ){
	    BasicBlock b = block_order.get( i );
	    BasicBlock jmpBlock = b.GetJump( );

	    if( jmpBlock != null ){
		ArrayList<SSAValue> jmp_ops = new ArrayList<SSAValue>( );
		jmp_ops.add( new SSAValue( jmpBlock.GetBlockId() ));
		
		Instruction jmp_instr = new Instruction( Opcode.bra, jmp_ops );
		b.add( jmp_instr );
	    }
	}
    }

    private static void GenMoves( ){
	
	BasicBlock n;

	for( int i = 0; i < block_order.size( ) - 1 ; i++ ){
	    BasicBlock b = block_order.get( i );
	    b.InitPhiIterator( );
	    
	    while( b.HasMorePhi( ) ){
		String phi_name = b.NextPhiName( );
		Instruction phi_instr = b.GetPhiInstr( phi_name );
		System.out.println("Going to Delete");
		DeletePhi( b, phi_instr );
	    }
	    
	}

    }

    private static BasicBlock GetBlockForMove( BasicBlock b, int prev_index ){
	
	BasicBlock p = b.GetPrev( prev_index );
	BasicBlock n = null;

	if( p == null ){
	    throw new Error(" Prev Block cannot be null for valid index");
	}
	
	if( b.NumPrev( ) > 1 && p.NumNext( ) > 1 ){
	    //we have to insert a new block between p and b

	    n = new BasicBlock( p.NumNext( ), b.GetSymbolTable( ) );
	    n.AddPrev( p );
	    n.AddNext( b );
	    n.SetImmDom( p ); //i think this is correct, ImmDom might not be needed at all.
	  
	    b.SetPrev( prev_index, n );
	   
	    int next_index;

	    for( next_index = 0; next_index < p.NumNext( ); next_index++ ){
		if( p.GetNext( next_index ) == b )
		    break;
	    }
	    p.SetNext( next_index, n );	   

	    //this case only happens in if/then cases in this language.
	    //last instruction in the p block is guarateed to be a branch
	    //so take the instruction and replace the first operand
	    //with the block id of n.

	    Instruction cond_instr = p.GetInstruction(p.GetNumInstr( )-1);
	    cond_instr.ReplaceOperand(1, 
				      new SSAValue(n.GetBlockId()));
	    
	}
	else{
	    n = p;
	}

	return n;	
    }

    private static void DeletePhi( BasicBlock b, Instruction phi_instr ){ 
	SSAValue phi_value = phi_instr.GetPhiValue( );
	ArrayList<SSAValue> phi_operands = phi_instr.Operands( );

	for( int i = 0; i < phi_operands.size( ) ;i++ ){
	    SSAValue phi_op = phi_operands.get(i);
	    
	    if( phi_op == null ){
		//means, was set on a return, just ignore.
		continue;
	    }
	    else{
		AddMoveToPred( b, i, phi_op, phi_value );
	    }
	}

	phi_instr.SetDeleted( );
    }

    private static boolean AddMoveToPred( BasicBlock b, int prev_index, 
					  SSAValue src, SSAValue dest ){
		  
	BasicBlock n = GetBlockForMove( b , prev_index );
	//add moves if regs are not the same
	
	if( src.GetReg( ) != dest.GetReg( ) ){
	    //create a move
	    ArrayList<SSAValue> ops = new ArrayList<SSAValue>( );
	    ops.add( src );
	    ops.add( dest );
	    
	    Instruction mov_instr = new Instruction( Opcode.move, ops );
	    n.add( mov_instr );
	    
	    dest.AddUse( mov_instr );
	    src.AddUse( mov_instr );
	}
	else{
	    System.out.println("No move added: src.reg = " + src + 
			       " and dest.reg = " + dest );
	}
	
	return true;
    }

}