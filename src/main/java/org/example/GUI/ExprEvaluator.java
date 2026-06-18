package org.example.GUI;

import java.util.Map;

/** 简单整数表达式求值器。支持变量(a-z)、整数、+-* /、括号、一元负号。 */
public class ExprEvaluator {
    private final String expr;
    private int pos;
    private final Map<String, Integer> vars;

    /** 对表达式求值，变量值从 vars 中查找（未找到默认为 0）。 */
    public static int eval(String expr, Map<String, Integer> vars) {
        if (expr == null || expr.isBlank()) return 0;
        return new ExprEvaluator(expr.trim(), vars).parseExpr();
    }

    private ExprEvaluator(String expr, Map<String, Integer> vars) {
        this.expr = expr;
        this.pos = 0;
        this.vars = vars;
    }

    private int parseExpr() {
        int left = parseTerm();
        while (pos < expr.length()) {
            char c = expr.charAt(pos);
            if (c == '+') { pos++; left += parseTerm(); }
            else if (c == '-') { pos++; left -= parseTerm(); }
            else break;
        }
        return left;
    }

    private int parseTerm() {
        int left = parseFactor();
        while (pos < expr.length()) {
            char c = expr.charAt(pos);
            if (c == '*') { pos++; left *= parseFactor(); }
            else if (c == '/') {
                pos++;
                int divisor = parseFactor();
                if (divisor != 0) left /= divisor;
            }
            else break;
        }
        return left;
    }

    private int parseFactor() {
        skipSpace();
        if (pos >= expr.length()) return 0;
        char c = expr.charAt(pos);
        if (c == '(') {
            pos++;
            int val = parseExpr();
            skipSpace();
            if (pos < expr.length() && expr.charAt(pos) == ')') pos++;
            return val;
        }
        if (c == '-') {
            pos++;
            return -parseFactor();
        }
        if (c >= 'a' && c <= 'z') {
            pos++;
            Integer val = vars.get(String.valueOf(c));
            return val != null ? val : 0;
        }
        return parseNumber();
    }

    private int parseNumber() {
        skipSpace();
        int start = pos;
        while (pos < expr.length() && expr.charAt(pos) >= '0' && expr.charAt(pos) <= '9') pos++;
        if (pos > start) return Integer.parseInt(expr.substring(start, pos));
        return 0;
    }

    private void skipSpace() {
        while (pos < expr.length() && expr.charAt(pos) == ' ') pos++;
    }
}
