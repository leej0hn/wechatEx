package io.communet.wechatex.hook;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.communet.wechatex.plugin.ADBlock;
import io.communet.wechatex.plugin.AntiRevoke;
import io.communet.wechatex.plugin.AntiSnsDelete;
import io.communet.wechatex.plugin.AutoLogin;
import io.communet.wechatex.plugin.HideModule;
import io.communet.wechatex.plugin.IPlugin;
import io.communet.wechatex.plugin.Limits;
import io.communet.wechatex.plugin.LuckMoney;
import io.communet.wechatex.util.HookParams;
import io.communet.wechatex.util.SearchClasses;

import static de.robv.android.xposed.XposedBridge.log;
/**
 * <p>function:
 * <p>User: LeeJohn
 * <p>Date: 2018/4/27
 * <p>Version: 1.0
 */

public class Main implements IXposedHookLoadPackage {

    private static IPlugin[] plugins={
            new ADBlock(),
            new AntiRevoke(),
            new AntiSnsDelete(),
            new AutoLogin(),
            new HideModule(),
            new LuckMoney(),
            new Limits(),
    };

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals(HookParams.WECHAT_PACKAGE_NAME)) {
            try {
                Log.i("Xposed","processName : " + lpparam.processName );
                XposedHelpers.findAndHookMethod(ContextWrapper.class, "attachBaseContext", Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        Context context = (Context) param.args[0];
                        String processName = lpparam.processName;
                        //Only hook important process
                        if (!processName.equals(HookParams.WECHAT_PACKAGE_NAME) &&
                                !processName.equals(HookParams.WECHAT_PACKAGE_NAME + ":tools")
                                ) {
                            return;
                        }
                        String versionName = getVersionName(context, HookParams.WECHAT_PACKAGE_NAME);
                        log("Found wechat version:" + versionName);
                        if (!HookParams.hasInstance()) {
                            SearchClasses.init(context, lpparam, versionName);
                            loadPlugins(lpparam);
                        }
                    }
                });
            } catch (Error | Exception e) {
                Log.e("Xposed",e.getMessage() );
            }

        }
    }
    private String getVersionName(Context context, String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packInfo = packageManager.getPackageInfo(packageName, 0);
            return packInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return "";
    }


    private void loadPlugins(XC_LoadPackage.LoadPackageParam lpparam) {
        for (IPlugin plugin:plugins) {
            try {
                plugin.hook(lpparam);
            } catch (Error | Exception e) {
                log("loadPlugins error" + e);
            }
        }

    }
}
