package com.fireheart.city.client.pc;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;

/** Game controller support for SolPlay: any GLFW gamepad maps to arrows, Space/Enter (A), Backspace (B), F (Y) and P (Start). */
final class Gamepad {
    private Gamepad() {}

    static final int[][] MAP = {
            {GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP, App.UP}, {GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN, App.DOWN},
            {GLFW.GLFW_GAMEPAD_BUTTON_DPAD_LEFT, App.LEFT}, {GLFW.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT, App.RIGHT},
            {GLFW.GLFW_GAMEPAD_BUTTON_A, App.SPACE}, {GLFW.GLFW_GAMEPAD_BUTTON_B, App.BACKSPACE},
            {GLFW.GLFW_GAMEPAD_BUTTON_Y, 70}, {GLFW.GLFW_GAMEPAD_BUTTON_START, 80}, {GLFW.GLFW_GAMEPAD_BUTTON_X, App.ENTER}};
    static final boolean[] down = new boolean[16];
    static final boolean[] stick = new boolean[4];
    static long lastPoll;
    static GLFWGamepadState state;
    static boolean present;

    static int pad() {
        for (int j = GLFW.GLFW_JOYSTICK_1; j <= GLFW.GLFW_JOYSTICK_4; j++) if (GLFW.glfwJoystickIsGamepad(j)) return j;
        return -1;
    }

    static boolean read() {
        try {
            int j = pad();
            present = j >= 0;
            if (!present) return false;
            if (state == null) state = GLFWGamepadState.create();
            return GLFW.glfwGetGamepadState(j, state);
        } catch (Throwable t) {
            present = false;
            return false;
        }
    }

    /** New presses since the last poll, as key codes. */
    static List<Integer> poll() {
        List<Integer> out = new ArrayList<>();
        if (!read()) return out;
        for (int i = 0; i < MAP.length; i++) {
            boolean d = state.buttons(MAP[i][0]) == GLFW.GLFW_PRESS;
            if (d && !down[i]) out.add(MAP[i][1]);
            down[i] = d;
        }
        float ax = state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_X), ay = state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y);
        boolean[] now = {ay < -0.6f, ay > 0.6f, ax < -0.6f, ax > 0.6f};
        int[] keys = {App.UP, App.DOWN, App.LEFT, App.RIGHT};
        for (int i = 0; i < 4; i++) {
            if (now[i] && !stick[i]) out.add(keys[i]);
            stick[i] = now[i];
        }
        return out;
    }

    static boolean held(int... keys) {
        if (!present || state == null) return false;
        float ax = state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_X), ay = state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y);
        float rt = state.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER), lt = state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER);
        for (int k : keys) {
            for (int[] m : MAP) if (m[1] == k && state.buttons(m[0]) == GLFW.GLFW_PRESS) return true;
            if ((k == App.UP || k == App.KW) && (ay < -0.4f || rt > 0.2f)) return true;
            if ((k == App.DOWN || k == App.KS) && (ay > 0.4f || lt > 0.2f)) return true;
            if ((k == App.LEFT || k == App.KA) && ax < -0.35f) return true;
            if ((k == App.RIGHT || k == App.KD) && ax > 0.35f) return true;
        }
        return false;
    }

    static float steer() {
        if (!present || state == null) return 0;
        float ax = state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_X);
        return Math.abs(ax) < 0.15f ? 0 : ax;
    }
}
