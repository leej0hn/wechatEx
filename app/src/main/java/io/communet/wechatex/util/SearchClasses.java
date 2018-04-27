package io.communet.wechatex.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.DexClass;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.communet.wechatex.hook.Main;

import static de.robv.android.xposed.XposedBridge.log;

public class SearchClasses {
    private static List<String> wxClasses = new ArrayList<String>();
    private static XSharedPreferences preferencesInstance = null;

    public static void init(Context context, XC_LoadPackage.LoadPackageParam lpparam, String versionName) {

        if (loadConfig(lpparam, versionName)) {
            return;
        }

        log("failed to load config, start finding...");
        HookParams hp = HookParams.getInstance();
        hp.versionName = versionName;
        hp.versionCode = HookParams.VERSION_CODE;
        int versionNum = getVersionNum(versionName);
        if (versionNum >= getVersionNum("6.5.6") && versionNum <= getVersionNum("6.5.23")) {
            hp.LuckyMoneyReceiveUIClassName = "com.tencent.mm.plugin.luckymoney.ui.En_fba4b94f";
        }
        if (versionNum < getVersionNum("6.5.8")) {
            hp.SQLiteDatabaseClassName = "com.tencent.mmdb.database.SQLiteDatabase";
        }
        if (versionNum < getVersionNum("6.5.4")) {
            hp.hasTimingIdentifier = false;
        }

        ApkFile apkFile = null;
        try {
            apkFile = new ApkFile(lpparam.appInfo.sourceDir);
            DexClass[] dexClasses = apkFile.getDexClasses();
            for (int i = 0; i < dexClasses.length; i++) {
                wxClasses.add(ReflectionUtil.getClassName(dexClasses[i]));
            }
        } catch (Error | Exception e) {
        } finally {
            try {
                apkFile.close();
            } catch (Exception e) {
            }
        }
        for (String wxClass : wxClasses) {
            log("Found wxClasses :" + wxClass);
        }


        Class ReceiveUIParamNameClass = ReflectionUtil.findClassesFromPackage(lpparam.classLoader, wxClasses, "com.tencent.mm", 1)
                .filterByMethod(String.class, "getInfo")
                .filterByMethod(int.class, "getType")
                .filterByMethod(void.class, "reset")
                .firstOrNull();
        log("Found ReceiveUIParamNameClass :" + ReceiveUIParamNameClass);

        hp.ReceiveUIParamNameClassName = ReceiveUIParamNameClass.getName();

        Class XMLParserClass = ReflectionUtil.findClassesFromPackage(lpparam.classLoader, wxClasses, "com.tencent.mm.sdk.platformtools", 0)
                .filterByMethod(Map.class, String.class, String.class)
                .firstOrNull();
        hp.XMLParserClassName = XMLParserClass.getName();

        hp.XMLParserMethod = ReflectionUtil.findMethodsByExactParameters(XMLParserClass, Map.class, String.class, String.class)
                .getName();


        ReflectionUtil.Classes storageClasses = ReflectionUtil.findClassesFromPackage(lpparam.classLoader, wxClasses, "com.tencent.mm.storage", 0);
        Class MsgInfoClass = storageClasses
                .filterByMethod(boolean.class, "isSystem")
                .firstOrNull();
        hp.MsgInfoClassName = MsgInfoClass.getName();
        if (versionNum < getVersionNum("6.5.8")) {
            Class MsgInfoStorageClass = storageClasses
                    .filterByMethod(long.class, MsgInfoClass)
                    .firstOrNull();
            hp.MsgInfoStorageClassName = MsgInfoStorageClass.getName();
            hp.MsgInfoStorageInsertMethod = ReflectionUtil.findMethodsByExactParameters(MsgInfoStorageClass, long.class, MsgInfoClass)
                    .getName();
        } else {
            Class MsgInfoStorageClass = storageClasses
                    .filterByMethod(long.class, MsgInfoClass, boolean.class)
                    .firstOrNull();
            hp.MsgInfoStorageClassName = MsgInfoStorageClass.getName();
            hp.MsgInfoStorageInsertMethod = ReflectionUtil.findMethodsByExactParameters(MsgInfoStorageClass, long.class, MsgInfoClass, boolean.class)
                    .getName();
        }


        Class LuckyMoneyReceiveUIClass = XposedHelpers.findClass(hp.LuckyMoneyReceiveUIClassName, lpparam.classLoader);
        hp.ReceiveUIMethod = ReflectionUtil.findMethodsByExactParameters(LuckyMoneyReceiveUIClass,
                boolean.class, int.class, int.class, String.class, ReceiveUIParamNameClass)
                .getName();


        Class RequestCallerClass = ReflectionUtil.findClassesFromPackage(lpparam.classLoader, wxClasses, "com.tencent.mm", 1)
                .filterByField("foreground", "boolean")
                .filterByMethod(void.class, int.class, String.class, int.class, boolean.class)
                .filterByMethod(void.class, "cancel", int.class)
                .filterByMethod(void.class, "reset")
                .firstOrNull();
        hp.RequestCallerClassName = RequestCallerClass.getName();

        hp.RequestCallerMethod = ReflectionUtil.findMethodsByExactParameters(RequestCallerClass,
                void.class, RequestCallerClass, int.class)
                .getName();


        Class NetworkRequestClass = ReflectionUtil.findClassesFromPackage(lpparam.classLoader, wxClasses, "com.tencent.mm", 1)
                .filterByMethod(void.class, "unhold")
                .filterByMethod(RequestCallerClass)
                .firstOrNull();
        hp.NetworkRequestClassName = NetworkRequestClass.getName();

        hp.GetNetworkByModelMethod = ReflectionUtil.findMethodsByExactParameters(NetworkRequestClass,
                RequestCallerClass)
                .getName();

        Class ReceiveLuckyMoneyRequestClass = ReflectionUtil.findClassesFromPackage(lpparam.classLoader, wxClasses, "com.tencent.mm.plugin.luckymoney", 1)
                .filterByField("msgType", "int")
                .filterByMethod(void.class, int.class, String.class, JSONObject.class)
                .firstOrNull();
        hp.ReceiveLuckyMoneyRequestClassName = ReceiveLuckyMoneyRequestClass.getName();

        hp.ReceiveLuckyMoneyRequestMethod = ReflectionUtil.findMethodsByExactParameters(ReceiveLuckyMoneyRequestClass,
                void.class, int.class, String.class, JSONObject.class)
                .getName();


        hp.LuckyMoneyRequestClassName = ReflectionUtil.findClassesFromPackage(lpparam.classLoader, wxClasses, "com.tencent.mm.plugin.luckymoney", 1)
                .filterByField("talker", "java.lang.String")
                .filterByMethod(void.class, int.class, String.class, JSONObject.class)
                .filterByMethod(int.class, "getType")
                .filterByNoMethod(boolean.class)
                .firstOrNull()
                .getName();

        hp.GetTransferRequestClassName = ReflectionUtil.findClassesFromPackage(lpparam.classLoader, wxClasses, "com.tencent.mm.plugin.remittance", 1)
                .filterByField("java.lang.String")
                .filterByNoField("int")

                .filterByMethod(void.class, int.class, String.class, JSONObject.class)
                .filterByMethod(String.class, "getUri")
                .firstOrNull()
                .getName();


        Class SelectConversationUIClass = XposedHelpers.findClass(hp.SelectConversationUIClassName, lpparam.classLoader);
        hp.SelectConversationUICheckLimitMethod = ReflectionUtil.findMethodsByExactParameters(SelectConversationUIClass,
                boolean.class, boolean.class)
                .getName();

        hp.ContactInfoClassName = ReflectionUtil.findClassesFromPackage(lpparam.classLoader, wxClasses, "com.tencent.mm.storage", 0)
                .filterByMethod(String.class, "getCityCode")
                .filterByMethod(String.class, "getCountryCode")
                .firstOrNull()
                .getName();

        saveConfig(context);
    }

