//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

package com.threerings.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;

import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Parses simple expressions using an implementation of the
 * <a href="http://en.wikipedia.org/wiki/Shunting-yard_algorithm">shunting-yard algorithm</a>.
 */
public class ExpressionParser<T>
{
    /**
     * Creates a new parser to read from the specified reader.
     */
    public ExpressionParser (Reader reader)
    {
        _strtok = new OperatorStreamTokenizer(reader);
    }

    /**
     * Parses the expression.
     */
    public T parse ()
        throws Exception
    {
        // read in the tokens
        int token;
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
                    break;

                case OperatorStreamTokenizer.TT_OPERATOR:
                    Operator op = OPERATORS.get(_strtok.sval);
                    if (op == null) {
                        throw new Exception("Invalid operator " + _strtok.sval);
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
                            handle(_operation.pop());
                        }
                    } catch (EmptyStackException e) {
                        throw new Exception("Mismatched parentheses.");
                    }
                    break;
            }
        }

        // process the remaining operators on the stack
        while (!_operation.isEmpty()) {
            Object top = _operation.pop();
            if (top.equals('(') || top.equals(')')) {
                throw new Exception("Mismatched parentheses.");
            }
            handle(top);
        }

        // the result should now be on the top of the output stack
        return _output.isEmpty() ? null : _output.peek();
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
    protected T handleOperator (String operator)
        throws Exception
    {
        throw new Exception("Unable to handle operator " + operator);
    }

    /**
     * Handles a function call.
     */
    protected T handleFunctionCall (String function)
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
        }

        @Override // documentation inherited
        public int nextToken ()
            throws IOException
        {
            int token = super.nextToken();
            int paired;
            switch (token) {
                case '.':
                case '*':
                case '/':
                case '%':
                case '^':
                    paired = -6;
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
                pushBack();
                sval = Character.toString((char)token);
            }
            return (ttype = TT_OPERATOR);
        }
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

        @Override // documentation inherited
        public String toString ()
        {
            return "i:" + name;
        }
    }

    /**
     * An function call identifier.
     */
    protected static class FunctionCall extends Identifier
    {
        /**
         * Creates a new function identifier.
         */
        public FunctionCall (String name)
        {
            super(name);
        }

        @Override // documentation inherited
        public <T> T handle (ExpressionParser<T> parser)
            throws Exception
        {
            return parser.handleFunctionCall(name);
        }

        @Override // documentation inherited
        public String toString ()
        {
            return "f:" + name;
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

        @Override // documentation inherited
        public <T> T handle (ExpressionParser<T> parser)
            throws Exception
        {
            return parser.handleArrayIndex(name);
        }

        @Override // documentation inherited
        public String toString ()
        {
            return "a:" + name;
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

        /**
         * Creates a new operator.
         */
        public Operator (String name, boolean rightAssociative, int precedence)
        {
            super(name);
            this.rightAssociative = rightAssociative;
            this.precedence = precedence;
        }

        @Override // documentation inherited
        public <T> T handle (ExpressionParser<T> parser)
            throws Exception
        {
            return parser.handleOperator(name);
        }

        @Override // documentation inherited
        public String toString ()
        {
            return "o:" + name;
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
    protected static void addOperator (String name, boolean rightAssociative, int precedence)
    {
        OPERATORS.put(name, new Operator(name, rightAssociative, precedence));
    }

    /** The operator map. */
    protected static final Map<String, Operator> OPERATORS = Maps.newHashMap();
    static {
        // follow Java's rules as closely as possible
        addOperator(".", false, 1);
        addOperator("++", true, 2);
        addOperator("--", true, 2);
        addOperator("!", true, 2);
        addOperator("*", false, 3);
        addOperator("/", false, 3);
        addOperator("%", false, 3);
        addOperator("+", false, 4);
        addOperator("-", false, 4);
        addOperator("<", false, 6);
        addOperator("<=", false, 6);
        addOperator(">", false, 6);
        addOperator(">=", false, 6);
        addOperator("==", false, 7);
        addOperator("!=", false, 7);
        addOperator("&", false, 8);
        addOperator("^", false, 9);
        addOperator("|", false, 10);
        addOperator("&&", false, 11);
        addOperator("||", false, 12);
    }
}
