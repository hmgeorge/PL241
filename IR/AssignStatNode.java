//assignment statement result
public class AssignStatNode extends StatNode{
    
    ExprNode lvalNode; //<- must be an of array type or ident type. 
    ExprNode rvalNode;

    public AssignStatNode( ExprNode lval, ExprNode rval ){
	lvalNode = lval; // lval *must* be of type array or ident..
	                   // but i thnk it will be taken care of by the parser.
	rvalNode = rval;
    }

    public String toString( int indent ){
	String indentstring = IdentTabs( indent );

	String letstring = indentstring + "LET ";
	letstring += lvalNode.toString( 0 ) + " <- " + rvalNode.toString( 0 );
	return letstring;
    }

    public int kind( ){
	return Node.ASSIGN;
    }
}