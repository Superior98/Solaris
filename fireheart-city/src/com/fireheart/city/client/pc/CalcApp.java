package com.fireheart.city.client.pc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

class CalcApp extends App {
    private String expr = "";
    private String result = "0";

    public String title() { return "Calculator"; }

    public int color() { return 0xFF5C677D; }

    private static final String[] KEYS = {"7", "8", "9", "/", "4", "5", "6", "*", "1", "2", "3", "-", "0", ".", "=", "+", "C", "(", ")", "<"};

    private int kw() {
        return Math.min(46, (w - 4) / 4);
    }

    public void addWidgets() {
        int kw = kw(), bx = x + w / 2 - kw * 2, by = y + 46;
        for (int i = 0; i < KEYS.length; i++) {
            String k = KEYS[i];
            os.widget(Button.builder(Component.literal(k), b -> press(k)).bounds(bx + (i % 4) * kw, by + (i / 4) * 22, kw - 2, 20).build());
        }
    }

    void press(String k) {
        switch (k) {
            case "C" -> { expr = ""; result = "0"; }
            case "<" -> { if (!expr.isEmpty()) expr = expr.substring(0, expr.length() - 1); }
            case "=" -> {
                try {
                    double v = new Parser(expr).parse();
                    result = (v == Math.rint(v) && Math.abs(v) < 1e15) ? String.valueOf((long) v) : String.format("%.6g", v);
                    expr = result;
                } catch (Exception e) {
                    result = "Error";
                }
            }
            default -> { if (expr.length() < 40) expr += k; }
        }
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        var f = os.font();
        int kw = kw(), bx = x + w / 2 - kw * 2, bw = kw * 4 - 2;
        g.fill(bx, y + 6, bx + bw, y + 40, 0xFF1B1F24);
        g.drawString(f, expr.isEmpty() ? "0" : expr, bx + bw - 4 - f.width(expr.isEmpty() ? "0" : expr), y + 10, 0xFF9AA5B1, false);
        g.pose().pushPose();
        g.pose().scale(1.5f, 1.5f, 1f);
        g.drawString(f, result, (int) ((bx + bw - 4 - f.width(result) * 1.5f) / 1.5f), (int) ((y + 24) / 1.5f), 0xFFFFFFFF, false);
        g.pose().popPose();
    }

    public boolean typed(char c) {
        if ("0123456789.+-*/()".indexOf(c) >= 0) { press(String.valueOf(c)); return true; }
        if (c == '=') { press("="); return true; }
        if (c == 'c' || c == 'C') { press("C"); return true; }
        return false;
    }

    public boolean key(int k) {
        if (k == ENTER || k == KP_ENTER) { press("="); return true; }
        if (k == BACKSPACE) { press("<"); return true; }
        return false;
    }

    static final class Parser {
        private final String s;
        private int i;

        Parser(String s) { this.s = s.replace(" ", ""); }

        double parse() {
            double v = expr();
            if (i != s.length()) throw new IllegalArgumentException();
            return v;
        }

        double expr() {
            double v = term();
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '+') { i++; v += term(); } else if (c == '-') { i++; v -= term(); } else break;
            }
            return v;
        }

        double term() {
            double v = factor();
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '*') { i++; v *= factor(); } else if (c == '/') { i++; v /= factor(); } else break;
            }
            return v;
        }

        double factor() {
            if (i < s.length() && s.charAt(i) == '-') { i++; return -factor(); }
            if (i < s.length() && s.charAt(i) == '(') {
                i++;
                double v = expr();
                if (i < s.length() && s.charAt(i) == ')') i++;
                return v;
            }
            int st = i;
            while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
            return Double.parseDouble(s.substring(st, i));
        }
    }
}
