import java.util.Hashtable;

public class Token {
    public static final int     T_ERROR =  0;
    public static final int     T_MULT = 1 ;
    public static final int	T_DIV = 2;
    public static final int	T_PLUS = 11;
    public static final int	T_MINUS = 12;
    public static final int	T_EQ = 20;
    public static final int	T_NEQ = 21;
    public static final int	T_LESS = 22;
    public static final int	T_GEQ = 23;
    public static final int	T_LEQ = 24;
    public static final int	T_GREAT = 25;
    public static final int	T_DOT = 30;
    public static final int	T_COMMA = 31;
    public static final int	T_RPAREN = 35;
    public static final int	T_BECOMES = 40;
    public static final int	T_THEN = 41;
    public static final int	T_DO = 42;
    public static final int	T_LPAREN = 50;
    public static final int	T_NUMBER = 60;
    public static final int	T_IDENT = 61;
    public static final int	T_SCOLON = 70;
    public static final int	T_RCURL = 80;
    public static final int	T_OD = 81;
    public static final int	T_FI = 82;
    public static final int	T_ELSE = 90;
    public static final int	T_LET = 100;
    public static final int	T_CALL = 101;
    public static final int	T_IF = 102;
    public static final int	T_WHILE = 103;
    public static final int	T_RETURN = 104;
    public static final int	T_VAR = 110;
    public static final int	T_ARRAY = 111;
    public static final int	T_FUNCTION = 112;
    public static final int	T_PROCEDURE = 113;
    public static final int	T_LCURL = 150;
    public static final int	T_MAIN = 200;
    public static final int     T_LSQUARE = 51;
    public static final int     T_RSQUARE = 83;
    public static final int	T_EOF = 255;
    	
}