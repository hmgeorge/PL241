import java.util.ArrayList;

// class to store array access operations.

public class ArrayNode extends ExprNode {
    
    private IdentNode arrayName;
    private ArrayList<ExprNode>  accessExp; //length of this _should_ be
                                              //equal to length of dimension list of the array
                                              //in the symbol table.quick check!

    public ArrayNode( IdentNode id, ArrayList<ExprNode> accexp ){
	
	arrayName = id;
	accessExp = accexp;
    }

    public String toString(int indent){
	//dont think we will need indent here.

	String astring = arrayName.toString(0) + " ";
	for( int i = 0; i < accessExp.size( ); i++ ){
	    astring += "[ " + accessExp.get( i ).toString(0)  + " ] ";
	}

	return astring;
    }

    public int kind( ){
	return Node.ARRAY;
    }

}