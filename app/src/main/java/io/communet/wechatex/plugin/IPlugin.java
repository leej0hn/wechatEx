package io.communet.wechatex.plugin;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public interface IPlugin {
    public void hook(XC_LoadPackage.LoadPackageParam lpparam);
}
