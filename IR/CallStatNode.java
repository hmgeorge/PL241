//wrapper class around callnode because Call is both
//a statement as well as an expression.

//Node node for a function call.
public class CallStatNode extends StatNode {
    public CallNode cnode;

    public CallStatNode( CallNode cn ){
	cnode = cn;
    }

    public String toString( int indent ) {
	return cnode.toString( indent );

    }

    //all access functions should call
    //corresponding access functions of 
    //callnode

    public int kind( ){
	return Node.CALLSTAT;
    }
}