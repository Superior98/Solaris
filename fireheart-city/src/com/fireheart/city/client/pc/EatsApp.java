package com.fireheart.city.client.pc;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** SolEats: browse restaurants, build a cart, tip the courier and track your order until the box is at your door. */
class EatsApp extends App {
    private final Btn btn = new Btn();
    private int scroll, maxScroll, page, tip = 2;
    private String shop = "";
    private final Map<String, Integer> cart = new LinkedHashMap<>();
    static final int HOME = 0, MENU = 1, CART = 2, ORDERS = 3;
    static final int[] SHOP_COL = {0xFFE63946, 0xFF7209B7, 0xFFF4A261};

    public String title() {
        return switch (page) {
            case MENU -> shopName(shop);
            case CART -> "Your cart";
            case ORDERS -> "Your orders";
            default -> "SolEats";
        };
    }

    public int color() { return 0xFFF77F00; }

    public boolean back() {
        if (page == HOME) return false;
        page = page == CART ? MENU : HOME;
        scroll = 0;
        return true;
    }

    List<String[]> rows() {
        List<String[]> out = new ArrayList<>();
        for (String r : os.data.menu) {
            String[] p = r.split("\\|");
            if (p.length >= 5) out.add(p);
        }
        return out;
    }

    String shopName(String id) {
        for (String[] p : rows()) if (p[0].equals(id)) return p[1];
        return "SolEats";
    }

    int price(String item) {
        for (String[] p : rows()) if (p[2].equals(item) && p[0].equals(shop)) return ClientPc.parse(p[4]);
        return 0;
    }

