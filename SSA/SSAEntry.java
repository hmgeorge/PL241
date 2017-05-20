//each entry in SSA is represented by this class.

public class SSAEntry{
    
    private int operation; //the operations, +,-,*,/,>,< etc, call and ret
    private ArrayList<Operand> rvals; //can be number or variable.
    private Operand lval; 
                                
    /* 
       e.g. 
       1) call( x, y, x - y + a * b )
          t1 = x1 - y1;
	  t2 = a1 * b1;
	  t3 = t1 + t2
	  SSA - call( x1, y1, t3 );
	  
       2) if( (a+b/c) > (a-b ) )
          t1 = b1 / c1;
	  t2 = a1 + t1;
	  t3 = a1 - b1;
	  SSA - t2 > t3 ;

       3) let a[3][x1+y1+z1] = a[x1+y1][5] - x1 + y1 * z1
          t1 = x1 + y1;
	  t2 = t1 + z1; //we have calculated a[3][t2]
	  t3 = x1 + y1;
	  t4 = a[t3][5] - x1;
	  t5 = y1 * z1;
	  SSA - let a[3][t2] = t4 + t5;
	  
	  
       
       each operand should be an identifier (defined or temporary)
       OR a number only. also use SSA for temporaries as well.
       but no phi assigments made.

    */

    public SSAEntry( int op, Operand lval, ArrayList<Operand> values ){
	operation = op;
	rvals = values;
    }

    public toString( ){
	
	String ssa_string = "" ;

	if( lval != null ){
	    ssa_string = lval.toString( );
	}

	ssa_string += operation.toString( );
	
	for( int i = 0; i < rvals; i++ ){
	    Operand o = rvals.get( i );
	    ssa_string += " " + o.toString( );
	}
	
	return ssa_string;
    }
}