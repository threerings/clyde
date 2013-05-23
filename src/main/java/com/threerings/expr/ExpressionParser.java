//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.expr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;

import java.util.EmptyStackException;
import java.util.Map;
import java.util.Stack;

import com.google.common.collect.Maps;

/**
 * Parses simple expressions using an implementation of the
 * <a href="http://en.wikipedia.org/wiki/Shunting-yard_algorithm">shunting-yard algorithm</a>.
 */
public class ExpressionParser<T>
{
    /**
     * Main method for testing.
     */
    public static void main (String... args)
        throws Exception
    {
        // run in a loop, parsing the input and printing it out in RPN
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = reader.readLine()) != null) {
            String result = new ExpressionParser<String>(new StringReader(line)) {
                @Override protected String handleNumber (double value) {
                    return String.valueOf(value);
                }
                @Override protected String handleString (String value) {
                    return "\"" + value + "\"";
                }
                @Override protected String handleOperator (String operator, int arity)
                        throws Exception {
                    StringBuilder buf = new StringBuilder();
                    for (int ii = 0; ii < arity; ii++) {
                        buf.insert(0, _output.pop() + " ");
                    }
                    return buf.toString() + operator;
                }
                @Override protected String handleFunctionCall (String function, int arity)
                        throws Exception {
                    StringBuilder buf = new StringBuilder();
                    for (int ii = 0; ii < arity; ii++) {
                        buf.insert(0, _output.pop() + " ");
                    }
                    return buf.toString() + function + "()";
                }
                @Override protected String handleArrayIndex (String array) throws Exception {
                    return _output.pop() + " " + array + "[]";
                }
                @Override protected String handleIdentifier (String name) {
                    return name;
                }
            }.parse();
            System.out.println(result);
        }
    }

    /**
     * Creates a new parser to read from the specified reader.
     */
    public ExpressionParser (Reader reader)
    {
        _strtok = new OperatorStreamTokenizer(reader);
    }

    /**
     * Parses the expression and returns the object at the top of the stack (or <code>null</code>
     * if the stack is empty).
     */
    public T parse ()
        throws Exception
    {
        // read in the tokens
        int token;
        int lastToken = OperatorStreamTokenizer.TT_NOTHING;
        while ((token = _strtok.nextToken()) != StreamTokenizer.TT_EOF) {
            switch (token) {
                case StreamTokenizer.TT_NUMBER:
                    handle(_strtok.nval);
                    break;

                case '\'':
                case '\"':
                    handle(_strtok.sval);
                    break;

                case StreamTokenizer.TT_WORD:
                    // function calls are followed by a left parenthesis;
                    // array indices by a left bracket
                    String sval = _strtok.sval;
                    int ntoken = _strtok.nextToken();
                    _strtok.pushBack();
                    if (ntoken == '(') {
                        _operation.push(new FunctionCall(sval));
                    } else if (ntoken == '[') {
                        _operation.push(new ArrayIndex(sval));
                    } else {
                        handle(new Identifier(sval));
                    }
                    break;

                case ',':
                    try {
                        while (!_operation.peek().equals('(')) {
                            handle(_operation.pop());
                        }
                    } catch (EmptyStackException e) {
                        throw new Exception("Misplaced separator or mismatched parentheses.");
                    }
                    // increment the function's arity
                    int size = _operation.size();
                    Object fn = (size < 2) ? null : _operation.get(size - 2);
                    if (!(fn instanceof FunctionCall)) {
                        throw new Exception("Misplaced separator.");
                    }
                    ((FunctionCall)fn).arity++;
                    break;

                case OperatorStreamTokenizer.TT_OPERATOR:
                    Operator op = OPERATORS.get(_strtok.sval);
                    if (op == null) {
                        throw new Exception("Invalid operator " + _strtok.sval);
                    }
                    if (op.arity == 2 && lastToken != StreamTokenizer.TT_NUMBER &&
                            lastToken != StreamTokenizer.TT_WORD && lastToken != '\'' &&
                            lastToken != '\"' && lastToken != ')' && lastToken != ']') {
                        // check for a unary alternate
                        op = UNARY_ALTERNATES.get(_strtok.sval);
                        if (op == null) {
                            throw new Exception("Too few operands for " + _strtok.sval);
                        }
                    }
                    while (!_operation.isEmpty()) {
                        Object top = _operation.peek();
                        if (!(top instanceof Operator)) {
                            break;
                        }
                        Operator otop = (Operator)top;
                        if ((op.rightAssociative ? (op.precedence <= otop.precedence) :
                                (op.precedence < otop.precedence))) {
                            break;
                        }
                        handle(_operation.pop());
                    }
                    _operation.push(op);
                    break;

                case '(':
                case '[':
                    _operation.push((char)token);
                    break;

                case ')':
                case ']':
                    Character left;
                    Class<?> clazz;
                    if (token == ')') {
                        left = '(';
                        clazz = FunctionCall.class;
                    } else {
                        left = '[';
                        clazz = ArrayIndex.class;
                    }
                    try {
                        Object top;
                        while (!(top = _operation.pop()).equals(left)) {
                            handle(top);
                        }
                        if (!_operation.isEmpty() && clazz.isInstance(_operation.peek())) {
                            top = _operation.pop();
                            if (top instanceof FunctionCall && lastToken != '(') {
                                ((FunctionCall)top).arity++;
                            }
                            handle(top);
                        }
                    } catch (EmptyStackException e) {
                        throw new Exception("Mismatched " + (char)token);
                    }
                    break;
            }
            lastToken = token;
        }

        // process the remaining operators on the stack
        while (!_operation.isEmpty()) {
            Object top = _operation.pop();
            if (top.equals('(') || top.equals('[')) {
                throw new Exception("Mismatched " + top);
            }
            handle(top);
        }

        // the result should now be on the top of the output stack
        return _output.isEmpty() ? null : _output.pop();
    }

    /**
     * Handles the supplied output value.
     */
    protected void handle (Object value)
        throws Exception
    {
        T result;
        if (value instanceof Double) {
            result = handleNumber(((Double)value).doubleValue());
        } else if (value instanceof String) {
            result = handleString((String)value);
        } else { // value instanceof Identifier
            result = ((Identifier)value).handle(this);
        }
        _output.push(result);
    }

    /**
     * Handles a number.
     */
    protected T handleNumber (double value)
        throws Exception
    {
        throw new Exception("Unable to handle number " + value);
    }

    /**
     * Handles a string.
     */
    protected T handleString (String value)
        throws Exception
    {
        throw new Exception("Unable to handle string " + value);
    }

    /**
     * Handles an operator.
     */
    protected T handleOperator (String operator, int arity)
        throws Exception
    {
        throw new Exception("Unable to handle operator " + operator);
    }

    /**
     * Handles a function call.
     */
    protected T handleFunctionCall (String function, int arity)
        throws Exception
    {
        throw new Exception("Unable to handle function " + function);
    }

    /**
     * Handles an array index.
     */
    protected T handleArrayIndex (String array)
        throws Exception
    {
        throw new Exception("Unable to handle array index " + array);
    }

    /**
     * Handles an identifier.
     */
    protected T handleIdentifier (String name)
        throws Exception
    {
        throw new Exception("Unable to handle identifier " + name);
    }

    /**
     * Extends {@link StreamTokenizer} slightly to handle multi-character operators.
     */
    protected static class OperatorStreamTokenizer extends StreamTokenizer
    {
        /** A constant indicating that nothing has yet been read (defined privately in the
         * parent class). */
        public static final int TT_NOTHING = -4;

        /** A constant indicating that an operator token has been read. */
        public static final int TT_OPERATOR = -5;

        /**
         * Creates a new tokenizer.
         */
        public OperatorStreamTokenizer (Reader reader)
        {
            super(reader);
            ordinaryChar('/');
            ordinaryChar('-');
            wordChars('_', '_');
            wordChars(':', ':');
        }

        @Override
        public int nextToken ()
            throws IOException
        {
            int token;
            if (_nttype != TT_NOTHING) {
                token = ttype = _nttype;
                sval = _nsval;
                nval = _nnval;
                _nttype = TT_NOTHING;
            } else {
                token = super.nextToken();
            }
            int paired;
            switch (token) {
                case '.':
                case '*':
                case '/':
                case '%':
                case '^':
                    paired = TT_NOTHING;
                    break;

                case '+':
                case '-':
                case '&':
                case '|':
                    paired = token;
                    break;

                case '<':
                case '>':
                case '=':
                case '!':
                    paired = '=';
                    break;

                default:
                    return token;
            }
            // read the next token and see if it forms a pair
            int ntoken = super.nextToken();
            if (ntoken == paired) {
                sval = new String(new char[] { (char)token, (char)paired });
            } else {
                // remember the token for next time
                _nttype = ntoken;
                _nsval = sval;
                _nnval = nval;
                sval = Character.toString((char)token);
            }
            return (ttype = TT_OPERATOR);
        }

        protected int _nttype = TT_NOTHING;
        protected String _nsval;
        protected double _nnval;
    }

    /**
     * Wraps a string to identify it as an identifier.
     */
    protected static class Identifier
    {
        /** The name of the identifier. */
        public final String name;

        /**
         * Creates a new identifier.
         */
        public Identifier (String name)
        {
            this.name = name;
        }

        /**
         * Calls the appropriate form of the handle method.
         */
        public <T> T handle (ExpressionParser<T> parser)
            throws Exception
        {
            return parser.handleIdentifier(name);
        }
    }

    /**
     * An function call identifier.
     */
    protected static class FunctionCall extends Identifier
    {
        /** The arity of the function. */
        public int arity;

        /**
         * Creates a new function identifier.
         */
        public FunctionCall (String name)
        {
            super(name);
        }

        @Override
        public <T> T handle (ExpressionParser<T> parser)
            throws Exception
        {
            return parser.handleFunctionCall(name, arity);
        }
    }

    /**
     * An array index identifier.
     */
    protected static class ArrayIndex extends Identifier
    {
        /**
         * Creates a new array index identifier.
         */
        public ArrayIndex (String name)
        {
            super(name);
        }

        @Override
        public <T> T handle (ExpressionParser<T> parser)
            throws Exception
        {
            return parser.handleArrayIndex(name);
        }
    }

    /**
     * An operator identifier.
     */
    protected static class Operator extends Identifier
    {
        /** Whether or not the operator is right, as opposed to left, associative. */
        public final boolean rightAssociative;

        /** The precedence of the operator. */
        public final int precedence;

        /** The arity of the operator. */
        public final int arity;

        /**
         * Creates a new operator.
         */
        public Operator (String name, boolean rightAssociative, int precedence, int arity)
        {
            super(name);
            this.rightAssociative = rightAssociative;
            this.precedence = precedence;
            this.arity = arity;
        }

        @Override
        public <T> T handle (ExpressionParser<T> parser)
            throws Exception
        {
            return parser.handleOperator(name, arity);
        }
    }

    /** The tokenizer from which we acquire tokens. */
    protected OperatorStreamTokenizer _strtok;

    /** The output stack. */
    protected Stack<T> _output = new Stack<T>();

    /** The operation stack. */
    protected Stack<Object> _operation = new Stack<Object>();

    /**
     * Adds an operator to the map.
     */
    protected static void addOperator (
        String name, boolean rightAssociative, int precedence, int arity)
    {
        OPERATORS.put(name, new Operator(name, rightAssociative, precedence, arity));
    }

    /**
     * Adds an unary alternate for operators that can be both unary and binary.
     */
    protected static void addUnaryAlternate (
        String name, boolean rightAssociative, int precedence, int arity)
    {
        UNARY_ALTERNATES.put(name, new Operator(name, rightAssociative, precedence, arity));
    }

    /** The operator map. */
    protected static final Map<String, Operator> OPERATORS = Maps.newHashMap();
    protected static final Map<String, Operator> UNARY_ALTERNATES = Maps.newHashMap();
    static {
        // follow Java's rules as closely as possible
        addOperator(".", false, 1, 2);
        addOperator("++", true, 2, 1);
        addOperator("--", true, 2, 1);
        addUnaryAlternate("+", true, 2, 1);
        addUnaryAlternate("-", true, 2, 1);
        addOperator("!", true, 2, 1);
        addOperator("*", false, 3, 2);
        addOperator("/", false, 3, 2);
        addOperator("%", false, 3, 2);
        addOperator("+", false, 4, 2);
        addOperator("-", false, 4, 2);
        addOperator("<", false, 6, 2);
        addOperator("<=", false, 6, 2);
        addOperator(">", false, 6, 2);
        addOperator(">=", false, 6, 2);
        addOperator("==", false, 7, 2);
        addOperator("!=", false, 7, 2);
        addOperator("&", false, 8, 2);
        addOperator("^", false, 9, 2);
        addOperator("|", false, 10, 2);
        addOperator("&&", false, 11, 2);
        addOperator("||", false, 12, 2);
    }
}
