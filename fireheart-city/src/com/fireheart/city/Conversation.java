package com.fireheart.city;

public class Conversation {
    public final Resident a;
    public final Resident b;
    private final Dialogue.Script script;
    private int index = 0;
    private int timer = 30;
    private int age = 0;
    private int apart = 0;
    private int waited = 0;

    public Conversation(Resident a, Resident b, Dialogue.Script script) {
        this.a = a;
        this.b = b;
        this.script = script;
    }

    public String kind() {
        return script.kind;
    }

    public Resident partnerOf(Resident r) {
        return r == a ? b : a;
    }

    public void tick(Resident caller) {
        if (caller != a) {
            if (a.isRemoved() || a.convo != this) caller.endConversation();
            return;
        }
        age++;
        if (b.isRemoved() || b.convo != this || age > 2400 || a.distanceToSqr(b) > 10 * 10 || a.activityName().equals("sleep") && !script.kind.equals("date")) {
            finish(false);
            return;
        }
        boolean close = "bank".equals(script.kind) ? a.distanceToSqr(b) < 4.0D * 4.0D : a.canSee(b, 3.5D);
        if (!close) {
            if (++apart > 200) finish(false);
            return;
        }
        apart = 0;
        if (--timer > 0) return;
        if (index >= script.lines.size()) {
            finish(true);
            return;
        }
        Dialogue.Line next = script.lines.get(index);
        Resident who = next.byA() ? a : b;
        if (who.crowded(5.0) && ++waited < 160) {
            timer = 10;
            return;
        }
        waited = 0;
        Dialogue.Line line = script.lines.get(index++);
        Resident speaker = line.byA() ? a : b;
        partnerOf(speaker).hush();
        int dur = 45 + line.text().length() * 2;
        speaker.sayLine(line.text(), dur + 10);
        if (line.gesture() > 0) speaker.gesture(line.gesture(), Math.min(dur, 60));
        if (line.action() != null) line.action().run();
        timer = dur;
    }

    public void abort() {
        finish(false);
    }

    private void finish(boolean completed) {
        if (completed) {
            script.onDone.run();
            a.data().setDirty();
        } else {
            a.hush();
            b.hush();
        }
        a.endConversation();
        b.endConversation();
        if (completed && script.after != null) script.after.run();
    }
}
