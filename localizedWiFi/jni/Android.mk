LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
APP_STL:= stlport_static
LOCAL_LDLIBS    := -llog
LOCAL_MODULE    := sun
LOCAL_SRC_FILES := ece596_ucsb_localizedwifi_JNI.cpp
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include-all


include $(BUILD_SHARED_LIBRARY)