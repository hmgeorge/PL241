#Makefile for the compiler project

ssa = bin/BasicBlock.class bin/Dominator.class

bin/Compiler.class:	Compiler.java bin/Parser.class bin/Opt.class bin/RegAlloc.class \
			bin/CodeGen.class
	javac -cp bin/ Compiler.java -d bin/

bin/Parser.class:	bin/Lexer.class ${ssa} Parser.java bin/FuncDetails.class \
			bin/ParseResult.class 
	javac -cp bin/ Parser.java -d bin/

bin/Lexer.class:	Lexer.java bin/Token.class
	javac -cp bin/ Lexer.java -d bin/

bin/SymbolTable.class:	bin/SSAValue.class bin/SymbolType.class SymbolTable.java 
	javac -cp bin/ SymbolTable.java -d bin/

bin/SymbolType.class:	SymbolType.java
	javac -cp bin/ SymbolType.java -d bin/

bin/ParseResult.class:	ParseResult.java
	javac -cp bin/ ParseResult.java -d bin/

bin/FuncDetails.class:	bin/SymbolTable.class FuncDetails.java
	javac -cp bin/ FuncDetails.java -d bin/

bin/Token.class:	Token.java	
	javac Token.java -d bin/

bin/Kind.class:	SSA/Kind.java
	javac SSA/Kind.java -d bin/

bin/Opcode.class:	SSA/Opcode.java
	javac SSA/Opcode.java -d bin/

bin/SSAValue.class:	bin/Kind.class SSA/SSAValue.java
	javac -cp SSA/ SSA/SSAValue.java -d bin/

bin/Instruction.class: bin/Opcode.class bin/SSAValue.class SSA/Instruction.java
	javac -cp SSA/ SSA/Instruction.java -d bin/

bin/BasicBlock.class: bin/SymbolTable.class bin/Instruction.class SSA/BasicBlock.java
	javac -cp bin/ SSA/BasicBlock.java -d bin/

bin/Dominator.class: bin/BasicBlock.class SSA/Dominator.java
	javac -cp bin/ SSA/Dominator.java -d bin/

bin/Opt.class:	bin/BasicBlock.class bin/Instruction.class bin/ParseResult.class Analysis/Opt.java
	javac -cp bin/ Analysis/Opt.java -d bin/

bin/Live.class:	RegAlloc/Live.java
	javac -cp bin/ RegAlloc/Live.java -d bin/

bin/RegAlloc.class:	bin/Live.class bin/Opt.class RegAlloc/RegAlloc.java
	javac -cp bin/ RegAlloc/RegAlloc.java -d bin/

bin/CodeGen.class:	bin/Opt.class bin/DLX.class CodeGen/CodeGen.java
	javac -cp bin/ CodeGen/CodeGen.java -d bin/

bin/DLX.class:	DLX.java
	javac DLX.java -d bin/

clean:
	rm bin/*.class

#bin/TokenType.class: TokenType.java
#	javac TokenType.java -d bin/
