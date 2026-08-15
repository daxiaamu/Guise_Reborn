#include <jni.h>
#include <vulkan/vulkan.h>

#include <algorithm>
#include <array>
#include <cstring>
#include <dlfcn.h>
#include <mutex>
#include <string_view>

using HookFunType = int (*)(void *func, void *replace, void **backup);
using UnhookFunType = int (*)(void *func);
using NativeOnModuleLoaded = void (*)(const char *name, void *handle);

struct NativeAPIEntries {
    uint32_t version;
    HookFunType hook_func;
    UnhookFunType unhook_func;
};

namespace {
constexpr std::string_view kVulkanLibraryName = "libvulkan.so";

HookFunType g_hook_func = nullptr;
std::mutex g_hook_mutex;
std::mutex g_renderer_mutex;
std::array<char, VK_MAX_PHYSICAL_DEVICE_NAME_SIZE> g_renderer{};
bool g_renderer_configured = false;

PFN_vkGetPhysicalDeviceProperties g_get_properties = nullptr;
PFN_vkGetPhysicalDeviceProperties2 g_get_properties2 = nullptr;
PFN_vkGetPhysicalDeviceProperties2KHR g_get_properties2_khr = nullptr;
void *g_get_properties_target = nullptr;
void *g_get_properties2_target = nullptr;
void *g_get_properties2_khr_target = nullptr;

bool HasSuffix(const char *value, std::string_view suffix) {
    if (value == nullptr) return false;
    const std::string_view text(value);
    return text.size() >= suffix.size() &&
           text.compare(text.size() - suffix.size(), suffix.size(), suffix) == 0;
}

void ApplyDeviceName(VkPhysicalDeviceProperties *properties) {
    if (properties == nullptr) return;
    std::lock_guard<std::mutex> lock(g_renderer_mutex);
    if (!g_renderer_configured) return;
    std::memcpy(properties->deviceName, g_renderer.data(), g_renderer.size());
}

VKAPI_ATTR void VKAPI_CALL GetPhysicalDevicePropertiesReplacement(
        VkPhysicalDevice physical_device,
        VkPhysicalDeviceProperties *properties) {
    if (g_get_properties == nullptr) return;
    g_get_properties(physical_device, properties);
    ApplyDeviceName(properties);
}

VKAPI_ATTR void VKAPI_CALL GetPhysicalDeviceProperties2Replacement(
        VkPhysicalDevice physical_device,
        VkPhysicalDeviceProperties2 *properties) {
    if (g_get_properties2 == nullptr) return;
    g_get_properties2(physical_device, properties);
    if (properties != nullptr) ApplyDeviceName(&properties->properties);
}

VKAPI_ATTR void VKAPI_CALL GetPhysicalDeviceProperties2KhrReplacement(
        VkPhysicalDevice physical_device,
        VkPhysicalDeviceProperties2 *properties) {
    if (g_get_properties2_khr == nullptr) return;
    g_get_properties2_khr(physical_device, properties);
    if (properties != nullptr) ApplyDeviceName(&properties->properties);
}

bool TargetAlreadyHooked(void *target) {
    return target == g_get_properties_target ||
           target == g_get_properties2_target ||
           target == g_get_properties2_khr_target;
}

template <typename Function>
void InstallHook(
        void *handle,
        const char *symbol,
        Function replacement,
        Function *backup,
        void **target_storage) {
    if (handle == nullptr || g_hook_func == nullptr || *backup != nullptr) return;
    void *target = dlsym(handle, symbol);
    if (target == nullptr || TargetAlreadyHooked(target)) return;

    void *original = nullptr;
    const int result = g_hook_func(
            target,
            reinterpret_cast<void *>(replacement),
            &original);
    if (result == 0 && original != nullptr) {
        *backup = reinterpret_cast<Function>(original);
        *target_storage = target;
    }
}

void InstallVulkanHooks(void *handle) {
    std::lock_guard<std::mutex> lock(g_hook_mutex);
    InstallHook(
            handle,
            "vkGetPhysicalDeviceProperties",
            &GetPhysicalDevicePropertiesReplacement,
            &g_get_properties,
            &g_get_properties_target);
    InstallHook(
            handle,
            "vkGetPhysicalDeviceProperties2",
            &GetPhysicalDeviceProperties2Replacement,
            &g_get_properties2,
            &g_get_properties2_target);
    InstallHook(
            handle,
            "vkGetPhysicalDeviceProperties2KHR",
            &GetPhysicalDeviceProperties2KhrReplacement,
            &g_get_properties2_khr,
            &g_get_properties2_khr_target);
}

void OnLibraryLoaded(const char *name, void *handle) {
    if (handle != nullptr && HasSuffix(name, kVulkanLibraryName)) {
        InstallVulkanHooks(handle);
    }
}

void HookAlreadyLoadedVulkan() {
#ifdef RTLD_NOLOAD
    void *handle = dlopen(kVulkanLibraryName.data(), RTLD_NOW | RTLD_NOLOAD);
    if (handle != nullptr) {
        InstallVulkanHooks(handle);
        dlclose(handle);
    }
#endif
}
}  // namespace

extern "C" JNIEXPORT void JNICALL
Java_com_houvven_guise_xposed_hook_VulkanPrivacyBridge_nativeConfigureRenderer(
        JNIEnv *env,
        jobject,
        jstring renderer) {
    if (renderer == nullptr) return;
    const char *utf = env->GetStringUTFChars(renderer, nullptr);
    if (utf == nullptr) return;
    const jsize utf_length = env->GetStringUTFLength(renderer);

    {
        std::lock_guard<std::mutex> lock(g_renderer_mutex);
        g_renderer.fill('\0');
        const size_t copy_length = std::min(
                static_cast<size_t>(utf_length),
                g_renderer.size() - 1);
        std::memcpy(g_renderer.data(), utf, copy_length);
        g_renderer_configured = copy_length > 0;
    }

    env->ReleaseStringUTFChars(renderer, utf);
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
    if (entries == nullptr || entries->hook_func == nullptr) return nullptr;
    g_hook_func = entries->hook_func;
    HookAlreadyLoadedVulkan();
    return &OnLibraryLoaded;
}
