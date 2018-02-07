LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := iWediaTunerTvInputService

LOCAL_SRC_FILES := $(call all-java-files-under, java)
LOCAL_SRC_FILES += $(call all-Iaidl-files-under, java)

LOCAL_STATIC_JAVA_LIBRARIES := com.iwedia.comedia.comm

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_DEX_PREOPT := false

LOCAL_MODULE_TAGS := optional

# Required for com.google.android.tv.permission.RECEIVE_INPUT_EVENT
LOCAL_PRIVILEGED_MODULE := true

LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)
