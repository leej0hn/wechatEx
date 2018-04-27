package io.communet.wechatex.plugin;


import android.app.Activity;
import android.widget.Button;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.communet.wechatex.util.HookParams;
import io.communet.wechatex.util.PreferencesUtils;


public class AutoLogin implements IPlugin {
    @Override
    public void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod(Activity.class, "onStart", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (!PreferencesUtils.isAutoLogin()) {
                        return;
                    }
                    if (!(param.thisObject instanceof Activity)) {
                        return;
                    }
                    Activity activity = (Activity) param.thisObject;
                    if (activity.getClass().getName().equals(HookParams.getInstance().WebWXLoginUIClassName)) {
                        Class clazz = activity.getClass();
                        Field field = XposedHelpers.findFirstFieldByExactType(clazz, Button.class);
                        Button button = (Button) field.get(activity);
                        if (button != null) {
                            button.performClick();
                        }
                    }

                } catch (Error | Exception e) {
                }
            }
        });

    }

}
