//result to store number

public class NumNode extends ExprNode {

    private Integer num;
    
    public NumNode( Integer i ){
	num = i;
    }

    public String toString(int indent){
	return num.toString();
    }

    public int kind( ){
	return Node.NUMBER;
    }
}