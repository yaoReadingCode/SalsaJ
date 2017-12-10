//EU_HOU
package ij.macro;
import ij.*;
import ij.process.*;
import ij.plugin.frame.Recorder;
import ij.util.Tools;

import java.util.*;
import java.awt.event.KeyEvent;

/**
 *  This is the recursive descent parser/interpreter for the ImageJ macro
 *  language.
 *
 * @author     Thomas
 * @created    23 novembre 2007
 */
public class Interpreter implements MacroConstants {

	final static int STACK_SIZE = 1000;
	final static int MAX_ARGS = 20;

	int pc;
	int token;
	int tokenAddress;
	double tokenValue;
	String tokenString;
	boolean looseSyntax = true;
	int lineNumber;
	boolean ignoreEOL = true;
	boolean statusUpdated;
	boolean showingProgress;
	boolean keysSet;
	boolean checkingType;
	int prefixValue;

	Variable[] stack;
	int topOfStack = -1;
	int topOfGlobals = -1;
	int startOfLocals = 0;

	static Interpreter instance, previousInstance;
	static boolean batchMode;
	static Vector imageTable;// images opened in batch mode
	boolean done;
	Program pgm;
	Functions func;
	boolean inFunction;
	String macroName;
	String argument;
	String returnValue;
	boolean calledMacro;// macros envoked by eval() or runMacro()
	double[] rgbWeights;
	boolean inPrint;
	static String additionalFunctions;


	/**
	 *  Interprets the specified string.
	 *
	 * @param  macro  Description of the Parameter
	 */
	public void run(String macro) {
		System.out.println("macro.run macro=" + macro);
		if (additionalFunctions != null) {
			if (!(macro.endsWith("\n") || additionalFunctions.startsWith("\n"))) {
				macro = macro + "\n" + additionalFunctions;
			} else {
				macro = macro + additionalFunctions;
			}
		}
		Tokenizer tok = new Tokenizer();
		Program pgm = tok.tokenize(macro);

		if (pgm.hasVars) {
			saveGlobals2(pgm);
		}
		run(pgm);
	}


	/**
	 *  Runs the specified macro, passing it a string argument and returning a
	 *  string value.
	 *
	 * @param  macro  Description of the Parameter
	 * @param  arg    Description of the Parameter
	 * @return        Description of the Return Value
	 */
	public String run(String macro, String arg) {
		argument = arg;
		calledMacro = true;
		if (IJ.getInstance() == null) {
			setBatchMode(true);
		}
		run(macro);
		return returnValue;
	}


	/**
	 *  Interprets the specified tokenized macro file starting at location 0.
	 *
	 * @param  pgm  Description of the Parameter
	 */
	public void run(Program pgm) {
		this.pgm = pgm;
		pc = -1;
		instance = this;
		if (!calledMacro) {
			batchMode = false;
			imageTable = null;
		}
		pushGlobals();
		if (func == null) {
			func = new Functions(this, pgm);
		}
		func.plotWindow = null;
		//IJ.showStatus("interpreting");
		while (pgm.code[pc + 1] == EOL) {
			pc++;
		}// skip comments
		if ((pgm.code[pc + 1] & 0xff) == MACRO && (pgm.code[pc + 2] & 0xff) == STRING_CONSTANT) {
			// run macro instead of skipping over it
			getToken();
			getToken();
		}
		doStatements();
		finishUp();
	}


	/**
	 *  Interprets the specified tokenized macro starting at the specified
	 *  location.
	 *
	 * @param  pgm        Description of the Parameter
	 * @param  macroLoc   Description of the Parameter
	 * @param  macroName  Description of the Parameter
	 */
	public void runMacro(Program pgm, int macroLoc, String macroName) {
		this.pgm = pgm;
		this.macroName = macroName;
		pc = macroLoc - 1;
		previousInstance = instance;
		instance = this;
		batchMode = false;
		imageTable = null;
		//IJ.showStatus("interpreting");
		pushGlobals();
		if (func == null) {
			func = new Functions(this, pgm);
		}
		func.plotWindow = null;
		if (macroLoc == 0) {
			doStatements();
		} else {
			doBlock();
		}
		finishUp();
		Recorder.recordInMacros = false;
	}