    private static int getVersionNum(String version) {
        String[] v = version.split("\\.");
        if (v.length == 3) {
            return Integer.valueOf(v[0]) * 100 * 100 + Integer.valueOf(v[1]) * 100 + Integer.valueOf(v[2]);
        }else {
            return 0;
        }
    }

    private static boolean loadConfig(XC_LoadPackage.LoadPackageParam lpparam, String curVersionName) {
        try {
            SharedPreferences pref = getPreferencesInstance();
            HookParams hp = new Gson().fromJson(pref.getString("params", ""), HookParams.class);

            if (hp == null
                    || !hp.versionName.equals(curVersionName)
                    || hp.versionCode != HookParams.VERSION_CODE) {
                return false;
            }

            HookParams.setInstance(hp);
            log("load config successful");
            return true;
        } catch (Error | Exception e) {
        }
        return false;
    }

    private static void saveConfig(Context context) {
        try {
            Intent saveConfigIntent = new Intent();
            saveConfigIntent.setAction(HookParams.SAVE_WECHAT_ENHANCEMENT_CONFIG);
            saveConfigIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            saveConfigIntent.putExtra("params", new Gson().toJson(HookParams.getInstance()));
            context.sendBroadcast(saveConfigIntent);
            log("saving config...");
        } catch (Error | Exception e) {
        }
    }

    private static XSharedPreferences getPreferencesInstance() {
        if (preferencesInstance == null) {
            preferencesInstance = new XSharedPreferences(Main.class.getPackage().getName(), HookParams.WECHAT_ENHANCEMENT_CONFIG_NAME);
            preferencesInstance.makeWorldReadable();
        } else {
            preferencesInstance.reload();
        }
        return preferencesInstance;
    }
}