    static ItemStack stack(String id) {
        try {
            if (com.fireheart.city.Dishes.BY_SOURCE.containsKey(id)) return com.fireheart.city.Dishes.make(id, 1, Long.MAX_VALUE / 4);
            return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(id)));
        } catch (RuntimeException e) {
            return ItemStack.EMPTY;
        }
    }

    int cartCount() {
        int n = 0;
        for (int v : cart.values()) n += v;
        return n;
    }

    int subtotal() {
        int n = 0;
        for (Map.Entry<String, Integer> e : cart.entrySet()) n += price(e.getKey()) * e.getValue();
        return n;
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        Font f = os.font();
        btn.clear();
        g.fill(x, y, x + w, y + h, 0xFFFFF8F0);
        int top = y + 2;
        g.enableScissor(x, top, x + w, y + h - 18);
        int yy = top - scroll;
        switch (page) {
            case HOME -> yy = home(g, f, mx, my, yy);
            case MENU -> yy = menu(g, f, mx, my, yy);
            case CART -> yy = cartPage(g, f, mx, my, yy);
            default -> yy = orders(g, f, yy);
        }
        g.disableScissor();
        maxScroll = Math.max(0, yy + scroll - (y + h - 18) + 4);
        g.fill(x, y + h - 18, x + w, y + h, 0xFFFFFFFF);
        g.fill(x, y + h - 18, x + w, y + h - 17, 0x22000000);
        int bw = w / 3;
        btn.draw(g, f, x + 2, y + h - 16, bw - 4, 14, page == HOME || page == MENU ? "§lShops" : "Shops", 0x00000000, 0xFFF77F00, mx, my, () -> { page = HOME; scroll = 0; });
        btn.draw(g, f, x + bw + 2, y + h - 16, bw - 4, 14, (page == CART ? "§l" : "") + "Cart" + (cartCount() > 0 ? " " + cartCount() : ""), 0x00000000, 0xFFF77F00, mx, my, () -> { if (!shop.isEmpty()) { page = CART; scroll = 0; } });
        btn.draw(g, f, x + 2 * bw + 2, y + h - 16, bw - 4, 14, (page == ORDERS ? "§l" : "") + "Orders" + (os.data.orders.isEmpty() ? "" : " •"), 0x00000000, 0xFFF77F00, mx, my, () -> { page = ORDERS; scroll = 0; });
    }

    int home(GuiGraphics g, Font f, int mx, int my, int yy) {
        g.drawString(f, "§lWhat are you craving?", x + 6, yy + 4, 0xFF222222, false);
        yy += 14;
        String bal = os.data.hasAccount ? "Savings " + os.data.savings + "¢" : "No bank account";
        g.drawString(f, "§8" + bal, x + 6, yy + 1, 0xFF666666, false);
        yy += 12;
        List<String> seen = new ArrayList<>();
        int k = 0;
        for (String[] p : rows()) {
            if (seen.contains(p[0])) continue;
            seen.add(p[0]);
            boolean open = p.length < 6 || p[5].equals("1");
            int col = SHOP_COL[k++ % SHOP_COL.length];
            int ch = 46;
            PhoneScreen.roundRect(g, x + 4, yy, x + w - 4, yy + ch, 6, 0xFFFFFFFF);
            PhoneScreen.roundRect(g, x + 4, yy, x + w - 4, yy + 18, 6, col);
            g.fill(x + 4, yy + 12, x + w - 4, yy + 18, col);
            g.renderItem(stack(p[2]), x + w - 22, yy + 1);
            g.drawString(f, "§l" + p[1], x + 9, yy + 5, 0xFFFFFFFF, false);
            String desc = p.length > 7 ? p[7] : "";
            g.drawString(f, f.plainSubstrByWidth(desc, w - 18), x + 9, yy + 21, 0xFF777777, false);
            String meta = "§6★ " + (p.length > 6 ? p[6] : "4.8") + "  §8·  " + (open ? "§215-25 min" : "§cclosed now");
            g.drawString(f, meta, x + 9, yy + 33, 0xFF444444, false);
            String id = p[0];
            btn.draw(g, f, x + 4, yy, w - 8, ch, "", 0x00000000, 0, mx, my, () -> {
                if (!id.equals(shop)) cart.clear();
                shop = id;
                page = MENU;
                scroll = 0;
            });
            yy += ch + 5;
        }
        if (seen.isEmpty()) g.drawCenteredString(f, "§8Menus are loading...", x + w / 2, yy + 10, 0xFF000000);
        return yy;
    }

    int menu(GuiGraphics g, Font f, int mx, int my, int yy) {
        for (String[] p : rows()) {
            if (!p[0].equals(shop)) continue;
            PhoneScreen.roundRect(g, x + 4, yy + 2, x + w - 4, yy + 26, 5, 0xFFFFFFFF);
            g.renderItem(stack(p[2]), x + 7, yy + 6);
            String name = p[3].replaceFirst("^(a|an|some) ", "");
            if (!name.isEmpty()) name = name.substring(0, 1).toUpperCase() + name.substring(1);
            g.drawString(f, f.plainSubstrByWidth(name, w - 82), x + 27, yy + 6, 0xFF222222, false);
            g.drawString(f, "§8" + p[4] + "¢", x + 27, yy + 16, 0xFF666666, false);
            String it = p[2];
            int n = cart.getOrDefault(it, 0);
            if (n > 0) {
                btn.draw(g, f, x + w - 52, yy + 8, 12, 12, "-", 0xFFE5E5E5, 0xFF222222, mx, my, () -> { int m = cart.getOrDefault(it, 0) - 1; if (m <= 0) cart.remove(it); else cart.put(it, m); });
                g.drawCenteredString(f, String.valueOf(n), x + w - 32, yy + 10, 0xFF222222);
            }
            btn.draw(g, f, x + w - 22, yy + 8, 14, 12, "+", 0xFFF77F00, 0xFFFFFFFF, mx, my, () -> cart.merge(it, 1, Integer::sum));
            yy += 28;
        }
        if (cartCount() > 0) {
            btn.draw(g, f, x + 6, yy + 4, w - 12, 16, "Cart · " + cartCount() + " · " + subtotal() + "¢", 0xFF2DC653, 0xFFFFFFFF, mx, my, () -> { page = CART; scroll = 0; });
            yy += 24;
        }
        return yy;
    }

    int cartPage(GuiGraphics g, Font f, int mx, int my, int yy) {
        g.drawString(f, "§l" + shopName(shop), x + 6, yy + 4, 0xFF222222, false);
        yy += 16;
        if (cart.isEmpty()) {
            g.drawCenteredString(f, "§8Your cart is empty.", x + w / 2, yy + 10, 0xFF000000);
            return yy + 30;
        }
        for (Map.Entry<String, Integer> e : cart.entrySet()) {
            g.renderItem(stack(e.getKey()), x + 6, yy);
            String label = stack(e.getKey()).getHoverName().getString();
            g.drawString(f, e.getValue() + "x " + f.plainSubstrByWidth(label, w - 66), x + 25, yy + 4, 0xFF222222, false);
            String pr = (price(e.getKey()) * e.getValue()) + "¢";
            g.drawString(f, pr, x + w - 6 - f.width(pr), yy + 4, 0xFF444444, false);
            yy += 18;
        }
        g.fill(x + 6, yy + 2, x + w - 6, yy + 3, 0x22000000);
        yy += 6;
        line(g, f, yy, "Subtotal", subtotal() + "¢");
        yy += 11;
        line(g, f, yy, "Delivery", "3¢");
        yy += 11;
        g.drawString(f, "Tip Pip", x + 6, yy + 3, 0xFF444444, false);
        int[] tips = {0, 2, 5, 10};
        int tx = x + w - 6 - tips.length * 20;
        for (int t : tips) {
            int tt = t;
            btn.draw(g, f, tx, yy, 18, 12, String.valueOf(t), tip == t ? 0xFFF77F00 : 0xFFE5E5E5, tip == t ? 0xFFFFFFFF : 0xFF222222, mx, my, () -> tip = tt);
            tx += 20;
        }
        yy += 16;
        int total = subtotal() + 3 + tip;
        line(g, f, yy, "§lTotal", "§l" + total + "¢");
        yy += 14;
        btn.draw(g, f, x + 6, yy, w - 12, 16, "Place order · " + total + "¢", 0xFF2DC653, 0xFFFFFFFF, mx, my, () -> {
            StringBuilder b = new StringBuilder();
            for (Map.Entry<String, Integer> e : cart.entrySet()) b.append(b.length() == 0 ? "" : ",").append(e.getKey()).append("*").append(e.getValue());
            os.send("food", shop, b + ";" + tip);
            cart.clear();
            page = ORDERS;
            scroll = 0;
        });
        yy += 20;
        for (var ln : f.split(net.minecraft.network.chat.Component.literal("§8Paid from savings. Pip leaves it in a box at your front door."), w - 12)) {
            g.drawString(f, ln, x + 6, yy, 0xFF888888, false);
            yy += 9;
        }
        return yy + 4;
    }

    void line(GuiGraphics g, Font f, int yy, String a, String b) {
        g.drawString(f, a, x + 6, yy, 0xFF444444, false);
        g.drawString(f, b, x + w - 6 - f.width(b.replaceAll("§.", "")), yy, 0xFF444444, false);
    }

    int orders(GuiGraphics g, Font f, int yy) {
        yy += 4;
        if (os.data.orders.isEmpty()) {
            g.drawCenteredString(f, "§8No active orders.", x + w / 2, yy + 10, 0xFF000000);
            g.drawCenteredString(f, "§8Hungry? Pick a shop!", x + w / 2, yy + 22, 0xFF000000);
            return yy + 40;
        }
        String[] steps = {"Order received", "Cooking", "Packed", "On the way", "Delivered!"};
        for (int i = os.data.orders.size() - 1; i >= 0; i--) {
            String[] p = os.data.orders.get(i).split("\\|");
            if (p.length < 4) continue;
            int st = Math.max(0, Math.min(4, ClientPc.parse(p[3])));
            String detail = p.length > 5 ? p[5] : steps[st];
            PhoneScreen.roundRect(g, x + 4, yy, x + w - 4, yy + 48, 6, 0xFFFFFFFF);
            g.drawString(f, "§l" + p[1], x + 9, yy + 4, 0xFF222222, false);
            String badge = st == 4 ? "§2✔ " + steps[st] : "§6" + steps[st];
            g.drawString(f, badge, x + w - 9 - f.width(badge.replaceAll("§.", "")), yy + 4, 0xFF222222, false);
            g.drawString(f, f.plainSubstrByWidth("§8" + p[2], w - 18), x + 9, yy + 14, 0xFF666666, false);
            int bx = x + 12, bw = w - 24;
            long tk = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
            for (int k = 0; k < 5; k++) {
                int sx = bx + k * bw / 4;
                if (k < 4) g.fill(sx + 3, yy + 29, sx + bw / 4 - 3, yy + 31, k < st ? 0xFF2DC653 : 0xFFDDDDDD);
                boolean pulse = k == st && st < 4 && tk / 8 % 2 == 0;
                g.fill(sx - 3, yy + 27, sx + 3, yy + 33, k < st || st == 4 ? 0xFF2DC653 : k == st ? (pulse ? 0xFFF77F00 : 0xFFFFB45C) : 0xFFDDDDDD);
            }
            if (st > 0 && st < 4) {
                String ic = st == 1 ? "♨" : st == 2 ? "▣" : "➜";
                int ix = bx + st * bw / 4 - f.width(ic) / 2;
                g.drawString(f, ic, ix, yy + 18, 0xFFF77F00, false);
            }
            g.drawString(f, f.plainSubstrByWidth((st == 4 ? "§2" : "§0") + detail, w - 18), x + 9, yy + 37, 0xFF222222, false);
            yy += 52;
        }
        return yy;
    }

    public boolean click(double mx, double my, int b) {
        return btn.click(mx, my);
    }

    public boolean scroll(double mx, double my, double d) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (d * 16)));
        return true;
    }
}