	/**
	 *  Saves global variablesk.
	 *
	 * @param  pgm  Description of the Parameter
	 */
	public void saveGlobals(Program pgm) {
		saveGlobals2(pgm);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  pgm  Description of the Parameter
	 */
	void saveGlobals2(Program pgm) {
		this.pgm = pgm;
		pc = -1;
		instance = this;
		func = new Functions(this, pgm);
		while (!done) {
			getToken();
			switch (token) {
							case VAR:
								doVar();
								break;
							case MACRO:
								skipMacro();
								break;
							case FUNCTION:
								skipFunction();
								break;
							default:
			}
		}
		instance = null;
		pgm.saveGlobals(this);
		pc = -1;
		topOfStack = -1;
		done = false;
	}


	/**
	 *  Gets the token attribute of the Interpreter object
	 */
	final void getToken() {
		if (done) {
			return;
		}
		token = pgm.code[++pc];
		//IJ.log(pc+" "+pgm.decodeToken(token));
		if (token <= 127) {
			return;
		}
		while (token == EOL && ignoreEOL) {
			token = pgm.code[++pc];
		}
		tokenAddress = token >> TOK_SHIFT;
		token = token & TOK_MASK;

		Symbol sym = pgm.table[tokenAddress];

		tokenString = sym.str;
		tokenValue = sym.value;
		done = token == EOF;
	}


	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Return Value
	 */
	final int nextToken() {
		return pgm.code[pc + 1] & TOK_MASK;
	}


	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Return Value
	 */
	final int nextNonEolToken() {
		int tok;
		int i = 1;

		do {
			tok = pgm.code[pc + i];
			i++;
		} while (tok == EOL);
		return tok & TOK_MASK;
	}


	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Return Value
	 */
	final int nextNextNonEolToken() {
		int tok;
		int i = 1;

		do {
			tok = pgm.code[pc + (i++)];
		} while (tok == EOL);
		do {
			tok = pgm.code[pc + (i++)];
		} while (tok == EOL);
		return tok & TOK_MASK;
	}


	/**
	 *  Description of the Method
	 */
	final void putTokenBack() {
		pc--;
		if (pc < 0) {
			pc = -1;
		}
	}


	/**
	 *  Description of the Method
	 */
	void doStatements() {
		while (!done) {
			doStatement();
		}
	}


	/**
	 *  Description of the Method
	 */
	final void doStatement() {
		getToken();
		switch (token) {
						case VAR:
							doVar();
							break;
						case PREDEFINED_FUNCTION:
							func.doFunction(pgm.table[tokenAddress].type);
							break;
						case USER_FUNCTION:
							runUserFunction();
							break;
						case RETURN:
							doReturn();
							break;
						case WORD:
							doAssignment();
							break;
						case IF:
							doIf();
							return;
						case ELSE:
							error("Else without if");
							return;
						case FOR:
							doFor();
							return;
						case WHILE:
							doWhile();
							return;
						case DO:
							doDo();
							return;
						case MACRO:
							skipMacro();
							return;
						case FUNCTION:
							skipFunction();
							return;
						case ';':
							return;
						case '{':
							putTokenBack();
							doBlock();
							return;
						case NUMBER:
						case NUMERIC_FUNCTION:
						case STRING_FUNCTION:
						case STRING_CONSTANT:
						case '(':
							putTokenBack();
							inPrint = true;

							String s = getString();

							inPrint = false;
							if (s != null && !"NaN".equals(s)) {
								IJ.log(s);
							}
							return;
						case EOF:
							break;
						default:
							error("Statement cannot begin with '" + pgm.decodeToken(token, tokenAddress) + "'");
		}
		if (!looseSyntax) {
			getToken();
			if (token != ';' && !done) {
				error("';' expected");
			}
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Return Value
	 */
	Variable runUserFunction() {
		int newPC = (int) tokenValue;
		int saveStartOfLocals = startOfLocals;

		startOfLocals = topOfStack + 1;

		int saveTOS = topOfStack;
		int nArgs = pushArgs();
		int savePC = pc;
		Variable value = null;

		pc = newPC;
		setupArgs(nArgs);

		boolean saveInFunction = inFunction;

		inFunction = true;
		try {
			doBlock();
		} catch (ReturnException e) {
			value = new Variable(0, e.value, e.str, e.array);
		}
		inFunction = saveInFunction;
		pc = savePC;
		trimStack(saveTOS, saveStartOfLocals);
		return value;
	}


	/**
	 *  Push function arguments onto the stack.
	 *
	 * @return    Description of the Return Value
	 */
	int pushArgs() {
		getLeftParen();

		int count = 0;
		Variable[] args = new Variable[MAX_ARGS];
		double value;

		if (nextToken() != ')') {
			do {
				if (count == MAX_ARGS) {
					error("Too many arguments");
				}
				int next = nextToken();
				int nextPlus = pgm.code[pc + 2] & 0xff;

				if (next == STRING_CONSTANT || next == STRING_FUNCTION) {
					args[count] = new Variable(0, 0.0, getString());
				} else if (next == USER_FUNCTION) {
					int savePC = pc;

					getToken();// the function

					boolean simpleFunctionCall = isSimpleFunctionCall(false);

					pc = savePC;
					if (simpleFunctionCall) {
						getToken();// the function

						Variable v2 = runUserFunction();

						if (v2 == null) {
							error("No return value");
						}
						args[count] = v2;
					} else {
						args[count] = new Variable(0, getExpression(), null);
					}
				} else if (next == WORD && (nextPlus == ',' || nextPlus == ')')) {
					value = 0.0;

					Variable[] array = null;
					String str = null;

					getToken();

					Variable v = lookupVariable();

					if (v != null) {
						int type = v.getType();

                        switch (type) {
                            case Variable.VALUE:
                                value = v.getValue();
                                break;
                            case Variable.ARRAY:
                                array = v.getArray();
                                break;
                            default:
                                str = v.getString();
                                break;
                        }
					}
					args[count] = new Variable(0, value, str, array);
				} else if (next == WORD && nextPlus == '[') {
					getToken();

					Variable v = lookupVariable();

					v = getArrayElement(v);
					if (v.getString() != null) {
						args[count] = new Variable(0, 0.0, v.getString(), null);
					} else {
						args[count] = new Variable(0, v.getValue(), null);
					}
				} else {
					args[count] = new Variable(0, getExpression(), null);
				}
				count++;
				getToken();
			} while (token == ',');
			putTokenBack();
		}
		int nArgs = count;

		while (count > 0) {
			push(args[--count], this);
		}
		getRightParen();
		return nArgs;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  nArgs  Description of the Parameter
	 */
	void setupArgs(int nArgs) {
		getLeftParen();

		int i = topOfStack;
		int count = nArgs;

		if (nextToken() != ')') {
			do {
				getToken();
				if (i >= 0) {
					stack[i].symTabIndex = tokenAddress;
				}
				i--;
				count--;
				getToken();
			} while (token == ',');
			putTokenBack();
		}
		if (count != 0) {
			error(nArgs + " argument" + (nArgs == 1 ? "" : "s") + " expected");
		}
		getRightParen();
	}

	// cache exception object for better performance
	ReturnException returnException;

	// Handle return statement
	/**
	 *  Description of the Method
	 */
	void doReturn() {
		double value = 0.0;
		String str = null;
		Variable[] array = null;

		getToken();
		if (token != ';') {
			boolean isString = token == STRING_CONSTANT || token == STRING_FUNCTION;
			boolean isArrayFunction = token == ARRAY_FUNCTION;

			if (token == WORD) {
				Variable v = lookupLocalVariable(tokenAddress);

				if (v != null && nextToken() == ';') {
					array = v.getArray();
					isString = v.getString() != null;
					//IJ.log("token==WORD: "+isString+" "+pgm.decodeToken(token, tokenAddress));
				}
			}
			putTokenBack();
			if (isString) {
				str = getString();
			} else if (isArrayFunction) {
				getToken();
				array = func.getArrayFunction(pgm.table[tokenAddress].type);
			} else if (array == null) {
				value = getExpression();
			}
		}
		if (inFunction) {
			if (returnException == null) {
				returnException = new ReturnException();
			}
			returnException.value = value;
			returnException.str = str;
			returnException.array = array;
			//throw new ReturnException(value, str, array);
			throw returnException;
		} else {
			finishUp();
			if (value != 0.0 || array != null) {
				error("Macros can only return strings");
			}
			returnValue = str;
			done = true;
		}
	}


	/**
	 *  Description of the Method
	 */
	void doFor() {
		boolean saveLooseSyntax = looseSyntax;

		looseSyntax = false;
		getToken();
		if (token != '(') {
			error("'(' expected");
		}
		getToken();// skip 'var'
		if (token != VAR) {
			putTokenBack();
		}
		do {
			if (nextToken() != ';') {
				getAssignmentExpression();
			}
			getToken();
		} while (token == ',');
		//IJ.log("token: "+pgm.decodeToken(token,tokenAddress));
		if (token != ';') {
			error("';' expected");
		}
		int condPC = pc;
		int incPC2;
		int startPC = 0;
		double cond = 1;

		while (true) {
			if (pgm.code[pc + 1] != ';') {
				cond = getLogicalExpression();
			}
			if (startPC == 0) {
				checkBoolean(cond);
			}
			getToken();
			if (token != ';') {
				error("';' expected");
			}
			int incPC = pc;
			// skip to start of code
			if (startPC != 0) {
				pc = startPC;
			} else {
				while (token != ')') {
					getToken();
					//IJ.log(pgm.decodeToken(token,tokenAddress));
					if (token == '{' || token == ';' || token == '(' || done) {
						error("')' expected");
					}
				}
			}
			startPC = pc;
			if (cond == 1) {
				doStatement();
			} else {
				skipStatement();
				break;
			}
			pc = incPC;// do increment
			do {
				if (nextToken() != ')') {
					getAssignmentExpression();
				}
				getToken();
			} while (token == ',');
			pc = condPC;
		}
		looseSyntax = saveLooseSyntax;
	}


	/**
	 *  Description of the Method
	 */
	void doWhile() {
		looseSyntax = false;

		int savePC = pc;
		boolean isTrue;

		do {
			pc = savePC;
			isTrue = getBoolean();
			if (isTrue) {
				doStatement();
			} else {
				skipStatement();
			}
		} while (isTrue && !done);
	}


	/**
	 *  Description of the Method
	 */
	void doDo() {
		looseSyntax = false;

		int savePC = pc;
		boolean isTrue;

		do {
			doStatement();
			getToken();
			if (token != WHILE) {
				error("'while' expected");
			}
			isTrue = getBoolean();
			if (isTrue) {
				pc = savePC;
			}
		} while (isTrue && !done);
	}


	/**
	 *  Description of the Method
	 */
	final void doBlock() {
		getToken();
		if (token != '{') {
			error("'{' expected");
		}
		while (!done) {
			getToken();
			if (token == '}') {
				break;
			}
			putTokenBack();
			doStatement();
		}
		if (token != '}') {
			error("'}' expected");
		}
	}


	/**
	 *  Description of the Method
	 */
	final void skipStatement() {
		getToken();
		//IJ.write("skipStatement: " +pgm.decodeToken(token, tokenAddress));
		switch (token) {
						case PREDEFINED_FUNCTION:
						case USER_FUNCTION:
						case VAR:
						case WORD:
						case '(':
						case PLUS_PLUS:
						case RETURN:
						case NUMERIC_FUNCTION:
						case STRING_FUNCTION:
							skipSimpleStatement();
							break;
						case IF:
							skipParens();
							skipStatement();
							getToken();
							if (token == ELSE) {
								skipStatement();
							} else {
								putTokenBack();
							}
							break;
						case FOR:
							skipParens();
							skipStatement();
							break;
						case WHILE:
							skipParens();
							skipStatement();
							break;
						case DO:
							skipStatement();
							getToken();// skip 'while'
							skipParens();
							break;
						case ';':
							break;
						case '{':
							putTokenBack();
							skipBlock();
							break;
						default:
							error("Skipped statement cannot begin with '" + pgm.decodeToken(token, tokenAddress) + "'");
		}
	}


	/**
	 *  Description of the Method
	 */
	final void skipBlock() {
		int count = 0;

		do {
			getToken();
			if (token == '{') {
				count++;
			} else if (token == '}') {
				count--;
			} else if (done) {
				error("'}' expected");
				return;
			}
		} while (count > 0);
	}


	/**
	 *  Description of the Method
	 */
	final void skipParens() {
		int count = 0;

		do {
			getToken();
			if (token == '(') {
				count++;
			} else if (token == ')') {
				count--;
			} else if (done) {
				error("')' expected");
				return;
			}
		} while (count > 0);
	}


	/**
	 *  Description of the Method
	 */
	final void skipSimpleStatement() {
		boolean finished = done;

		getToken();
		while (!finished && !done) {
            switch (token) {
                case ';':
                    finished = true;
                    break;
                case ELSE:
                case PREDEFINED_FUNCTION:
                    error("';' expected");
                    break;
                default:
                    getToken();
                    break;
            }
		}
	}


	/**
	 *  Skips a user-defined function.
	 */
	void skipFunction() {
		getToken();// skip function id
		skipParens();
		skipBlock();
	}


	/**
	 *  Description of the Method
	 */
	void skipMacro() {
		getToken();// skip macro label
		skipBlock();
	}


	/**
	 *  Description of the Method
	 */
	final void doAssignment() {
		int next = pgm.code[pc + 1] & 0xff;

		if (next == '[') {
			doArrayElementAssignment();
			return;
		}
		int type = getExpressionType();

		switch (type) {
						case Variable.STRING:
							doStringAssignment();
							break;
						case Variable.ARRAY:
							doArrayAssignment();
							break;
						case USER_FUNCTION:
							doUserFunctionAssignment();
							break;
						case STRING_FUNCTION:
							doNumericStringAssignment();
							break;
						default:
							putTokenBack();
							getAssignmentExpression();
		}
	}


	/**
	 *  Gets the expressionType attribute of the Interpreter object
	 *
	 * @return    The expressionType value
	 */
	int getExpressionType() {
		int rightSideToken = pgm.code[pc + 2];
		int tok = rightSideToken & 0xff;

		if (tok == STRING_CONSTANT) {
			return Variable.STRING;
		}
		if (tok == STRING_FUNCTION) {
			int address = rightSideToken >> TOK_SHIFT;

			if (pgm.table[address].type == DIALOG) {
				int token2 = pgm.code[pc + 4];
				String name = pgm.table[token2 >> TOK_SHIFT].str;

				if ("getNumber".equals(name) || "getCheckbox".equals(name)) {
					return STRING_FUNCTION;
				}
			}
			return Variable.STRING;
		}
		if (tok == ARRAY_FUNCTION) {
			return Variable.ARRAY;
		}
		if (tok == USER_FUNCTION) {
			return USER_FUNCTION;
		}
		if (tok != WORD) {
			return Variable.VALUE;
		}
		Variable v = lookupVariable(rightSideToken >> TOK_SHIFT);

		if (v == null) {
			return Variable.VALUE;
		}
		int type = v.getType();

		if (type != Variable.ARRAY) {
			return type;
		}
		if (pgm.code[pc + 3] == '.') {
			return Variable.VALUE;
		}
		if (pgm.code[pc + 3] != '[') {
			return Variable.ARRAY;
		}
		int savePC = pc;

		getToken();//"="
		getToken();//the variable
		checkingType = true;

		int index = getIndex();

		checkingType = false;
		pc = savePC - 1;
		getToken();

		Variable[] array = v.getArray();

		if (index < 0 || index >= array.length) {
			return Variable.VALUE;
		}
		return array[index].getType();
	}


	/**
	 *  Handles string functions such as Dialog.getNumber() that return a number.
	 */
	final void doNumericStringAssignment() {
		putTokenBack();
		getToken();

		Variable v = lookupLocalVariable(tokenAddress);

		if (v == null) {
			v = push(tokenAddress, 0.0, null, this);
		}
		getToken();
		if (token != '=') {
			error("'=' expected");
		}
		v.setValue(getExpression());
	}


	/**
	 *  Description of the Method
	 */
	final void doArrayElementAssignment() {
		Variable v = lookupLocalVariable(tokenAddress);

		if (v == null) {
			error("Undefined identifier");
		}
		if (pgm.code[pc + 5] == ';' && (pgm.code[pc + 4] == PLUS_PLUS || pgm.code[pc + 4] == MINUS_MINUS)) {
			putTokenBack();
			getFactor();
			return;
		}
		int index = getIndex();
		int expressionType = getExpressionType();

		if (expressionType == Variable.ARRAY) {
			error("Arrays of arrays not supported");
		}
		getToken();

		int op = token;

		if (!(op == '=' || op == PLUS_EQUAL || op == MINUS_EQUAL || op == MUL_EQUAL || op == DIV_EQUAL)) {
			error("'=', '+=', '-=', '*=' or '/=' expected");
			return;
		}
		if (op != '=' && (expressionType == Variable.STRING || expressionType == Variable.ARRAY)) {
			error("'=' expected");
			return;
		}
		Variable[] array = v.getArray();

		if (array == null) {
			error("Array expected");
		}
		if (index < 0 || index >= array.length) {
			error("Index (" + index + ") out of 0-" + (array.length - 1) + " range");
		}
		int next = nextToken();

		switch (expressionType) {
						case Variable.STRING:
							array[index].setString(getString());
							break;
						case Variable.ARRAY:
							getToken();
							if (token == ARRAY_FUNCTION) {
								array[index].setArray(func.getArrayFunction(pgm.table[tokenAddress].type));
							}
							break;
						default:
							switch (op) {
											case '=':
												array[index].setValue(getExpression());
												break;
											case PLUS_EQUAL:
												array[index].setValue(array[index].getValue() + getExpression());
												break;
											case MINUS_EQUAL:
												array[index].setValue(array[index].getValue() - getExpression());
												break;
											case MUL_EQUAL:
												array[index].setValue(array[index].getValue() * getExpression());
												break;
											case DIV_EQUAL:
												array[index].setValue(array[index].getValue() / getExpression());
												break;
							}
							break;
		}
	}


	/**
	 *  Description of the Method
	 */
	final void doUserFunctionAssignment() {
		//IJ.log("doUserFunctionAssignment0: "+pgm.decodeToken(token, tokenAddress));
		putTokenBack();

		int savePC = pc;

		getToken();// the variable
		getToken();// '='
		getToken();// the function

		boolean simpleAssignment = isSimpleFunctionCall(true);

		pc = savePC;
		if (!simpleAssignment) {
			getAssignmentExpression();
		} else {
			getToken();

			Variable v1 = lookupLocalVariable(tokenAddress);

			if (v1 == null) {
				v1 = push(tokenAddress, 0.0, null, this);
			}
			getToken();
			if (token != '=') {
				error("'=' expected");
			}
			getToken();// the function

			Variable v2 = runUserFunction();

			if (v2 == null) {
				error("No return value");
			}
			int type = v2.getType();

            switch (type) {
                case Variable.VALUE:
                    v1.setValue(v2.getValue());
                    break;
                case Variable.ARRAY:
                    v1.setArray(v2.getArray());
                    break;
                default:
                    v1.setString(v2.getString());
                    break;
            }
		}
	}


	/**
	 *  Gets the simpleFunctionCall attribute of the Interpreter object
	 *
	 * @param  assignment  Description of the Parameter
	 * @return             The simpleFunctionCall value
	 */
	boolean isSimpleFunctionCall(boolean assignment) {
		int count = 0;

		do {
			getToken();
			//IJ.log(pgm.decodeToken(token, tokenAddress));
			if (token == '(') {
				count++;
			} else if (token == ')') {
				count--;
			} else if (done) {
				error("')' expected");
			}
		} while (count > 0);
		getToken();
		if (assignment) {
			return token == ';';
		} else {
			return token == ',' || token == ')';
		}
	}


	/**
	 *  Description of the Method
	 */
	final void doStringAssignment() {
		Variable v = lookupLocalVariable(tokenAddress);

		if (v == null) {
			if (nextToken() == '=') {
				v = push(tokenAddress, 0.0, null, this);
			} else {
				error("Undefined identifier");
			}
		}
		getToken();
		if (token != '=') {
			error("'=' expected");
			return;
		}
		v.setString(getString());
	}


	/**
	 *  Description of the Method
	 */
	final void doArrayAssignment() {
		Variable v = lookupLocalVariable(tokenAddress);

		if (v == null) {
			if (nextToken() == '=') {
				v = push(tokenAddress, 0.0, null, this);
			} else {
				error("Undefined identifier");
			}
		}
		getToken();
		if (token != '=') {
			error("'=' expected");
			return;
		}
		getToken();
        switch (token) {
            case ARRAY_FUNCTION:
                v.setArray(func.getArrayFunction(pgm.table[tokenAddress].type));
                break;
            case WORD:
                Variable v2 = lookupVariable();

                v.setArray(v2.getArray());
                break;
            default:
                error("Array expected");
                break;
        }
	}


	/**
	 *  Description of the Method
	 */
	final void doIf() {
		looseSyntax = false;

		boolean b = getBoolean();

		if (b) {
			doStatement();
		} else {
			skipStatement();
		}

		int next = nextNonEolToken();

		if (next == ';') {
			getToken();
			next = nextNonEolToken();
		}
		if (next == ELSE) {
			getToken();
			if (b) {
				skipStatement();
			} else {
				doStatement();
			}
		}
	}


	/**
	 *  Gets the boolean attribute of the Interpreter object
	 *
	 * @return    The boolean value
	 */
	final boolean getBoolean() {
		getLeftParen();

		double value = getLogicalExpression();

		checkBoolean(value);
		getRightParen();
		return !(value == 0.0);
	}


	/**
	 *  Gets the logicalExpression attribute of the Interpreter object
	 *
	 * @return    The logicalExpression value
	 */
	final double getLogicalExpression() {
		double v1 = getBooleanExpression();
		int next = nextNonEolToken();

		if (!(next == LOGICAL_AND || next == LOGICAL_OR)) {
			return v1;
		}
		checkBoolean(v1);
		getToken();

		int op = token;
		double v2 = getLogicalExpression();

		checkBoolean(v2);
		if (op == LOGICAL_AND) {
			return (int) v1 & (int) v2;
		} else if (op == LOGICAL_OR) {
			return (int) v1 | (int) v2;
		}
		return v1;
	}


	/**
	 *  Gets the booleanExpression attribute of the Interpreter object
	 *
	 * @return    The booleanExpression value
	 */
	final double getBooleanExpression() {
		double v1 = 0.0;
		String s1 = null;
		int next = pgm.code[pc + 1];
		int tok = next & TOK_MASK;

		if (tok == STRING_CONSTANT || tok == STRING_FUNCTION || isString(next)) {
			s1 = getString();
		} else {
			v1 = getExpression();
		}
		next = nextToken();
		if (next >= EQ && next <= LTE) {
			getToken();

			int op = token;

			if (s1 != null) {
				return compareStrings(s1, getString(), op);
			}
			double v2 = getExpression();

			switch (op) {
							case EQ:
								v1 = v1 == v2 ? 1.0 : 0.0;
								break;
							case NEQ:
								v1 = v1 != v2 ? 1.0 : 0.0;
								break;
							case GT:
								v1 = v1 > v2 ? 1.0 : 0.0;
								break;
							case GTE:
								v1 = v1 >= v2 ? 1.0 : 0.0;
								break;
							case LT:
								v1 = v1 < v2 ? 1.0 : 0.0;
								break;
							case LTE:
								v1 = v1 <= v2 ? 1.0 : 0.0;
								break;
			}
		} else if (s1 != null) {
			v1 = Tools.parseDouble(s1, 0.0);
		}
		return v1;
	}

	// returns true if the specified token is a string variable
	/**
	 *  Gets the string attribute of the Interpreter object
	 *
	 * @param  token  Description of the Parameter
	 * @return        The string value
	 */
	boolean isString(int token) {
		if ((token & TOK_MASK) != WORD) {
			return false;
		}
		Variable v = lookupVariable(token >> TOK_SHIFT);

		if (v == null) {
			return false;
		}
		if (pgm.code[pc + 2] == '[') {
			Variable[] array = v.getArray();

			if (array != null && array.length > 0) {
				return array[0].getType() == Variable.STRING;
			}
		}
		return v.getType() == Variable.STRING;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  s1  Description of the Parameter
	 * @param  s2  Description of the Parameter
	 * @param  op  Description of the Parameter
	 * @return     Description of the Return Value
	 */
	double compareStrings(String s1, String s2, int op) {
		int result;

		result = s1.compareToIgnoreCase(s2);

		double v1 = 0.0;

		switch (op) {
						case EQ:
							v1 = result == 0 ? 1.0 : 0.0;
							break;
						case NEQ:
							v1 = result != 0 ? 1.0 : 0.0;
							break;
						case GT:
							v1 = result > 0 ? 1.0 : 0.0;
							break;
						case GTE:
							v1 = result >= 0 ? 1.0 : 0.0;
							break;
						case LT:
							v1 = result < 0 ? 1.0 : 0.0;
							break;
						case LTE:
							v1 = result <= 0 ? 1.0 : 0.0;
							break;
		}
		return v1;
	}


	/**
	 *  Gets the assignmentExpression attribute of the Interpreter object
	 *
	 * @return    The assignmentExpression value
	 */
	final double getAssignmentExpression() {
		int tokPlus2 = pgm.code[pc + 2];

		if ((pgm.code[pc + 1] & 0xff) == WORD && (tokPlus2 == '=' || tokPlus2 == PLUS_EQUAL
				 || tokPlus2 == MINUS_EQUAL || tokPlus2 == MUL_EQUAL || tokPlus2 == DIV_EQUAL)) {
			getToken();

			Variable v = lookupLocalVariable(tokenAddress);

			if (v == null) {
				v = push(tokenAddress, 0.0, null, this);
			}
			getToken();

			double value = 0.0;

			if (token == '=') {
				value = getAssignmentExpression();
			} else {
				value = v.getValue();
				switch (token) {
								case PLUS_EQUAL:
									value += getAssignmentExpression();
									break;
								case MINUS_EQUAL:
									value -= getAssignmentExpression();
									break;
								case MUL_EQUAL:
									value *= getAssignmentExpression();
									break;
								case DIV_EQUAL:
									value /= getAssignmentExpression();
									break;
				}
			}
			v.setValue(value);
			return value;
		} else {
			return getLogicalExpression();
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  value  Description of the Parameter
	 */
	final void checkBoolean(double value) {
		if (!(value == 0.0 || value == 1.0)) {
			error("Boolean expression expected");
		}
	}


	/**
	 *  Description of the Method
	 */
	void doVar() {
		getToken();
		while (token == WORD) {
			if (nextToken() == '=') {
				doAssignment();
			} else {
				Variable v = lookupVariable(tokenAddress);

				if (v == null) {
					push(tokenAddress, 0.0, null, this);
				}
			}
			getToken();
			if (token == ',') {
				getToken();
			} else {
				putTokenBack();
				break;
			}
		}
	}


	/**
	 *  Gets the leftParen attribute of the Interpreter object
	 */
	final void getLeftParen() {
		getToken();
		if (token != '(') {
			error("'(' expected");
		}
	}


	/**
	 *  Gets the rightParen attribute of the Interpreter object
	 */
	final void getRightParen() {
		getToken();
		if (token != ')') {
			error("')' expected");
		}
	}


	/**
	 *  Gets the parens attribute of the Interpreter object
	 */
	final void getParens() {
		if (nextToken() == '(') {
			getLeftParen();
			getRightParen();
		}
	}


	/**
	 *  Gets the comma attribute of the Interpreter object
	 */
	final void getComma() {
		getToken();
		if (token != ',') {
			if (looseSyntax) {
				putTokenBack();
			} else {
				error("',' expected");
			}
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  message  Description of the Parameter
	 */
	void error(String message) {
		boolean showMessage = !done;

		token = EOF;
		tokenString = "";
		IJ.showStatus("");
		IJ.showProgress(0, 0);
		batchMode = false;
		imageTable = null;
		WindowManager.setTempCurrentImage(null);
		if (showMessage) {
			String line = getErrorLine();

			if (line.length() > 120) {
				line = line.substring(0, 119) + "...";
			}
			IJ.showMessage("Macro Error", message + " in line " + lineNumber + ".\n \n" + line);
			throw new RuntimeException(Macro.MACRO_CANCELED);
		}
		done = true;
	}


	/**
	 *  Gets the errorLine attribute of the Interpreter object
	 *
	 * @return    The errorLine value
	 */
	String getErrorLine() {
		int savePC = pc;

		lineNumber = 1;
		ignoreEOL = false;
		pc = -1;

		int lineStart = -1;

		while (pc < savePC) {
			getToken();
			if (token == EOL) {
				lineNumber++;
				lineStart = pc;
			}
		}

		String line = "";

		pc = lineStart;
		getToken();

		String str;
		double v;

		while (token != EOL && !done) {
			str = pgm.decodeToken(token, tokenAddress);
			if (pc == savePC) {
				str = "<" + str + ">";
			}
			line += str + " ";
			getToken();
		}
		return line;
	}


	/**
	 *  Gets the string attribute of the Interpreter object
	 *
	 * @return    The string value
	 */
	final String getString() {
		String str = getStringTerm();

		while (true) {
			getToken();
			if (token == '+') {
				str += getStringTerm();
			} else {
				putTokenBack();
				break;
			}
		}
        return str;
	}


	/**
	 *  Gets the stringTerm attribute of the Interpreter object
	 *
	 * @return    The stringTerm value
	 */
	final String getStringTerm() {
		String str;

		getToken();
		switch (token) {
						case STRING_CONSTANT:
							str = tokenString;
							break;
						case STRING_FUNCTION:
							str = func.getStringFunction(pgm.table[tokenAddress].type);
							break;
						case USER_FUNCTION:
							Variable v = runUserFunction();

							if (v == null) {
								error("No return value");
							}
							str = v.getString();
							if (str == null) {
								double value = v.getValue();

								if ((int) value == value) {
									str = IJ.d2s(value, 0);
								} else {
									str = "" + value;
								}
							}
							break;
						case WORD:
							str = lookupStringVariable();
							if (str != null) {
								break;
							}
						// else fall through
						default:
							putTokenBack();

							double value = getStringExpression();

							if ((int) value == value) {
								str = IJ.d2s(value, 0);
							} else {
								str = "" + value;
								if (inPrint && value != Double.POSITIVE_INFINITY && value != Double.NEGATIVE_INFINITY
										 && value != Double.NaN && (str.length() - str.indexOf('.')) > 6 && str.indexOf('E') == -1) {
									str = IJ.d2s(value, 4);
								}
							}
		}
		return str;
	}


	/**
	 *  Gets the stringFunction attribute of the Interpreter object
	 *
	 * @return    The stringFunction value
	 */
	final boolean isStringFunction() {
		Symbol symbol = pgm.table[tokenAddress];

		return symbol.type == D2S;
	}


	/**
	 *  Gets the expression attribute of the Interpreter object
	 *
	 * @return    The expression value
	 */
	final double getExpression() {
		double value = getTerm();
		int next;

        label:
        while (true) {
            next = nextNonEolToken();
            switch (next) {
                case '+':
                    getToken();
                    value += getTerm();
                    break;
                case '-':
                    getToken();
                    value -= getTerm();
                    break;
                default:
                    break label;
            }
        }
		return value;
	}


	/**
	 *  Gets the term attribute of the Interpreter object
	 *
	 * @return    The term value
	 */
	final double getTerm() {
		double value = getFactor();
		boolean done = false;
		int next;

		while (!done) {
			next = nextToken();
			switch (next) {
							case '*':
								getToken();
								value *= getFactor();
								break;
							case '/':
								getToken();
								value /= getFactor();
								break;
							case '%':
								getToken();
								value %= getFactor();
								break;
							case '&':
								getToken();
								value = (int) value & (int) getFactor();
								break;
							case '|':
								getToken();
								value = (int) value | (int) getFactor();
								break;
							case '^':
								getToken();
								value = (int) value ^ (int) getFactor();
								break;
							case SHIFT_RIGHT:
								getToken();
								value = (int) value >> (int) getFactor();
								break;
							case SHIFT_LEFT:
								getToken();
								value = (int) value << (int) getFactor();
								break;
							default:
								done = true;
								break;
			}
		}
		return value;
	}


	/**
	 *  Gets the factor attribute of the Interpreter object
	 *
	 * @return    The factor value
	 */
	final double getFactor() {
		double value = 0.0;
		Variable v = null;

		getToken();
		switch (token) {
						case NUMBER:
							value = tokenValue;
							break;
						case NUMERIC_FUNCTION:
							value = func.getFunctionValue(pgm.table[tokenAddress].type);
							break;
						case STRING_FUNCTION:
							String str = func.getStringFunction(pgm.table[tokenAddress].type);

							value = Tools.parseDouble(str);
							if (Double.isNaN(value)) {
								error("Numeric value expected");
							}
							break;
						case USER_FUNCTION:
							v = runUserFunction();
							if (v == null) {
								error("No return value");
							}
							if (v.getString() != null) {
								error("Numeric return value expected");
							} else {
								value = v.getValue();
							}
							break;
						case TRUE:
							value = 1.0;
							break;
						case FALSE:
							value = 0.0;
							break;
						case PI:
							value = Math.PI;
							break;
						case NaN:
							value = Double.NaN;
							break;
						case WORD:
							v = lookupVariable();
							if (v == null) {
								return 0.0;
							}
							int next = nextToken();

                            switch (next) {
                                case '[':
                                    v = getArrayElement(v);
                                    value = v.getValue();
                                    next = nextToken();
                                    break;
                                case '.':
                                    value = getArrayLength(v);
                                    next = nextToken();
                                    break;
                                default:
                                    if (prefixValue != 0 && !checkingType) {
                                        v.setValue(v.getValue() + prefixValue);
                                        prefixValue = 0;
                                    }
                                    value = v.getValue();
                                    break;
                            }
							if (!(next == PLUS_PLUS || next == MINUS_MINUS)) {
								break;
							}
							getToken();
							if (token == PLUS_PLUS) {
								v.setValue(v.getValue() + (checkingType ? 0 : 1));
							} else {
								v.setValue(v.getValue() - (checkingType ? 0 : 1));
							}
							break;
						case (int) '(':
							value = getLogicalExpression();
							getRightParen();
							break;
						case PLUS_PLUS:
							prefixValue = 1;
							value = getFactor();
							break;
						case MINUS_MINUS:
							prefixValue = -1;
							value = getFactor();
							break;
						case '!':
							value = getFactor();
							if (value == 0.0 || value == 1.0) {
								value = value == 0.0 ? 1.0 : 0.0;
							} else {
								error("Boolean expected");
							}
							break;
						case '-':
							value = -getFactor();
							break;
						case '~':
							value = ~(int) getFactor();
							break;
						default:
							error("Number or numeric function expected");
		}
		// IJ.log("getFactor: "+value+" "+pgm.decodeToken(preToken,0));
		return value;
	}


	/**
	 *  Gets the arrayElement attribute of the Interpreter object
	 *
	 * @param  v  Description of the Parameter
	 * @return    The arrayElement value
	 */
	final Variable getArrayElement(Variable v) {
		int index = getIndex();
		Variable[] array = v.getArray();

		if (array == null) {
			error("Array expected");
		}
		if (index < 0 || index >= array.length) {
			error("Index (" + index + ") out of 0-" + (array.length - 1) + " range");
		}
		return array[index];
	}


	/**
	 *  Gets the arrayLength attribute of the Interpreter object
	 *
	 * @param  v  Description of the Parameter
	 * @return    The arrayLength value
	 */
	final double getArrayLength(Variable v) {
		getToken();// '.'
		getToken();
		if (!(token == WORD && "length".equals(tokenString))) {
			error("'length' expected");
		}
		Variable[] array = v.getArray();

		if (array == null) {
			error("Array expected");
		}
		return array.length;
	}


	/**
	 *  Gets the stringExpression attribute of the Interpreter object
	 *
	 * @return    The stringExpression value
	 */
	final double getStringExpression() {
		double value = getTerm();

        label:
        while (true) {
            getToken();
            switch (token) {
                case '+':
                    getToken();
                    if (token == STRING_CONSTANT || token == STRING_FUNCTION) {
                        putTokenBack();
                        putTokenBack();
                        break label;
                    }
                    if (token == WORD) {
                        Variable v = lookupVariable(tokenAddress);

                        if (v != null && v.getString() != null) {
                            putTokenBack();
                            putTokenBack();
                            break label;
                        }
                    }
                    putTokenBack();
                    value += getTerm();
                    break;
                case '-':
                    value -= getTerm();
                    break;
                default:
                    putTokenBack();
                    break label;
            }
        }
        return value;
	}


	/**
	 *  Searches the local and global sections of the stack for. the specified
	 *  variable. Returns null if it is not found.
	 *
	 * @param  symTabAddress  Description of the Parameter
	 * @return                Description of the Return Value
	 */
	final Variable lookupLocalVariable(int symTabAddress) {
		//IJ.log("lookupLocalVariable: "+topOfStack+" "+startOfLocals+" "+topOfGlobals);
		Variable v = null;

		for (int i = topOfStack; i >= startOfLocals; i--) {
			if (stack[i].symTabIndex == symTabAddress) {
				v = stack[i];
				break;
			}
		}
		if (v == null) {
			for (int i = topOfGlobals; i >= 0; i--) {
				if (stack[i].symTabIndex == symTabAddress) {
					v = stack[i];
					break;
				}
			}
		}
		return v;
	}


	/**
	 *  Searches the entire stack for the specified variable. Returns null if it is
	 *  not found.
	 *
	 * @param  symTabAddress  Description of the Parameter
	 * @return                Description of the Return Value
	 */
	final Variable lookupVariable(int symTabAddress) {
		Variable v = null;

		for (int i = topOfStack; i >= 0; i--) {
			if (stack[i].symTabIndex == symTabAddress) {
				v = stack[i];
				break;
			}
		}
		return v;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  var     Description of the Parameter
	 * @param  interp  Description of the Parameter
	 * @return         Description of the Return Value
	 */
	Variable push(Variable var, Interpreter interp) {
		if (stack == null) {
			stack = new Variable[STACK_SIZE];
		}
		if (topOfStack >= (STACK_SIZE - 2)) {
			interp.error("Stack overflow");
		} else {
			topOfStack++;
		}
		stack[topOfStack] = var;
		return var;
	}


	/**
	 *  Description of the Method
	 */
	void pushGlobals() {
		if (pgm.globals == null) {
			return;
		}
		if (stack == null) {
			stack = new Variable[STACK_SIZE];
		}
		for (int i = 0; i < pgm.globals.length; i++) {
			topOfStack++;
			stack[topOfStack] = pgm.globals[i];
		}
		topOfGlobals = topOfStack;
	}


	/**
	 *  Creates a Variable and pushes it onto the stack.
	 *
	 * @param  symTabLoc  Description of the Parameter
	 * @param  value      Description of the Parameter
	 * @param  str        Description of the Parameter
	 * @param  interp     Description of the Parameter
	 * @return            Description of the Return Value
	 */
	Variable push(int symTabLoc, double value, String str, Interpreter interp) {
		Variable var = new Variable(symTabLoc, value, str);

		if (stack == null) {
			stack = new Variable[STACK_SIZE];
		}
		if (topOfStack >= (STACK_SIZE - 2)) {
			interp.error("Stack overflow");
		} else {
			topOfStack++;
		}
		stack[topOfStack] = var;
		return var;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  previousTOS            Description of the Parameter
	 * @param  previousStartOfLocals  Description of the Parameter
	 */
	void trimStack(int previousTOS, int previousStartOfLocals) {
		for (int i = previousTOS + 1; i <= topOfStack; i++) {
			stack[i] = null;
		}
		topOfStack = previousTOS;
		startOfLocals = previousStartOfLocals;
		//IJ.log("trimStack: "+topOfStack);
	}


	/**
	 *  Searches the entire stack for the variable associated with the current
	 *  token. Aborts the macro if it is not found.
	 *
	 * @return    Description of the Return Value
	 */
	final Variable lookupVariable() {
		Variable v = null;

		if (stack == null) {
			undefined();
			return v;
		}
		boolean found = false;

		for (int i = topOfStack; i >= 0; i--) {
			v = stack[i];
			//IJ.log(I+"  "+v+"  "+v.symTabIndex+"  "+tokenAddress);
			if (v.symTabIndex == tokenAddress) {
				found = true;
				break;
			}
		}
		if (!found) {
			undefined();
		}
		return v;
	}


	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Return Value
	 */
	final String lookupStringVariable() {
		if (stack == null) {
			undefined();
			return "";
		}
		boolean found = false;
		String str = null;

		for (int i = topOfStack; i >= 0; i--) {
			if (stack[i].symTabIndex == tokenAddress) {
				Variable v = stack[i];

				found = true;

				int next = nextToken();

                switch (next) {
                    case '[':
                        int savePC = pc;
                        int index = getIndex();
                        Variable[] array = v.getArray();

                        if (array == null) {
                            error("Array expected");
                        }
                        if (index < 0 || index >= array.length) {
                            error("Index (" + index + ") out of 0-" + (array.length - 1) + " range");
                        }
                        str = array[index].getString();
                        if (str == null) {
                            pc = savePC - 1;
                            getToken();
                        }
                        break;
                    case '.':
                        str = null;
                        break;
                    default:
                        if (v.getArray() != null) {
                            getToken();
                            error("'[' or '.' expected");
                        }
                        str = v.getString();
                        break;
                }
				break;
			}
		}
		if (!found) {
			undefined();
		}
		return str;
	}


	/**
	 *  Gets the index attribute of the Interpreter object
	 *
	 * @return    The index value
	 */
	int getIndex() {
		getToken();
		if (token != '[') {
			error("'['expected");
		}
		int index = (int) getExpression();

		getToken();
		if (token != ']') {
			error("']' expected");
		}
		return index;
	}


	/**
	 *  Description of the Method
	 */
	void undefined() {
		if (nextToken() == '(') {
			error("Undefined identifier");
		} else {
			error("Undefined variable");
		}
	}


	/**
	 *  Description of the Method
	 */
	void dump() {
		getParens();
		if (!done) {
			pgm.dumpSymbolTable();
			pgm.dumpProgram();
			dumpStack();
		}
	}


	/**
	 *  Description of the Method
	 */
	void dumpStack() {
		IJ.log("");
		IJ.log("Stack");
		if (stack != null) {
			for (int i = topOfStack; i >= 0; i--) {
				IJ.log(i + " " + pgm.table[stack[i].symTabIndex].str + " " + stack[i]);
			}
		}
	}


	/**
	 *  Description of the Method
	 */
	void finishUp() {
		func.updateDisplay();
		if (func.plotWindow != null) {
			func.plotWindow.draw();
		}
		instance = null;
		if (!calledMacro) {
			if (batchMode) {
				showingProgress = true;
			}
			batchMode = false;
			imageTable = null;
			WindowManager.setTempCurrentImage(null);
		}
		if (showingProgress) {
			IJ.showProgress(0, 0);
		}
		if (keysSet) {
			IJ.setKeyUp(KeyEvent.VK_ALT);
			IJ.setKeyUp(KeyEvent.VK_SHIFT);
			IJ.setKeyUp(KeyEvent.VK_SPACE);
		}
		if (rgbWeights != null) {
			ColorProcessor.setWeightingFactors(rgbWeights[0], rgbWeights[1], rgbWeights[2]);
		}
		if (func.writer != null) {
			func.writer.close();
		}
	}


	/**
	 *  Aborts currently running macro.
	 */
	public static void abort() {
		abort2(instance);
	}


	/**
	 *  Aborts the macro that was running when this one started.
	 */
	static void abortPrevious() {
		if (previousInstance != null) {
			abort2(previousInstance);
			IJ.beep();
			previousInstance = null;
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  interpreter  Description of the Parameter
	 */
	private static void abort2(Interpreter interpreter) {
		if (interpreter != null) {
			if (!interpreter.calledMacro) {
				batchMode = false;
				imageTable = null;
			}
			interpreter.done = true;
			IJ.showStatus("Macro aborted");
		}
	}


	/**
	 *  Gets the instance attribute of the Interpreter class
	 *
	 * @return    The instance value
	 */
	public static Interpreter getInstance() {
		return instance;
	}


	/**
	 *  Sets the batchMode attribute of the Interpreter class
	 *
	 * @param  b  The new batchMode value
	 */
	static void setBatchMode(boolean b) {
		batchMode = b;
		if (!b) {
			imageTable = null;
		}
	}


	/**
	 *  Gets the batchMode attribute of the Interpreter class
	 *
	 * @return    The batchMode value
	 */
	public static boolean isBatchMode() {
		return batchMode;
	}


	/**
	 *  Adds a feature to the BatchModeImage attribute of the Interpreter class
	 *
	 * @param  imp  The feature to be added to the BatchModeImage attribute
	 */
	public static void addBatchModeImage(ImagePlus imp) {
		if (!batchMode || imp == null) {
			return;
		}
		if (imageTable == null) {
			imageTable = new Vector();
		}
		//IJ.log("add: "+imp+"  "+imageTable.size());
		imageTable.addElement(imp);
	}


	/**
	 *  Description of the Method
	 *
	 * @param  imp  Description of the Parameter
	 */
	public static void removeBatchModeImage(ImagePlus imp) {
		if (imageTable != null && imp != null) {
			int index = imageTable.indexOf(imp);
			//IJ.log("remove: "+imp+"  "+imageTable.size());
			if (index != -1) {
				imageTable.removeElementAt(index);
			}
		}
	}


	/**
	 *  Gets the batchModeImageIDs attribute of the Interpreter class
	 *
	 * @return    The batchModeImageIDs value
	 */
	public static int[] getBatchModeImageIDs() {
		if (!batchMode || imageTable == null) {
			return new int[0];
		}
		int n = imageTable.size();
		int[] imageIDs = new int[n];

		for (int i = 0; i < n; i++) {
			ImagePlus imp = (ImagePlus) imageTable.elementAt(i);

			imageIDs[i] = imp.getID();
		}
		return imageIDs;
	}


	/**
	 *  Gets the batchModeImageCount attribute of the Interpreter class
	 *
	 * @return    The batchModeImageCount value
	 */
	public static int getBatchModeImageCount() {
		if (!batchMode || imageTable == null) {
			return 0;
		} else {
			return imageTable.size();
		}
	}


	/**
	 *  Gets the batchModeImage attribute of the Interpreter class
	 *
	 * @param  id  Description of the Parameter
	 * @return     The batchModeImage value
	 */
	public static ImagePlus getBatchModeImage(int id) {
		if (!batchMode || imageTable == null) {
			return null;
		}
		for (Enumeration en = Interpreter.imageTable.elements(); en.hasMoreElements(); ) {
			ImagePlus imp = (ImagePlus) en.nextElement();

			if (id == imp.getID()) {
				return imp;
			}
		}
		return null;
	}


	/**
	 *  Gets the lastBatchModeImage attribute of the Interpreter class
	 *
	 * @return    The lastBatchModeImage value
	 */
	public static ImagePlus getLastBatchModeImage() {
		if (!batchMode || imageTable == null) {
			return null;
		}
		int size = imageTable.size();

		if (size == 0) {
			return null;
		}
		return (ImagePlus) imageTable.elementAt(size - 1);
	}


	/**
	 *  The specified string, if not null, is added to strings passed to the run()
	 *  method.
	 *
	 * @param  functions  The new additionalFunctions value
	 */
	public static void setAdditionalFunctions(String functions) {
		additionalFunctions = functions;
	}


	/**
	 *  Gets the additionalFunctions attribute of the Interpreter class
	 *
	 * @return    The additionalFunctions value
	 */
	public static String getAdditionalFunctions() {
		return additionalFunctions;
	}

}// class Interpreter


