-dontwarn com.houvven.**

# Loaded by LSPosed from META-INF/xposed/java_init.list. Keep only the binary entry name,
# constructor and framework callbacks; private implementation details remain eligible for R8.
-keep,allowoptimization class com.houvven.guise.xposed.HookInit {
    public <init>();
    public void onModuleLoaded(io.github.libxposed.api.XposedModuleInterface$ModuleLoadedParam);
    public boolean onHotReloading(io.github.libxposed.api.XposedModuleInterface$HotReloadingParam);
    public void onPackageReady(io.github.libxposed.api.XposedModuleInterface$PackageReadyParam);
}

# JNI symbol lookup depends on this binary class and method name.
-keep,allowoptimization class com.houvven.guise.xposed.hook.VulkanPrivacyBridge {
    private native void nativeConfigureRenderer(java.lang.String);
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
   static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
   static **$* *;
}
-keepclassmembers class <2>$<3> {
   kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
   public static ** INSTANCE;
}
-keepclassmembers class <1> {
   public static <1> INSTANCE;
   kotlinx.serialization.KSerializer serializer(...);
}

-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
