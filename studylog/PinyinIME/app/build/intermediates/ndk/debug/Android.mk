LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := libjni_pinyinime
LOCAL_LDFLAGS := -Wl,--build-id
LOCAL_SRC_FILES := \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/Android.mk \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/command/Makefile \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/command/pinyinime_dictbuilder.cpp \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/share/spellingtrie.cpp \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/share/lpicache.cpp \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/share/mystdlib.cpp \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/share/dicttrie.cpp \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/share/searchutility.cpp \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/share/ngram.cpp \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/share/splparser.cpp \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/share/matrixsearch.cpp \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/share/dictbuilder.cpp \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/share/userdict.cpp \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/share/utf16char.cpp \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/share/spellingtable.cpp \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/share/dictlist.cpp \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/share/pinyinime.cpp \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/share/sync.cpp \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/share/utf16reader.cpp \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/android/com_android_inputmethod_pinyin_PinyinDecoderService.cpp \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/data/rawdict_utf16_65105_freq.txt \
	/home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni/data/valid_utf16.txt \

LOCAL_C_INCLUDES += /home/caipengli/AndroidStudioProjects/PinyinIME/app/src/main/jni
LOCAL_C_INCLUDES += /home/caipengli/AndroidStudioProjects/PinyinIME/app/src/debug/jni

include $(BUILD_SHARED_LIBRARY)
