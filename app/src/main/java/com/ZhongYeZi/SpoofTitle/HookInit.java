package com.ZhongYeZi.SpoofTitle;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Display;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HookInit implements IXposedHookLoadPackage {

    private static final List<WeakReference<View>> TRACKED_VIEWS = new ArrayList<>();

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName == null) return;
            hookWindowManager(lpparam.classLoader, true);
            hookSecureFlag(lpparam.classLoader);
    }

    private void hookSecureFlag(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(Window.class, "setFlags",
                    int.class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            int flags = (Integer) param.args[0];
                            int mask = (Integer) param.args[1];
                            if ((flags & WindowManager.LayoutParams.FLAG_SECURE) != 0) {
                                param.args[0] = flags & ~WindowManager.LayoutParams.FLAG_SECURE;
                                XposedBridge.log("SpoofTitle: 拦截 Window.setFlags 移除 FLAG_SECURE");
                            }
                        }
                    });

            XposedHelpers.findAndHookMethod(SurfaceView.class, "setSecure",
                    boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if ((Boolean) param.args[0]) {
                                param.args[0] = false;
                                XposedBridge.log("SpoofTitle: 拦截 SurfaceView.setSecure(true) -> false");
                            }
                        }
                    });

        } catch (Throwable t) {
            XposedBridge.log("SpoofTitle: Hook SecureFlag 失败: " + t.getMessage());
        }
    }

    private void hookWindowManager(final ClassLoader classLoader, final boolean removeSecure) {
        try {
            Class<?> wmgClass = XposedHelpers.findClass("android.view.WindowManagerGlobal", classLoader);
            
            XposedBridge.hookAllMethods(wmgClass, "addView", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    View view = null;
                    WindowManager.LayoutParams lp = null;
                    
                    for (Object arg : param.args) {
                        if (arg instanceof View) {
                            view = (View) arg;
                        } else if (arg instanceof WindowManager.LayoutParams) {
                            lp = (WindowManager.LayoutParams) arg;
                        }
                    }

                    if (lp != null) {
                        spoofLayoutParams(lp, classLoader, removeSecure);
                    }
                    
                    if (view != null) {
                        synchronized (TRACKED_VIEWS) {
                            TRACKED_VIEWS.add(new WeakReference<>(view));
                        }
                    }
                }
            });

            XposedBridge.hookAllMethods(wmgClass, "updateViewLayout", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    WindowManager.LayoutParams lp = null;
                    for (Object arg : param.args) {
                        if (arg instanceof WindowManager.LayoutParams) {
                            lp = (WindowManager.LayoutParams) arg;
                            break;
                        }
                    }
                    
                    if (lp != null) {
                        spoofLayoutParams(lp, classLoader, removeSecure);
                    }
                }
            });

        } catch (Throwable t) {
            XposedBridge.log("SpoofTitle: Hook WindowManager 失败: " + t.getMessage());
        }
    }

    private void spoofLayoutParams(WindowManager.LayoutParams lp, ClassLoader classLoader, boolean removeSecure) {
        int magicFlags = WindowManager.LayoutParams.FLAG_DITHER | 1048576 | 262696 | 131072 | 4136;
        lp.flags |= magicFlags;
        
        if (removeSecure) {
             lp.flags &= ~WindowManager.LayoutParams.FLAG_SECURE;
        }
        
        lp.dimAmount = 0;
        
        //emmm 这些标识也不知道对不对我用的一加反正是没问题了hahaha
        if (RomUtil.isMiui()) {
            lp.setTitle("com.miui.screenrecorder");
        } else if (RomUtil.isOnePlus()) {
            lp.setTitle("com.oplus.screenrecorder.FloatView"); 
        } else if (RomUtil.isRealme()) {
            lp.setTitle("com.oplus.screenrecorder.FloatView"); 
        } else if (RomUtil.isSamsung()) {
            lp.setTitle("com.samsung.android.app.screenrecorder");
        } else if (RomUtil.isEmui()) {
            lp.setTitle("ScreenRecoderTimer");
        } else if (RomUtil.isVivo()) {
            lp.setTitle("screen_record_menu");
        } else if (RomUtil.isOppo()) {
            lp.setTitle("com.coloros.screenrecorder.FloatView");
        } else if (RomUtil.isSmartisan()) {
            lp.setTitle("");
        } else if (RomUtil.isFlyme()) {
            try {
                lp.setTitle("SysScreenRecorder");
                Class<?> meizuParamsClass = classLoader.loadClass("android.view.MeizuLayoutParams");
                Field flagField = meizuParamsClass.getDeclaredField("flags");
                flagField.setAccessible(true);
                Object meizuParams = meizuParamsClass.newInstance();
                flagField.setInt(meizuParams, magicFlags);
                Field mzParamsField = lp.getClass().getField("meizuParams");
                mzParamsField.set(lp, meizuParams);
            } catch (Throwable t) {
                XposedBridge.log("SpoofTitle: Flyme spoof failed: " + t.getMessage());
            }
        }
    }
}
