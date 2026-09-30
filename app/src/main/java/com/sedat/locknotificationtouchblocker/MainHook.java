package com.sedat.locknotificationtouchblocker;

import android.app.KeyguardManager;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Lockscreen Notification Touch Blocker
 *
 * Scope: com.android.systemui only.
 *
 * Behaviour:
 *  - On the keyguard/lock screen, a gesture that STARTS on an
 *    ExpandableNotificationRow is consumed by NotificationStackScrollLayout.
 *  - Tap, horizontal dismiss, long press and expand/collapse gestures that
 *    start on the notification card therefore do not reach SystemUI handlers.
 *  - Touches outside notification rows are not changed.
 *  - Once the keyguard is unlocked, SystemUI works normally.
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "LNTB";
    private static final String SYSTEM_UI = "com.android.systemui";
    private static final String STACK_CLASS =
            "com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout";
    private static final String ROW_CLASS =
            "com.android.systemui.statusbar.notification.row.ExpandableNotificationRow";

    // One NotificationStackScrollLayout per active shade in normal phone UI.
    // Weak keys avoid retaining SystemUI views after reinflation/config changes.
    private static final Map<Object, Boolean> BLOCKED_GESTURES =
            Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!SYSTEM_UI.equals(lpparam.packageName)) return;

        XposedBridge.log(TAG + ": loaded in SystemUI; process=" + lpparam.processName);

        final Class<?> rowClass = XposedHelpers.findClassIfExists(ROW_CLASS, lpparam.classLoader);
        final Class<?> stackClass = XposedHelpers.findClassIfExists(STACK_CLASS, lpparam.classLoader);

        if (rowClass == null) {
            XposedBridge.log(TAG + ": ERROR row class not found: " + ROW_CLASS);
        } else {
            hookRowFallback(rowClass);
        }

        if (stackClass == null) {
            XposedBridge.log(TAG + ": ERROR stack class not found: " + STACK_CLASS);
            return;
        }

        hookStack(stackClass, rowClass);
        XposedBridge.log(TAG + ": hooks installed successfully");
    }

    private static void hookStack(Class<?> stackClass, Class<?> rowClass) {
        // Intercept the gesture before the row/swipe/expand handlers receive it.
        XposedBridge.hookAllMethods(stackClass, "onInterceptTouchEvent", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args.length == 0 || !(param.args[0] instanceof MotionEvent)) return;
                if (!(param.thisObject instanceof ViewGroup)) return;

                ViewGroup stack = (ViewGroup) param.thisObject;
                MotionEvent ev = (MotionEvent) param.args[0];
                int action = ev.getActionMasked();

                if (action == MotionEvent.ACTION_DOWN) {
                    boolean block = isKeyguardLocked(stack)
                            && isPointOnNotificationRow(stack, ev.getX(), ev.getY(), rowClass);
                    BLOCKED_GESTURES.put(stack, block);
                    if (block) {
                        XposedBridge.log(TAG + ": BLOCK DOWN x=" + (int) ev.getX()
                                + " y=" + (int) ev.getY());
                        param.setResult(true);
                    }
                    return;
                }

                if (Boolean.TRUE.equals(BLOCKED_GESTURES.get(stack))) {
                    param.setResult(true);
                }
            }
        });

        // When the stack has intercepted the gesture, consume it without running
        // NotificationStackScrollLayout's own scroll/swipe/expand touch handling.
        XposedBridge.hookAllMethods(stackClass, "onTouchEvent", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args.length == 0 || !(param.args[0] instanceof MotionEvent)) return;
                if (!(param.thisObject instanceof ViewGroup)) return;

                ViewGroup stack = (ViewGroup) param.thisObject;
                MotionEvent ev = (MotionEvent) param.args[0];

                if (Boolean.TRUE.equals(BLOCKED_GESTURES.get(stack))) {
                    param.setResult(true);
                    int action = ev.getActionMasked();
                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        BLOCKED_GESTURES.remove(stack);
                    }
                }
            }
        });
    }

    /**
     * Backup protection for ROMs where the parent stack does not intercept a
     * particular row touch path. This blocks direct row touch handling only
     * while KeyguardManager reports an active lock screen.
     */
    private static void hookRowFallback(Class<?> rowClass) {
        XposedBridge.hookAllMethods(rowClass, "onTouchEvent", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!(param.thisObject instanceof View)) return;
                if (isKeyguardLocked((View) param.thisObject)) {
                    param.setResult(true);
                }
            }
        });
    }

    private static boolean isKeyguardLocked(View view) {
        try {
            Context context = view.getContext();
            KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            return km != null && km.isKeyguardLocked();
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": keyguard check failed: " + t);
            // Fail open: never break SystemUI if detection fails.
            return false;
        }
    }

    /**
     * Tests direct stack children using their translated Y position. AOSP's own
     * NotificationStackScrollLayout uses the same general geometry model for
     * locating the ExpandableView under a pointer.
     */
    private static boolean isPointOnNotificationRow(
            ViewGroup stack, float x, float y, Class<?> rowClass) {
        if (rowClass == null) return false;
        if (x < 0 || x > stack.getWidth()) return false;

        final int count = stack.getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            View child = stack.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE) continue;
            if (!rowClass.isInstance(child)) continue;

            float top = child.getTranslationY();
            float bottom = top + child.getHeight();

            // NotificationStackScrollLayout treats the row as full stack width
            // for touch conflict handling on keyguard, so we intentionally only
            // need Y containment here.
            if (y >= top && y <= bottom) return true;
        }
        return false;
    }
}
