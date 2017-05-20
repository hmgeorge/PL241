import java.util.ArrayList;

//Node node for a function call.
public class CallNode extends ExprNode {
    public IdentNode fname;
    ArrayList<ExprNode> paramlist;

    public CallNode( IdentNode f, ArrayList<ExprNode> plist ){
	fname = f;
	paramlist = plist;
    }

    public String toString( int indent ) {
	String indentstring = IdentTabs( indent );
	
	String cstring = indentstring + "CALL " + fname.toString(0) + "( ";
	for( int i = 0; i < paramlist.size( ); i++ ){
	    ExprNode p = paramlist.get(i);
	    cstring += p.toString(0) + ", "; //yes an extra comma
	}

	cstring += ") ";
	return cstring;
    }	

    public int kind( ){
	return Node.CALLEXPR;
    }
}