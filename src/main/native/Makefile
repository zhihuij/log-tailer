#!/usr/bin/make -f
#
# Makefile for C code
#

# C sources to compile

LIBNAME := inodeutil
CLASS_NAME := com.netease.util.InodeUtil
CLASS_FILE_PATH := $(subst .,/,$(CLASS_NAME))
FILE_NAME_PREFIX := $(subst .,_,$(CLASS_NAME))

TARGET_DIR := ../resources
TARGET=$(TARGET_DIR)/lib/lib$(LIBNAME)$(LIB_EXT)
JAVA_TARGET_DIR := ../../../target/classes

UNAME := $(shell uname)
ifeq ($(UNAME), Linux)
LIB_EXT := .so
JNI_INCLUDE := /usr/lib/jvm/java-6-openjdk-amd64/include
CCFLAGS := -O2 -shared -Wall -fPIC
endif
ifeq ($(UNAME), Darwin)
LIB_EXT := .jnilib
JNI_INCLUDE := /System/Library/Frameworks/JavaVM.framework/Versions/Current/Headers
CCFLAGS := -O2 -dynamiclib
endif

WORKING_DIR := .
INCLUDES := -I $(JNI_INCLUDE) -I $(WORKING_DIR)

# classpath for javah
ifdef CLASSPATH
JAVAH_CLASSPATH = $(JAVA_TARGET_DIR):$(CLASSPATH)
else
JAVAH_CLASSPATH = $(JAVA_TARGET_DIR)
endif

all: $(TARGET)

$(TARGET): $(FILE_NAME_PREFIX).h
	@mkdir -p `dirname $@`
	gcc $(CCFLAGS) $(INCLUDES) $(FILE_NAME_PREFIX).cpp -o $(TARGET)

$(FILE_NAME_PREFIX).h: $(JAVA_TARGET_DIR)/$(CLASS_FILE_PATH).class
	javah -force -classpath $(JAVAH_CLASSPATH) $(CLASS_NAME)
