public class OpKind{
    
    int optoken;

    public OpKind( int t ){
	optoken = t;
    }

    public String toString( int i ){

	switch( optoken ){
	case Token.T_PLUS:
	    return "+";
	case Token.T_MINUS:
	    return "-";
	case Token.T_MULT:
	    return "*";
	case Token.T_DIV:
	    return "/";
	case Token.T_EQ:
	    return "==";
	case Token.T_NEQ:
	    return "!=";
	case Token.T_LESS:
	    return "<";
	case Token.T_LEQ:
	    return "<=";
	case Token.T_GREAT:
	    return ">";
	case Token.T_GEQ:
	    return ">=";
	default:
	    break;
	}

	return "Invalid";
    }
}