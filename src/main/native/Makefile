#!/usr/bin/make -f
#
# Makefile for C code
#

# C sources to compile

LIBNAME := inodeutil
CLASS_NAME := com.netease.util.InodeUtil
CLASS_FILE_PATH := $(subst .,/,$(CLASS_NAME))
FILE_NAME_PREFIX := $(subst .,_,$(CLASS_NAME))

JAVA_TARGET_DIR := ../../../target/classes
TARGET_DIR := ../resources

TARGET=$(TARGET_DIR)/lib/lib$(LIBNAME)$(LIB_EXT)

UNAME := $(shell uname)
ifeq ($(UNAME), Linux)
LIB_EXT := .so
endif
ifeq ($(UNAME), Darwin)
LIB_EXT := .jnilib
endif

WORKING_DIR := .
JAVA_BUILD_DIR := $(TARGET_DIR)

ifeq ($(UNAME), Linux)
JNI_INCLUDE := /usr/lib/jvm/java-6-openjdk/include
endif
ifeq ($(UNAME), Darwin)
JNI_INCLUDE := /System/Library/Frameworks/JavaVM.framework/Versions/Current/Headers
endif
INCLUDES := -I $(JNI_INCLUDE) -I $(WORKING_DIR)

ifeq ($(UNAME), Linux)
	CCFLAGS := -O2 -shared
endif
ifeq ($(UNAME), Darwin)
	CCFLAGS := -O2 -dynamiclib
endif
INCLUDES := -I $(JNI_INCLUDE) -I $(WORKING_DIR)

# classpath for javah
ifdef CLASSPATH
JAVAH_CLASSPATH = $(JAVA_BUILD_DIR):$(CLASSPATH)
else
JAVAH_CLASSPATH = $(JAVA_BUILD_DIR)
endif

all: $(TARGET)

$(TARGET): $(FILE_NAME_PREFIX).h
	@mkdir -p `dirname $@`
	gcc $(CCFLAGS) $(INCLUDES) $(FILE_NAME_PREFIX).cpp -o $(TARGET)

com_netease_util_InodeUtil.h: $(JAVA_TARGET_DIR)/$(CLASS_FILE_PATH).class
	javah -force -classpath $(JAVAH_CLASSPATH) $(CLASS_NAME)
