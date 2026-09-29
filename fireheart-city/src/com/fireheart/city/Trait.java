package com.fireheart.city;

public enum Trait {
    CHEERFUL(1.3, 0.10), SHY(0.6, 0.05), CURIOUS(1.2, 0.15), GRUMPY(0.7, 0.05), ADVENTUROUS(1.0, 0.35),
    FRIENDLY(1.4, 0.10), DREAMY(0.9, 0.20), TALKATIVE(1.6, 0.10), LAIDBACK(1.2, 0.25);

    public final double chattiness;
    public final double skyChance;

    Trait(double chattiness, double skyChance) {
        this.chattiness = chattiness;
        this.skyChance = skyChance;
    }

    public String adjective() {
        return this == LAIDBACK ? "laid-back" : name().toLowerCase();
    }
}
